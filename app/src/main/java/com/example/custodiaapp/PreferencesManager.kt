package com.example.custodiaapp

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.time.LocalDate
import java.time.YearMonth

class PreferencesManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "custody_preferences"
        private const val TAG = "PreferencesManager"

        // Claves de preferencias como constantes
        private const val KEY_PARENT1_NAME = "parent1Name"
        private const val KEY_PARENT2_NAME = "parent2Name"
        private const val KEY_CUSTODY_PATTERN_POSITION = "custodyPatternPosition"
        private const val KEY_START_WITH_PARENT = "startWithParent"
        private const val KEY_WEEKDAYS_PARENT = "weekdaysParent"
        private const val KEY_WEEKENDS_PARENT = "weekendsParent"
        private const val KEY_CUSTOM_DAYS_PARENT1 = "customDaysParent1"
        private const val KEY_CUSTOM_DAYS_PARENT2 = "customDaysParent2"
        private const val KEY_CUSTOM_START_WITH_PARENT = "customStartWithParent"
        private const val KEY_START_DATE = "startDate"
        private const val KEY_PATTERN_STARTS_WITH_PARENT = "patternStartsWithParent"
        private const val KEY_PATTERN_APPLICATION_MODE = "patternApplicationMode"
        private const val KEY_CHANGE_DAY_OF_WEEK = "changeDayOfWeek"
        private const val KEY_EVEN_YEAR_STARTS_WITH = "evenYearStartsWith"
        private const val KEY_ODD_YEAR_STARTS_WITH = "oddYearStartsWith"
        private const val KEY_CURRENT_YEAR_MONTH = "currentYearMonth"
        private const val KEY_SUMMER_DIVISION = "summerDivision"
        private const val KEY_PATTERN_CHANGES_COUNT = "patternChangesCount"
        private const val KEY_NO_CUSTODY_PERIODS_COUNT = "noCustodyPeriodsCount"
        private const val KEY_SPECIAL_DATES_COUNT = "specialDatesCount"
        private const val KEY_SUMMER_EVENTS_COUNT = "summerEventsCount"
        private const val KEY_HAS_CONFIGURATION = "hasConfiguration"

        // Valores por defecto
        private const val DEFAULT_PARENT1_NAME = "Custodio 1"
        private const val DEFAULT_PARENT2_NAME = "Custodio 2"
        private const val DEFAULT_PATTERN_STARTS_WITH = 1
        private const val DEFAULT_CHANGE_DAY_OF_WEEK = 1
        private const val DEFAULT_APPLICATION_MODE = "FORWARD"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveConfiguration(viewModel: CustodyViewModel) {
        prefs.edit().apply {
            // Nombres de padres
            putString(KEY_PARENT1_NAME, viewModel.parent1Name)
            putString(KEY_PARENT2_NAME, viewModel.parent2Name)

            // Patrón de custodia
            savePatternConfiguration(this, viewModel)

            // Configuración de inicio del patrón
            savePatternStartConfiguration(this, viewModel)

            // Alternancia por años (solo para verano y leyendas)
            putInt(KEY_EVEN_YEAR_STARTS_WITH, viewModel.evenYearStartsWith)
            putInt(KEY_ODD_YEAR_STARTS_WITH, viewModel.oddYearStartsWith)

            // Mes actual
            putString(KEY_CURRENT_YEAR_MONTH, viewModel.currentYearMonth.toString())

            // Configuración de verano
            putString(KEY_SUMMER_DIVISION, viewModel.summerDivision.name)

            // Guardar cambios de patrón
            savePatternChanges(this, viewModel)

            // Guardar períodos sin custodia
            saveNoCustodyPeriods(this, viewModel)

            // Guardar eventos especiales y de verano
            saveSpecialDates(this, viewModel)
            saveSummerEvents(this, viewModel)

            putBoolean(KEY_HAS_CONFIGURATION, true)
            apply()
        }
    }

    private fun savePatternConfiguration(editor: SharedPreferences.Editor, viewModel: CustodyViewModel) {
        editor.apply {
            putInt(KEY_CUSTODY_PATTERN_POSITION, when (viewModel.custodyPattern) {
                is AlternateWeeks -> 0
                is AlternateDays -> 1
                is WeekdaysWeekends -> 2
                is CustomDaysPattern -> 3
            })

            when (val pattern = viewModel.custodyPattern) {
                is AlternateWeeks -> putInt(KEY_START_WITH_PARENT, pattern.startWithParent)
                is AlternateDays -> putInt(KEY_START_WITH_PARENT, pattern.startWithParent)
                is WeekdaysWeekends -> {
                    putInt(KEY_WEEKDAYS_PARENT, pattern.weekdaysParent)
                    putInt(KEY_WEEKENDS_PARENT, pattern.weekendsParent)
                }
                is CustomDaysPattern -> {
                    putInt(KEY_CUSTOM_DAYS_PARENT1, pattern.daysForParent1)
                    putInt(KEY_CUSTOM_DAYS_PARENT2, pattern.daysForParent2)
                    putInt(KEY_CUSTOM_START_WITH_PARENT, pattern.startWithParent)
                }
            }
        }
    }

    private fun savePatternStartConfiguration(editor: SharedPreferences.Editor, viewModel: CustodyViewModel) {
        editor.apply {
            putString(KEY_START_DATE, viewModel.startDate.toString())
            putInt(KEY_PATTERN_STARTS_WITH_PARENT, viewModel.patternStartsWithParent)
            putString(KEY_PATTERN_APPLICATION_MODE, viewModel.patternApplicationMode)
            putInt(KEY_CHANGE_DAY_OF_WEEK, viewModel.changeDayOfWeek)
        }
    }

    private fun savePatternChanges(editor: SharedPreferences.Editor, viewModel: CustodyViewModel) {
        editor.putInt(KEY_PATTERN_CHANGES_COUNT, viewModel.patternChanges.size)

        viewModel.patternChanges.forEachIndexed { index, change ->
            val prefix = "patternChange_${index}_"
            editor.putString("${prefix}startDate", change.startDate.toString())
            editor.putInt("${prefix}patternType", when (change.pattern) {
                is AlternateWeeks -> 0
                is AlternateDays -> 1
                is WeekdaysWeekends -> 2
                is CustomDaysPattern -> 3
            })

            when (val p = change.pattern) {
                is AlternateWeeks -> editor.putInt("${prefix}startWithParent", p.startWithParent)
                is AlternateDays -> editor.putInt("${prefix}startWithParent", p.startWithParent)
                is WeekdaysWeekends -> {
                    editor.putInt("${prefix}weekdaysParent", p.weekdaysParent)
                    editor.putInt("${prefix}weekendsParent", p.weekendsParent)
                }
                is CustomDaysPattern -> {
                    editor.putInt("${prefix}customDaysParent1", p.daysForParent1)
                    editor.putInt("${prefix}customDaysParent2", p.daysForParent2)
                    editor.putInt("${prefix}customStartWithParent", p.startWithParent)
                }
            }

            editor.putInt("${prefix}changeDayOfWeek", change.changeDayOfWeek)
            editor.putInt("${prefix}startsWithParent", change.startsWithParent)
            editor.putString("${prefix}description", change.description)
        }
    }

    private fun saveNoCustodyPeriods(editor: SharedPreferences.Editor, viewModel: CustodyViewModel) {
        editor.putInt(KEY_NO_CUSTODY_PERIODS_COUNT, viewModel.noCustodyPeriods.size)

        viewModel.noCustodyPeriods.forEachIndexed { index, period ->
            val prefix = "noCustodyPeriod_${index}_"
            editor.putString("${prefix}start", period.startDate.toString())
            editor.putString("${prefix}end", period.endDate.toString())
            editor.putString("${prefix}desc", period.description)
        }
    }

    private fun saveSpecialDates(editor: SharedPreferences.Editor, viewModel: CustodyViewModel) {
        editor.putInt(KEY_SPECIAL_DATES_COUNT, viewModel.specialDates.size)

        viewModel.specialDates.forEachIndexed { index, date ->
            val prefix = "specialDate_${index}_"
            editor.putString("${prefix}date", date.date.toString())
            editor.putString("${prefix}parent", date.parent.name)
            editor.putString("${prefix}desc", date.description)
        }
    }

    private fun saveSummerEvents(editor: SharedPreferences.Editor, viewModel: CustodyViewModel) {
        editor.putInt(KEY_SUMMER_EVENTS_COUNT, viewModel.summerEvents.size)

        viewModel.summerEvents.forEachIndexed { index, event ->
            val prefix = "summerEvent_${index}_"
            editor.putString("${prefix}start", event.startDate.toString())
            editor.putString("${prefix}end", event.endDate.toString())
            editor.putString("${prefix}parent", event.parent.name)
            editor.putString("${prefix}desc", event.description)
        }
    }

    fun loadConfiguration(viewModel: CustodyViewModel) {
        try {
            // Nombres de padres
            viewModel.parent1Name = prefs.getString(KEY_PARENT1_NAME, DEFAULT_PARENT1_NAME) ?: DEFAULT_PARENT1_NAME
            viewModel.parent2Name = prefs.getString(KEY_PARENT2_NAME, DEFAULT_PARENT2_NAME) ?: DEFAULT_PARENT2_NAME

            // Patrón de custodia
            loadPatternConfiguration(viewModel)

            // Configuración de inicio del patrón
            loadPatternStartConfiguration(viewModel)

            // Alternancia por años
            viewModel.evenYearStartsWith = prefs.getInt(KEY_EVEN_YEAR_STARTS_WITH, 1)
            viewModel.oddYearStartsWith = prefs.getInt(KEY_ODD_YEAR_STARTS_WITH, 2)

            // Mes actual
            viewModel.currentYearMonth = parseYearMonthSafely(
                prefs.getString(KEY_CURRENT_YEAR_MONTH, YearMonth.now().toString())
            )

            // Configuración de verano
            viewModel.summerDivision = parseEnumSafely<VacationDivision>(
                prefs.getString(KEY_SUMMER_DIVISION, VacationDivision.HALF.name)
            ) ?: VacationDivision.HALF

            // Cargar cambios de patrón
            loadPatternChanges(viewModel)

            // Cargar períodos sin custodia
            loadNoCustodyPeriods(viewModel)

            // Cargar eventos especiales y de verano
            loadSpecialDates(viewModel)
            loadSummerEvents(viewModel)

        } catch (e: Exception) {
            Log.e(TAG, "Error loading configuration", e)
        }
    }

    private fun loadPatternConfiguration(viewModel: CustodyViewModel) {
        val patternPos = prefs.getInt(KEY_CUSTODY_PATTERN_POSITION, 0)
        val startWithParent = prefs.getInt(KEY_START_WITH_PARENT, 1)
        val weekdaysParent = prefs.getInt(KEY_WEEKDAYS_PARENT, 1)
        val weekendsParent = prefs.getInt(KEY_WEEKENDS_PARENT, 2)
        val customDaysParent1 = prefs.getInt(KEY_CUSTOM_DAYS_PARENT1, 7)
        val customDaysParent2 = prefs.getInt(KEY_CUSTOM_DAYS_PARENT2, 7)
        val customStartWithParent = prefs.getInt(KEY_CUSTOM_START_WITH_PARENT, 1)

        viewModel.custodyPattern = when (patternPos) {
            0 -> AlternateWeeks(startWithParent = startWithParent)
            1 -> AlternateDays(startWithParent = startWithParent)
            2 -> WeekdaysWeekends(
                weekdaysParent = weekdaysParent,
                weekendsParent = weekendsParent
            )
            3 -> CustomDaysPattern(
                daysForParent1 = customDaysParent1,
                daysForParent2 = customDaysParent2,
                startWithParent = customStartWithParent
            )
            else -> AlternateWeeks(startWithParent = 1)
        }
    }

    private fun loadPatternStartConfiguration(viewModel: CustodyViewModel) {
        viewModel.startDate = parseLocalDateSafely(
            prefs.getString(KEY_START_DATE, null)
        )
        viewModel.patternStartsWithParent = prefs.getInt(KEY_PATTERN_STARTS_WITH_PARENT, DEFAULT_PATTERN_STARTS_WITH)
        viewModel.patternApplicationMode = prefs.getString(KEY_PATTERN_APPLICATION_MODE, DEFAULT_APPLICATION_MODE)
            ?: DEFAULT_APPLICATION_MODE
        viewModel.changeDayOfWeek = prefs.getInt(KEY_CHANGE_DAY_OF_WEEK, DEFAULT_CHANGE_DAY_OF_WEEK)
    }

    private fun loadPatternChanges(viewModel: CustodyViewModel) {
        val count = prefs.getInt(KEY_PATTERN_CHANGES_COUNT, 0)
        viewModel.patternChanges.clear()

        for (i in 0 until count) {
            try {
                val prefix = "patternChange_${i}_"
                parseLocalDateSafely(prefs.getString("${prefix}startDate", null))
                val patternType = prefs.getInt("${prefix}patternType", 0)

                when (patternType) {
                    0 -> AlternateWeeks(startWithParent = prefs.getInt("${prefix}startWithParent", 1))
                    1 -> AlternateDays(startWithParent = prefs.getInt("${prefix}startWithParent", 1))
                    2 -> WeekdaysWeekends(
                        weekdaysParent = prefs.getInt("${prefix}weekdaysParent", 1),
                        weekendsParent = prefs.getInt("${prefix}weekendsParent", 2)
                    )
                    3 -> CustomDaysPattern(
                        daysForParent1 = prefs.getInt("${prefix}customDaysParent1", 7),
                        daysForParent2 = prefs.getInt("${prefix}customDaysParent2", 7),
                        startWithParent = prefs.getInt("${prefix}customStartWithParent", 1)
                    )
                    else -> null
                }

                // Aquí necesitarías crear el objeto PatternChange apropiado
                // viewModel.patternChanges.add(PatternChange(...))
            } catch (e: Exception) {
                Log.e(TAG, "Error loading pattern change $i", e)
            }
        }
    }

    private fun loadNoCustodyPeriods(viewModel: CustodyViewModel) {
        val count = prefs.getInt(KEY_NO_CUSTODY_PERIODS_COUNT, 0)
        viewModel.noCustodyPeriods.clear()

        for (i in 0 until count) {
            try {
                val prefix = "noCustodyPeriod_${i}_"
                val start = parseLocalDateSafely(prefs.getString("${prefix}start", null))
                val end = parseLocalDateSafely(prefs.getString("${prefix}end", null))
                val desc = prefs.getString("${prefix}desc", "") ?: ""

                viewModel.noCustodyPeriods.add(NoCustodyPeriod(start, end, desc))
            } catch (e: Exception) {
                Log.e(TAG, "Error loading no custody period $i", e)
            }
        }
    }

    private fun loadSpecialDates(viewModel: CustodyViewModel) {
        val count = prefs.getInt(KEY_SPECIAL_DATES_COUNT, 0)
        viewModel.specialDates.clear()

        for (i in 0 until count) {
            try {
                val prefix = "specialDate_${i}_"
                val date = parseLocalDateSafely(prefs.getString("${prefix}date", null))
                val parent = parseEnumSafely<ParentType>(prefs.getString("${prefix}parent", null)) ?: continue
                val desc = prefs.getString("${prefix}desc", "") ?: ""

                viewModel.specialDates.add(SpecialDate(date, parent, desc))
            } catch (e: Exception) {
                Log.e(TAG, "Error loading special date $i", e)
            }
        }
    }

    private fun loadSummerEvents(viewModel: CustodyViewModel) {
        val count = prefs.getInt(KEY_SUMMER_EVENTS_COUNT, 0)
        viewModel.summerEvents.clear()

        for (i in 0 until count) {
            try {
                val prefix = "summerEvent_${i}_"
                val start = parseLocalDateSafely(prefs.getString("${prefix}start", null))
                val end = parseLocalDateSafely(prefs.getString("${prefix}end", null))
                val parent = parseEnumSafely<ParentType>(prefs.getString("${prefix}parent", null)) ?: continue
                val desc = prefs.getString("${prefix}desc", "") ?: ""

                viewModel.summerEvents.add(SummerEvent(start, end, parent, desc))
            } catch (e: Exception) {
                Log.e(TAG, "Error loading summer event $i", e)
            }
        }
    }

    fun hasConfiguration(): Boolean = prefs.getBoolean(KEY_HAS_CONFIGURATION, false)

    // Funciones de utilidad para parsing seguro
    private fun parseLocalDateSafely(dateString: String?): LocalDate {
        return try {
            if (dateString != null) LocalDate.parse(dateString) else LocalDate.now()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing LocalDate: $dateString", e)
            LocalDate.now()
        }
    }

    private fun parseYearMonthSafely(yearMonthString: String?): YearMonth {
        return try {
            YearMonth.parse(yearMonthString ?: YearMonth.now().toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing YearMonth: $yearMonthString", e)
            YearMonth.now()
        }
    }

    private inline fun <reified T : Enum<T>> parseEnumSafely(enumString: String?): T? {
        return try {
            enumString?.let { enumValueOf<T>(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing enum ${T::class.simpleName}: $enumString", e)
            null
        }
    }
    fun getParent1Name(): String {
        return prefs.getString(KEY_PARENT1_NAME, DEFAULT_PARENT1_NAME) ?: DEFAULT_PARENT1_NAME
    }

    fun getParent2Name(): String {
        return prefs.getString(KEY_PARENT2_NAME, DEFAULT_PARENT2_NAME) ?: DEFAULT_PARENT2_NAME
    }

    // ========== FUNCIONES PREMIUM ==========

    private val KEY_IS_PREMIUM = "is_premium"
    private val KEY_INSTALL_DATE = "install_date"
    private val KEY_TRIAL_DAYS = 30

    /**
     * Verifica si el usuario tiene la versión Premium
     */
    fun isPremium(): Boolean {
        return prefs.getBoolean(KEY_IS_PREMIUM, false)
    }

    /**
     * Activa o desactiva la versión Premium
     */
    fun setPremium(isPremium: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply()
        Log.d(TAG, "Premium status changed: $isPremium")
    }

    /**
     * Obtiene la fecha de instalación de la app
     * Si no existe, la crea con la fecha actual
     */
    /**
     * Obtiene la fecha de instalación de la app
     * Si no existe, la crea con la fecha actual
     */
    fun getInstallDate(): LocalDate {
        val dateString = prefs.getString(KEY_INSTALL_DATE, null)

        return if (dateString != null) {
            try {
                LocalDate.parse(dateString)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing install date: $dateString", e)
                val now = LocalDate.now()
                setInstallDate(now)
                now
            }
        } else {
            // Primera vez que se ejecuta la app
            val now = LocalDate.now()
            setInstallDate(now)
            setPremium(false)  // Asegurar que Premium está desactivado en primera instalación
            Log.d(TAG, "Install date set to: $now - Premium set to false")
            now
        }
    }


    /**
     * Establece la fecha de instalación (solo se debe usar internamente)
     */
    private fun setInstallDate(date: LocalDate) {
        prefs.edit().putString(KEY_INSTALL_DATE, date.toString()).apply()
    }

    /**
     * Calcula los días restantes del periodo de prueba
     * @return Días restantes (0 si ya expiró, -1 si es Premium)
     */
    fun getTrialDaysRemaining(): Int {
        if (isPremium()) {
            return -1 // Es Premium, no hay límite
        }

        val installDate = getInstallDate()
        val today = LocalDate.now()
        val daysSinceInstall = java.time.temporal.ChronoUnit.DAYS.between(installDate, today).toInt()
        val remaining = KEY_TRIAL_DAYS - daysSinceInstall

        Log.d(TAG, "Trial days: installed on $installDate, days since: $daysSinceInstall, remaining: $remaining")

        return maxOf(0, remaining) // No devolver negativos
    }

    /**
     * Verifica si el periodo de prueba ha expirado
     * @return true si expiró y NO es Premium
     */
    fun isTrialExpired(): Boolean {
        return !isPremium() && getTrialDaysRemaining() == 0
    }

    /**
     * Para testing: resetear fecha de instalación (solo modo debug)
     */
    fun resetInstallDateForTesting() {
        setInstallDate(LocalDate.now())
        Log.d(TAG, "Install date reset for testing")
    }

    /**
     * Para testing: simular que han pasado X días desde instalación
     */
    fun simulateDaysPassedForTesting(days: Int) {
        val fakeInstallDate = LocalDate.now().minusDays(days.toLong())
        setInstallDate(fakeInstallDate)
        Log.d(TAG, "Simulated install date: $fakeInstallDate ($days days ago)")
    }

}
