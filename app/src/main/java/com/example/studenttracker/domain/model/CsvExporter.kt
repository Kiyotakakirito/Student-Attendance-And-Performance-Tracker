package com.example.studenttracker.domain.model

object CsvExporter {

    fun cell(value: String): String {
        val trimmed = value.trimStart()
        val safe = if (value.startsWith('\t') || value.startsWith('\r') || value.startsWith('\n') || trimmed.firstOrNull() in listOf('=', '+', '-', '@')) "'$value" else value
        return "\"${safe.replace("\"", "\"\"")}\""
    }
    fun encode(rows: List<List<String>>): String = "\uFEFF" + rows.joinToString("\r\n") { row -> row.joinToString(",", transform = ::cell) } + "\r\n"
}
