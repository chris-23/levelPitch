package io.github.cnissler.levelpitch.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun noAppLocaleMeansSystem() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTags(""))
    }

    @Test
    fun matchesByLanguageIgnoringRegion() {
        assertEquals(AppLanguage.GERMAN, AppLanguage.fromTags("de-AT"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTags("en-GB,de"))
    }

    @Test
    fun untranslatedLanguagesFallBackToSystem() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTags("fr-FR"))
    }
}
