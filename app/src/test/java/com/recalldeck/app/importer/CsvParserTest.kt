package com.recalldeck.app.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing fixture $name" }
            .bufferedReader().use { it.readText() }

    @Test
    fun semicolonCsv_withHeaderQuotesAndEscapes() {
        val result = CsvParser.parse(fixture("sample_semicolon.csv"))
        val success = result as ImportResult.Success
        assertEquals(3, success.cards.size)
        assertEquals("What is the capital of Japan?", success.cards[0].question)
        assertEquals("Tokyo", success.cards[0].answer)
        assertEquals("What is 2+2; roughly?", success.cards[1].question)
        assertEquals("4; four", success.cards[1].answer)
        assertEquals("He said \"hello\"", success.cards[2].question)
    }

    @Test
    fun commaCsv_withQuotedDelimiter() {
        val result = CsvParser.parse(fixture("sample_comma.csv"))
        val success = result as ImportResult.Success
        assertEquals(3, success.cards.size)
        assertEquals("Largest ocean, by area?", success.cards[1].question)
        assertEquals("Pacific Ocean", success.cards[1].answer)
    }

    @Test
    fun explicitDelimiter_overridesDetection() {
        val text = "a,b;c,d"
        val success = CsvParser.parse(text, delimiter = ';') as ImportResult.Success
        assertEquals(1, success.cards.size)
        assertEquals("a,b", success.cards[0].question)
        assertEquals("c,d", success.cards[0].answer)
    }

    @Test
    fun extraColumns_onlyFirstTwoUsed() {
        val text = "question one;answer one;extra;more"
        val success = CsvParser.parse(text) as ImportResult.Success
        assertEquals("answer one", success.cards[0].answer)
    }

    // --- optional third explanation column ---

    @Test
    fun thirdColumn_becomesElaboration() {
        val text = "question;answer;explanation\nq1;a1;why it is so\nq2;a2\n"
        val success = CsvParser.parse(text) as ImportResult.Success
        assertEquals(2, success.cards.size)
        assertEquals("why it is so", success.cards[0].elaboration)
        assertNull(success.cards[1].elaboration)
    }

    @Test
    fun twoColumnFiles_haveNullElaboration() {
        val text = "question;answer\nq1;a1\nq2;a2\n"
        val success = CsvParser.parse(text) as ImportResult.Success
        assertEquals(2, success.cards.size)
        assertTrue(success.cards.all { it.elaboration == null })
        assertEquals("q1", success.cards[0].question)
        assertEquals("a1", success.cards[0].answer)
    }

    @Test
    fun blankThirdColumn_isNullElaboration() {
        val text = "q1;a1;\nq2;a2;   \n"
        val success = CsvParser.parse(text) as ImportResult.Success
        assertTrue(success.cards.all { it.elaboration == null })
    }

    @Test
    fun quotedThirdColumn_keepsSemicolonsAndQuotes() {
        val text = "q1;a1;\"details; with \"\"quotes\"\" inside\"\n"
        val success = CsvParser.parse(text) as ImportResult.Success
        assertEquals("details; with \"quotes\" inside", success.cards[0].elaboration)
    }

    @Test
    fun threeColumnHeader_isSkipped() {
        val text = "question;answer;explanation\nq1;a1;e1\n"
        val success = CsvParser.parse(text) as ImportResult.Success
        assertEquals(1, success.cards.size)
        assertEquals("e1", success.cards[0].elaboration)
    }

    @Test
    fun garbageThirdColumn_neverCrashes() {
        val binary = ByteArray(64) { (it * 41 % 256).toByte() }.toString(Charsets.ISO_8859_1)
        val result = CsvParser.parse("q1;a1;$binary\n")
        assertTrue(result is ImportResult.Failure || result is ImportResult.Success)
    }

    @Test
    fun blankLines_andBlankFields_areSkipped()  {
        val text = "q1;a1\n\n;\nq2;a2\nq3;\n"
        val success = CsvParser.parse(text) as ImportResult.Success
        assertEquals(2, success.cards.size)
    }

    @Test
    fun crlfLineEndings_areHandled() {
        val text = "q1;a1\r\nq2;a2\r\n"
        val success = CsvParser.parse(text) as ImportResult.Success
        assertEquals(2, success.cards.size)
    }

    // --- garbage / malformed input ---

    @Test
    fun emptyInput_returnsFriendlyError() {
        assertTrue(CsvParser.parse("") is ImportResult.Failure)
        assertTrue(CsvParser.parse("   \n ") is ImportResult.Failure)
    }

    @Test
    fun noDelimiter_returnsFriendlyError() {
        val result = CsvParser.parse("just some words\nno delimiters here")
        assertTrue(result is ImportResult.Failure)
        assertTrue((result as ImportResult.Failure).message.contains("delimiter"))
    }

    @Test
    fun unclosedQuote_returnsFriendlyError() {
        val result = CsvParser.parse("\"unclosed;answer\nnext;row")
        assertTrue(result is ImportResult.Failure)
        assertTrue((result as ImportResult.Failure).message.contains("quote"))
    }

    @Test
    fun binaryGarbage_neverCrashes() {
        val binary = ByteArray(512) { (it * 37 % 256).toByte() }.toString(Charsets.ISO_8859_1)
        val result = CsvParser.parse(binary)
        assertTrue(result is ImportResult.Failure || result is ImportResult.Success)
    }

    @Test
    fun onlyHeaderRow_returnsFriendlyError() {
        assertTrue(CsvParser.parse("question;answer") is ImportResult.Failure)
    }
}
