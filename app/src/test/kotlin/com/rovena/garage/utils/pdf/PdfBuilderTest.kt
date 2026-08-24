package com.rovena.garage.utils.pdf

import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.NumberFormat
import java.util.Locale

/**
 * PDF reports always render numbers in Western digits regardless of the active
 * locale (see [withWesternNumerals]'s doc) - a formal report may leave the app
 * entirely, and Western digits are the standard convention for Arabic-language
 * business documents even though the surrounding text stays fully localized.
 * There's no device/emulator in this sandbox or CI to visually confirm PDF
 * text shaping or page mirroring on-screen (see README's Known Limitations),
 * but this specific, previously-unverified numeral behavior doesn't need one -
 * it's just Locale/NumberFormat plumbing, directly assertable here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class PdfBuilderTest {

    private fun contextForLocale(locale: Locale): android.content.Context {
        val base = ApplicationProvider.getApplicationContext<android.content.Context>()
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }

    @Test
    fun `withWesternNumerals renders Western digits for an Arabic locale context`() {
        val arabicContext = contextForLocale(Locale.forLanguageTag("ar"))
        val arabicFormatted = NumberFormat.getIntegerInstance(arabicContext.resources.configuration.locales[0]).format(1234)
        // Sanity check the test setup itself: a plain Arabic-locale context must actually
        // produce Eastern Arabic-Indic digits, or this test would trivially pass for the
        // wrong reason (nothing to convert away from).
        assertNotEquals("1,234", arabicFormatted)

        val westernizedContext = arabicContext.withWesternNumerals()
        val westernFormatted = NumberFormat.getIntegerInstance(westernizedContext.resources.configuration.locales[0]).format(1234)

        assertEquals("1,234", westernFormatted)
    }

    @Test
    fun `withWesternNumerals is a no-op for a locale that already uses Western digits`() {
        val englishContext = contextForLocale(Locale.US)
        val result = englishContext.withWesternNumerals()

        assertEquals("1,234", NumberFormat.getIntegerInstance(result.resources.configuration.locales[0]).format(1234))
    }
}
