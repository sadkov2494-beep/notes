package com.notes.vault.util

import com.notes.vault.data.model.ChecklistItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownHelperTest {

    @Test
    fun wrapBold_wrapsText() {
        assertEquals("**hello**", MarkdownHelper.wrapBold("hello"))
    }

    @Test
    fun prefixHeading_addsHashPrefix() {
        assertEquals("# Title", MarkdownHelper.prefixHeading("Title", 1))
        assertEquals("## Title", MarkdownHelper.prefixHeading("Title", 2))
    }

    @Test
    fun prefixBullet_addsDash() {
        assertEquals("- item", MarkdownHelper.prefixBullet("item"))
    }

    @Test
    fun parseChecklist_roundTrip() {
        val items = listOf(
            ChecklistItem("1", "Buy milk", false),
            ChecklistItem("2", "Call mom", true)
        )
        val json = """[{"id":"1","text":"Buy milk","checked":false},{"id":"2","text":"Call mom","checked":true}]"""
        val parsed = MarkdownHelper.parseChecklist(json)
        assertEquals(2, parsed.size)
        assertEquals("Buy milk", parsed[0].text)
        assertTrue(parsed[1].checked)
    }
}
