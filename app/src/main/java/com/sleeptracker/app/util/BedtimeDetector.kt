package com.sleeptracker.app.util

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.util.concurrent.TimeUnit

/**
 * Estimates last night's sleep period by asking Android for real, on-device signals about phone
 * usage - never a made-up or hard-coded time.
 *
 * There's no public Android API that reports "the user was asleep," so rather than trusting a
 * single screen-off event (which could just as easily be a two-minute break as an actual night's
 * sleep), this looks at every screen-off/screen-on pair in the lookback window and picks the
 * single *longest* continuous stretch the screen stayed off - the one most likely to actually be
 * sleep, as opposed to the phone simply being set down for a while. Since API 28,
 * [UsageStatsManager]'s event log records [UsageEvents.Event.SCREEN_NON_INTERACTIVE] /
 * [UsageEvents.Event.SCREEN_INTERACTIVE] every time the display turns off/on, system-wide - the
 * closest thing to ground truth any app can query after the fact. That's gated behind the
 * special Usage Access app-op (`PACKAGE_USAGE_STATS`), which the user grants from Settings, not
 * through a runtime permission dialog.
 */
object BedtimeDetector {

    /** How far back to search for the longest screen-off stretch. Wide enough to comfortably
     *  contain last night's sleep regardless of what time this is tapped today, tight enough that
     *  it won't reach back into the *previous* night as well. */
    private val LOOKBACK = TimeUnit.HOURS.toMillis(30)

    /** Window and minimum duration used by [findRecentScreenOffPeriods] - a wider, unfiltered
     *  view alongside the single best guess above, so the user can pick manually if the "longest"
     *  pick wasn't actually last night's sleep. */
    private val RECENT_PERIODS_LOOKBACK = TimeUnit.HOURS.toMillis(24)
    private val RECENT_PERIODS_MIN_DURATION = TimeUnit.HOURS.toMillis(1)

    sealed class Result {
        /** The longest screen-off stretch found in the window. [source] describes how reliable
         *  the underlying signal is. */
        data class Detected(val startEpochMillis: Long, val endEpochMillis: Long, val source: Source) : Result()

        /** Usage Access hasn't been granted - nothing was queried yet. */
        data object PermissionRequired : Result()

        /** Usage Access is granted, but no usable signal was found in the search window (e.g. a
         *  freshly-set-up device, or an OEM usage-stats implementation that withholds data). */
        data object Unavailable : Result()
    }

    enum class Source {
        /** Built from direct system records of the display turning off and back on - the most
         *  trustworthy signal available to any app. */
        SCREEN_EVENTS,

        /** No usable screen on/off event pairs were found, so this falls back to the longest gap
         *  between any recorded app activity - real data, just a coarser proxy for "the phone
         *  wasn't touched" than explicit screen events. */
        APP_INACTIVITY_GAP
    }

    /** A single screen-off stretch, as returned by [findRecentScreenOffPeriods]. */
    data class ScreenOffPeriod(val startEpochMillis: Long, val endEpochMillis: Long)

    /** Whether the special Usage Access app-op is currently granted to this app. */
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Opens the system's Usage Access settings page, scrolled to this app where possible, so
     *  the user can grant it. This is the only way a third-party app can obtain this permission -
     *  there is no in-app runtime-permission dialog for it. */
    fun openUsageAccessSettings(context: Context) {
        val specific = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        runCatching { context.startActivity(specific) }.onFailure {
            // Some OEM Settings apps don't accept a package-scoped deep link to this particular
            // screen; falling back to the unscoped version still gets the user to the right
            // page, just without it being pre-scrolled to this app.
            runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        }
    }

