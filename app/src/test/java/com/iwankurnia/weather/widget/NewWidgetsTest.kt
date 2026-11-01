package com.iwankurnia.weather.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewWidgetsTest {

    @Test
    fun stripLines_splitsSummaryIntoHeadlineAndDetail() {
        assertEquals(
            "Rain likely from around 16:00" to "Very hot until then",
            stripLines("Rain likely from around 16:00. Very hot until then."),
        )
        assertEquals("Dry and warm for the next few hours" to null, stripLines("Dry and warm for the next few hours."))
        assertEquals("Rain likely now" to null, stripLines("Rain likely now."))
    }

    @Test
    fun strip_dropsSummaryWhenNarrow() {
        assertTrue(stripShowsSummary(widthDp = 390f)) // 4×1
        assertTrue(stripShowsSummary(widthDp = 260f))
        assertFalse(stripShowsSummary(widthDp = 190f)) // 2×1
    }

    @Test
    fun sceneBitmap_matchesWidgetButStaysSmall() {
        assertEquals(735 to 315, sceneBitmapSize(widthDp = 280f, heightDp = 120f, density = 2.625f))
        // A big widget on a dense screen is capped at 960 px wide, keeping its shape.
        val (w, h) = sceneBitmapSize(widthDp = 400f, heightDp = 240f, density = 3.5f)
        assertEquals(960, w)
        assertEquals(576, h)
        assertNull(sceneBitmapSizeOrNull(widthDp = 0f, heightDp = 120f, density = 2.625f))
    }

    @Test
    fun sceneRain_stormHasMoreDropsAndSlant_allInsideTheScene() {
        val rain = sceneRainDrops(seed = 7, width = 310f, height = 150f, storm = false)
        val storm = sceneRainDrops(seed = 7, width = 310f, height = 150f, storm = true)
        assertTrue(storm.size > rain.size)
        val slant = { d: SceneDrop -> (d.x - d.endX) / (d.y - d.endY) }
        assertTrue(slant(storm.first()) > slant(rain.first()))
        (rain + storm).forEach { assertTrue(it.y in 0f..150f && it.endY < it.y) }
        assertEquals(rain, sceneRainDrops(seed = 7, width = 310f, height = 150f, storm = false)) // same seed, same frame
    }
}
