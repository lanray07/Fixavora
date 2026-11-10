package com.fixavora.app.reports

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.fixavora.app.data.*
import com.fixavora.app.domain.Money
import com.fixavora.app.ui.t
import java.io.OutputStream
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object Exports {
    // Prevent spreadsheet formula injection as well as RFC4180 escaping.
    fun csvCell(value: String): String {
        val safe = if (value.trimStart().firstOrNull() in listOf('=', '+', '-', '@', '\t', '\r', '\n')) "'$value" else value
        return "\"${safe.replace("\"", "\"\"")}\""
    }
    fun csv(context: Context, records: List<HomeRecord>, output: OutputStream) {
        val keys = listOf("record_type", "title", "property", "room", "date", "due_date", "status", "amount", "currency", "notes")
        output.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write("\uFEFF" + keys.joinToString(",") { csvCell(context.t(it)) } + "\r\n")
            records.forEach { r ->
                writer.write(listOf(context.t(r.kind.lowercase()), r.title, records.find { it.id == r.propertyId }?.title.orEmpty(), records.find { it.id == r.roomId }?.title.orEmpty(), r.date, r.due.orEmpty(), context.t(if (r.done) "completed" else r.status.lowercase()), if (r.kind == "EXPENSE") Money.decimal(r.amountMinor, r.currency).toPlainString() else "", r.currency, r.notes).joinToString(",", transform = ::csvCell) + "\r\n")
            }
        }
    }
    fun summary(context: Context, records: List<HomeRecord>, locale: Locale, range: String): List<String> = buildList {
        add("Fixavora • ${context.t("maintenance_history")}")
        add(range)
        val totals = records.filter { it.kind == "EXPENSE" }.groupBy { it.currency }
        totals.forEach { (currency, values) ->
            val amount = values.fold(java.math.BigInteger.ZERO) { a, r -> a.add(java.math.BigInteger.valueOf(r.amountMinor)) }
            val format = Money.formatter(currency,locale)
            val decimal = java.math.BigDecimal(amount, java.util.Currency.getInstance(currency).defaultFractionDigits)
            add("${context.t("total")} ($currency): ${format.format(decimal)}")
        }
        records.forEach { r ->
            add("${context.t(r.kind.lowercase())}: ${r.title}")
            val date = r.due?.let { LocalDate.parse(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) }
            add(listOfNotNull(date, context.t(if (r.done) "completed" else r.status.lowercase()), r.notes.takeIf(String::isNotBlank)).joinToString(" • "))
        }
    }
    fun pdf(lines: List<String>, output: OutputStream, locale: Locale = Locale.getDefault()) {
        val document = PdfDocument()
        val paint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; textLocale = locale; color = android.graphics.Color.rgb(23, 42, 58) }
        var page: PdfDocument.Page? = null
        var y = 0f
        var number = 0
        fun newPage() {
            page?.let(document::finishPage)
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, ++number).create())
            y = 42f
        }
        try {
            newPage()
            for (line in lines) {
                val layout = android.text.StaticLayout.Builder.obtain(line,0,line.length,paint,505)
                    .setTextDirection(if(locale.language=="ar") android.text.TextDirectionHeuristics.FIRSTSTRONG_RTL else android.text.TextDirectionHeuristics.FIRSTSTRONG_LTR)
                    .setIncludePad(false).setLineSpacing(3f,1f).build()
                for(index in 0 until layout.lineCount) {
                    val top=layout.getLineTop(index);val height=layout.getLineBottom(index)-top
                    if (y+height > 795) newPage()
                    page!!.canvas.apply {
                        save();clipRect(45f,y,550f,y+height)
                        translate(45f,y-top);layout.draw(this);restore()
                    }
                    y += height
                }
                y += 6
            }
            page?.let(document::finishPage); page = null
            document.writeTo(output)
        } finally { document.close() }
    }
}
