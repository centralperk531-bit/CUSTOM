package com.example.custodiaapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Environment
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class CalendarPdfExporter(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val custodyCalculator: MainActivity.CustodyCalculator
) {

    /**
     * Exporta calendario para un rango de fechas personalizado (máximo 12 meses)
     */
    fun exportDateRangeCalendarToPdf(
        startDate: LocalDate,
        endDate: LocalDate,
        onSuccess: (filePath: String) -> Unit,
        onError: (error: String) -> Unit
    ) {
        try {
            // Validar que no superen 12 meses
            val monthsBetween = ChronoUnit.MONTHS.between(
                YearMonth.from(startDate),
                YearMonth.from(endDate)
            ) + 1

            if (monthsBetween > 12) {
                onError("El rango no puede superar 12 meses. Actualmente: $monthsBetween meses")
                return
            }

            if (startDate.isAfter(endDate)) {
                onError("La fecha de inicio debe ser anterior a la fecha de fin")
                return
            }

            // Crear directorio
            val pdfDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "Calendarios Custodia"
            )
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }

            // Nombre del archivo
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val pdfFile = File(
                pdfDir,
                "calendario_custodia_${startDate.format(dateFormatter)}_${endDate.format(dateFormatter)}.pdf"
            )

            // Crear documento PDF en A4 apaisado
            val writer = PdfWriter(pdfFile)
            val pdfDoc = PdfDocument(writer)
            pdfDoc.defaultPageSize = com.itextpdf.kernel.geom.PageSize.A4
            val document = Document(pdfDoc)

            document.setMargins(8f, 8f, 8f, 8f)

            // ========== LOGO ==========
            try {
                android.util.Log.d("PDF", "🔍 Intentando cargar logo...")

                val bitmap = BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.logo_app  // O logo_pdf si lo creaste
                )

                android.util.Log.d("PDF", "Bitmap cargado: bitmap=$bitmap")

                if (bitmap != null) {
                    android.util.Log.d("PDF", "✅ Bitmap no nulo. Tamaño: ${bitmap.width}x${bitmap.height}")

                    // Redimensionar si es muy grande
                    val scaledBitmap = if (bitmap.width > 512 || bitmap.height > 512) {
                        val scale = 512.0 / maxOf(bitmap.width, bitmap.height)
                        val newWidth = (bitmap.width * scale).toInt()
                        val newHeight = (bitmap.height * scale).toInt()
                        android.util.Log.d("PDF", "📐 Redimensionando a ${newWidth}x${newHeight}")
                        Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                    } else {
                        bitmap
                    }

                    val logoFile = File(context.cacheDir, "temp_logo.png")
                    FileOutputStream(logoFile).use { fos ->
                        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    }

                    android.util.Log.d("PDF", "💾 Logo guardado en: ${logoFile.absolutePath}")

                    val imageData = com.itextpdf.io.image.ImageDataFactory.create(logoFile.absolutePath)
                    val logoImage = com.itextpdf.layout.element.Image(imageData)
                    logoImage.setWidth(75f)  // Tamaño del logo
                    logoImage.setFixedPosition(
                        pdfDoc.defaultPageSize.width - 110f,  // Esquina derecha
                        pdfDoc.defaultPageSize.height - 85f, // Arriba
                        75f
                    )

                    document.add(logoImage)
                    android.util.Log.d("PDF", "✅ Logo agregado en esquina superior derecha")

                    logoFile.delete()
                    bitmap.recycle()
                    if (bitmap != scaledBitmap) scaledBitmap.recycle()

                } else {
                    android.util.Log.e("PDF", "❌ BitmapFactory devolvió NULL")
                }
            } catch (e: Exception) {
                android.util.Log.e("PDF", "❌ EXCEPCIÓN al cargar logo: ${e.message}", e)
                e.printStackTrace()
            }

            // Espaciado después del logo
            document.add(Paragraph(" ").setMarginBottom(3f))

            // Encabezado
            val parent1Name = preferencesManager.getParent1Name()
            val parent2Name = preferencesManager.getParent2Name()

            val title = Paragraph("CALENDARIO DE CUSTODIA")
                .setBold()
                .setFontSize(16f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(3f)

            document.add(title)

            val dateRange = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es", "ES"))
            val subtitle = Paragraph("${startDate.format(dateRange)} - ${endDate.format(dateRange)}")
                .setFontSize(12f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(2f)
            document.add(subtitle)

            val namesSubtitle = Paragraph("$parent1Name y $parent2Name")
                .setFontSize(11f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(3f)
            document.add(namesSubtitle)

            // ========== COPYRIGHT ==========
            val copyright = Paragraph("© 2026 IsmaelCR - Mi Turno Family")
                .setFontSize(10f)
                .setFontColor(DeviceRgb(150, 150, 150))
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(5f)
            document.add(copyright)

            // Generar meses a incluir
            val monthsToInclude = mutableListOf<YearMonth>()
            var currentYearMonth = YearMonth.from(startDate)
            val endYearMonth = YearMonth.from(endDate)

            while (!currentYearMonth.isAfter(endYearMonth)) {
                monthsToInclude.add(currentYearMonth)
                currentYearMonth = currentYearMonth.plusMonths(1)
            }

            // Determinar columnas según número de meses
            val columns = when {
                monthsToInclude.size <= 3 -> monthsToInclude.size
                monthsToInclude.size <= 6 -> 3
                else -> 4
            }

            // Tabla principal
            val columnWidths = FloatArray(columns) { 1f }
            val mainTable = Table(columnWidths)
            mainTable.setWidth(UnitValue.createPercentValue(100f))

            monthsToInclude.forEach { yearMonth ->
                val monthCell = createMonthTable(
                    yearMonth,
                    parent1Name,
                    parent2Name,
                    custodyCalculator,
                    startDate,
                    endDate
                )
                mainTable.addCell(monthCell)
            }
            // Completar última fila con celdas vacías si es necesario
            val cellsInLastRow = monthsToInclude.size % columns
            if (cellsInLastRow > 0) {
                val emptyCellsNeeded = columns - cellsInLastRow
                repeat(emptyCellsNeeded) {
                    mainTable.addCell(Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
                }
            }

            document.add(mainTable)

            // Leyenda
            document.add(Paragraph("").setMarginTop(8f))
            // addLegend(document, parent1Name, parent2Name) //

            // Estadísticas del rango
            document.add(Paragraph("").setMarginTop(12f))
            addRangeStatistics(document, startDate, endDate, parent1Name, parent2Name)

            // ========== RECUADROS DE FIRMAS ==========
            document.add(Paragraph("").setMarginTop(5f))

            val signatureTable = Table(floatArrayOf(1f, 1f))
            signatureTable.setWidth(UnitValue.createPercentValue(100f))

            val sig1Cell = Cell()
            sig1Cell.add(Paragraph("Fecha: _______________________").setFontSize(8f))
            sig1Cell.add(Paragraph("Nombre Tutor 1:").setFontSize(7f).setMarginTop(8f))
            sig1Cell.add(Paragraph("Firma: _______________________").setFontSize(8f).setMarginTop(6f))
            sig1Cell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            sig1Cell.setPadding(4f)
            signatureTable.addCell(sig1Cell)

            val sig2Cell = Cell()
            sig2Cell.add(Paragraph("Fecha: _______________________").setFontSize(8f))
            sig2Cell.add(Paragraph("Nombre Tutor 2:").setFontSize(7f).setMarginTop(8f))
            sig2Cell.add(Paragraph("Firma: _______________________").setFontSize(8f).setMarginTop(6f))
            sig2Cell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            sig2Cell.setPadding(4f)
            signatureTable.addCell(sig2Cell)

            document.add(signatureTable)

            document.close()
            onSuccess(pdfFile.absolutePath)
        } catch (e: Exception) {
            onError("Error al generar calendario: ${e.message}")
        }
    }

    /**
     * Método legacy para exportar año completo (mantiene compatibilidad)
     */
    fun exportYearCalendarToPdf(
        year: Int,
        onSuccess: (filePath: String) -> Unit,
        onError: (error: String) -> Unit
    ) {
        val startDate = LocalDate.of(year, 1, 1)
        val endDate = LocalDate.of(year, 12, 31)
        exportDateRangeCalendarToPdf(startDate, endDate, onSuccess, onError)
    }

    private fun createMonthTable(
        yearMonth: YearMonth,
        _parent1Name: String,
        _parent2Name: String,
        custodyCalculator: MainActivity.CustodyCalculator,
        rangeStart: LocalDate? = null,
        rangeEnd: LocalDate? = null
    ): Cell {
        val monthName = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES")))
        val monthTable = Table(floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 1f))

        // Título del mes
        val monthTitle = Paragraph(monthName.uppercase())
            .setBold()
            .setFontSize(11f)
            .setTextAlignment(TextAlignment.CENTER)
        val titleCell = Cell(1, 7)
        titleCell.add(monthTitle)
        titleCell.setBackgroundColor(DeviceRgb(94, 82, 64))
        titleCell.setFontColor(DeviceRgb(255, 255, 255))
        titleCell.setPadding(3f)
        monthTable.addCell(titleCell)

        // Días de la semana
        val dayHeaders = arrayOf("L", "M", "X", "J", "V", "S", "D")
        dayHeaders.forEach { day ->
            val cell = Cell()
            cell.add(Paragraph(day).setBold().setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
            cell.setBackgroundColor(DeviceRgb(245, 241, 232))
            cell.setPadding(2f)
            monthTable.addCell(cell)
        }

        // Cálculo correcto del primer día (LUNES = 0)
        val firstDay = yearMonth.atDay(1)
        val startDayOfWeek = when (firstDay.dayOfWeek.value) {
            7 -> 6
            else -> firstDay.dayOfWeek.value - 1
        }

        // Celdas vacías antes del día 1
        repeat(startDayOfWeek) {
            monthTable.addCell(Cell().add(Paragraph("")))
        }

        // Días del mes
        for (day in 1..yearMonth.lengthOfMonth()) {
            val date = yearMonth.atDay(day)

            // Verificar si está dentro del rango solicitado
            val isInRange = (rangeStart == null || !date.isBefore(rangeStart)) &&
                    (rangeEnd == null || !date.isAfter(rangeEnd))

            if (!isInRange) {
                // Día fuera del rango: celda gris claro
                val cell = Cell()
                cell.add(Paragraph(day.toString()).setFontSize(9f).setTextAlignment(TextAlignment.CENTER))
                cell.setBackgroundColor(DeviceRgb(240, 240, 240))
                cell.setFontColor(DeviceRgb(180, 180, 180))
                cell.setPadding(2f)
                cell.setHeight(14f)
                monthTable.addCell(cell)
            } else {
                val custody = custodyCalculator.getCustodyForDate(date)

                val color = when (custody.parent) {
                    ParentType.PARENT1 -> DeviceRgb(255, 231, 128)
                    ParentType.PARENT2 -> DeviceRgb(149, 169, 255)
                    else -> DeviceRgb(208, 208, 208)
                }

                val cellText = day.toString()
                val cell = Cell()
                cell.add(Paragraph(cellText).setBold().setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
                cell.setBackgroundColor(color)
                cell.setPadding(2f)
                cell.setHeight(14f)
                monthTable.addCell(cell)
            }
        }

        monthTable.setBorder(SolidBorder(1f))

        val wrapperCell = Cell()
        wrapperCell.add(monthTable)
        wrapperCell.setPadding(3f)
        return wrapperCell
    }

    /* private fun addLegend(
        document: Document,
        parent1Name: String,
        parent2Name: String
    ) {
        val legendTable = Table(floatArrayOf(1f, 1f, 1f))
        legendTable.setWidth(UnitValue.createPercentValue(100f))

        val cell1 = Cell()
        cell1.setBackgroundColor(DeviceRgb(255, 231, 128))
        cell1.add(Paragraph(parent1Name).setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
        cell1.setPadding(6f)
        legendTable.addCell(cell1)

        val cell2 = Cell()
        cell2.setBackgroundColor(DeviceRgb(149, 169, 255))
        cell2.add(Paragraph(parent2Name).setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
        cell2.setPadding(6f)
        legendTable.addCell(cell2)

        val cell3 = Cell()
        cell3.setBackgroundColor(DeviceRgb(208, 208, 208))
        cell3.add(Paragraph("Sin custodia").setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
        cell3.setPadding(6f)
        legendTable.addCell(cell3)

        document.add(legendTable)
    } */

    private fun addRangeStatistics(
        document: Document,
        startDate: LocalDate,
        endDate: LocalDate,
        parent1Name: String,
        parent2Name: String
    ) {
        var parent1Days = 0
        var parent2Days = 0
        var noCustodyDays = 0

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val custody = custodyCalculator.getCustodyForDate(currentDate)
            when (custody.parent) {
                ParentType.PARENT1 -> parent1Days++
                ParentType.PARENT2 -> parent2Days++
                ParentType.NONE -> noCustodyDays++
            }
            currentDate = currentDate.plusDays(1)
        }

        val totalDays = parent1Days + parent2Days + noCustodyDays
        val parent1Percent = if (totalDays > 0) (parent1Days * 100.0 / totalDays) else 0.0
        val parent2Percent = if (totalDays > 0) (parent2Days * 100.0 / totalDays) else 0.0

        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es", "ES"))
        val statsTitle = Paragraph("📊 ESTADÍSTICAS DEL PERÍODO (${startDate.format(dateFormatter)} - ${endDate.format(dateFormatter)})")
            .setBold()
            .setFontSize(14f)
            .setTextAlignment(TextAlignment.CENTER)
        document.add(statsTitle)

        val statsTable = Table(floatArrayOf(2f, 1f, 1f))
        statsTable.setWidth(UnitValue.createPercentValue(70f))
        statsTable.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)

        // Encabezados
        val headerCell1 = Cell().add(Paragraph("Custodio").setBold().setFontSize(10f))
        headerCell1.setBackgroundColor(DeviceRgb(94, 82, 64))
        headerCell1.setFontColor(DeviceRgb(255, 255, 255))
        headerCell1.setPadding(4f)
        statsTable.addCell(headerCell1)

        val headerCell2 = Cell().add(Paragraph("Días").setBold().setFontSize(10f))
        headerCell2.setBackgroundColor(DeviceRgb(94, 82, 64))
        headerCell2.setFontColor(DeviceRgb(255, 255, 255))
        headerCell2.setPadding(4f)
        statsTable.addCell(headerCell2)

        val headerCell3 = Cell().add(Paragraph("Porcentaje").setBold().setFontSize(10f))
        headerCell3.setBackgroundColor(DeviceRgb(94, 82, 64))
        headerCell3.setFontColor(DeviceRgb(255, 255, 255))
        headerCell3.setPadding(4f)
        statsTable.addCell(headerCell3)

        // Parent1
        val p1Cell1 = Cell().add(Paragraph(parent1Name).setFontSize(10f))
        p1Cell1.setBackgroundColor(DeviceRgb(255, 231, 128))
        p1Cell1.setPadding(4f)
        statsTable.addCell(p1Cell1)

        val p1Cell2 = Cell().add(Paragraph("$parent1Days").setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
        p1Cell2.setPadding(4f)
        statsTable.addCell(p1Cell2)

        val p1Cell3 = Cell().add(Paragraph(String.format("%.1f%%", parent1Percent)).setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
        p1Cell3.setPadding(4f)
        statsTable.addCell(p1Cell3)

        // Parent2
        val p2Cell1 = Cell().add(Paragraph(parent2Name).setFontSize(10f))
        p2Cell1.setBackgroundColor(DeviceRgb(149, 169, 255))
        p2Cell1.setPadding(4f)
        statsTable.addCell(p2Cell1)

        val p2Cell2 = Cell().add(Paragraph("$parent2Days").setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
        p2Cell2.setPadding(4f)
        statsTable.addCell(p2Cell2)

        val p2Cell3 = Cell().add(Paragraph(String.format("%.1f%%", parent2Percent)).setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
        p2Cell3.setPadding(4f)
        statsTable.addCell(p2Cell3)

        // Sin custodia
        if (noCustodyDays > 0) {
            val ncCell1 = Cell().add(Paragraph("Sin custodia").setFontSize(10f))
            ncCell1.setBackgroundColor(DeviceRgb(208, 208, 208))
            ncCell1.setPadding(4f)
            statsTable.addCell(ncCell1)

            val ncCell2 = Cell().add(Paragraph("$noCustodyDays").setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
            ncCell2.setPadding(4f)
            statsTable.addCell(ncCell2)

            val noCustodyPercent = if (totalDays > 0) (noCustodyDays * 100.0 / totalDays) else 0.0
            val ncCell3 = Cell().add(Paragraph(String.format("%.1f%%", noCustodyPercent)).setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
            ncCell3.setPadding(4f)
            statsTable.addCell(ncCell3)
        }

        // TOTAL
        val totalCell1 = Cell().add(Paragraph("TOTAL").setBold().setFontSize(10f))
        totalCell1.setBackgroundColor(DeviceRgb(220, 220, 220))
        totalCell1.setPadding(4f)
        statsTable.addCell(totalCell1)

        val totalCell2 = Cell().add(Paragraph("$totalDays días").setBold().setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
        totalCell2.setBackgroundColor(DeviceRgb(220, 220, 220))
        totalCell2.setPadding(4f)
        statsTable.addCell(totalCell2)

        val totalCell3 = Cell().add(Paragraph("100%").setBold().setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
        totalCell3.setBackgroundColor(DeviceRgb(220, 220, 220))
        totalCell3.setPadding(4f)
        statsTable.addCell(totalCell3)

        document.add(statsTable)
    }
}