    /**
     * Every screen-off stretch of at least [RECENT_PERIODS_MIN_DURATION] found within the last
     * [RECENT_PERIODS_LOOKBACK], newest first. Unlike [detectLongestScreenOffPeriod], this
     * doesn't try to pick "the" sleep period - it surfaces every long off-stretch (naps, an
     * evening away from the phone, last night's actual sleep, etc.) so the user can pick the
     * right one manually when the single best-guess detection picks the wrong stretch. Requires
     * Usage Access (see [hasUsageAccess]) and, like the rest of this object, only reads real
     * on-device signals - an empty list means none were found, never a guess.
     */
    fun findRecentScreenOffPeriods(context: Context): List<ScreenOffPeriod> {
        if (!hasUsageAccess(context)) return emptyList()
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

        val now = System.currentTimeMillis()
        val windowStart = now - RECENT_PERIODS_LOOKBACK

        val events = runCatching { usageStatsManager.queryEvents(windowStart, now) }.getOrNull()
            ?: return emptyList()
        val event = UsageEvents.Event()

        val periods = mutableListOf<ScreenOffPeriod>()
        var screenOffAt: Long? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> screenOffAt = event.timeStamp
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    val offAt = screenOffAt
                    if (offAt != null) {
                        if (event.timeStamp - offAt >= RECENT_PERIODS_MIN_DURATION) {
                            periods += ScreenOffPeriod(offAt, event.timeStamp)
                        }
                        screenOffAt = null
                    }
                }
            }
        }
        // Screen is still off right now - include that trailing, still-ongoing stretch too.
        screenOffAt?.let { offAt ->
            if (now - offAt >= RECENT_PERIODS_MIN_DURATION) {
                periods += ScreenOffPeriod(offAt, now)
            }
        }

        return periods.sortedByDescending { it.startEpochMillis }
    }


    /**
     * Finds the single longest continuous screen-off stretch within [LOOKBACK] of now. Never
     * invents a value: returns [Result.PermissionRequired] or [Result.Unavailable] rather than
     * guessing when a real signal can't be found.
     */
    fun detectLongestScreenOffPeriod(context: Context): Result {
        if (!hasUsageAccess(context)) return Result.PermissionRequired

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return Result.Unavailable

        val now = System.currentTimeMillis()
        val windowStart = now - LOOKBACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val longest = longestScreenOffSpan(usageStatsManager, windowStart, now)
            if (longest != null) {
                return Result.Detected(longest.first, longest.second, Source.SCREEN_EVENTS)
            }
        }

        // Fall back to the longest gap between any recorded app activity - available since API
        // 21, so this also covers API 26-27 devices where the explicit screen-event stream
        // above doesn't exist at all.
        val longestGap = longestInactivityGap(usageStatsManager, windowStart, now)
        return if (longestGap != null) {
            Result.Detected(longestGap.first, longestGap.second, Source.APP_INACTIVITY_GAP)
        } else {
            Result.Unavailable
        }
    }

    /** Walks the [UsageEvents.Event] log chronologically, pairing each SCREEN_NON_INTERACTIVE
     *  with the next SCREEN_INTERACTIVE after it, and keeps whichever pair spans the longest
     *  duration. If the screen is still off at [end] (the window closes mid-sleep), that stretch
     *  is measured up to [end] so a currently-ongoing sleep period can still be picked up. */
    private fun longestScreenOffSpan(usageStatsManager: UsageStatsManager, start: Long, end: Long): Pair<Long, Long>? {
        val events = runCatching { usageStatsManager.queryEvents(start, end) }.getOrNull() ?: return null
        val event = UsageEvents.Event()

        var screenOffAt: Long? = null
        var bestStart: Long? = null
        var bestEnd: Long? = null

        fun considerSpan(spanStart: Long, spanEnd: Long) {
            if (bestStart == null || (spanEnd - spanStart) > (bestEnd!! - bestStart!!)) {
                bestStart = spanStart
                bestEnd = spanEnd
            }
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> screenOffAt = event.timeStamp
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    val offAt = screenOffAt
                    if (offAt != null) {
                        considerSpan(offAt, event.timeStamp)
                        screenOffAt = null
                    }
                }
            }
        }
        // Screen was still off when the window closed - count that trailing stretch too, since
        // it's very plausibly "still asleep right now."
        screenOffAt?.let { considerSpan(it, end) }

        val finalStart = bestStart ?: return null
        val finalEnd = bestEnd ?: return null
        return finalStart to finalEnd
    }

    /** Fallback for pre-API 28 devices: the longest stretch between consecutive app-foreground
     *  events, as a coarser proxy for "the phone wasn't touched for a while." */
    private fun longestInactivityGap(usageStatsManager: UsageStatsManager, start: Long, end: Long): Pair<Long, Long>? {
        val events = runCatching { usageStatsManager.queryEvents(start, end) }.getOrNull() ?: return null
        val event = UsageEvents.Event()

        val foregroundTimestamps = mutableListOf<Long>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundTimestamps.add(event.timeStamp)
            }
        }
        if (foregroundTimestamps.isEmpty()) return null
        foregroundTimestamps.sort()

        var bestStart = start
        var bestEnd = foregroundTimestamps.first()
        var bestDuration = bestEnd - bestStart

        for (i in 0 until foregroundTimestamps.size - 1) {
            val gapStart = foregroundTimestamps[i]
            val gapEnd = foregroundTimestamps[i + 1]
            if (gapEnd - gapStart > bestDuration) {
                bestStart = gapStart
                bestEnd = gapEnd
                bestDuration = gapEnd - gapStart
            }
        }
        val trailingGap = end - foregroundTimestamps.last()
        if (trailingGap > bestDuration) {
            bestStart = foregroundTimestamps.last()
            bestEnd = end
            bestDuration = trailingGap
        }

        return if (bestDuration > 0) bestStart to bestEnd else null
    }
}
