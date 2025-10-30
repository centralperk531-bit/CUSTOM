package com.example.custodiaapp

import android.os.Bundle
import android.view.View
import android.widget.*
import android.app.DatePickerDialog
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: CustodyViewModel
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var calendarAdapter: CalendarPagerAdapter

    private val custodyCalculator by lazy { CustodyCalculator(viewModel) }
    private val calendarRenderer by lazy { CalendarRenderer(viewModel) }

    // Views principales
    private val edtParent1 by lazy { findViewById<EditText>(R.id.edtParent1) }
    private val edtParent2 by lazy { findViewById<EditText>(R.id.edtParent2) }
    private val edtStartDate by lazy { findViewById<EditText>(R.id.edtStartDate) }
    private val edtSearchDate by lazy { findViewById<EditText>(R.id.edtSearchDate) }
    private val tvResult by lazy { findViewById<TextView>(R.id.tvResult) }
    private val tvStats by lazy { findViewById<TextView>(R.id.tvStats) }
    private val progressBar by lazy { findViewById<ProgressBar>(R.id.progressBar) }
    private val viewPager by lazy { findViewById<ViewPager2>(R.id.calendarViewPager) }
    private val tabHost by lazy { findViewById<TabHost>(R.id.tabHost) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViewModel()
        setupUI()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(this)[CustodyViewModel::class.java]
        preferencesManager = PreferencesManager(this)

        if (preferencesManager.hasConfiguration()) {
            preferencesManager.loadConfiguration(viewModel)
        }

        edtParent1.setText(viewModel.parent1Name)
        edtParent2.setText(viewModel.parent2Name)
        edtStartDate.setText(viewModel.startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
    }

    private fun setupUI() {
        setupTabs()
        setupViewPager()
        setupSpinners()
        setupListeners()
        updateDisplay()
    }

    private fun setupTabs() {
        tabHost.setup()

        data class TabConfig(val spec: String, val labelRes: Int, val contentId: Int)
        val tabs = listOf(
            TabConfig("calendar", R.string.tab_calendar, R.id.tabCalendar),
            TabConfig("search", R.string.tab_search, R.id.tabSearch),
            TabConfig("stats", R.string.tab_stats, R.id.tabStats),
            TabConfig("config", R.string.tab_config, R.id.tabConfig)
        )

        tabs.forEach { tab ->
            tabHost.addTab(
                tabHost.newTabSpec(tab.spec)
                    .setIndicator(getString(tab.labelRes))
                    .setContent(tab.contentId)
            )
        }

        findViewById<TabLayout>(R.id.tabLayout).apply {
            tabs.forEach { addTab(newTab().setText(getString(it.labelRes))) }
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tabHost.currentTab = tab?.position ?: 0
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }
    }

    private fun setupViewPager() {
        calendarAdapter = CalendarPagerAdapter(this, calendarRenderer, viewModel)
        viewPager.apply {
            adapter = calendarAdapter
            setCurrentItem(calendarAdapter.getInitialPosition(), false)
            setPageTransformer { page, position ->
                val absPos = kotlin.math.abs(position)
                page.alpha = 1 - (absPos * 0.3f)
                page.scaleY = 0.85f + (1 - absPos) * 0.15f
            }
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val monthOffset = position - calendarAdapter.getInitialPosition()
                    viewModel.currentYearMonth = YearMonth.now().plusMonths(monthOffset.toLong())
                }
            })
        }
    }

    private fun setupSpinners() {
        data class SpinnerConfig(val id: Int, val arrayId: Int, val initialPos: Int)
        val spinnerConfigs = listOf(
            SpinnerConfig(R.id.spinnerPattern, R.array.custody_patterns, 0),
            SpinnerConfig(R.id.spinnerDay, R.array.days_of_week, 1),
            SpinnerConfig(R.id.spinnerSummerDiv, R.array.summer_divisions, 0),
            SpinnerConfig(R.id.spinnerSummerYearRule, R.array.year_rule_selector, 0)
        )

        spinnerConfigs.forEach { config ->
            findViewById<Spinner>(config.id).apply {
                adapter = ArrayAdapter.createFromResource(
                    this@MainActivity, config.arrayId, android.R.layout.simple_spinner_item
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                setSelection(config.initialPos)
            }
        }

        setupDynamicParentSpinners()
        syncSpinnersWithViewModel()
    }

    private fun setupDynamicParentSpinners() {
        val parentNames = arrayOf(viewModel.parent1Name, viewModel.parent2Name)

        val parentSpinnerIds = listOf(
            R.id.spinnerEvenYearStarts,
            R.id.spinnerOddYearStarts,
            R.id.spinnerSummerFirst,
            R.id.spinnerChristmasDiv
        )

        parentSpinnerIds.forEach { spinnerId ->
            findViewById<Spinner>(spinnerId).apply {
                val currentSelection = selectedItemPosition
                adapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_spinner_item,
                    parentNames
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                if (currentSelection in 0..1) {
                    setSelection(currentSelection)
                }
            }
        }
    }

    private fun syncSpinnersWithViewModel() {
        findViewById<Spinner>(R.id.spinnerPattern).setSelection(
            when (viewModel.custodyPattern) {
                is AlternateWeeks -> 0
                is AlternateDays -> 1
                is WeekdaysWeekends -> 2
                else -> 3
            }
        )
        findViewById<Spinner>(R.id.spinnerDay).setSelection(viewModel.changeDayOfWeek)
        findViewById<Spinner>(R.id.spinnerSummerDiv).setSelection(viewModel.summerDivision.ordinal)
        findViewById<Spinner>(R.id.spinnerChristmasDiv).setSelection(
            if (viewModel.christmasPeriod1FirstParent == ParentType.PARENT1) 0 else 1
        )
        findViewById<Spinner>(R.id.spinnerEvenYearStarts).setSelection(
            if (viewModel.evenYearStartsWith == 1) 0 else 1
        )
        findViewById<Spinner>(R.id.spinnerOddYearStarts).setSelection(
            if (viewModel.oddYearStartsWith == 1) 0 else 1
        )
        findViewById<Spinner>(R.id.spinnerSummerFirst).setSelection(
            if (viewModel.summerFirstParent == ParentType.PARENT1) 0 else 1
        )
        findViewById<Spinner>(R.id.spinnerSummerYearRule).setSelection(viewModel.summerYearRule.ordinal)
    }

    private fun setupListeners() {
        edtSearchDate.setOnClickListener { showDatePicker() }
        edtStartDate.setOnClickListener { showStartDatePicker() }

        edtParent1.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val name = edtParent1.text.toString().trim()
                viewModel.parent1Name = name.ifEmpty { getString(R.string.default_parent1) }
                setupDynamicParentSpinners()
                updateDisplay()
            }
        }

        edtParent2.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val name = edtParent2.text.toString().trim()
                viewModel.parent2Name = name.ifEmpty { getString(R.string.default_parent2) }
                setupDynamicParentSpinners()
                updateDisplay()
            }
        }

        findViewById<Button>(R.id.btnSearch).setOnClickListener { searchCustody() }
        findViewById<Button>(R.id.btnSaveConfig).setOnClickListener { saveConfiguration() }
        findViewById<Button>(R.id.btnManageSpecialDates).setOnClickListener { showSpecialDatesManager() }
        findViewById<Button>(R.id.btnManageNoCustody).setOnClickListener { showPeriodsManager() }
        findViewById<Button>(R.id.btnManageChristmas).setOnClickListener { showChristmasManager() }
        findViewById<Button>(R.id.btnManageEaster).setOnClickListener { showEasterManager() }

        setupSpinnerListener(R.id.spinnerPattern) { pos ->
            viewModel.custodyPattern = when(pos) {
                0 -> AlternateWeeks(startWithParent = 1)
                1 -> AlternateDays(startWithParent = 1)
                2 -> WeekdaysWeekends(weekdaysParent = 1, weekendsParent = 2)
                else -> CustomPattern()
            }
        }

        setupSpinnerListener(R.id.spinnerDay) { viewModel.changeDayOfWeek = it }
        setupSpinnerListener(R.id.spinnerSummerDiv) { pos ->
            viewModel.summerDivision = VacationDivision.values()[pos.coerceIn(0, 4)]
        }
        setupSpinnerListener(R.id.spinnerChristmasDiv) { pos ->
            viewModel.christmasPeriod1FirstParent = if (pos == 0) ParentType.PARENT1 else ParentType.PARENT2
            viewModel.christmasPeriod2FirstParent = if (pos == 0) ParentType.PARENT2 else ParentType.PARENT1
        }
        setupSpinnerListener(R.id.spinnerEvenYearStarts) {
            viewModel.evenYearStartsWith = if (it == 0) 1 else 2
        }
        setupSpinnerListener(R.id.spinnerOddYearStarts) {
            viewModel.oddYearStartsWith = if (it == 0) 1 else 2
        }
        setupSpinnerListener(R.id.spinnerSummerFirst) {
            viewModel.summerFirstParent = if (it == 0) ParentType.PARENT1 else ParentType.PARENT2
        }
        setupSpinnerListener(R.id.spinnerSummerYearRule) { pos ->
            viewModel.summerYearRule = YearRule.values()[pos.coerceIn(0, 2)]
        }
    }

    private fun setupSpinnerListener(spinnerId: Int, onSelected: (Int) -> Unit) {
        findViewById<Spinner>(spinnerId).onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onSelected(position)
                updateDisplay()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun saveConfiguration() {
        viewModel.parent1Name = edtParent1.text.toString().trim()
            .ifEmpty { getString(R.string.default_parent1) }
        viewModel.parent2Name = edtParent2.text.toString().trim()
            .ifEmpty { getString(R.string.default_parent2) }

        // Guardar fecha de inicio
        val dateStr = edtStartDate.text.toString().trim()
        if (dateStr.isNotEmpty()) {
            try {
                val parts = dateStr.split("/")
                viewModel.startDate = LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            } catch (e: Exception) {
                Toast.makeText(this, "Fecha de inicio inválida", Toast.LENGTH_SHORT).show()
                return
            }
        }

        setupDynamicParentSpinners()
        preferencesManager.saveConfiguration(viewModel)
        updateDisplay()
        Toast.makeText(this, R.string.success_saved, Toast.LENGTH_SHORT).show()
    }

    private fun updateDisplay() {
        if (::calendarAdapter.isInitialized) {
            calendarAdapter.notifyDataSetChanged()
        }
        updateStatsAsync()
    }

    private fun updateStatsAsync() {
        progressBar.visibility = View.VISIBLE
        tvStats.visibility = View.GONE

        lifecycleScope.launch {
            val stats = withContext(Dispatchers.Default) {
                StatsCalculator(custodyCalculator, viewModel).calculateYearStats()
            }
            tvStats.text = stats
            tvStats.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        }
    }

    private fun searchCustody() {
        val dateStr = edtSearchDate.text.toString().trim()
        if (dateStr.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_date, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val parts = dateStr.split("/")
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt()

            val date = LocalDate.of(year, month, day)
            val custody = custodyCalculator.getCustodyForDate(date)

            tvResult.text = buildString {
                append("${getString(R.string.label_date)}: ")
                append(date.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale.getDefault())))
                append("\n\n")
                if (custody.parent == ParentType.NONE) {
                    append("Sin custodia")
                } else {
                    append("${getString(R.string.label_custody)}: ${custody.parentName}")
                }
                if (custody.note.isNotEmpty()) append("\n\n${custody.note}")
            }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_invalid_date, Toast.LENGTH_SHORT).show()
        }
    }

    // ============= DIÁLOGO DE FECHAS ESPECIALES =============
    private fun showSpecialDatesManager() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_summer_event, null)

        val edtStartDate = dialogView.findViewById<EditText>(R.id.edtSummerStartDate)
        val edtEndDate = dialogView.findViewById<EditText>(R.id.edtSummerEndDate)
        val edtDesc = dialogView.findViewById<EditText>(R.id.edtSummerDesc)
        val spinnerParent = dialogView.findViewById<Spinner>(R.id.spinnerSummerParent)

        // Ocultar campos de fecha final (solo necesitamos una fecha para fechas especiales)
        edtEndDate.visibility = View.GONE
        // Ocultar el TextView "Fecha de fin" (es el tercer TextView del layout)
        val layout = dialogView as LinearLayout
        if (layout.childCount > 2) {
            layout.getChildAt(2).visibility = View.GONE // TextView "Fecha de fin"
        }

        val parents = arrayOf(viewModel.parent1Name, viewModel.parent2Name, "Sin custodia")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, parents)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerParent.adapter = adapter

        edtStartDate.setOnClickListener {
            showDatePickerDialog { date -> edtStartDate.setText(date) }
        }

        AlertDialog.Builder(this)
            .setTitle("Añadir Fecha Especial")
            .setView(dialogView)
            .setPositiveButton("Guardar") { dialog, _ ->
                val dateStr = edtStartDate.text.toString()
                val description = edtDesc.text.toString().ifEmpty { "Fecha especial" }
                val selectedParentIndex = spinnerParent.selectedItemPosition

                if (dateStr.isNotEmpty()) {
                    try {
                        val parts = dateStr.split("/")
                        val date = LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())

                        val parentType = when (selectedParentIndex) {
                            0 -> ParentType.PARENT1
                            1 -> ParentType.PARENT2
                            2 -> ParentType.NONE
                            else -> ParentType.PARENT1
                        }

                        viewModel.specialDates.add(SpecialDate(date, parentType, description))
                        updateDisplay()
                        Toast.makeText(this, "Fecha especial guardada", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Por favor completa la fecha", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Ver Lista") { _, _ -> showSpecialDatesList() }
            .show()
    }

    private fun showSpecialDatesList() {
        if (viewModel.specialDates.isEmpty()) {
            Toast.makeText(this, "No hay fechas especiales registradas", Toast.LENGTH_SHORT).show()
            return
        }

        val items = viewModel.specialDates.map {
            "${it.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} - ${getParentName(it.parent)} - ${it.description}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Fechas Especiales (${viewModel.specialDates.size})")
            .setItems(items) { _, which ->
                val selectedDate = viewModel.specialDates[which]
                AlertDialog.Builder(this)
                    .setTitle("¿Eliminar fecha especial?")
                    .setMessage("${selectedDate.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}\n${selectedDate.description}")
                    .setPositiveButton("Eliminar") { _, _ ->
                        viewModel.specialDates.removeAt(which)
                        updateDisplay()
                        Toast.makeText(this, "Fecha eliminada", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun showPeriodsList() {
        val allPeriods = mutableListOf<Pair<String, Int>>()

        viewModel.noCustodyPeriods.forEachIndexed { index, period ->
            allPeriods.add("${period.startDate.format(DateTimeFormatter.ofPattern("dd/MM/yy"))} - ${period.endDate.format(DateTimeFormatter.ofPattern("dd/MM/yy"))} | Sin custodia | ${period.description}" to index)
        }

        val summerOffset = viewModel.noCustodyPeriods.size
        viewModel.summerEvents.forEachIndexed { index, event ->
            val parentName = getParentName(event.parent)
            allPeriods.add("${event.startDate.format(DateTimeFormatter.ofPattern("dd/MM/yy"))} - ${event.endDate.format(DateTimeFormatter.ofPattern("dd/MM/yy"))} | $parentName | ${event.description}" to (summerOffset + index))
        }

        if (allPeriods.isEmpty()) {
            Toast.makeText(this, "No hay períodos registrados", Toast.LENGTH_SHORT).show()
            return
        }

        val items = allPeriods.map { it.first }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Períodos Especiales (${allPeriods.size})")
            .setItems(items) { _, which ->
                val selectedIndex = allPeriods[which].second
                val isSummerEvent = selectedIndex >= viewModel.noCustodyPeriods.size

                AlertDialog.Builder(this)
                    .setTitle("¿Eliminar período?")
                    .setMessage(items[which])
                    .setPositiveButton("Eliminar") { _, _ ->
                        if (isSummerEvent) {
                            viewModel.summerEvents.removeAt(selectedIndex - viewModel.noCustodyPeriods.size)
                        } else {
                            viewModel.noCustodyPeriods.removeAt(selectedIndex)
                        }
                        updateDisplay()
                        Toast.makeText(this, "Período eliminado", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun getParentName(parent: ParentType): String = when(parent) {
        ParentType.PARENT1 -> viewModel.parent1Name
        ParentType.PARENT2 -> viewModel.parent2Name
        ParentType.NONE -> "Sin custodia"
    }

    private fun showDatePickerDialog(onDateSelected: (String) -> Unit) {
        val calendar = java.util.Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            onDateSelected("%02d/%02d/%04d".format(day, month + 1, year))
        }, calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
    }

    private fun showDatePicker() {
        val calendar = java.util.Calendar.getInstance()
        edtSearchDate.text.toString().trim().takeIf { it.isNotEmpty() }?.let { dateText ->
            try {
                val parts = dateText.split("/")
                calendar.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
            } catch (_: Exception) { }
        }

        DatePickerDialog(this, { _, year, month, day ->
            edtSearchDate.setText("%02d/%02d/%04d".format(day, month + 1, year))
        }, calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
    }

    private fun showStartDatePicker() {
        val calendar = java.util.Calendar.getInstance()
        edtStartDate.text.toString().trim().takeIf { it.isNotEmpty() }?.let { dateText ->
            try {
                val parts = dateText.split("/")
                calendar.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
            } catch (_: Exception) { }
        }

        DatePickerDialog(this, { _, year, month, day ->
            val selectedDate = "%02d/%02d/%04d".format(day, month + 1, year)
            edtStartDate.setText(selectedDate)

            // Actualizar inmediatamente
            try {
                viewModel.startDate = LocalDate.of(year, month + 1, day)
                updateDisplay()
            } catch (e: Exception) {
                Toast.makeText(this, "Fecha inválida", Toast.LENGTH_SHORT).show()
            }
        }, calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
    }

    // ============= GESTIÓN DE NAVIDAD =============
    private fun showChristmasManager() {
        showPeriodsManager(prefilledDescription = "Navidad")
    }

    // ============= GESTIÓN DE SEMANA SANTA =============
    private fun showEasterManager() {
        val currentYear = LocalDate.now().year
        val easterDates = mutableListOf<Pair<Int, LocalDate>>()

        // Calcular Semana Santa para año actual y siguiente
        for (year in currentYear..(currentYear + 1)) {
            val easterDate = calculateEasterSunday(year)
            easterDates.add(year to easterDate)
        }

        // Mostrar diálogo informativo
        val message = buildString {
            easterDates.forEach { (year, date) ->
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
                append("📅 Semana Santa $year:\n")
                append("   Domingo de Resurrección: ${date.format(formatter)}\n\n")
            }
            append("Pulsa 'Configurar' para añadir este período")
        }

        AlertDialog.Builder(this)
            .setTitle("🐣 Información de Semana Santa")
            .setMessage(message)
            .setPositiveButton("Configurar Semana Santa") { _, _ ->
                showPeriodsManager(prefilledDescription = "Semana Santa")
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    /**
     * Calcula el Domingo de Resurrección usando el algoritmo de Gauss
     * Fuente: https://es.wikipedia.org/wiki/Computus
     */
    private fun calculateEasterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1

        return LocalDate.of(year, month, day)
    }

    // ============= GESTIÓN DE PERÍODOS (MODIFICADA PARA SOPORTAR PRE-RELLENO) =============
    private fun showPeriodsManager(prefilledDescription: String = "") {
        val dialogView = layoutInflater.inflate(R.layout.dialog_summer_event, null)

        val edtStartDate = dialogView.findViewById<EditText>(R.id.edtSummerStartDate)
        val edtEndDate = dialogView.findViewById<EditText>(R.id.edtSummerEndDate)
        val edtDesc = dialogView.findViewById<EditText>(R.id.edtSummerDesc)
        val spinnerParent = dialogView.findViewById<Spinner>(R.id.spinnerSummerParent)

        // Pre-rellenar descripción si se proporciona
        if (prefilledDescription.isNotEmpty()) {
            edtDesc.setText(prefilledDescription)
        }

        val parents = arrayOf(viewModel.parent1Name, viewModel.parent2Name, "Sin custodia")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, parents)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerParent.adapter = adapter

        edtStartDate.setOnClickListener {
            showDatePickerDialog { date -> edtStartDate.setText(date) }
        }

        edtEndDate.setOnClickListener {
            showDatePickerDialog { date -> edtEndDate.setText(date) }
        }

        val title = when (prefilledDescription) {
            "Navidad" -> "🎄 Añadir Período de Navidad"
            "Semana Santa" -> "🐣 Añadir Período de Semana Santa"
            else -> "Añadir Período Especial"
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Guardar") { dialog, _ ->
                val startDateStr = edtStartDate.text.toString()
                val endDateStr = edtEndDate.text.toString()
                val description = edtDesc.text.toString().ifEmpty {
                    if (prefilledDescription.isNotEmpty()) prefilledDescription else "Período especial"
                }
                val selectedParentIndex = spinnerParent.selectedItemPosition

                if (startDateStr.isNotEmpty() && endDateStr.isNotEmpty()) {
                    try {
                        val startParts = startDateStr.split("/")
                        val startDate = LocalDate.of(startParts[2].toInt(), startParts[1].toInt(), startParts[0].toInt())

                        val endParts = endDateStr.split("/")
                        val endDate = LocalDate.of(endParts[2].toInt(), endParts[1].toInt(), endParts[0].toInt())

                        val parentType = when (selectedParentIndex) {
                            0 -> ParentType.PARENT1
                            1 -> ParentType.PARENT2
                            2 -> ParentType.NONE
                            else -> ParentType.NONE
                        }

                        if (parentType == ParentType.NONE) {
                            viewModel.noCustodyPeriods.add(NoCustodyPeriod(startDate, endDate, description))
                        } else {
                            viewModel.summerEvents.add(SummerEvent(startDate, endDate, parentType, description))
                        }

                        updateDisplay()
                        Toast.makeText(this, "Período guardado correctamente", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Por favor completa las fechas", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Ver Lista") { _, _ -> showPeriodsList() }
            .show()
    }

    // ============= CALCULADORAS Y CLASES INTERNAS =============
    data class CustodyInfo(
        val parent: ParentType,
        val parentName: String,
        val note: String,
        val isVacation: Boolean
    )

    class CustodyCalculator(private val viewModel: CustodyViewModel) {
        fun getCustodyForDate(date: LocalDate): CustodyInfo =
            getNoCustodyInfo(date)
                ?: getSpecialDateCustody(date)
                ?: getSummerEventCustody(date)
                ?: getVacationCustody(date)
                ?: getRegularCustody(date)

        private fun getNoCustodyInfo(date: LocalDate) =
            viewModel.noCustodyPeriods.find { date in it.startDate..it.endDate }?.let {
                CustodyInfo(ParentType.NONE, "Sin custodia", it.description, true)
            }

        private fun getSpecialDateCustody(date: LocalDate) =
            viewModel.specialDates.find { it.date == date }?.let {
                CustodyInfo(it.parent, getParentName(it.parent), it.description, true)
            }

        private fun getSummerEventCustody(date: LocalDate) =
            viewModel.summerEvents.find { date in it.startDate..it.endDate }?.let {
                CustodyInfo(it.parent, getParentName(it.parent), it.description, true)
            }

        private fun getVacationCustody(date: LocalDate): CustodyInfo? {
            val year = date.year
            val summerRange = LocalDate.of(year, 7, 1)..LocalDate.of(year, 8, 31)
            if (date in summerRange) {
                return getVacationInfo(date, summerRange, viewModel.summerDivision,
                    viewModel.summerFirstParent, viewModel.summerYearRule, year, "Verano")
            }

            if (date in viewModel.christmasPeriod1Start..viewModel.christmasPeriod1End) {
                val parent = getChristmasParent(year, viewModel.christmasPeriod1FirstParent,
                    viewModel.christmasPeriod1YearRule)
                return CustodyInfo(parent, getParentName(parent), "Navidad (1ª parte)", true)
            }

            if (date in viewModel.christmasPeriod2Start..viewModel.christmasPeriod2End) {
                val parent = getChristmasParent(year, viewModel.christmasPeriod2FirstParent,
                    viewModel.christmasPeriod2YearRule)
                return CustodyInfo(parent, getParentName(parent), "Navidad (2ª parte)", true)
            }

            if (!viewModel.easterDisabled && date in viewModel.easterStart..viewModel.easterEnd) {
                return getVacationInfo(date, viewModel.easterStart..viewModel.easterEnd,
                    viewModel.easterDivision, viewModel.easterFirstParent,
                    viewModel.easterYearRule, year, "Semana Santa")
            }

            return null
        }

        private fun getVacationInfo(
            date: LocalDate, range: ClosedRange<LocalDate>, division: VacationDivision,
            firstParent: ParentType, yearRule: YearRule, year: Int, label: String
        ) = VacationCalculator.getParentForDate(
            date, range.start, range.endInclusive, division, firstParent, yearRule, year
        )?.let { CustodyInfo(it, getParentName(it), label, true) }

        private fun getChristmasParent(year: Int, firstParent: ParentType, yearRule: YearRule): ParentType {
            val isEvenYear = year % 2 == 0
            return when {
                yearRule == YearRule.ALWAYS -> firstParent
                (yearRule == YearRule.EVEN) == isEvenYear -> firstParent
                else -> firstParent.toggle()
            }
        }

        private fun getRegularCustody(date: LocalDate): CustodyInfo {
            val year = date.year
            val yearStartParent = if (year % 2 == 0) viewModel.evenYearStartsWith else viewModel.oddYearStartsWith
            val navEnd = viewModel.christmasPeriod2End
            val effectiveNavEnd = if (navEnd.year < year)
                LocalDate.of(year, navEnd.monthValue, navEnd.dayOfMonth) else navEnd

            var firstMonday = effectiveNavEnd.plusDays(1)
            while (firstMonday.dayOfWeek.value != 1) firstMonday = firstMonday.plusDays(1)

            val startDate = if (date.isBefore(firstMonday)) effectiveNavEnd.plusDays(1) else firstMonday
            val pattern = when (val p = viewModel.custodyPattern) {
                is AlternateWeeks -> p.copy(startWithParent = yearStartParent)
                is AlternateDays -> p.copy(startWithParent = yearStartParent)
                else -> p
            }

            val parent = pattern.getParentForDate(date, startDate)
            return CustodyInfo(
                if (parent == 1) ParentType.PARENT1 else ParentType.PARENT2,
                if (parent == 1) viewModel.parent1Name else viewModel.parent2Name,
                "", false
            )
        }

        private fun getParentName(parent: ParentType) = when(parent) {
            ParentType.PARENT1 -> viewModel.parent1Name
            ParentType.PARENT2 -> viewModel.parent2Name
            ParentType.NONE -> "Sin custodia"
        }
    }

    object VacationCalculator {
        fun getParentForDate(
            date: LocalDate, startDate: LocalDate, endDate: LocalDate,
            division: VacationDivision, firstParent: ParentType,
            yearRule: YearRule, year: Int
        ): ParentType? {
            val effectiveFirst = if (yearRule != YearRule.ALWAYS &&
                ((yearRule == YearRule.EVEN) != (year % 2 == 0))) {
                firstParent.toggle()
            } else firstParent

            val days = ChronoUnit.DAYS.between(startDate, date)
            return when (division) {
                VacationDivision.HALF -> {
                    val half = (ChronoUnit.DAYS.between(startDate, endDate) + 1) / 2
                    if (days < half) effectiveFirst else effectiveFirst.toggle()
                }
                VacationDivision.FULL -> effectiveFirst
                VacationDivision.ALTERNATE_DAYS -> {
                    // Alternar cada 7 días completos desde el día de inicio
                    if (days / 7 % 2 == 0L) effectiveFirst else effectiveFirst.toggle()
                }
                VacationDivision.ALTERNATE_WEEKS -> {
                    // Alternar por semanas naturales (lunes a domingo)
                    // Calcular en qué semana natural está la fecha
                    val startDayOfWeek = startDate.dayOfWeek.value // 1=Lun, 7=Dom

                    // Días hasta el primer domingo desde startDate
                    val daysToFirstSunday = 7 - startDayOfWeek

                    // Si estamos en la semana parcial inicial
                    if (days <= daysToFirstSunday) {
                        return effectiveFirst
                    }

                    // Calcular número de semana completa después de la primera parcial
                    val daysAfterFirstWeek = days - daysToFirstSunday - 1
                    val weekNumber = daysAfterFirstWeek / 7

                    // Alternar: semana parcial inicial + semanas pares = primer custodio
                    if (weekNumber % 2 == 0L) effectiveFirst.toggle() else effectiveFirst
                }
                VacationDivision.BIWEEKLY -> {
                    // Para verano: dividir por quincenas (1-15 y 16-31)
                    val dayOfMonth = date.dayOfMonth
                    val month = date.monthValue

                    val quincena = when {
                        month == 7 && dayOfMonth <= 15 -> 0  // 1-15 julio
                        month == 7 && dayOfMonth >= 16 -> 1  // 16-31 julio
                        month == 8 && dayOfMonth <= 15 -> 2  // 1-15 agosto
                        month == 8 && dayOfMonth >= 16 -> 3  // 16-31 agosto
                        else -> 0
                    }

                    if (quincena % 2 == 0) effectiveFirst else effectiveFirst.toggle()
                }
            }
        }
    }

    class StatsCalculator(
        private val custodyCalculator: CustodyCalculator,
        private val viewModel: CustodyViewModel
    ) {
        fun calculateYearStats(): String {
            val year = LocalDate.now().year
            var p1Days = 0
            var p2Days = 0
            var noDays = 0
            val noDetails = mutableMapOf<String, Int>()

            var current = LocalDate.of(year, 1, 1)
            val end = LocalDate.of(year, 12, 31)
            val total = ChronoUnit.DAYS.between(current, end) + 1

            while (!current.isAfter(end)) {
                when (custodyCalculator.getCustodyForDate(current).parent) {
                    ParentType.PARENT1 -> p1Days++
                    ParentType.PARENT2 -> p2Days++
                    ParentType.NONE -> {
                        noDays++
                        val desc = custodyCalculator.getCustodyForDate(current).note.ifEmpty { "Sin especificar" }
                        noDetails[desc] = (noDetails[desc] ?: 0) + 1
                    }
                }
                current = current.plusDays(1)
            }

            return buildString {
                append("═══ ESTADÍSTICAS $year ═══\n\n")
                append("${viewModel.parent1Name}:\n  $p1Days días (${String.format("%.1f", p1Days * 100.0 / total)}%)\n\n")
                append("${viewModel.parent2Name}:\n  $p2Days días (${String.format("%.1f", p2Days * 100.0 / total)}%)\n\n")
                if (noDays > 0) {
                    append("Sin custodia:\n  $noDays días (${String.format("%.1f", noDays * 100.0 / total)}%)\n")
                    noDetails.forEach { (desc, count) -> append("  - $desc: $count días\n") }
                    append("\n")
                }
                append("Diferencia entre padres: ${kotlin.math.abs(p1Days - p2Days)} días")
            }
        }
    }
}