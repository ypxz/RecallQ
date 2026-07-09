package com.recalldeck.app.srs

import org.junit.Assert.assertEquals
import org.junit.Test

class TypeAnswerTest {

    @Test
    fun `normalize lowercases strips punctuation and collapses whitespace`() {
        assertEquals("hello world", TypeAnswer.normalize("  Hello,   World! "))
        assertEquals("adp atp", TypeAnswer.normalize("ADP/ATP"))
    }

    @Test
    fun `exact match is correct`() {
        assertEquals(TypeAnswer.Verdict.CORRECT, TypeAnswer.judge("Mitochondria", "mitochondria"))
        assertEquals(TypeAnswer.Verdict.CORRECT, TypeAnswer.judge("the cell.", "The Cell"))
    }

    @Test
    fun `small typo is almost`() {
        assertEquals(TypeAnswer.Verdict.ALMOST, TypeAnswer.judge("mitochondira", "mitochondria"))
        assertEquals(TypeAnswer.Verdict.ALMOST, TypeAnswer.judge("photosinthesis", "photosynthesis"))
    }

    @Test
    fun `wrong answer is wrong`() {
        assertEquals(TypeAnswer.Verdict.WRONG, TypeAnswer.judge("ribosome", "mitochondria"))
        assertEquals(TypeAnswer.Verdict.WRONG, TypeAnswer.judge("", "mitochondria"))
    }

    @Test
    fun `levenshtein distances`() {
        assertEquals(0, TypeAnswer.levenshtein("abc", "abc"))
        assertEquals(1, TypeAnswer.levenshtein("abc", "abd"))
        assertEquals(3, TypeAnswer.levenshtein("", "abc"))
        assertEquals(3, TypeAnswer.levenshtein("kitten", "sitting"))
    }
}
