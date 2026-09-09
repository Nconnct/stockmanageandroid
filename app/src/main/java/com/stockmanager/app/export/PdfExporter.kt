package com.stockmanager.app.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.stockmanager.app.util.Formatting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 36f
private const val ROW_HEIGHT = 22f
private const val HEADER_HEIGHT = 26f

object PdfExporter {

    suspend fun exportTable(
        context: Context,
        uri: Uri,
        reportTitle: String,
        headers: List<String>,
        rows: List<List<String>>,
        columnWeights: List<Float>,
        footerRow: List<String>? = null,
    ) = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val usableWidth = PAGE_WIDTH - 2 * MARGIN
        val colWidths = columnWeights.map { it / columnWeights.sum() * usableWidth }
        val colX = mutableListOf(MARGIN)
        colWidths.forEachIndexed { i, w -> colX.add(colX[i] + w) }

        val titlePaint = Paint().apply { color = Color.BLACK; textSize = 16f; isFakeBoldText = true; isAntiAlias = true }
        val subtitlePaint = Paint().apply { color = Color.GRAY; textSize = 9f; isAntiAlias = true }
        val headerBgPaint = Paint().apply { color = Color.parseColor("#2563EB") }
        val headerTextPaint = Paint().apply { color = Color.WHITE; textSize = 10f; isFakeBoldText = true; isAntiAlias = true }
        val cellPaint = Paint().apply { color = Color.BLACK; textSize = 10f; isAntiAlias = true }
        val altRowPaint = Paint().apply { color = Color.parseColor("#F3F4F6") }
        val footerTextPaint = Paint().apply { color = Color.BLACK; textSize = 10f; isFakeBoldText = true; isAntiAlias = true }
        val linePaint = Paint().apply { color = Color.BLACK; strokeWidth = 1f }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN

        fun drawTitleBlock() {
            canvas.drawText("Stock Manager — $reportTitle", MARGIN, y + 14f, titlePaint)
            canvas.drawText("Generated on ${Formatting.dateTime(Date().time)}", MARGIN, y + 28f, subtitlePaint)
            y += 42f
        }

        fun drawHeaderRow() {
            canvas.drawRect(MARGIN, y, MARGIN + usableWidth, y + HEADER_HEIGHT, headerBgPaint)
            headers.forEachIndexed { i, h ->
                canvas.drawText(h, colX[i] + 4f, y + HEADER_HEIGHT - 8f, headerTextPaint)
            }
            y += HEADER_HEIGHT
        }

        fun newPage() {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN
            drawHeaderRow()
        }

        drawTitleBlock()
        drawHeaderRow()

        rows.forEachIndexed { rowIndex, row ->
            if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN) newPage()
            if (rowIndex % 2 == 1) {
                canvas.drawRect(MARGIN, y, MARGIN + usableWidth, y + ROW_HEIGHT, altRowPaint)
            }
            row.forEachIndexed { i, cell ->
                canvas.drawText(cell, colX[i] + 4f, y + ROW_HEIGHT - 7f, cellPaint)
            }
            y += ROW_HEIGHT
        }

        if (rows.isEmpty()) {
            canvas.drawText("No records found for this report.", MARGIN, y + 16f, cellPaint)
            y += ROW_HEIGHT
        }

        footerRow?.let { footer ->
            if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN) newPage()
            canvas.drawLine(MARGIN, y, MARGIN + usableWidth, y, linePaint)
            y += 2f
            footer.forEachIndexed { i, cell ->
                if (cell.isNotEmpty()) canvas.drawText(cell, colX[i] + 4f, y + ROW_HEIGHT - 7f, footerTextPaint)
            }
            y += ROW_HEIGHT
        }

        document.finishPage(page)

        context.contentResolver.openOutputStream(uri)?.use { out -> document.writeTo(out) }
        document.close()
    }
}
