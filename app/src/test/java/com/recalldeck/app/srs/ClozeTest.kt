package com.recalldeck.app.srs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClozeTest {

    private val text = "The {{c1::mitochondria}} is the {{c2::powerhouse}} of the {{c1::cell}}."

    @Test
    fun `indices returns distinct sorted indices`() {
        assertEquals(listOf(1, 2), Cloze.indices(text))
        assertEquals(emptyList<Int>(), Cloze.indices("no cloze here"))
    }

    @Test
    fun `isValid detects cloze markers`() {
        assertTrue(Cloze.isValid(text))
        assertFalse(Cloze.isValid("plain text"))
        assertFalse(Cloze.isValid("{{c::missing index}}"))
    }

    @Test
    fun `renderQuestion masks target index and reveals others`() {
        assertEquals(
            "The [...] is the powerhouse of the [...].",
            Cloze.renderQuestion(text, 1),
        )
        assertEquals(
            "The mitochondria is the [...] of the cell.",
            Cloze.renderQuestion(text, 2),
        )
    }

    @Test
    fun `renderAnswer reveals all clozes`() {
        assertEquals(
            "The mitochondria is the powerhouse of the cell.",
            Cloze.renderAnswer(text),
        )
    }

    @Test
    fun `answerFor joins hidden texts of one index`() {
        assertEquals("mitochondria, cell", Cloze.answerFor(text, 1))
        assertEquals("powerhouse", Cloze.answerFor(text, 2))
        assertEquals("", Cloze.answerFor(text, 3))
    }
}
