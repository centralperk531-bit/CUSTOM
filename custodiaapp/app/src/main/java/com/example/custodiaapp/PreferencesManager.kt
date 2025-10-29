package com.example.custodiaapp

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.YearMonth

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "custody_preferences",
        Context.MODE_PRIVATE
    )

    fun saveConfiguration(viewModel: CustodyViewModel) {
        prefs.edit().apply {
            // Nombres de padres
            putString("parent1Name", viewModel.parent1Name)
            putString("parent2Name", viewModel.parent2Name)

            // Patrón de custodia
            putInt("custodyPatternPosition", when (viewModel.custodyPattern) {
                is AlternateWeeks -> 0
                is AlternateDays -> 1
                is WeekdaysWeekends -> 2
                is CustomPattern -> 3
            })

            // Guardar parámetros específicos del patrón
            when (val pattern = viewModel.custodyPattern) {
                is AlternateWeeks -> putInt("startWithParent", pattern.startWithParent)
                is AlternateDays -> putInt("startWithParent", pattern.startWithParent)
                is WeekdaysWeekends -> {
                    putInt("weekdaysParent", pattern.weekdaysParent)
                    putInt("weekendsParent", pattern.weekendsParent)
                }
                is CustomPattern -> {} // No hay parámetros adicionales
            }

            // Configuración de inicio
            putString("startDate", viewModel.startDate.toString())
            putInt("changeDayOfWeek", viewModel.changeDayOfWeek)

            // NUEVO: Alternancia por años
            putInt("evenYearStartsWith", viewModel.evenYearStartsWith)
            putInt("oddYearStartsWith", viewModel.oddYearStartsWith)

            // Mes actual
            putString("currentYearMonth", viewModel.currentYearMonth.toString())

            // Configuración de verano (ya modificado para julio-agosto)
            putString("summerFirstParent", viewModel.summerFirstParent.name)
            putString("summerDivision", viewModel.summerDivision.name)
            putString("summerYearRule", viewModel.summerYearRule.name)

            // MODIFICADO: Configuración de Navidad - 2 PERÍODOS
            // Período 1
            putString("christmasPeriod1Start", viewModel.christmasPeriod1Start.toString())
            putString("christmasPeriod1End", viewModel.christmasPeriod1End.toString())
            putString("christmasPeriod1FirstParent", viewModel.christmasPeriod1FirstParent.name)
            putString("christmasPeriod1YearRule", viewModel.christmasPeriod1YearRule.name)

            // Período 2
            putString("christmasPeriod2Start", viewModel.christmasPeriod2Start.toString())
            putString("christmasPeriod2End", viewModel.christmasPeriod2End.toString())
            putString("christmasPeriod2FirstParent", viewModel.christmasPeriod2FirstParent.name)
            putString("christmasPeriod2YearRule", viewModel.christmasPeriod2YearRule.name)

            // Mantener configuración antigua por compatibilidad
            putString("christmasFirstParent", viewModel.christmasFirstParent.name)
            putString("christmasDivision", viewModel.christmasDivision.name)
            putString("christmasYearRule", viewModel.christmasYearRule.name)
            putString("christmasStart", viewModel.christmasStart.toString())
            putString("christmasEnd", viewModel.christmasEnd.toString())

            // Configuración de Semana Santa
            putString("easterFirstParent", viewModel.easterFirstParent.name)
            putString("easterDivision", viewModel.easterDivision.name)
            putString("easterYearRule", viewModel.easterYearRule.name)
            putString("easterStart", viewModel.easterStart.toString())
            putString("easterEnd", viewModel.easterEnd.toString())
            putBoolean("easterDisabled", viewModel.easterDisabled)

            // NUEVO: Guardar períodos sin custodia
            putInt("noCustodyPeriodsCount", viewModel.noCustodyPeriods.size)
            viewModel.noCustodyPeriods.forEachIndexed { index, period ->
                putString("noCustodyPeriod_${index}_start", period.startDate.toString())
                putString("noCustodyPeriod_${index}_end", period.endDate.toString())
                putString("noCustodyPeriod_${index}_desc", period.description)
            }

            putBoolean("hasConfiguration", true)
            apply()
        }
    }

    fun loadConfiguration(viewModel: CustodyViewModel) {
        // Nombres de padres
        viewModel.parent1Name = prefs.getString("parent1Name", "Custodio 1") ?: "Custodio 1"
        viewModel.parent2Name = prefs.getString("parent2Name", "Custodio 2") ?: "Custodio 2"

        // Patrón de custodia
        val patternPos = prefs.getInt("custodyPatternPosition", 0)
        val startWithParent = prefs.getInt("startWithParent", 1)
        val weekdaysParent = prefs.getInt("weekdaysParent", 1)
        val weekendsParent = prefs.getInt("weekendsParent", 2)

        viewModel.custodyPattern = when (patternPos) {
            0 -> AlternateWeeks(startWithParent = startWithParent)
            1 -> AlternateDays(startWithParent = startWithParent)
            2 -> WeekdaysWeekends(
                weekdaysParent = weekdaysParent,
                weekendsParent = weekendsParent
            )
            3 -> CustomPattern()
            else -> AlternateWeeks(startWithParent = 1)
        }

        // Configuración de inicio
        val startDateStr = prefs.getString("startDate", "2024-01-01") ?: "2024-01-01"
        viewModel.startDate = try {
            LocalDate.parse(startDateStr)
        } catch (e: Exception) {
            LocalDate.of(2024, 1, 1)
        }
        viewModel.changeDayOfWeek = prefs.getInt("changeDayOfWeek", 1)

        // NUEVO: Cargar alternancia por años
        viewModel.evenYearStartsWith = prefs.getInt("evenYearStartsWith", 1)
        viewModel.oddYearStartsWith = prefs.getInt("oddYearStartsWith", 2)

        // Mes actual
        val yearMonthStr = prefs.getString("currentYearMonth", YearMonth.now().toString())
            ?: YearMonth.now().toString()
        viewModel.currentYearMonth = try {
            YearMonth.parse(yearMonthStr)
        } catch (e: Exception) {
            YearMonth.now()
        }

        // Configuración de verano
        viewModel.summerFirstParent = try {
            ParentType.valueOf(prefs.getString("summerFirstParent", "PARENT1") ?: "PARENT1")
        } catch (e: Exception) {
            ParentType.PARENT1
        }
        viewModel.summerDivision = try {
            VacationDivision.valueOf(prefs.getString("summerDivision", "HALF") ?: "HALF")
        } catch (e: Exception) {
            VacationDivision.HALF
        }
        viewModel.summerYearRule = try {
            YearRule.valueOf(prefs.getString("summerYearRule", "EVEN") ?: "EVEN")
        } catch (e: Exception) {
            YearRule.EVEN
        }

        // MODIFICADO: Cargar configuración de Navidad - 2 PERÍODOS
        // Período 1
        val christmas1StartStr = prefs.getString("christmasPeriod1Start", "2024-12-23") ?: "2024-12-23"
        viewModel.christmasPeriod1Start = try {
            LocalDate.parse(christmas1StartStr)
        } catch (e: Exception) {
            LocalDate.of(2024, 12, 23)
        }
        val christmas1EndStr = prefs.getString("christmasPeriod1End", "2024-12-30") ?: "2024-12-30"
        viewModel.christmasPeriod1End = try {
            LocalDate.parse(christmas1EndStr)
        } catch (e: Exception) {
            LocalDate.of(2024, 12, 30)
        }
        viewModel.christmasPeriod1FirstParent = try {
            ParentType.valueOf(prefs.getString("christmasPeriod1FirstParent", "PARENT1") ?: "PARENT1")
        } catch (e: Exception) {
            ParentType.PARENT1
        }
        viewModel.christmasPeriod1YearRule = try {
            YearRule.valueOf(prefs.getString("christmasPeriod1YearRule", "EVEN") ?: "EVEN")
        } catch (e: Exception) {
            YearRule.EVEN
        }

        // Período 2
        val christmas2StartStr = prefs.getString("christmasPeriod2Start", "2024-12-31") ?: "2024-12-31"
        viewModel.christmasPeriod2Start = try {
            LocalDate.parse(christmas2StartStr)
        } catch (e: Exception) {
            LocalDate.of(2024, 12, 31)
        }
        val christmas2EndStr = prefs.getString("christmasPeriod2End", "2025-01-08") ?: "2025-01-08"
        viewModel.christmasPeriod2End = try {
            LocalDate.parse(christmas2EndStr)
        } catch (e: Exception) {
            LocalDate.of(2025, 1, 8)
        }
        viewModel.christmasPeriod2FirstParent = try {
            ParentType.valueOf(prefs.getString("christmasPeriod2FirstParent", "PARENT2") ?: "PARENT2")
        } catch (e: Exception) {
            ParentType.PARENT2
        }
        viewModel.christmasPeriod2YearRule = try {
            YearRule.valueOf(prefs.getString("christmasPeriod2YearRule", "EVEN") ?: "EVEN")
        } catch (e: Exception) {
            YearRule.EVEN
        }

        // Configuración antigua (mantener por compatibilidad)
        viewModel.christmasFirstParent = try {
            ParentType.valueOf(prefs.getString("christmasFirstParent", "PARENT1") ?: "PARENT1")
        } catch (e: Exception) {
            ParentType.PARENT1
        }
        viewModel.christmasDivision = try {
            VacationDivision.valueOf(prefs.getString("christmasDivision", "HALF") ?: "HALF")
        } catch (e: Exception) {
            VacationDivision.HALF
        }
        viewModel.christmasYearRule = try {
            YearRule.valueOf(prefs.getString("christmasYearRule", "EVEN") ?: "EVEN")
        } catch (e: Exception) {
            YearRule.EVEN
        }
        val christmasStartStr = prefs.getString("christmasStart", "2024-12-23") ?: "2024-12-23"
        viewModel.christmasStart = try {
            LocalDate.parse(christmasStartStr)
        } catch (e: Exception) {
            LocalDate.of(2024, 12, 23)
        }
        val christmasEndStr = prefs.getString("christmasEnd", "2025-01-08") ?: "2025-01-08"
        viewModel.christmasEnd = try {
            LocalDate.parse(christmasEndStr)
        } catch (e: Exception) {
            LocalDate.of(2025, 1, 8)
        }

        // Configuración de Semana Santa
        viewModel.easterFirstParent = try {
            ParentType.valueOf(prefs.getString("easterFirstParent", "PARENT2") ?: "PARENT2")
        } catch (e: Exception) {
            ParentType.PARENT2
        }
        viewModel.easterDivision = try {
            VacationDivision.valueOf(prefs.getString("easterDivision", "HALF") ?: "HALF")
        } catch (e: Exception) {
            VacationDivision.HALF
        }
        viewModel.easterYearRule = try {
            YearRule.valueOf(prefs.getString("easterYearRule", "ODD") ?: "ODD")
        } catch (e: Exception) {
            YearRule.ODD
        }
        val easterStartStr = prefs.getString("easterStart", "2024-03-28") ?: "2024-03-28"
        viewModel.easterStart = try {
            LocalDate.parse(easterStartStr)
        } catch (e: Exception) {
            LocalDate.of(2024, 3, 28)
        }
        val easterEndStr = prefs.getString("easterEnd", "2024-04-01") ?: "2024-04-01"
        viewModel.easterEnd = try {
            LocalDate.parse(easterEndStr)
        } catch (e: Exception) {
            LocalDate.of(2024, 4, 1)
        }
        viewModel.easterDisabled = prefs.getBoolean("easterDisabled", false)

        // NUEVO: Cargar períodos sin custodia
        val noCustodyCount = prefs.getInt("noCustodyPeriodsCount", 0)
        viewModel.noCustodyPeriods.clear()
        for (i in 0 until noCustodyCount) {
            val startStr = prefs.getString("noCustodyPeriod_${i}_start", null)
            val endStr = prefs.getString("noCustodyPeriod_${i}_end", null)
            val desc = prefs.getString("noCustodyPeriod_${i}_desc", "")

            if (startStr != null && endStr != null) {
                try {
                    val period = NoCustodyPeriod(
                        startDate = LocalDate.parse(startStr),
                        endDate = LocalDate.parse(endStr),
                        description = desc ?: ""
                    )
                    viewModel.noCustodyPeriods.add(period)
                } catch (e: Exception) {
                    // Ignorar períodos con formato inválido
                }
            }
        }
    }

    fun hasConfiguration(): Boolean {
        return prefs.getBoolean("hasConfiguration", false)
    }
}