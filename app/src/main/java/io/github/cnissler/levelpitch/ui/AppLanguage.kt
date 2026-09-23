package io.github.cnissler.levelpitch.ui

/** Languages the app is translated into; [SYSTEM] follows the phone's language. */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    GERMAN("de");

    companion object {
        /** The choice for the app's current locale list tags ("" = none set, i.e. system). */
        fun fromTags(tags: String): AppLanguage {
            val first = tags.split(',').first().trim().substringBefore('-').lowercase()
            return entries.find { it.tag == first && it != SYSTEM } ?: SYSTEM
        }
    }
}
