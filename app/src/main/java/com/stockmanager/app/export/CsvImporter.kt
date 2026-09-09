package com.stockmanager.app.export

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class ImportedProductRow(
    val name: String,
    val category: String,
    val quality: String,
    val quantity: Int,
    val price: Double,
)

private val NAME_ALIASES = listOf("name", "product", "product name")
private val CATEGORY_ALIASES = listOf("category", "cat")
private val QUALITY_ALIASES = listOf("quality", "grade", "quality/grade")
private val QUANTITY_ALIASES = listOf("quantity", "qty", "stock", "items")
private val PRICE_ALIASES = listOf("price", "unit price", "unitprice", "cost", "rate")

object CsvImporter {

    /** Expected header row (any order, case-insensitive): Name, Category, Quality, Quantity, Price. */
    suspend fun importProducts(context: Context, uri: Uri): Pair<List<ImportedProductRow>, List<String>> =
        withContext(Dispatchers.IO) {
            val errors = mutableListOf<String>()
            val results = mutableListOf<ImportedProductRow>()

            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                    val headerLine = reader.readLine()
                        ?: throw IllegalArgumentException("The file is empty.")
                    val headers = parseCsvLine(headerLine).map { it.trim().lowercase() }

                    val nameIdx = indexOfAny(headers, NAME_ALIASES)
                    if (nameIdx == -1) {
                        throw IllegalArgumentException(
                            "Could not find a 'Name' column. The first row must contain headers " +
                                "like: Name, Category, Quality, Quantity, Price."
                        )
                    }
                    val categoryIdx = indexOfAny(headers, CATEGORY_ALIASES)
                    val qualityIdx = indexOfAny(headers, QUALITY_ALIASES)
                    val quantityIdx = indexOfAny(headers, QUANTITY_ALIASES)
                    val priceIdx = indexOfAny(headers, PRICE_ALIASES)

                    var lineNum = 1
                    reader.forEachLine { line ->
                        lineNum++
                        if (line.isBlank()) return@forEachLine
                        val cols = parseCsvLine(line)

                        val name = cols.getOrNull(nameIdx)?.trim() ?: ""
                        if (name.isEmpty()) {
                            errors.add("Row $lineNum: missing product name, skipped.")
                            return@forEachLine
                        }

                        val category = if (categoryIdx >= 0) cols.getOrNull(categoryIdx)?.trim().orEmpty() else ""
                        val quality = if (qualityIdx >= 0) cols.getOrNull(qualityIdx)?.trim().orEmpty() else ""

                        val quantity = if (quantityIdx >= 0) {
                            val raw = cols.getOrNull(quantityIdx)?.trim().orEmpty()
                            raw.toIntOrNull() ?: run {
                                if (raw.isNotEmpty()) errors.add("Row $lineNum: invalid quantity, defaulted to 0.")
                                0
                            }
                        } else 0

                        val price = if (priceIdx >= 0) {
                            val raw = cols.getOrNull(priceIdx)?.trim().orEmpty()
                            raw.toDoubleOrNull() ?: run {
                                if (raw.isNotEmpty()) errors.add("Row $lineNum: invalid price, defaulted to 0.")
                                0.0
                            }
                        } else 0.0

                        results.add(ImportedProductRow(name, category, quality, quantity, price))
                    }
                }
            }
            results to errors
        }

    private fun indexOfAny(headers: List<String>, aliases: List<String>): Int {
        for (alias in aliases) {
            val idx = headers.indexOf(alias)
            if (idx != -1) return idx
        }
        return -1
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(sb.toString()); sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}
