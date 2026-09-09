package com.stockmanager.app.export

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter

object CsvExporter {

    suspend fun exportTable(
        context: Context,
        uri: Uri,
        headers: List<String>,
        rows: List<List<Any?>>,
        footerRow: List<Any?>? = null,
    ) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            BufferedWriter(OutputStreamWriter(out, Charsets.UTF_8)).use { writer ->
                writer.write(headers.joinToString(",") { escape(it) })
                writer.newLine()
                rows.forEach { row ->
                    writer.write(row.joinToString(",") { escape(it?.toString() ?: "") })
                    writer.newLine()
                }
                footerRow?.let { footer ->
                    writer.write(footer.joinToString(",") { escape(it?.toString() ?: "") })
                    writer.newLine()
                }
            }
        }
    }

    private fun escape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
