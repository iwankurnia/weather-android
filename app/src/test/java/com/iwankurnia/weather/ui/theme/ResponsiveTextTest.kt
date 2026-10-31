package com.iwankurnia.weather.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ResponsiveTextTest {

    @Test
    fun textScale_followsWindowWidth() {
        assertEquals(1.0f, textScaleFor(411), 0.001f) // the phone the mockups were drawn for
        assertEquals(0.876f, textScaleFor(360), 0.001f) // smaller phone
        assertEquals(0.85f, textScaleFor(300), 0.001f) // narrow split screen: never below 85%
        assertEquals(1.25f, textScaleFor(840), 0.001f) // tablet or landscape: never above 125%
    }
}
