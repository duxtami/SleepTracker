package com.sleeptracker.app.data.model

/** How the user felt about a sleep session. */
enum class Mood(val emoji: String, val label: String) {
    GREAT("😄", "Great"),
    GOOD("🙂", "Good"),
    OKAY("😐", "Okay"),
    BAD("😕", "Bad"),
    AWFUL("😣", "Awful");

    companion object {
        fun fromNameOrNull(name: String?): Mood? = entries.firstOrNull { it.name == name }
    }
}

/** App-wide theme mode selectable from Settings. */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK, AMOLED
}

/** Accent color source. */
enum class ColorStyle {
    DYNAMIC, LAVENDER, TEAL, SUNSET, FOREST, ROSE, OCEAN, AMBER
}
