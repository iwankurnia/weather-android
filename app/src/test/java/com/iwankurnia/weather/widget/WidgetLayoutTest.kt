package com.iwankurnia.weather.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetLayoutTest {

    @Test
    fun largeLayout_followsSize() {
        assertEquals(LargeLayout.ROW, largeLayoutFor(widthDp = 250f, heightDp = 70f)) // 4×1
        assertEquals(LargeLayout.ROW, largeLayoutFor(widthDp = 350f, heightDp = 90f)) // 5×1
        // Pixel launcher on a 411 dp wide phone: one row is ~103 dp, too short for the two-row card.
        assertEquals(LargeLayout.ROW, largeLayoutFor(widthDp = 390f, heightDp = 103f))
        assertEquals(LargeLayout.NARROW, largeLayoutFor(widthDp = 180f, heightDp = 140f)) // 3×2
        assertEquals(LargeLayout.STANDARD, largeLayoutFor(widthDp = 270f, heightDp = 130f)) // 4×2
        assertEquals(LargeLayout.STANDARD, largeLayoutFor(widthDp = 350f, heightDp = 150f)) // 5×2
        assertEquals(LargeLayout.ROOMY, largeLayoutFor(widthDp = 280f, heightDp = 190f)) // 4×3
        assertEquals(LargeLayout.NARROW, largeLayoutFor(widthDp = 200f, heightDp = 250f)) // 3×4: still narrow
    }

    @Test
    fun smallLayout_followsSize() {
        assertEquals(SmallLayout.MINI, smallLayoutFor(widthDp = 60f, heightDp = 60f)) // 1×1
        assertEquals(SmallLayout.MINI, smallLayoutFor(widthDp = 60f, heightDp = 130f)) // 1×2
        assertEquals(SmallLayout.ROW, smallLayoutFor(widthDp = 130f, heightDp = 60f)) // 2×1
        assertEquals(SmallLayout.MINI, smallLayoutFor(widthDp = 100f, heightDp = 103f)) // 1×1, Pixel launcher
        assertEquals(SmallLayout.ROW, smallLayoutFor(widthDp = 190f, heightDp = 103f)) // 2×1, Pixel launcher
        assertEquals(SmallLayout.ROW, smallLayoutFor(widthDp = 250f, heightDp = 70f)) // 4×1
        assertEquals(SmallLayout.STANDARD, smallLayoutFor(widthDp = 130f, heightDp = 130f)) // 2×2
        assertEquals(SmallLayout.STANDARD, smallLayoutFor(widthDp = 200f, heightDp = 130f)) // 3×2
        assertEquals(SmallLayout.ROOMY, smallLayoutFor(widthDp = 200f, heightDp = 200f)) // 3×3
    }
}
