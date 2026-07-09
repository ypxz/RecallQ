package com.recalldeck.app.importer

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
class PdfTextExtractorTest {

    private lateinit var extractor: PdfTextExtractor

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        extractor = PdfTextExtractor(context)
    }

    private fun fixtureStream(name: String) =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing fixture $name" }

    @Test
    fun samplePdf_extractsQaText() {
        val result = extractor.extractText(fixtureStream("sample_qa.pdf"))
        val success = result as PdfExtractionResult.Success
        assertTrue(success.text.contains("powerhouse of the cell"))
        assertTrue(success.text.contains("mitochondrion"))
        assertTrue(success.text.contains("Deoxyribonucleic acid"))
    }

    @Test
    fun samplePdf_textFeedsHeuristicParser() {
        val result = extractor.extractText(fixtureStream("sample_qa.pdf"))
        val text = (result as PdfExtractionResult.Success).text
        val parsed = QaHeuristicParser.parse(text) as ImportResult.Success
        assertTrue(parsed.cards.size >= 3)
    }

    @Test
    fun binaryGarbage_returnsFriendlyError() {
        val result = extractor.extractText(fixtureStream("garbage.bin"))
        assertTrue(result is PdfExtractionResult.Failure)
        assertTrue((result as PdfExtractionResult.Failure).message.isNotBlank())
    }

    @Test
    fun fakePdfHeader_returnsFriendlyError() {
        val result = extractor.extractText(fixtureStream("fake.pdf"))
        assertTrue(result is PdfExtractionResult.Failure)
    }

    @Test
    fun emptyStream_returnsFriendlyError() {
        val result = extractor.extractText(ByteArrayInputStream(ByteArray(0)))
        assertTrue(result is PdfExtractionResult.Failure)
    }
}
