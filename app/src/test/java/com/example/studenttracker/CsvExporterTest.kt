package com.example.studenttracker

import com.example.studenttracker.domain.model.CsvExporter
import org.junit.Assert.*
import org.junit.Test

class CsvExporterTest {
    @Test fun quotesCommasQuotesAndLineBreaks() {
        assertEquals("\"Doe, \"\"Jane\"\"\nStudent\"", CsvExporter.cell("Doe, \"Jane\"\nStudent"))
    }
    @Test fun neutralizesFormulasIncludingWhitespacePrefixes() {
        listOf("=SUM(A1)", "+123", "-1", "@test", "  =HYPERLINK(x)", "\t1").forEach {
            assertTrue(CsvExporter.cell(it).startsWith("\"'"))
        }
        assertEquals("\"Student 1\"", CsvExporter.cell("Student 1"))
    }
    @Test fun usesUtf8BomAndCrLfForSpreadsheetCompatibility() {
        assertEquals("\uFEFF\"Name\",\"Marks\"\r\n\"Anita\",\"0\"\r\n", CsvExporter.encode(listOf(listOf("Name", "Marks"), listOf("Anita", "0"))))
    }
}
