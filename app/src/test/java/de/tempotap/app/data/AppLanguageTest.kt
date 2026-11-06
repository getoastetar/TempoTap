package de.tempotap.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `language tags map to supported languages`() {
        assertEquals(AppLanguage.GERMAN, AppLanguage.fromTag("de"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
    }

    @Test
    fun `unknown or missing tags have no language`() {
        assertNull(AppLanguage.fromTag(null))
        assertNull(AppLanguage.fromTag("fr"))
    }
}
