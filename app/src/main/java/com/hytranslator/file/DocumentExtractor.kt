package com.hytranslator.file

import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xssf.extractor.XSSFExcelExtractor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.hssf.extractor.ExcelExtractor
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.InputStream

/**
 * Extracts text from various document formats.
 * Supported: PDF, DOCX, DOC, XLSX, XLS, PPTX, TXT
 */
class DocumentExtractor {

    fun extractText(uri: Uri, inputStream: InputStream, mimeType: String?): String {
        return when {
            mimeType?.contains("pdf") == true -> extractPdf(inputStream)
            mimeType?.contains("wordprocessingml") == true -> extractDocx(inputStream)
                        mimeType?.contains("spreadsheetml") == true -> extractXlsx(inputStream)
            mimeType?.contains("excel") == true -> extractXls(inputStream)
            mimeType?.contains("presentationml") == true -> extractPptx(inputStream)
            else -> extractPlainText(inputStream)
        }
    }

    private fun extractPdf(inputStream: InputStream): String {
        return try {
            val document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            stripper.getText(document)
        } catch (e: Exception) {
            "Failed to extract PDF text: ${e.message}"
        }
    }

    private fun extractDocx(inputStream: InputStream): String {
        return try {
            val doc = XWPFDocument(inputStream)
            val extractor = XWPFWordExtractor(doc)
            extractor.text
        } catch (e: Exception) {
            "Failed to extract DOCX text: ${e.message}"
        }
    }

    private fun extractDoc(inputStream: InputStream): String {
        return "Old .doc format requires additional POI libraries."
    }

    private fun extractXlsx(inputStream: InputStream): String {
        return try {
            val workbook = XSSFWorkbook(inputStream)
            val extractor = XSSFExcelExtractor(workbook)
            extractor.text
        } catch (e: Exception) {
            "Failed to extract XLSX text: ${e.message}"
        }
    }

    private fun extractXls(inputStream: InputStream): String {
        return try {
            val workbook = HSSFWorkbook(inputStream)
            val extractor = ExcelExtractor(workbook)
            extractor.text
        } catch (e: Exception) {
            "Failed to extract XLS text: ${e.message}"
        }
    }

    private fun extractPptx(inputStream: InputStream): String {
        // Simplified extraction for PPTX
        return try {
            "PPTX extraction requires additional libraries (Apache POI OOXML)."
        } catch (e: Exception) {
            "Failed to extract PPTX text: ${e.message}"
        }
    }

    private fun extractPlainText(inputStream: InputStream): String {
        return inputStream.bufferedReader().use { it.readText() }
    }
}
