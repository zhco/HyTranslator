package com.hytranslator.web

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.File

/**
 * Translates HTML files in a folder from source to target language.
 * Preserves HTML structure while translating text nodes.
 */
class HtmlTranslator {

    fun translateHtmlFolder(
        folder: File,
        sourceLang: String,
        targetLang: String,
        translateText: (String, String, String) -> String
    ): List<File> {
        val htmlFiles = folder.walk()
            .filter { it.isFile && it.extension.lowercase() in listOf("html", "htm") }
            .toList()
        val translatedFiles = mutableListOf<File>()

        htmlFiles.forEach { htmlFile ->
            val translated = translateHtmlFile(htmlFile, sourceLang, targetLang, translateText)
            val outputFile = File(htmlFile.parent, "${htmlFile.nameWithoutExtension}_${targetLang}.html")
            outputFile.writeText(translated)
            translatedFiles.add(outputFile)
        }

        return translatedFiles
    }

    private fun translateHtmlFile(
        file: File,
        sourceLang: String,
        targetLang: String,
        translateText: (String, String, String) -> String
    ): String {
        val doc: Document = Jsoup.parse(file, "UTF-8")
        translateDocument(doc, sourceLang, targetLang, translateText)
        return doc.html()
    }

    private fun translateDocument(
        doc: Document,
        sourceLang: String,
        targetLang: String,
        translateText: (String, String, String) -> String
    ) {
        // Translate text nodes in body
        val body = doc.body()
        if (body != null) {
            translateElement(body, sourceLang, targetLang, translateText)
        }

        // Update meta tags
        doc.head()?.select("meta[name=description], meta[name=keywords]")?.forEach { meta ->
            val content = meta.attr("content")
            if (content.isNotEmpty()) {
                val translated = translateText(content, sourceLang, targetLang)
                meta.attr("content", translated)
            }
        }

        // Update title
        val title = doc.title()
        if (title.isNotEmpty()) {
            doc.title(translateText(title, sourceLang, targetLang))
        }

        // Update lang attribute
        doc.select("html[lang]").forEach { html ->
            html.attr("lang", targetLang)
        }
    }

    private fun translateElement(
        element: Element,
        sourceLang: String,
        targetLang: String,
        translateText: (String, String, String) -> String
    ) {
        // Skip script, style, code, pre elements
        if (element.tagName() in setOf("script", "style", "code", "pre")) {
            return
        }

        // Translate text nodes
        element.textNodes().forEach { textNode ->
            val text = textNode.text().trim()
            if (text.isNotEmpty() && text.length > 1) {
                val translated = translateText(text, sourceLang, targetLang)
                textNode.text(translated)
            }
        }

        // Translate attributes
        val translatableAttrs = setOf("title", "alt", "placeholder", "aria-label")
        translatableAttrs.forEach { attr ->
            val value = element.attr(attr)
            if (value.isNotEmpty()) {
                val translated = translateText(value, sourceLang, targetLang)
                element.attr(attr, translated)
            }
        }

        // Recursively process child elements
        element.children().forEach { child ->
            translateElement(child, sourceLang, targetLang, translateText)
        }
    }
}
