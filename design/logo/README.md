# SleepTracker logo

The single source of truth for every icon/artwork in this app is the official brand mark
supplied by the app owner: a circular badge (dark navy `#232935` fill, `#E5EAF0` sleeping
"Android head + zzZ" glyph). There is no vector/SVG source file - every asset below is
derived directly from that one image. The previous generated `sleeptracker_logo.svg` has
been deleted; it wasn't in use anywhere and no longer applies now that a new official mark
exists.

Three derivatives of that one badge exist, for three different platform reasons:

| File | Used for | Why it's shaped this way |
|---|---|---|
| `app/src/main/res/drawable-nodpi/ic_launcher_foreground.png` (1024x1024 canvas, glyph only - no baked-in circle - scaled/positioned via the same transform the full badge would use, ~56% width, transparent padding) | Adaptive icon foreground **and** monochrome layer (`mipmap-anydpi-v26/ic_launcher.xml`/`ic_launcher_round.xml`) | Android's adaptive icon spec names two circles on the 108dp canvas: a 66dp "safe zone" guaranteed never clipped by any possible mask, and a 72dp "keyline" circle that Android's own Asset Studio template treats as the real content boundary and that mainstream launchers (Pixel, most OEMs) actually render up to. This asset is sized against the 72dp keyline - its farthest pixel sits ~12px inside that boundary (in a 1024px working canvas, about 1% of the canvas) - matching how real-world adaptive icons are normally sized, rather than the stricter 66dp zone, which left it looking smaller/more padded than a typical app icon. Positioned using the same scale and origin the full circular badge would use, so the glyph sits exactly where it does in the source artwork (not re-centered on its own off-center bounding box), just larger. The badge's navy circle is deliberately *not* drawn here - it's the separate flat-color `ic_launcher_background.xml` layer instead. An earlier version baked a second, independently-scaled navy circle into this file, which produced a visible seam/ring where its raster edge didn't land exactly on the background layer's edge; keeping this file glyph-only removes that second edge entirely, leaving only one navy boundary - the mask's own. |
| `app/src/main/res/drawable-nodpi/ic_splash_logo.png` (1024x1024, the **complete** badge - navy circle + glyph, exactly as supplied - scaled to ~70% width) | Splash screen only, via `drawable/ic_splash_icon.xml` | Unlike the launcher icon, this one keeps the badge's own navy backdrop rather than isolating the glyph. The splash's `windowSplashScreenBackground` is a near-white/near-black neutral, and the glyph's light color alone has very low contrast against a near-white background - the badge's built-in navy circle is what keeps the mark legible either way, at a size that reads as prominently as the original design intent. `ic_splash_icon.xml` wraps it in its own `<adaptive-icon>` (transparent background layer) purely so the platform SplashScreen API takes the same non-cropping sizing path used for real adaptive icons, rather than plain-drawable "legacy icon" auto-shrink. |
| `app/src/main/res/drawable-nodpi/sleep_artwork.png` (1024x1024, glyph only, transparent background, ~86% width) | About screen (`SettingsScreen.kt`) only | Displayed inside a 56dp rounded box that already paints the `#232935` navy background and clips to a rounded-square shape, so this asset only needs the glyph itself (no baked-in circle) at a bold, near-full-bleed scale. |

`ic_launcher_background.xml` (flat `#232935`, matching the badge's own navy exactly) is
unchanged - just a plain rectangle, no shading, still correct as-is for the new mark. Since
the foreground has no circle of its own, this flat fill *is* the icon's entire visible
background shape once the launcher masks it - drawn edge-to-edge with zero resize artifacts,
so there's nothing for a border/seam to form against.

`ic_launcher_monochrome` (Android 13+ themed icons) reuses `ic_launcher_foreground.png`
directly rather than a separate file: the system only reads this layer's alpha channel and
paints it with the user's chosen tint color, so the underlying source pixels don't matter -
only the silhouette shape does, which this glyph-only file already has.

## Notification icon

`ic_notification.xml` is a separate, simplified abstract mark (three ascending "Z" strokes
only - no mascot, no fill, no background shape) - not derived from the badge photo, kept as
its own small hand-drawn vector so it stays legible as a pure alpha silhouette at 24dp. This
wasn't changed as part of the new brand mark rollout.

## Splash screen

Exactly one splash screen, implemented through the platform SplashScreen API - there is no
second, in-app Compose splash, no custom oversized ImageView, and no manual scaling code.
`MainActivity` attaches a custom exit animation via `splashScreen.setOnExitAnimationListener`:
a 400ms fade + 6% scale-up as the single splash view is removed, revealing the
already-composed Home screen underneath immediately.

## Status bar transparency

Two separate, independent things both had to be fixed - one wasn't enough on its own:

1. `enableEdgeToEdge()` was being called with no arguments, which defaults to
   `SystemBarStyle.auto(...)` - a style that paints its own semi-opaque scrim behind the
   status/navigation bars whenever the library isn't sure content will have enough contrast.
   `MainActivity` now passes explicit fully-transparent `SystemBarStyle.auto(Color.TRANSPARENT,
   Color.TRANSPARENT)` for both bars.
2. Separately, `Window.isStatusBarContrastEnforced` / `isNavigationBarContrastEnforced`
   (API 29+) is the platform's *own* protective contrast scrim, independent of whatever color
   is set on `statusBarColor`/`navigationBarColor`. Both are now also declared directly as
   static theme attributes (`android:enforceStatusBarContrast`/`android:enforceNavigationBarContrast`
   in `Theme.SleepTracker`) so the window starts correctly on the very first frame - including
   right after `installSplashScreen()`'s automatic `postSplashScreenTheme` swap on a cold
   launch - not only once runtime code has had a chance to run.

Confirmed nothing else in the codebase touches `statusBarColor`, `navigationBarColor`, or
these contrast flags outside `MainActivity.kt`, `Theme.kt`, and `Theme.SleepTracker` itself,
so there's no competing/conflicting logic on any individual screen.
