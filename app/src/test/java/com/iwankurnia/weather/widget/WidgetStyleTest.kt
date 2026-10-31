package com.iwankurnia.weather.widget

import com.iwankurnia.weather.model.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStyleTest {

    @Test
    fun widgetStyle_thresholds() {
        assertFalse(widgetTextStyle(41).whiteWithShadow)
        assertTrue(widgetTextStyle(40).whiteWithShadow)
        assertTrue(widgetTextStyle(0).whiteWithShadow)
        assertFalse(widgetTextStyle(100).whiteWithShadow)
    }

    @Test
    fun widgetStyle_backgroundAlphaFollowsOpacity() {
        assertEquals(1f, widgetTextStyle(100).backgroundAlpha, 0f)
        assertEquals(0.6f, widgetTextStyle(60).backgroundAlpha, 0.0001f)
        assertEquals(0f, widgetTextStyle(0).backgroundAlpha, 0f)
    }

    @Test
    fun widgetStyle_clampsOutOfRange() {
        assertEquals(1f, widgetTextStyle(150).backgroundAlpha, 0f)
        assertEquals(0f, widgetTextStyle(-5).backgroundAlpha, 0f)
        assertTrue(widgetTextStyle(-5).whiteWithShadow)
    }

    @Test
    fun formatDegrees_roundsAndAvoidsMinusZero() {
        assertEquals("33°", formatDegrees(33.4))
        assertEquals("34°", formatDegrees(33.5))
        assertEquals("0°", formatDegrees(-0.4))
    }

    @Test
    fun areaLine_keepsTheAreaWhenLocationMayBeStale() {
        val snapshot = SampleData.dayPartlyCloudy
        val stale = WidgetState(snapshot, locationMayBeStale = true, canLocate = true)
        assertEquals(snapshot.place.areaName, stale.areaLine())
        assertEquals(LOCATION_STALE_TEXT, stale.staleHint())
    }

    @Test
    fun staleHint_hiddenWithBackgroundLocation() {
        val current = WidgetState(SampleData.dayPartlyCloudy, locationMayBeStale = false, canLocate = true)
        assertEquals(SampleData.dayPartlyCloudy.place.areaName, current.areaLine())
        assertNull(current.staleHint())
    }

    @Test
    fun placeholder_saysUpdatingOnceLocationIsAllowed() {
        assertEquals(UPDATING_TEXT, WidgetState(null, locationMayBeStale = true, canLocate = true).placeholderText())
        assertEquals(SET_UP_TEXT, WidgetState(null, locationMayBeStale = true, canLocate = false).placeholderText())
    }
}
