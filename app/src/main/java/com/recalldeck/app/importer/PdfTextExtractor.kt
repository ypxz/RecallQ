package com.recalldeck.app.importer

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream

/** Result of extracting text from a PDF file. */
sealed class PdfExtractionResult {
    data class Success(val text: String) : PdfExtractionResult()
    data class Failure(val message: String) : PdfExtractionResult()
}

/**
 * Offline PDF text extraction using PdfBox-Android. No OCR: PDFs without an
 * embedded text layer (e.g. scanned documents) yield a friendly error.
 */
class PdfTextExtractor(context: Context) {

    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    fun extractText(input: InputStream): PdfExtractionResult {
        val document = try {
            PDDocument.load(input)
        } catch (e: Exception) {
            return PdfExtractionResult.Failure(
                "This file doesn't look like a valid PDF. Try a different file."
            )
        }
        return document.use { doc ->
            try {
                if (doc.isEncrypted) {
                    return@use PdfExtractionResult.Failure(
                        "This PDF is password-protected and can't be imported."
                    )
                }
                val text = PDFTextStripper().getText(doc)
                if (text.isBlank()) {
                    PdfExtractionResult.Failure(
                        "No text found in this PDF. Scanned PDFs without a text layer aren't supported."
                    )
                } else {
                    PdfExtractionResult.Success(text)
                }
            } catch (e: Exception) {
                PdfExtractionResult.Failure(
                    "Couldn't read text from this PDF. The file may be corrupted."
                )
            }
        }
    }
}
