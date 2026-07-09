package com.recalldeck.app.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QaHeuristicParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing fixture $name" }
            .bufferedReader().use { it.readText() }

    // --- Q:/A: preset ---

    @Test
    fun qaPairs_parsesFixture() {
        val result = QaHeuristicParser.parse(fixture("sample_qa.txt"), ParserPreset.QA_PAIRS)
        val success = result as ImportResult.Success
        assertEquals(3, success.cards.size)
        assertEquals("What is the capital of France?", success.cards[0].question)
        assertEquals("Paris.", success.cards[0].answer)
        assertTrue(success.cards[1].answer.contains("mass more than twice"))
    }

    @Test
    fun qaPairs_lowercaseAndDotVariants() {
        val text = "q: first question\na: first answer\nQ. second question\nA. second answer"
        val success = QaHeuristicParser.parse(text, ParserPreset.QA_PAIRS) as ImportResult.Success
        assertEquals(2, success.cards.size)
        assertEquals("second answer", success.cards[1].answer)
    }

    @Test
    fun qaPairs_questionWithoutAnswerIsSkipped() {
        val text = "Q: orphan question\n\nQ: real question\nA: real answer"
        val success = QaHeuristicParser.parse(text, ParserPreset.QA_PAIRS) as ImportResult.Success
        assertEquals(1, success.cards.size)
        assertEquals("real question", success.cards[0].question)
    }

    // --- numbered preset ---

    @Test
    fun numbered_parsesBothNumberStyles() {
        val result = QaHeuristicParser.parse(fixture("sample_numbered.txt"), ParserPreset.NUMBERED)
        val success = result as ImportResult.Success
        assertEquals(3, success.cards.size)
        assertEquals("What is photosynthesis?", success.cards[0].question)
        assertEquals("Name the three states of matter.", success.cards[1].question)
        assertEquals("Au", success.cards[2].answer)
    }

    @Test
    fun numbered_multiLineAnswer() {
        val text = "1. Question one?\nline a\nline b\n2. Question two?\nanswer two"
        val success = QaHeuristicParser.parse(text, ParserPreset.NUMBERED) as ImportResult.Success
        assertEquals("line a\nline b", success.cards[0].answer)
    }

    // --- question-mark preset ---

    @Test
    fun questionMark_parsesFixture() {
        val result =
            QaHeuristicParser.parse(fixture("sample_questions.txt"), ParserPreset.QUESTION_MARK)
        val success = result as ImportResult.Success
        assertEquals(3, success.cards.size)
        assertEquals("Who wrote Hamlet?", success.cards[1].question)
        assertEquals("William Shakespeare.", success.cards[1].answer)
    }

    // --- term-definition preset ---

    @Test
    fun termDefinition_parsesHyphenAndEnDash() {
        val result = QaHeuristicParser.parse(fixture("sample_terms.txt"), ParserPreset.TERM_DEFINITION)
        val success = result as ImportResult.Success
        assertEquals(4, success.cards.size)
        assertEquals("Osmosis", success.cards[0].question)
        assertEquals("Photosynthesis", success.cards[3].question)
    }

    @Test
    fun termDefinition_ignoresLinesWithoutSeparator() {
        val text = "Alpha - first letter\njust a plain line\nBeta - second letter"
        val success =
            QaHeuristicParser.parse(text, ParserPreset.TERM_DEFINITION) as ImportResult.Success
        assertEquals(2, success.cards.size)
    }

    // --- auto-detection ---

    @Test
    fun autoDetect_picksQaPairs() {
        val result = QaHeuristicParser.parse(fixture("sample_qa.txt"))
        val success = result as ImportResult.Success
        assertEquals(ParserPreset.QA_PAIRS, success.presetUsed)
        assertEquals(3, success.cards.size)
    }

    @Test
    fun autoDetect_picksNumbered() {
        val result = QaHeuristicParser.parse(fixture("sample_numbered.txt"))
        val success = result as ImportResult.Success
        assertEquals(ParserPreset.NUMBERED, success.presetUsed)
    }

    @Test
    fun autoDetect_picksQuestionMark() {
        val result = QaHeuristicParser.parse(fixture("sample_questions.txt"))
        val success = result as ImportResult.Success
        assertEquals(ParserPreset.QUESTION_MARK, success.presetUsed)
    }

    @Test
    fun autoDetect_picksTermDefinition() {
        val result = QaHeuristicParser.parse(fixture("sample_terms.txt"))
        val success = result as ImportResult.Success
        assertEquals(ParserPreset.TERM_DEFINITION, success.presetUsed)
    }

    // --- garbage / malformed input ---

    @Test
    fun garbageText_returnsFriendlyError() {
        val result = QaHeuristicParser.parse(fixture("garbage.txt"))
        assertTrue(result is ImportResult.Failure)
        assertTrue((result as ImportResult.Failure).message.isNotBlank())
    }

    @Test
    fun blankInput_returnsFriendlyError() {
        assertTrue(QaHeuristicParser.parse("   \n\n  ") is ImportResult.Failure)
        assertTrue(QaHeuristicParser.parse("") is ImportResult.Failure)
    }

    @Test
    fun binaryGarbage_neverCrashes() {
        val binary = ByteArray(512) { (it * 31 % 256).toByte() }.toString(Charsets.UTF_8)
        val result = QaHeuristicParser.parse(binary)
        assertTrue(result is ImportResult.Failure || result is ImportResult.Success)
    }

    @Test
    fun explicitPresetWithNoMatches_returnsFriendlyError() {
        val result = QaHeuristicParser.parse("no structure here at all", ParserPreset.QA_PAIRS)
        assertTrue(result is ImportResult.Failure)
        assertTrue((result as ImportResult.Failure).message.contains("preset"))
    }
}
