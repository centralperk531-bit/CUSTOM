package com.example.custodiaapp

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import android.view.animation.AnimationUtils
import java.util.Calendar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: CustodyViewModel
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var calendarAdapter: CalendarPagerAdapter

    private val custodyCalculator by lazy { CustodyCalculator(viewModel) }
    private val calendarRenderer by lazy { CalendarRenderer(viewModel) }
    private val rangeSelectionManager = RangeSelectionManager()
    private var pendingEventType: String? = null // "PERIOD", "SPECIAL_DATE", "CHRISTMAS", "EASTER"
    private val selectionModeToast by lazy {
        Toast.makeText(this, "", Toast.LENGTH_SHORT)
    }

    // Views principales
    private val edtParent1 by lazy { findViewById<EditText>(R.id.edtParent1) }
    private val edtParent2 by lazy { findViewById<EditText>(R.id.edtParent2) }
    private val edtStartDate by lazy { findViewById<EditText>(R.id.edtStartDate) }
    private val tvResult by lazy { findViewById<TextView>(R.id.tvResult) }
    private val tvStats by lazy { findViewById<TextView>(R.id.tvStats) }
    private val progressBar by lazy { findViewById<ProgressBar>(R.id.progressBar) }
    private val viewPager by lazy { findViewById<ViewPager2>(R.id.calendarViewPager) }
    private val radioYearMode by lazy { findViewById<RadioButton>(R.id.radioStatsYearMode) }
    private val radioCustomMode by lazy { findViewById<RadioButton>(R.id.radioStatsCustomMode) }
    private val edtStatsStartDate by lazy { findViewById<EditText>(R.id.edtStatsStartDate) }
    private val edtStatsEndDate by lazy { findViewById<EditText>(R.id.edtStatsEndDate) }
    private val btnCalculateStats by lazy { findViewById<Button>(R.id.btnCalculateStats) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Forzar tamaño de fuente estándar en todos los dispositivos
        val configuration = resources.configuration
        configuration.fontScale = 1.2f
        resources.updateConfiguration(configuration, resources.displayMetrics)

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
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        fun createTabView(iconRes: Int): View {
            val view = layoutInflater.inflate(R.layout.tab_icon, null)
            view.findViewById<ImageView>(R.id.tabIcon).setImageResource(iconRes)
            return view
        }

        tabLayout.addTab(tabLayout.newTab().setCustomView(createTabView(R.drawable.ic_calendar)))
        tabLayout.addTab(tabLayout.newTab().setCustomView(createTabView(R.drawable.ic_search)))
        tabLayout.addTab(tabLayout.newTab().setCustomView(createTabView(R.drawable.ic_stats)))
        tabLayout.addTab(tabLayout.newTab().setCustomView(createTabView(R.drawable.ic_config)))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val view = tab.customView ?: return
                val container = view.findViewById<LinearLayout>(R.id.tabContainer)
                container.setBackgroundResource(R.drawable.tab_background_selected)

                val anim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.tab_scale_up)
                container.startAnimation(anim)
                view.isSelected = true

                showTabContent(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                val view = tab.customView ?: return
                val container = view.findViewById<LinearLayout>(R.id.tabContainer)
                container.setBackgroundResource(R.drawable.tab_background_unselected)

                val anim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.tab_scale_down)
                container.startAnimation(anim)
                view.isSelected = false
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun showTabContent(position: Int) {
        findViewById<View>(R.id.tabCalendar).visibility = View.GONE
        findViewById<View>(R.id.tabSearch).visibility = View.GONE
        findViewById<View>(R.id.tabStats).visibility = View.GONE
        findViewById<View>(R.id.tabConfig).visibility = View.GONE

        when (position) {
            0 -> findViewById<View>(R.id.tabCalendar).visibility = View.VISIBLE
            1 -> findViewById<View>(R.id.tabSearch).visibility = View.VISIBLE
            2 -> findViewById<View>(R.id.tabStats).visibility = View.VISIBLE
            3 -> findViewById<View>(R.id.tabConfig).visibility = View.VISIBLE
        }
    }

    private fun setupViewPager() {
        calendarRenderer.rangeSelectionManager = rangeSelectionManager
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
            SpinnerConfig(R.id.spinnerSummerDiv, R.array.summer_divisions, 0)
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
            R.id.spinnerPatternStartsWith
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
                is CustomDaysPattern -> 3
            }
        )
        findViewById<Spinner>(R.id.spinnerDay).setSelection(viewModel.changeDayOfWeek - 1)
        findViewById<Spinner>(R.id.spinnerDay).setSelection(viewModel.changeDayOfWeek)
        findViewById<Spinner>(R.id.spinnerSummerDiv).setSelection(viewModel.summerDivision.ordinal)
        findViewById<Spinner>(R.id.spinnerEvenYearStarts).setSelection(
            if (viewModel.evenYearStartsWith == 1) 0 else 1
        )
        findViewById<Spinner>(R.id.spinnerOddYearStarts).setSelection(
            if (viewModel.oddYearStartsWith == 1) 0 else 1
        )
        findViewById<Spinner>(R.id.spinnerPatternStartsWith).setSelection(
            if (viewModel.patternStartsWithParent == 1) 0 else 1
        )

        // RadioGroup
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupApplicationMode)
        when (viewModel.patternApplicationMode) {
            "FORWARD" -> radioGroup.check(R.id.radioModeForward)
            "FROM_DATE" -> radioGroup.check(R.id.radioModeFromDate)
        }
    }
    private fun setupListeners() {
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
        findViewById<Button>(R.id.btnManagePatternChanges).setOnClickListener { showPatternChangesManager() }
        findViewById<Button>(R.id.btnDeleteAll).setOnClickListener { showDeleteAllConfirmation() }
        findViewById<ExtendedFloatingActionButton>(R.id.fabExportPdf).setOnClickListener {
            showExportRangeDatePickerDialog()
        }

        setupSpinnerListener(R.id.spinnerPattern) { pos ->
            when(pos) {
                0 -> viewModel.custodyPattern = AlternateWeeks(startWithParent = 1)
                1 -> viewModel.custodyPattern = AlternateDays(startWithParent = 1)
                2 -> viewModel.custodyPattern = WeekdaysWeekends(weekdaysParent = 1, weekendsParent = 2)
                3 -> {
                    // Mostrar diálogo de configuración personalizada
                    showCustomPatternDialog()
                }
            }
        }

        /* setupSpinnerListener(R.id.spinnerDay) { viewModel.changeDayOfWeek = it } */
        setupSpinnerListener(R.id.spinnerDay) { position ->
            viewModel.changeDayOfWeek = position + 1  // 0->1, 1->2, ..., 6->7
        }
        findViewById<Spinner>(R.id.spinnerDay).setSelection(viewModel.changeDayOfWeek - 1)
        setupSpinnerListener(R.id.spinnerSummerDiv) { pos ->
            viewModel.summerDivision = VacationDivision.values()[pos.coerceIn(0, 4)]
        }
        setupSpinnerListener(R.id.spinnerEvenYearStarts) {
            viewModel.evenYearStartsWith = if (it == 0) 1 else 2
        }
        setupSpinnerListener(R.id.spinnerOddYearStarts) {
            viewModel.oddYearStartsWith = if (it == 0) 1 else 2
        }
        setupSpinnerListener(R.id.spinnerPatternStartsWith) {
            viewModel.patternStartsWithParent = if (it == 0) 1 else 2
        }

        // RadioGroup listener
        findViewById<RadioGroup>(R.id.radioGroupApplicationMode).setOnCheckedChangeListener { _, checkedId ->
            viewModel.patternApplicationMode = when (checkedId) {
                R.id.radioModeForward -> "FORWARD"
                R.id.radioModeFromDate -> "FROM_DATE"
                else -> "FORWARD"
            }
            updateDisplay()
        }
        // Listeners para estadísticas personalizadas
        edtStatsStartDate.setOnClickListener { showStatsDatePicker(true) }
        edtStatsEndDate.setOnClickListener { showStatsDatePicker(false) }
        btnCalculateStats.setOnClickListener { calculateCustomStats() }

        radioYearMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                edtStatsStartDate.isEnabled = false
                edtStatsEndDate.isEnabled = false
                updateStatsAsync()
            }
        }

        radioCustomMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                edtStatsStartDate.isEnabled = true
                edtStatsEndDate.isEnabled = true

                // Pre-rellenar con el año actual si están vacíos
                if (edtStatsStartDate.text.isEmpty()) {
                    edtStatsStartDate.setText("01/01/${LocalDate.now().year}")
                }
                if (edtStatsEndDate.text.isEmpty()) {
                    edtStatsEndDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                }
            }
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
        // PROTEGER pendingEventType durante el updateDisplay
        val savedEventType = pendingEventType

        if (::calendarAdapter.isInitialized) {
            calendarAdapter.notifyDataSetChanged()
        }
        updateStatsAsync()

        // RESTAURAR pendingEventType después de actualizar
        if (savedEventType != null) {
            pendingEventType = savedEventType
            android.util.Log.d("CustodiaApp", "updateDisplay - pendingEventType restaurado a: $savedEventType")
        }
    }

    private fun updateStatsAsync() {
        progressBar.visibility = View.VISIBLE
        tvStats.visibility = View.GONE

        lifecycleScope.launch {
            val stats = withContext(Dispatchers.Default) {
                if (radioYearMode.isChecked) {
                    // Modo año en curso
                    StatsCalculator(custodyCalculator, viewModel).calculateYearStats()
                } else if (radioCustomMode.isChecked) {

                    // Modo personalizado - recalcular con las fechas actuales
                    try {
                        val startStr = edtStatsStartDate.text.toString().trim()
                        val endStr = edtStatsEndDate.text.toString().trim()

                        if (startStr.isNotEmpty() && endStr.isNotEmpty()) {
                            val partsStart = startStr.split("/")
                            val partsEnd = endStr.split("/")
                            val startDate = LocalDate.of(partsStart[2].toInt(), partsStart[1].toInt(), partsStart[0].toInt())
                            val endDate = LocalDate.of(partsEnd[2].toInt(), partsEnd[1].toInt(), partsEnd[0].toInt())
                            StatsCalculator(custodyCalculator, viewModel).calculateRangeStats(startDate, endDate)
                        } else {
                            "Selecciona las fechas y pulsa Calcular"
                        }
                    } catch (e: Exception) {
                        "Error al calcular estadísticas"
                    }
                } else {
                    // Por defecto, año en curso
                    StatsCalculator(custodyCalculator, viewModel).calculateYearStats()
                }
            }

            tvStats.text = stats
            tvStats.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        }
    }


    private fun searchCustody() {
        try {
            val datePicker = findViewById<DatePicker>(R.id.datePickerSearch)
            val day = datePicker.dayOfMonth
            val month = datePicker.month + 1
            val year = datePicker.year

            val date = LocalDate.of(year, month, day)
            val custody = custodyCalculator.getCustodyForDate(date)

            tvResult.text = buildString {
                append("${getString(R.string.label_date)}: ")
                append(date.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale("es", "ES"))))
                append("\n\n")
                if (custody.parent == ParentType.NONE) {
                    append("Sin custodia")
                } else {
                    append("${getString(R.string.label_custody)}: ${custody.parentName}")
                }
                if (custody.note.isNotEmpty()) append("\n\n${custody.note}")
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al consultar fecha", Toast.LENGTH_SHORT).show()
        }
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

    private fun showStartDatePicker() {
        // SIEMPRE usar la fecha de HOY al abrir el calendario
        val calendar = java.util.Calendar.getInstance()

        DatePickerDialog(this, { _, year, month, day ->
            val selectedDate = "%02d/%02d/%04d".format(day, month + 1, year)
            edtStartDate.setText(selectedDate)

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

    // ============= GESTIÓN DE FECHAS DESDE CALENDARIO =============
    fun showDatePickerForContextMenu(date: LocalDate, onDateConfigured: () -> Unit) {
        if (!rangeSelectionManager.isSelecting) {
            // Primera selección: marcar inicio
            rangeSelectionManager.startSelection(date)
            updateDisplay()
            Toast.makeText(this, "Fecha inicio seleccionada. Toca otra fecha para completar el rango.", Toast.LENGTH_SHORT).show()
        } else {
            // Segunda selección: completar rango y mostrar diálogo
            rangeSelectionManager.updateEndDate(date)
            val range = rangeSelectionManager.completeSelection()

            if (range != null) {
                // Llamar al diálogo correspondiente según el tipo
                when (pendingEventType) {
                    "PERIOD" -> showRangeConfigDialog(range.first, range.second, "") { updateDisplay() }
                    "CHRISTMAS" -> showRangeConfigDialog(range.first, range.second, "Navidad") { updateDisplay() }
                    "EASTER" -> showRangeConfigDialog(range.first, range.second, "Semana Santa") { updateDisplay() }
                    "SPECIAL_DATE" -> {
                        // Para fecha especial solo usar el primer día
                        showSpecialDateConfigDialog(range.first)
                    }
                }
            }

            rangeSelectionManager.clearSelection()
            pendingEventType = null
            updateDisplay()
        }
    }

    fun onCalendarDateClicked(date: LocalDate) {
        // LOG DE DEPURACIÓN
        android.util.Log.d("CustodiaApp", "onCalendarDateClicked llamado con fecha: $date")
        android.util.Log.d("CustodiaApp", "pendingEventType actual: $pendingEventType")
        android.util.Log.d("CustodiaApp", "rangeSelectionManager.isSelecting: ${rangeSelectionManager.isSelecting}")

        if (pendingEventType == null) {
            // No hay selección activa, ignorar
            android.util.Log.d("CustodiaApp", "pendingEventType es null, saliendo...")
            Toast.makeText(this, "⚠️ DEBUG: pendingEventType es NULL", Toast.LENGTH_SHORT).show()
            return
        }

        if (!rangeSelectionManager.isSelecting) {
            // Primera selección: marcar inicio
            android.util.Log.d("CustodiaApp", "Primera selección - marcando inicio")
            rangeSelectionManager.startSelection(date)
            updateDisplay()
            showSelectionToast("Fecha inicio: ${date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))}. Selecciona fecha final.")
        } else {
            // Segunda selección: completar rango
            android.util.Log.d("CustodiaApp", "Segunda selección - completando rango")
            rangeSelectionManager.updateEndDate(date)
            val range = rangeSelectionManager.completeSelection()

            if (range != null) {
                // Llamar al diálogo correspondiente según el tipo
                // NO limpiar pendingEventType aquí, se limpiará en los botones del diálogo
                when (pendingEventType) {
                    "PERIOD" -> showRangeConfigDialog(range.first, range.second, "") { updateDisplay() }
                    "CHRISTMAS" -> showRangeConfigDialog(range.first, range.second, "Navidad") { updateDisplay() }
                    "EASTER" -> showRangeConfigDialog(range.first, range.second, "Semana Santa") { updateDisplay() }
                    "SPECIAL_DATE" -> {
                        // Para fecha especial solo usar el primer día
                        showSpecialDateConfigDialog(range.first)
                    }
                }
            }

            // Limpiar solo el rangeSelectionManager, NO el pendingEventType
            // El pendingEventType se limpiará cuando el usuario pulse Guardar o Cancelar
            rangeSelectionManager.clearSelection()
            updateDisplay()
        }
    }

    private fun showSelectionToast(message: String) {
        selectionModeToast.setText(message)
        selectionModeToast.show()
    }

    private fun showRangeConfigDialog(startDate: LocalDate, endDate: LocalDate, prefilledDescription: String = "", onDateConfigured: () -> Unit) {
        // Verificar si hay conflictos con eventos existentes en el rango
        val conflictingEvents = mutableListOf<String>()
        var current = startDate
        while (!current.isAfter(endDate)) {
            viewModel.specialDates.find { it.date == current }?.let {
                conflictingEvents.add("Fecha especial: ${it.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} - ${it.description}")
            }
            current = current.plusDays(1)
        }

        val conflictingPeriods = viewModel.summerEvents.filter { event ->
            !(event.endDate.isBefore(startDate) || event.startDate.isAfter(endDate))
        }.map { "${it.description}: ${it.startDate.format(DateTimeFormatter.ofPattern("dd/MM/yy"))} - ${it.endDate.format(DateTimeFormatter.ofPattern("dd/MM/yy"))}" }

        val conflictingNoCustody = viewModel.noCustodyPeriods.filter { period ->
            !(period.endDate.isBefore(startDate) || period.startDate.isAfter(endDate))
        }.map { "Sin custodia: ${it.startDate.format(DateTimeFormatter.ofPattern("dd/MM/yy"))} - ${it.endDate.format(DateTimeFormatter.ofPattern("dd/MM/yy"))}" }

        conflictingEvents.addAll(conflictingPeriods)
        conflictingEvents.addAll(conflictingNoCustody)

        if (conflictingEvents.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("⚠️ Conflictos detectados")
                .setMessage("El rango seleccionado solapa con:\n\n${conflictingEvents.joinToString("\n")}\n\n¿Quieres eliminar estos eventos y crear el nuevo?")
                .setPositiveButton("Sí, sobrescribir") { _, _ ->
                    // Eliminar eventos conflictivos
                    var currentDate = startDate
                    while (!currentDate.isAfter(endDate)) {
                        viewModel.specialDates.removeAll { it.date == currentDate }
                        currentDate = currentDate.plusDays(1)
                    }
                    viewModel.summerEvents.removeAll { event ->
                        !(event.endDate.isBefore(startDate) || event.startDate.isAfter(endDate))
                    }
                    viewModel.noCustodyPeriods.removeAll { period ->
                        !(period.endDate.isBefore(startDate) || period.startDate.isAfter(endDate))
                    }

                    // Continuar con el diálogo de configuración
                    showRangeConfigDialogInternal(startDate, endDate, prefilledDescription, onDateConfigured)
                }
                .setNegativeButton("No, cancelar") { _, _ ->
                    pendingEventType = null
                    rangeSelectionManager.clearSelection()
                    updateDisplay()
                }
                .show()
            return
        }

        showRangeConfigDialogInternal(startDate, endDate, prefilledDescription, onDateConfigured)
    }

    private fun showRangeConfigDialogInternal(startDate: LocalDate, endDate: LocalDate, prefilledDescription: String = "", onDateConfigured: () -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_summer_event, null)

        val edtStartDate = dialogView.findViewById<EditText>(R.id.edtSummerStartDate)
        val edtEndDate = dialogView.findViewById<EditText>(R.id.edtSummerEndDate)
        val edtDesc = dialogView.findViewById<EditText>(R.id.edtSummerDesc)
        val spinnerParent = dialogView.findViewById<Spinner>(R.id.spinnerSummerParent)
        val tvLegend = dialogView.findViewById<TextView>(R.id.tvYearRuleLegend)

        // Pre-rellenar descripción si existe
        if (prefilledDescription.isNotEmpty()) {
            edtDesc.setText(prefilledDescription)

            // Mostrar leyenda para Navidad y Semana Santa
            val parent1Name = viewModel.parent1Name
            val parent2Name = viewModel.parent2Name
            val evenYearStarts = if (viewModel.evenYearStartsWith == 1) parent1Name else parent2Name
            val oddYearStarts = if (viewModel.oddYearStartsWith == 1) parent1Name else parent2Name

            tvLegend.text = "ℹ️ Recuerda:\n• Años PARES: empieza $evenYearStarts\n• Años IMPARES: empieza $oddYearStarts"
            tvLegend.visibility = View.VISIBLE
        } else {
            tvLegend.visibility = View.GONE
        }

        // Pre-rellenar con el rango seleccionado
        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
        edtStartDate.setText(startDate.format(formatter))
        edtEndDate.setText(endDate.format(formatter))

        // Deshabilitar edición de fechas
        edtStartDate.isEnabled = false
        edtEndDate.isEnabled = false

        val parents = arrayOf(viewModel.parent1Name, viewModel.parent2Name, "Sin custodia")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, parents)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerParent.adapter = adapter

        val title = when (prefilledDescription) {
            "Navidad" -> "🎄 Configurar Período de Navidad"
            "Semana Santa" -> "🐣 Configurar Período de Semana Santa"
            else -> "Configurar período seleccionado"
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Guardar") { dialog, _ ->
                val description = edtDesc.text.toString().ifEmpty {
                    if (prefilledDescription.isNotEmpty()) prefilledDescription else "Período especial"
                }
                val selectedParentIndex = spinnerParent.selectedItemPosition

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

                // Limpiar el modo de selección completamente
                pendingEventType = null
                rangeSelectionManager.clearSelection()

                onDateConfigured()
                updateDisplay()
                Toast.makeText(this, "Período guardado", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                // CANCELAR: Limpiar completamente y salir del modo de selección
                pendingEventType = null
                rangeSelectionManager.clearSelection()
                updateDisplay()
                dialog.dismiss()
            }
            .setNeutralButton("Atrás") { dialog, _ ->
                // ATRÁS: NO limpiar pendingEventType, solo limpiar fechas seleccionadas
                android.util.Log.d("CustodiaApp", "Botón Atrás pulsado - pendingEventType ANTES: $pendingEventType")

                rangeSelectionManager.clearSelection()

                dialog.dismiss()

                // Volver a la pestaña del calendario
                findViewById<TabLayout>(R.id.tabLayout).getTabAt(0)?.select()

                android.util.Log.d("CustodiaApp", "Botón Atrás - pendingEventType ANTES updateDisplay: $pendingEventType")

                // Forzar actualización del calendario
                updateDisplay()

                android.util.Log.d("CustodiaApp", "Botón Atrás - pendingEventType DESPUÉS updateDisplay: $pendingEventType")

                // Mostrar toast recordando cómo seleccionar (con delay para que se vea)
                viewPager.postDelayed({
                    val message = when (pendingEventType) {
                        "CHRISTMAS" -> "🎄 Modo activo: Toca FECHA INICIO del rango de Navidad"
                        "EASTER" -> "🐣 Modo activo: Toca FECHA INICIO del rango de Semana Santa"
                        "PERIOD" -> "📅 Modo activo: Toca FECHA INICIO del rango"
                        "SPECIAL_DATE" -> "📅 Modo activo: Toca UNA fecha especial"
                        else -> "⚠️ ERROR: pendingEventType perdido ($pendingEventType)"
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }, 300)
            }
            .show()
    }

    // ============= DIÁLOGOS DE GESTIÓN =============

    private fun showSpecialDatesManager() {
        // Mostrar opciones: añadir o ver lista
        val options = if (viewModel.specialDates.isEmpty()) {
            arrayOf("➕ Añadir fecha especial")
        } else {
            arrayOf("➕ Añadir fecha especial", "📋 Ver lista (${viewModel.specialDates.size})")
        }

        AlertDialog.Builder(this)
            .setTitle("Gestionar Fechas Especiales")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Añadir nueva fecha especial
                        pendingEventType = "SPECIAL_DATE"
                        rangeSelectionManager.clearSelection()
                        findViewById<TabLayout>(R.id.tabLayout).getTabAt(0)?.select()
                        showSelectionToast("📅 Selecciona UNA fecha en el calendario para la fecha especial")
                    }
                    1 -> showSpecialDatesList()
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showSpecialDateConfigDialog(date: LocalDate) {
        // Verificar si ya existe un evento en esta fecha
        val existingSpecialDate = viewModel.specialDates.find { it.date == date }
        val existingPeriod = viewModel.summerEvents.find { date in it.startDate..it.endDate }
        val existingNoCustody = viewModel.noCustodyPeriods.find { date in it.startDate..it.endDate }

        if (existingSpecialDate != null || existingPeriod != null || existingNoCustody != null) {
            val existingDesc = when {
                existingSpecialDate != null -> "Fecha especial: ${existingSpecialDate.description}"
                existingPeriod != null -> "Período: ${existingPeriod.description}"
                existingNoCustody != null -> "Sin custodia: ${existingNoCustody.description}"
                else -> "Evento existente"
            }

            AlertDialog.Builder(this)
                .setTitle("⚠️ Fecha ocupada")
                .setMessage("Ya existe un evento en esta fecha:\n\n$existingDesc\n\n¿Quieres eliminarlo y crear uno nuevo?")
                .setPositiveButton("Sí, sobrescribir") { _, _ ->
                    // Eliminar eventos existentes
                    existingSpecialDate?.let { viewModel.specialDates.remove(it) }
                    existingPeriod?.let { viewModel.summerEvents.remove(it) }
                    existingNoCustody?.let { viewModel.noCustodyPeriods.remove(it) }

                    // Continuar con el diálogo de configuración
                    showSpecialDateConfigDialogInternal(date)
                }
                .setNegativeButton("No, cancelar") { _, _ ->
                    pendingEventType = null
                    rangeSelectionManager.clearSelection()
                    updateDisplay()
                }
                .show()
            return
        }

        showSpecialDateConfigDialogInternal(date)
    }

    private fun showSpecialDateConfigDialogInternal(date: LocalDate) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_summer_event, null)

        val edtStartDate = dialogView.findViewById<EditText>(R.id.edtSummerStartDate)
        val edtEndDate = dialogView.findViewById<EditText>(R.id.edtSummerEndDate)
        val edtDesc = dialogView.findViewById<EditText>(R.id.edtSummerDesc)
        val spinnerParent = dialogView.findViewById<Spinner>(R.id.spinnerSummerParent)

        // Ocultar fecha final
        edtEndDate.visibility = View.GONE
        val layout = dialogView as LinearLayout
        if (layout.childCount > 2) {
            layout.getChildAt(2).visibility = View.GONE
        }

        // Pre-rellenar con la fecha seleccionada
        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
        edtStartDate.setText(date.format(formatter))
        edtStartDate.isEnabled = false

        val parents = arrayOf(viewModel.parent1Name, viewModel.parent2Name, "Sin custodia")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, parents)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerParent.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Configurar Fecha Especial")
            .setView(dialogView)
            .setPositiveButton("Guardar") { dialog, _ ->
                val description = edtDesc.text.toString().ifEmpty { "Fecha especial" }
                val selectedParentIndex = spinnerParent.selectedItemPosition

                val parentType = when (selectedParentIndex) {
                    0 -> ParentType.PARENT1
                    1 -> ParentType.PARENT2
                    2 -> ParentType.NONE
                    else -> ParentType.PARENT1
                }

                viewModel.specialDates.add(SpecialDate(date, parentType, description))

                // Limpiar el modo de selección completamente
                pendingEventType = null
                rangeSelectionManager.clearSelection()

                updateDisplay()
                Toast.makeText(this, "Fecha especial guardada", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                // CANCELAR: Limpiar completamente y salir del modo de selección
                pendingEventType = null
                rangeSelectionManager.clearSelection()
                updateDisplay()
                dialog.dismiss()
            }
            .setNeutralButton("Atrás") { dialog, _ ->
                // ATRÁS: NO limpiar pendingEventType, solo limpiar fechas
                android.util.Log.d("CustodiaApp", "Botón Atrás (Fecha Especial) - pendingEventType ANTES: $pendingEventType")

                rangeSelectionManager.clearSelection()

                dialog.dismiss()

                // Volver a la pestaña del calendario
                findViewById<TabLayout>(R.id.tabLayout).getTabAt(0)?.select()

                // Forzar actualización del calendario
                updateDisplay()

                android.util.Log.d("CustodiaApp", "Botón Atrás (Fecha Especial) - pendingEventType DESPUÉS: $pendingEventType")

                // Mostrar toast recordando cómo seleccionar (con delay para que se vea)
                viewPager.postDelayed({
                    Toast.makeText(this, "📅 Modo activo: Toca la fecha especial", Toast.LENGTH_LONG).show()
                }, 300)
            }
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

    private fun showChristmasManager() {
        // Contar eventos de Navidad existentes
        val christmasEvents = viewModel.summerEvents.filter {
            it.description.contains("Navidad", ignoreCase = true)
        }

        val parent1Name = viewModel.parent1Name
        val parent2Name = viewModel.parent2Name
        val evenYearStarts = if (viewModel.evenYearStartsWith == 1) parent1Name else parent2Name
        val oddYearStarts = if (viewModel.oddYearStartsWith == 1) parent1Name else parent2Name

        // Crear las opciones con el recordatorio incluido en la primera línea
        val options = mutableListOf<String>()
        options.add("➕ Añadir nuevo periodo")
        if (christmasEvents.isNotEmpty()) {
            options.add("📋 Ver lista de Navidades (${christmasEvents.size})")
        }
        options.add("ℹ️  PARES: $evenYearStarts | IMPARES: $oddYearStarts")

        AlertDialog.Builder(this)
            .setTitle("🎄 Gestionar periodo Navideño")
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        // Añadir nueva Navidad
                        pendingEventType = "CHRISTMAS"
                        rangeSelectionManager.clearSelection()
                        findViewById<TabLayout>(R.id.tabLayout).getTabAt(0)?.select()
                        showSelectionToast("🎄 Selecciona el RANGO de Navidad en el calendario")
                    }
                    1 -> {
                        if (christmasEvents.isNotEmpty()) {
                            showPeriodsList()
                        } else {
                            // Si no hay eventos, la opción 1 es el info, no hacer nada
                        }
                    }
                    2 -> {
                        // Es solo información, no hacer nada o volver a mostrar el diálogo
                    }
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showEasterManager() {
        val currentYear = LocalDate.now().year
        val easterDates = mutableListOf<Pair<Int, Pair<LocalDate, LocalDate>>>()

        for (year in currentYear..(currentYear + 1)) {
            val easterSunday = calculateEasterSunday(year)
            val mondayBeforeEaster = easterSunday.minusDays(6)
            easterDates.add(year to (mondayBeforeEaster to easterSunday))
        }

        val parent1Name = viewModel.parent1Name
        val parent2Name = viewModel.parent2Name
        val evenYearStarts = if (viewModel.evenYearStartsWith == 1) parent1Name else parent2Name
        val oddYearStarts = if (viewModel.oddYearStartsWith == 1) parent1Name else parent2Name

        // Contar eventos de Semana Santa existentes
        val easterEvents = viewModel.summerEvents.filter {
            it.description.contains("Semana Santa", ignoreCase = true) ||
                    it.description.contains("Pascua", ignoreCase = true)
        }

        val message = buildString {
            easterDates.forEach { (year, dates) ->
                val (monday, sunday) = dates
                val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
                append("📅 Semana Santa $year:\n")
                append("   Lunes ${monday.format(formatter)} - Domingo ${sunday.format(formatter)}\n\n")
            }
            append("ℹ️ Recuerda:\n")
            append("• Años PARES: empieza $evenYearStarts\n")
            append("• Años IMPARES: empieza $oddYearStarts")
        }

        val builder = AlertDialog.Builder(this)
            .setTitle("🐣 Información de Semana Santa")
            .setMessage(message)
            .setPositiveButton("Añadir Semana Santa") { _, _ ->
                pendingEventType = "EASTER"
                rangeSelectionManager.clearSelection()
                findViewById<TabLayout>(R.id.tabLayout).getTabAt(0)?.select()
                showSelectionToast("🐣 Selecciona el RANGO de Semana Santa en el calendario")
            }
            .setNegativeButton("Cerrar", null)

        // Añadir botón de ver lista si hay eventos
        if (easterEvents.isNotEmpty()) {
            builder.setNeutralButton("Ver lista (${easterEvents.size})") { _, _ ->
                showPeriodsList()
            }
        }

        builder.show()
    }


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

    private fun showPeriodsManager(prefilledDescription: String = "") {
        // Contar todos los períodos
        val totalPeriods = viewModel.noCustodyPeriods.size + viewModel.summerEvents.size

        // Mostrar opciones: añadir o ver lista
        val options = if (totalPeriods == 0) {
            arrayOf("➕ Añadir período especial")
        } else {
            arrayOf("➕ Añadir período especial", "📋 Ver lista ($totalPeriods)")
        }

        AlertDialog.Builder(this)
            .setTitle("Gestionar Períodos Especiales")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Añadir nuevo período
                        pendingEventType = "PERIOD"
                        rangeSelectionManager.clearSelection()
                        findViewById<TabLayout>(R.id.tabLayout).getTabAt(0)?.select()
                        val message = if (prefilledDescription == "Navidad") {
                            "🎄 Selecciona el RANGO en el calendario"
                        } else {
                            "📅 Selecciona el RANGO en el calendario para el período especial"
                        }
                        showSelectionToast(message)
                    }
                    1 -> showPeriodsList()
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showDeleteAllConfirmation() {
        val options = arrayOf(
            "🗑️ Borrar TODO (eventos + nombres)",
            "📅 Borrar solo eventos (mantener nombres)"
        )

        AlertDialog.Builder(this)
            .setTitle("⚠️ Opciones de borrado")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> confirmDeleteAll()
                    1 -> confirmDeleteEventsOnly()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmDeleteAll() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Borrar TODO")
            .setMessage("Se eliminarán:\n\n• Fechas especiales\n• Períodos especiales\n• Navidad y Semana Santa\n• Períodos sin custodia\n• Cambios de patrón\n• Nombres de custodios\n\nEsta acción NO se puede deshacer.")
            .setPositiveButton("Sí, borrar todo") { _, _ ->
                viewModel.specialDates.clear()
                viewModel.summerEvents.clear()
                viewModel.noCustodyPeriods.clear()
                viewModel.patternChanges.clear()

                viewModel.parent1Name = "Custodio 1"
                viewModel.parent2Name = "Custodio 2"
                edtParent1.setText(viewModel.parent1Name)
                edtParent2.setText(viewModel.parent2Name)

                setupDynamicParentSpinners()
                preferencesManager.saveConfiguration(viewModel)
                updateDisplay()
                Toast.makeText(this, "✅ Todo eliminado", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmDeleteEventsOnly() {
        AlertDialog.Builder(this)
            .setTitle("📅 Borrar solo eventos")
            .setMessage("Se eliminarán:\n\n• Fechas especiales\n• Períodos especiales\n• Navidad y Semana Santa\n• Períodos sin custodia\n• Cambios de patrón\n\nSe mantendrán:\n• Nombres de custodios\n• Configuración base del patrón\n\nEsta acción NO se puede deshacer.")
            .setPositiveButton("Sí, borrar eventos") { _, _ ->
                viewModel.specialDates.clear()
                viewModel.summerEvents.clear()
                viewModel.noCustodyPeriods.clear()
                viewModel.patternChanges.clear()

                preferencesManager.saveConfiguration(viewModel)
                updateDisplay()
                Toast.makeText(this, "✅ Eventos eliminados", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ============= GESTIÓN DE CAMBIOS DE PATRÓN =============

    data class PatternChange(
        val startDate: LocalDate,
        val pattern: CustodyPattern,
        val changeDayOfWeek: Int,
        val startsWithParent: Int,
        val description: String = ""
    )

    private fun showPatternChangesManager() {
        val totalChanges = viewModel.patternChanges.size

        if (totalChanges == 0) {
            // Si no hay cambios, ir directo a añadir
            AlertDialog.Builder(this)
                .setTitle("Gestionar Cambios de Patrón")
                .setMessage("Aquí puedes añadir múltiples cambios de patrón a lo largo del tiempo por acuerdos o modificaciones judiciales.")
                .setPositiveButton("➕ Añadir cambio") { _, _ ->
                    showAddPatternChangeDialog()
                }
                .setNegativeButton("Cerrar", null)
                .show()
        } else {
            // Si hay cambios, mostrar opciones
            val options = arrayOf(
                "➕ Añadir cambio de patrón",
                "📋 Ver lista de cambios ($totalChanges)"
            )

            AlertDialog.Builder(this)
                .setTitle("Gestionar Cambios de Patrón")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> showAddPatternChangeDialog()
                        1 -> showPatternChangesList()
                    }
                }
                .setNegativeButton("Cerrar", null)
                .show()
        }
    }

    private fun showAddPatternChangeDialog() {
        // Si no existe el layout, crearlo dinámicamente
        val scrollView = ScrollView(this)
        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
        }

        // Fecha de inicio
        val tvDateLabel = TextView(this).apply {
            text = "Fecha desde cuando aplica:"
            textSize = 16f
            setPadding(0, 0, 0, 10)
        }
        linearLayout.addView(tvDateLabel)

        val edtDate = EditText(this).apply {
            hint = "dd/MM/yyyy"
            setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            isFocusable = false
            setOnClickListener {
                val calendar = java.util.Calendar.getInstance()
                DatePickerDialog(this@MainActivity, { _, year, month, day ->
                    setText("%02d/%02d/%04d".format(day, month + 1, year))
                }, calendar.get(java.util.Calendar.YEAR),
                    calendar.get(java.util.Calendar.MONTH),
                    calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
            }
        }
        linearLayout.addView(edtDate)

        // Patrón
        val tvPatternLabel = TextView(this).apply {
            text = "Patrón de custodia:"
            textSize = 16f
            setPadding(0, 30, 0, 10)
        }
        linearLayout.addView(tvPatternLabel)

        val spinnerPattern = Spinner(this).apply {
            adapter = ArrayAdapter.createFromResource(
                this@MainActivity, R.array.custody_patterns, android.R.layout.simple_spinner_item
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }
        linearLayout.addView(spinnerPattern)

        // Día de cambio
        val tvDayLabel = TextView(this).apply {
            text = "Día de cambio:"
            textSize = 16f
            setPadding(0, 30, 0, 10)
        }
        linearLayout.addView(tvDayLabel)

        val spinnerDay = Spinner(this).apply {
            adapter = ArrayAdapter.createFromResource(
                this@MainActivity, R.array.days_of_week, android.R.layout.simple_spinner_item
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(0) // Lunes por defecto
        }
        linearLayout.addView(spinnerDay)

        // Quién empieza
        val tvStartsLabel = TextView(this).apply {
            text = "Empieza con:"
            textSize = 16f
            setPadding(0, 30, 0, 10)
        }
        linearLayout.addView(tvStartsLabel)

        val spinnerStarts = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                arrayOf(viewModel.parent1Name, viewModel.parent2Name)
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }
        linearLayout.addView(spinnerStarts)

        // Descripción opcional
        val tvDescLabel = TextView(this).apply {
            text = "Descripción (opcional):"
            textSize = 16f
            setPadding(0, 30, 0, 10)
        }
        linearLayout.addView(tvDescLabel)

        val edtDesc = EditText(this).apply {
            hint = "Ej: Acuerdo judicial febrero 2025"
            setSingleLine(false)
            maxLines = 3
        }
        linearLayout.addView(edtDesc)

        scrollView.addView(linearLayout)

        AlertDialog.Builder(this)
            .setTitle("➕ Añadir Cambio de Patrón")
            .setView(scrollView)
            .setPositiveButton("Guardar") { dialog, _ ->
                try {
                    val dateStr = edtDate.text.toString()
                    val parts = dateStr.split("/")
                    val date = LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())

                    val pattern = when(spinnerPattern.selectedItemPosition) {
                        0 -> AlternateWeeks(startWithParent = if (spinnerStarts.selectedItemPosition == 0) 1 else 2)
                        1 -> AlternateDays(startWithParent = if (spinnerStarts.selectedItemPosition == 0) 1 else 2)
                        2 -> WeekdaysWeekends(
                            weekdaysParent = if (spinnerStarts.selectedItemPosition == 0) 1 else 2,
                            weekendsParent = if (spinnerStarts.selectedItemPosition == 0) 2 else 1
                        )
                        else -> AlternateWeeks(startWithParent = 1)
                    }

                    val change = PatternChange(
                        startDate = date,
                        pattern = pattern,
                        changeDayOfWeek = spinnerDay.selectedItemPosition + 1,
                        startsWithParent = if (spinnerStarts.selectedItemPosition == 0) 1 else 2,
                        description = edtDesc.text.toString().ifEmpty { "Cambio de patrón" }
                    )

                    viewModel.patternChanges.add(change)
                    viewModel.patternChanges.sortBy { it.startDate }

                    updateDisplay()
                    Toast.makeText(this, "Cambio de patrón guardado", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: Fecha inválida", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showPatternChangesList() {
        if (viewModel.patternChanges.isEmpty()) {
            Toast.makeText(this, "No hay cambios de patrón registrados", Toast.LENGTH_SHORT).show()
            return
        }

        val items = viewModel.patternChanges.map { change ->
            val patternName = when(change.pattern) {
                is AlternateWeeks -> "Semanas alternas"
                is AlternateDays -> "Días alternos"
                is WeekdaysWeekends -> "Entre semana/Fines de semana"
                else -> "Personalizado"
            }
            val dayNames = arrayOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
            val dayName = dayNames[change.changeDayOfWeek % 7]
            val startsName = if (change.startsWithParent == 1) viewModel.parent1Name else viewModel.parent2Name

            "${change.startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} - $patternName (cambio: $dayName, empieza: $startsName)\n${change.description}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Cambios de Patrón (${viewModel.patternChanges.size})")
            .setItems(items) { _, which ->
                val change = viewModel.patternChanges[which]
                AlertDialog.Builder(this)
                    .setTitle("¿Eliminar cambio de patrón?")
                    .setMessage("${change.startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}\n${change.description}")
                    .setPositiveButton("Eliminar") { _, _ ->
                        viewModel.patternChanges.removeAt(which)
                        updateDisplay()
                        Toast.makeText(this, "Cambio eliminado", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .setPositiveButton("Cerrar", null)
            .show()
    }

    // ============= CALCULADORAS Y CLASES INTERNAS =============
    data class CustodyInfo(
        val parent: ParentType,
        val parentName: String,
        val note: String,
        val isVacation: Boolean
    )


    // ============= FUNCIONES PARA ESTADÍSTICAS PERSONALIZADAS =============
    private fun showStatsDatePicker(isStartDate: Boolean) {
        val calendar = java.util.Calendar.getInstance()

        DatePickerDialog(this, { _, year, month, day ->
            val selectedDate = "%02d/%02d/%04d".format(day, month + 1, year)
            if (isStartDate) {
                edtStatsStartDate.setText(selectedDate)
            } else {
                edtStatsEndDate.setText(selectedDate)
            }
        }, calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
    }

    private fun calculateCustomStats() {
        try {
            val startStr = edtStatsStartDate.text.toString().trim()
            val endStr = edtStatsEndDate.text.toString().trim()

            if (startStr.isEmpty() || endStr.isEmpty()) {
                Toast.makeText(this, "Debes seleccionar ambas fechas", Toast.LENGTH_SHORT).show()
                return
            }

            val partsStart = startStr.split("/")
            val partsEnd = endStr.split("/")

            val startDate = LocalDate.of(partsStart[2].toInt(), partsStart[1].toInt(), partsStart[0].toInt())
            val endDate = LocalDate.of(partsEnd[2].toInt(), partsEnd[1].toInt(), partsEnd[0].toInt())

            if (endDate.isBefore(startDate)) {
                Toast.makeText(this, "La fecha final debe ser posterior a la inicial", Toast.LENGTH_SHORT).show()
                return
            }

            // Calcular estadísticas para el rango personalizado
            progressBar.visibility = View.VISIBLE
            tvStats.visibility = View.GONE

            lifecycleScope.launch {
                val stats = withContext(Dispatchers.Default) {
                    StatsCalculator(custodyCalculator, viewModel).calculateRangeStats(startDate, endDate)
                }

                tvStats.text = stats
                tvStats.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error en las fechas: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ============= CALCULADORAS Y CLASES INTERNAS =============
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

            // VERANO: Se asigna automáticamente según años pares/impares
            val summerRange = LocalDate.of(year, 7, 1)..LocalDate.of(year, 8, 31)
            if (date in summerRange) {
                val isEvenYear = year % 2 == 0
                val summerFirstParent = if (isEvenYear) {
                    if (viewModel.evenYearStartsWith == 1) ParentType.PARENT1 else ParentType.PARENT2
                } else {
                    if (viewModel.oddYearStartsWith == 1) ParentType.PARENT1 else ParentType.PARENT2
                }

                return getVacationInfo(
                    date,
                    summerRange,
                    viewModel.summerDivision,
                    summerFirstParent,
                    YearRule.ALWAYS,
                    year,
                    "Verano"
                )
            }

            // NAVIDAD Y SEMANA SANTA: Solo si están configurados manualmente
            val christmasEvents = viewModel.summerEvents.filter {
                it.description.contains("Navidad", ignoreCase = true)
            }
            christmasEvents.forEach { event ->
                if (date in event.startDate..event.endDate) {
                    return CustodyInfo(event.parent, getParentName(event.parent), event.description, true)
                }
            }

            val easterEvents = viewModel.summerEvents.filter {
                it.description.contains("Semana Santa", ignoreCase = true) ||
                        it.description.contains("Pascua", ignoreCase = true)
            }
            easterEvents.forEach { event ->
                if (date in event.startDate..event.endDate) {
                    return CustodyInfo(event.parent, getParentName(event.parent), event.description, true)
                }
            }

            return null
        }

        private fun getVacationInfo(
            date: LocalDate, range: ClosedRange<LocalDate>, division: VacationDivision,
            firstParent: ParentType, yearRule: YearRule, year: Int, label: String
        ) = VacationCalculator.getParentForDate(
            date, range.start, range.endInclusive, division, firstParent, yearRule, year
        )?.let { CustodyInfo(it, getParentName(it), label, true) }

        private fun getRegularCustody(date: LocalDate): CustodyInfo {
            // Buscar el cambio de patrón aplicable
            val applicableChange = viewModel.patternChanges
                .filter { it.startDate <= date }
                .maxByOrNull { it.startDate }

            // 🔍 DEBUG
            android.util.Log.d("CUSTODY_DEBUG", "=== CAMBIOS TOTALES: ${viewModel.patternChanges.size}")
            viewModel.patternChanges.forEach {
                android.util.Log.d("CUSTODY_DEBUG", "Cambio: ${it.startDate} - ${it.description}")
            }
            android.util.Log.d("CUSTODY_DEBUG", "Cambio aplicable para $date: $applicableChange")

            if (applicableChange != null) {
                // HAY CAMBIO - usar sus parámetros
                val effectivePattern = when (applicableChange.pattern) {
                    is AlternateWeeks -> (applicableChange.pattern as AlternateWeeks).copy(
                        startWithParent = applicableChange.startsWithParent
                    )
                    is AlternateDays -> (applicableChange.pattern as AlternateDays).copy(
                        startWithParent = applicableChange.startsWithParent
                    )
                    else -> applicableChange.pattern
                }

                // Usar la fecha del cambio como inicio
                val parent = effectivePattern.getParentForDate(
                    date,
                    applicableChange.startDate,
                    applicableChange.changeDayOfWeek
                )

                return CustodyInfo(
                    if (parent == 1) ParentType.PARENT1 else ParentType.PARENT2,
                    if (parent == 1) viewModel.parent1Name else viewModel.parent2Name,
                    "", false
                )
            }

            // NO HAY CAMBIOS - configuración base
            val year = date.year
            val configuredStartDate = viewModel.startDate
            val changeDayOfWeek = viewModel.changeDayOfWeek
            val patternStartsWithParent = viewModel.patternStartsWithParent
            val applicationMode = viewModel.patternApplicationMode

            val effectiveStartDate: LocalDate = when (applicationMode) {
                "FORWARD" -> {
                    val configuredYear = configuredStartDate.year
                    var changeDayInWeek = configuredStartDate

                    if (configuredStartDate.dayOfWeek.value != changeDayOfWeek) {
                        while (changeDayInWeek.dayOfWeek.value != changeDayOfWeek) {
                            changeDayInWeek = changeDayInWeek.minusDays(1)
                        }
                    }

                    if (date.year >= configuredYear) {
                        changeDayInWeek
                    } else {
                        var firstChangeDay = LocalDate.of(date.year, 1, 1)
                        while (firstChangeDay.dayOfWeek.value != changeDayOfWeek) {
                            firstChangeDay = firstChangeDay.plusDays(1)
                        }
                        firstChangeDay
                    }
                }
                "FROM_DATE" -> {
                    var changeDayInWeek = configuredStartDate

                    if (configuredStartDate.dayOfWeek.value == changeDayOfWeek) {
                        changeDayInWeek = configuredStartDate
                    } else {
                        while (changeDayInWeek.dayOfWeek.value != changeDayOfWeek) {
                            changeDayInWeek = changeDayInWeek.minusDays(1)
                        }
                    }

                    if (date.isBefore(changeDayInWeek)) {
                        var firstChangeDay = LocalDate.of(year, 1, 1)
                        while (firstChangeDay.dayOfWeek.value != changeDayOfWeek) {
                            firstChangeDay = firstChangeDay.plusDays(1)
                        }
                        firstChangeDay
                    } else {
                        changeDayInWeek
                    }
                }
                else -> {
                    var changeDayInWeek = configuredStartDate
                    if (configuredStartDate.dayOfWeek.value != changeDayOfWeek) {
                        while (changeDayInWeek.dayOfWeek.value != changeDayOfWeek) {
                            changeDayInWeek = changeDayInWeek.minusDays(1)
                        }
                    }
                    changeDayInWeek
                }
            }

            val pattern = when (val p = viewModel.custodyPattern) {
                is AlternateWeeks -> p.copy(startWithParent = patternStartsWithParent)
                is AlternateDays -> p.copy(startWithParent = patternStartsWithParent)
                else -> p
            }

            val parent = pattern.getParentForDate(date, effectiveStartDate, changeDayOfWeek)
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
                    if (days / 7 % 2 == 0L) effectiveFirst else effectiveFirst.toggle()
                }
                VacationDivision.ALTERNATE_WEEKS -> {
                    val startDayOfWeek = startDate.dayOfWeek.value
                    val daysToFirstSunday = 7 - startDayOfWeek

                    if (days <= daysToFirstSunday) {
                        return effectiveFirst
                    }

                    val daysAfterFirstWeek = days - daysToFirstSunday - 1
                    val weekNumber = daysAfterFirstWeek / 7

                    if (weekNumber % 2 == 0L) effectiveFirst.toggle() else effectiveFirst
                }
                VacationDivision.BIWEEKLY -> {
                    val dayOfMonth = date.dayOfMonth
                    val month = date.monthValue

                    val quincena = when {
                        month == 7 && dayOfMonth <= 15 -> 0
                        month == 7 && dayOfMonth >= 16 -> 1
                        month == 8 && dayOfMonth <= 15 -> 2
                        month == 8 && dayOfMonth >= 16 -> 3
                        else -> 0
                    }

                    if (quincena % 2 == 0) effectiveFirst else effectiveFirst.toggle()
                }
            }
        }
    }

    // ============= CLASE STATSCALCULATOR MEJORADA =============
    class StatsCalculator(
        private val custodyCalculator: CustodyCalculator,
        private val viewModel: CustodyViewModel
    ) {

        // Método ORIGINAL mantenido para compatibilidad
        fun calculateYearStats(): String {
            val year = LocalDate.now().year
            return calculateRangeStats(
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31)
            )
        }

        // NUEVO MÉTODO para calcular estadísticas en rango personalizado
        fun calculateRangeStats(startDate: LocalDate, endDate: LocalDate): String {
            var p1Days = 0
            var p2Days = 0
            var noDays = 0

            val noDetails = mutableMapOf<String, Int>()

            // Contadores de eventos incluidos
            var patternChangesCount = 0
            var specialDatesCount = 0
            var summerEventsCount = 0
            var noCustodyPeriodsCount = 0

            var current = startDate
            val total = ChronoUnit.DAYS.between(startDate, endDate) + 1

            while (!current.isAfter(endDate)) {
                val custody = custodyCalculator.getCustodyForDate(current)

                when (custody.parent) {
                    ParentType.PARENT1 -> p1Days++
                    ParentType.PARENT2 -> p2Days++
                    ParentType.NONE -> {
                        noDays++
                        val desc = custody.note.ifEmpty { "Sin especificar" }
                        noDetails[desc] = (noDetails[desc] ?: 0) + 1
                    }
                }

                // Contar eventos especiales
                if (custody.note.isNotEmpty()) {
                    when {
                        viewModel.specialDates.any { it.date == current } -> specialDatesCount++
                        viewModel.summerEvents.any { current in it.startDate..it.endDate } -> summerEventsCount++
                        viewModel.noCustodyPeriods.any { current in it.startDate..it.endDate } -> noCustodyPeriodsCount++
                    }
                }

                current = current.plusDays(1)
            }

            // Contar cambios de patrón que afectan al rango
            patternChangesCount = viewModel.patternChanges.count {
                it.startDate in startDate..endDate
            }

            return buildString {
                append("📊 ESTADÍSTICAS\n")
                append("Período: ${startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} - ${endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}\n")
                append("Total: $total días\n\n")

                append("👥 CUSTODIA\n")
                append("• ${viewModel.parent1Name}: $p1Days días (${String.format("%.1f", p1Days * 100.0 / total)}%)\n")
                append("• ${viewModel.parent2Name}: $p2Days días (${String.format("%.1f", p2Days * 100.0 / total)}%)\n")

                if (noDays > 0) {
                    append("• Sin custodia: $noDays días (${String.format("%.1f", noDays * 100.0 / total)}%)\n")
                    noDetails.forEach { (desc, count) ->
                        append("  - $desc: $count días\n")
                    }
                }

                append("\n📈 DIFERENCIA\n")
                append("${kotlin.math.abs(p1Days - p2Days)} días de diferencia\n")

                // Mostrar eventos incluidos en el cálculo
                append("\n✅ EVENTOS INCLUIDOS\n")
                append("• Patrón base: Sí\n")
                if (patternChangesCount > 0) append("• Cambios de patrón: $patternChangesCount\n")
                if (specialDatesCount > 0) append("• Fechas especiales: $specialDatesCount días\n")
                if (summerEventsCount > 0) append("• Períodos vacacionales: $summerEventsCount días\n")
                if (noCustodyPeriodsCount > 0) append("• Períodos sin custodia: $noCustodyPeriodsCount días\n")
            }
        }
    }

    // dialogo de personalizar //
    private fun showCustomPatternDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_pattern, null)

        val edtDaysParent1 = dialogView.findViewById<EditText>(R.id.edtDaysParent1)
        val edtDaysParent2 = dialogView.findViewById<EditText>(R.id.edtDaysParent2)
        val spinnerStarts = dialogView.findViewById<Spinner>(R.id.spinnerStartsWith)

        // Configurar spinner con los nombres de los padres
        val parents = arrayOf(viewModel.parent1Name, viewModel.parent2Name)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, parents)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStarts.adapter = adapter

        // Si ya hay un patrón personalizado, cargar sus valores
        if (viewModel.custodyPattern is CustomDaysPattern) {
            val current = viewModel.custodyPattern as CustomDaysPattern
            edtDaysParent1.setText(current.daysForParent1.toString())
            edtDaysParent2.setText(current.daysForParent2.toString())
            spinnerStarts.setSelection(if (current.startWithParent == 1) 0 else 1)
        } else {
            edtDaysParent1.setText("7")
            edtDaysParent2.setText("7")
        }

        AlertDialog.Builder(this)
            .setTitle("Patrón Personalizado")
            .setView(dialogView)
            .setPositiveButton("Guardar") { dialog, _ ->
                val days1 = edtDaysParent1.text.toString().toIntOrNull() ?: 7
                val days2 = edtDaysParent2.text.toString().toIntOrNull() ?: 7
                val startWith = if (spinnerStarts.selectedItemPosition == 0) 1 else 2

                if (days1 > 0 && days2 > 0) {
                    viewModel.custodyPattern = CustomDaysPattern(
                        daysForParent1 = days1,
                        daysForParent2 = days2,
                        startWithParent = startWith
                    )
                    updateDisplay()
                    Toast.makeText(this, "Patrón personalizado configurado: $days1 días / $days2 días", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Los días deben ser mayores a 0", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                // Si cancela y no había patrón personalizado previo, volver a Semanas Alternas
                if (viewModel.custodyPattern !is CustomDaysPattern) {
                    findViewById<Spinner>(R.id.spinnerPattern).setSelection(0)
                    viewModel.custodyPattern = AlternateWeeks(startWithParent = 1)
                }
                dialog.dismiss()
            }
            .show()
    }
    // ============= EXPORTACIÓN A PDF =============

    private fun showExportCalendarDialog() {
        android.util.Log.e("PDF_DEBUG", "showExportCalendarDialog INICIO")

        val currentYear = LocalDate.now().year
        android.util.Log.e("PDF_DEBUG", "Año actual: $currentYear")

        val options = arrayOf(
            "Año $currentYear",
            "Año ${currentYear - 1}",
            "Año ${currentYear + 1}",
            "Personalizado"
        )

        android.util.Log.e("PDF_DEBUG", "Opciones: ${options.joinToString()}")

        try {
            val dialog = AlertDialog.Builder(this)
                .setTitle("Exportar Calendario a PDF")
                .setSingleChoiceItems(options, -1) { dialog, which ->
                    android.util.Log.e("PDF_DEBUG", "Opción seleccionada: $which")
                    when (which) {
                        0 -> exportCalendarToPdf(currentYear)
                        1 -> exportCalendarToPdf(currentYear - 1)
                        2 -> exportCalendarToPdf(currentYear + 1)
                        3 -> showYearPickerDialog()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar", null)
                .create()

            dialog.show()
            android.util.Log.e("PDF_DEBUG", "Dialog.show() ejecutado")
        } catch (e: Exception) {
            android.util.Log.e("PDF_DEBUG", "ERROR: ${e.message}")
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }




    private fun showYearPickerDialog() {
        val currentYear = LocalDate.now().year
        val years = Array(5) { (currentYear - 2 + it).toString() }

        AlertDialog.Builder(this)
            .setTitle("Selecciona el año")
            .setItems(years) { _, which ->
                val selectedYear = (currentYear - 2 + which)
                exportCalendarToPdf(selectedYear)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun exportCalendarToPdf(year: Int) {
        Toast.makeText(this, "Generando PDF de $year...", Toast.LENGTH_SHORT).show()

        val exporter = CalendarPdfExporter(this, preferencesManager, custodyCalculator)

        exporter.exportYearCalendarToPdf(
            year = year,
            onSuccess = { filePath ->
                val fileName = java.io.File(filePath).name
                Toast.makeText(
                    this,
                    "✅ Calendario exportado: $fileName",
                    Toast.LENGTH_LONG
                ).show()

                // Abrir el PDF automáticamente
                try {
                    val file = java.io.File(filePath)
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.fileprovider",
                        file
                    )
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    startActivity(android.content.Intent.createChooser(intent, "Abrir PDF"))
                } catch (e: Exception) {
                    Toast.makeText(this, "PDF guardado en: $filePath", Toast.LENGTH_LONG).show()
                }
            },
            onError = { error ->
                Toast.makeText(this, "❌ Error: $error", Toast.LENGTH_LONG).show()
            }
        )
    }
    // ==================== EXPORTACIÓN PDF ====================

    private fun showExportRangeDatePickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_export_date_range, null)

        val startDateButton = dialogView.findViewById<Button>(R.id.btnSelectStartDate)
        val endDateButton = dialogView.findViewById<Button>(R.id.btnSelectEndDate)
        val startDateText = dialogView.findViewById<TextView>(R.id.tvStartDateSelected)
        val endDateText = dialogView.findViewById<TextView>(R.id.tvEndDateSelected)
        val exportButton = dialogView.findViewById<Button>(R.id.btnExportRange)
        val errorText = dialogView.findViewById<TextView>(R.id.tvDateRangeError)

        var selectedStartDate: LocalDate? = null
        var selectedEndDate: LocalDate? = null

        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es", "ES"))

        // Selector de fecha de inicio
        startDateButton.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            if (selectedStartDate != null) {
                calendar.set(selectedStartDate!!.year, selectedStartDate!!.monthValue - 1, selectedStartDate!!.dayOfMonth)
            }

            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedStartDate = LocalDate.of(year, month + 1, dayOfMonth)
                    startDateText.text = selectedStartDate!!.format(dateFormatter)
                    startDateText.visibility = View.VISIBLE
                    errorText.visibility = View.GONE

                    // Validar rango si ya hay fecha de fin
                    if (selectedEndDate != null) {
                        validateDateRange(selectedStartDate!!, selectedEndDate!!, errorText)
                    }
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Selector de fecha de fin
        endDateButton.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            if (selectedEndDate != null) {
                calendar.set(selectedEndDate!!.year, selectedEndDate!!.monthValue - 1, selectedEndDate!!.dayOfMonth)
            }

            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedEndDate = LocalDate.of(year, month + 1, dayOfMonth)
                    endDateText.text = selectedEndDate!!.format(dateFormatter)
                    endDateText.visibility = View.VISIBLE
                    errorText.visibility = View.GONE

                    // Validar rango si ya hay fecha de inicio
                    if (selectedStartDate != null) {
                        validateDateRange(selectedStartDate!!, selectedEndDate!!, errorText)
                    }
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Crear diálogo
        val dialog = AlertDialog.Builder(this)
            .setTitle("Exportar calendario personalizado")
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .create()

        // Botón exportar
        exportButton.setOnClickListener {
            if (selectedStartDate == null || selectedEndDate == null) {
                errorText.text = "⚠️ Selecciona ambas fechas"
                errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (!validateDateRange(selectedStartDate!!, selectedEndDate!!, errorText)) {
                return@setOnClickListener
            }

            dialog.dismiss()
            exportCalendarToCustomRange(selectedStartDate!!, selectedEndDate!!)
        }

        dialog.show()
    }

    private fun validateDateRange(startDate: LocalDate, endDate: LocalDate, errorText: TextView): Boolean {
        if (startDate.isAfter(endDate)) {
            errorText.text = "⚠️ La fecha de inicio debe ser anterior a la fecha de fin"
            errorText.visibility = View.VISIBLE
            return false
        }

        val monthsBetween = ChronoUnit.MONTHS.between(
            YearMonth.from(startDate),
            YearMonth.from(endDate)
        ) + 1

        if (monthsBetween > 12) {
            errorText.text = "⚠️ El rango no puede superar 12 meses (seleccionados: $monthsBetween)"
            errorText.visibility = View.VISIBLE
            return false
        }

        errorText.visibility = View.GONE
        return true
    }

    private fun exportCalendarToCustomRange(startDate: LocalDate, endDate: LocalDate) {
        // ========== VERIFICAR PREMIUM ==========
        if (!preferencesManager.isPremium()) {
            showPremiumRequiredDialog()
            return
        }

        // Si es Premium, continuar con la exportación
        val pdfExporter = CalendarPdfExporter(this, preferencesManager, custodyCalculator)

        lifecycleScope.launch {
            pdfExporter.exportDateRangeCalendarToPdf(
                startDate,
                endDate,
                onSuccess = { filePath ->
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "✅ PDF generado", Toast.LENGTH_LONG).show()

                        // Abrir el PDF
                        try {
                            val file = File(filePath)
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                this@MainActivity,
                                "${packageName}.fileprovider",
                                file
                            )
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                            intent.setDataAndType(uri, "application/pdf")
                            intent.flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@MainActivity,
                                "PDF guardado en: $filePath",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "❌ Error: $error", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    // ========== DIÁLOGO PREMIUM REQUERIDO ==========
    private fun showPremiumRequiredDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("🌟 Función Premium")
            .setMessage("La exportación a PDF es una función exclusiva de la versión Premium.\n\n¿Deseas desbloquear todas las funciones Premium?")
            .setPositiveButton("Ver Premium") { _, _ ->
                // TODO: Navegar a PremiumFragment (lo haremos en el siguiente paso)
                Toast.makeText(this, "Próximamente: pantalla Premium", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Ahora no", null)
            .show()
    }


}