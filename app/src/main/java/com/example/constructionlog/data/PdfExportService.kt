package com.example.constructionlog.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class PdfExportService(
    private val context: Context,
    private val repository: LogRepository
) {
    suspend fun exportDayPdf(targetUri: Uri, dayStartMillis: Long, projectId: Long): Result<Unit> = runCatching {
        val zone = ZoneId.systemDefault()
        val day = Instant.ofEpochMilli(dayStartMillis).atZone(zone).toLocalDate()
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val logs = repository.getLogsInDateRange(projectId, start, end)
        if (logs.isEmpty()) {
            throw IllegalStateException("该日期没有日志")
        }

        val document = PdfDocument()
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 14f
        }

        var pageNo = 1
        logs.forEach { item ->
            val pageInfo = PdfDocument.PageInfo.Builder(1080, 1920, pageNo).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas
            val pageWidth = pageInfo.pageWidth.toFloat()
            val pageHeight = pageInfo.pageHeight.toFloat()

            var y = 70f
            canvas.drawText("施工日志 ${DATE_FORMATTER.format(day)}", 40f, y, titlePaint)
            y += 50f
            canvas.drawText("部位: ${item.log.location}", 40f, y, bodyPaint)
            y += 30f
            canvas.drawText("天气: ${item.log.weather.ifBlank { "-" }}", 40f, y, bodyPaint)
            y += 30f
            canvas.drawText("内容:", 40f, y, bodyPaint)
            y += 24f

            item.log.content.chunked(38).take(10).forEach { line ->
                canvas.drawText(line, 40f, y, bodyPaint)
                y += 22f
            }

            y += 10f
            val imageWidth = 300
            val imageHeight = 220
            val gap = 20f
            var x = 40f
            item.images.forEach { image ->
                if (y + imageHeight > pageHeight - 40f) return@forEach
                val bitmap = decodeBitmap(Uri.parse(image.imageUri))
                if (bitmap != null) {
                    val scaled = Bitmap.createScaledBitmap(bitmap, imageWidth, imageHeight, true)
                    canvas.drawBitmap(scaled, x, y, null)
                    bitmap.recycle()
                    scaled.recycle()
                    if (x + imageWidth * 2 + gap <= pageWidth) {
                        x += imageWidth + gap
                    } else {
                        x = 40f
                        y += imageHeight + gap
                    }
                }
            }

            document.finishPage(page)
            pageNo += 1
        }

        context.contentResolver.openOutputStream(targetUri)?.use { output ->
            document.writeTo(output)
        } ?: throw IllegalStateException("无法写入PDF")

        document.close()
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        val fromStream = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        }.getOrNull()
        if (fromStream != null) return fromStream

        val path = uri.path ?: return null
        return runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
    }
}
