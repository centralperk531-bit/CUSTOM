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
                is CustomDaysPattern -> 3
                else -> 0
            })

            // Guardar parámetros específicos del patrón
            when (val pattern = viewModel.custodyPattern) {
                is AlternateWeeks -> putInt("startWithParent", pattern.startWithParent)
                is AlternateDays -> putInt("startWithParent", pattern.startWithParent)
                is WeekdaysWeekends -> {
                    putInt("weekdaysParent", pattern.weekdaysParent)
                    putInt("weekendsParent", pattern.weekendsParent)
                }
                is CustomDaysPattern -> {
                    putInt("customDaysParent1", pattern.daysForParent1)
                    putInt("customDaysParent2", pattern.daysForParent2)
                    putInt("customStartWithParent", pattern.startWithParent)
                }
                else -> {}
            }

            // NUEVA LÓGICA: Configuración de inicio del patrón
            putString("startDate", viewModel.startDate.toString())
            putInt("patternStartsWithParent", viewModel.patternStartsWithParent)
            putString("patternApplicationMode", viewModel.patternApplicationMode)
            putInt("changeDayOfWeek", viewModel.changeDayOfWeek)

            // Alternancia por años (solo para verano y leyendas)
            putInt("evenYearStartsWith", viewModel.evenYearStartsWith)
            putInt("oddYearStartsWith", viewModel.oddYearStartsWith)

            // Mes actual
            putString("currentYearMonth", viewModel.currentYearMonth.toString())

            // Configuración de verano (solo división)
            putString("summerDivision", viewModel.summerDivision.name)

            // NUEVO: Guardar cambios de patrón
            putInt("patternChangesCount", viewModel.patternChanges.size)
            viewModel.patternChanges.forEachIndexed { index, change ->
                putString("patternChange_${index}_startDate", change.startDate.toString())
                putInt("patternChange_${index}_patternType", when (change.pattern) {
                    is AlternateWeeks -> 0
                    is AlternateDays -> 1
                    is WeekdaysWeekends -> 2
                    is CustomDaysPattern -> 3
                    else -> 0
                })
                // Guardar parámetros del patrón
                when (val p = change.pattern) {
                    is AlternateWeeks -> putInt("patternChange_${index}_startWithParent", p.startWithParent)
                    is AlternateDays -> putInt("patternChange_${index}_startWithParent", p.startWithParent)
                    is WeekdaysWeekends -> {
                        putInt("patternChange_${index}_weekdaysParent", p.weekdaysParent)
                        putInt("patternChange_${index}_weekendsParent", p.weekendsParent)
                    }
                    is CustomDaysPattern -> {
                        putInt("patternChange_${index}_customDaysParent1", p.daysForParent1)
                        putInt("patternChange_${index}_customDaysParent2", p.daysForParent2)
                        putInt("patternChange_${index}_customStartWithParent", p.startWithParent)
                    }
                    else -> {}
                }
                putInt("patternChange_${index}_changeDayOfWeek", change.changeDayOfWeek)
                putInt("patternChange_${index}_startsWithParent", change.startsWithParent)
                putString("patternChange_${index}_description", change.description)
            }

            // Mantener configuración antigua por compatibilidad (pero ya no se usa)
            @Suppress("DEPRECATION")
            putString("christmasPeriod1Start", viewModel.christmasPeriod1Start.toString())
            @Suppress("DEPRECATION")
            putString("christmasPeriod1End", viewModel.christmasPeriod1End.toString())
            @Suppress("DEPRECATION")
            putString("christmasPeriod1FirstParent", viewModel.christmasPeriod1FirstParent.name)
            @Suppress("DEPRECATION")
            putString("christmasPeriod1YearRule", viewModel.christmasPeriod1YearRule.name)
            @Suppress("DEPRECATION")
            putString("christmasPeriod2Start", viewModel.christmasPeriod2Start.toString())
            @Suppress("DEPRECATION")
            putString("christmasPeriod2End", viewModel.christmasPeriod2End.toString())
            @Suppress("DEPRECATION")
            putString("christmasPeriod2FirstParent", viewModel.christmasPeriod2FirstParent.name)
            @Suppress("DEPRECATION")
            putString("christmasPeriod2YearRule", viewModel.christmasPeriod2YearRule.name)
            @Suppress("DEPRECATION")
            putString("christmasFirstParent", viewModel.christmasFirstParent.name)
            @Suppress("DEPRECATION")
            putString("christmasDivision", viewModel.christmasDivision.name)
            @Suppress("DEPRECATION")
            putString("christmasYearRule", viewModel.christmasYearRule.name)
            @Suppress("DEPRECATION")
            putString("christmasStart", viewModel.christmasStart.toString())
            @Suppress("DEPRECATION")
            putString("christmasEnd", viewModel.christmasEnd.toString())

            // Configuración de Semana Santa (mantener por compatibilidad)
            @Suppress("DEPRECATION")
            putString("easterFirstParent", viewModel.easterFirstParent.name)
            @Suppress("DEPRECATION")
            putString("easterDivision", viewModel.easterDivision.name)
            @Suppress("DEPRECATION")
            putString("easterYearRule", viewModel.easterYearRule.name)
            @Suppress("DEPRECATION")
            putString("easterStart", viewModel.easterStart.toString())
            @Suppress("DEPRECATION")
            putString("easterEnd", viewModel.easterEnd.toString())
            @Suppress("DEPRECATION")
            putBoolean("easterDisabled", viewModel.easterDisabled)

            // Guardar períodos sin custodia
            putInt("noCustodyPeriodsCount", viewModel.noCustodyPeriods.size)
            viewModel.noCustodyPeriods.forEachIndexed { index, period ->
                putString("noCustodyPeriod_${index}_start", period.startDate.toString())
                putString("noCustodyPeriod_${index}_end", period.endDate.toString())
                putString("noCustodyPeriod_${index}_desc", period.description)
            }

            // Guardar eventos especiales y de verano
            putInt("specialDatesCount", viewModel.specialDates.size)
            viewModel.specialDates.forEachIndexed { index, date ->
                putString("specialDate_${index}_date", date.date.toString())
                putString("specialDate_${index}_parent", date.parent.name)
                putString("specialDate_${index}_desc", date.description)
            }

            putInt("summerEventsCount", viewModel.summerEvents.size)
            viewModel.summerEvents.forEachIndexed { index, event ->
                putString("summerEvent_${index}_start", event.startDate.toString())
                putString("summerEvent_${index}_end", event.endDate.toString())
                putString("summerEvent_${index}_parent", event.parent.name)
                putString("summerEvent_${index}_desc", event.description)
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
        val customDaysParent1 = prefs.getInt("customDaysParent1", 7)
        val customDaysParent2 = prefs.getInt("customDaysParent2", 7)
        val customStartWithParent = prefs.getInt("customStartWithParent", 1)

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

        // NUEVA LÓGICA: Configuración de inicio del patrón
        val startDateStr = prefs.getString("startDate", LocalDate.now().toString())
            ?: LocalDate.now().toString()
        viewModel.startDate = try {
            LocalDate.parse(startDateStr)
        } catch (e: Exception) {
            LocalDate.now()
        }
        viewModel.patternStartsWithParent = prefs.getInt("patternStartsWithParent", 1)
        viewModel.patternApplicationMode = prefs.getString("patternApplicationMode", "FORWARD")
            ?: "FORWARD"
        viewModel.changeDayOfWeek = prefs.getInt("changeDayOfWeek", 1)

        // Cargar alternancia por años (solo para verano y leyendas)
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

        // Configuración de verano (solo división)
        viewModel.summerDivision = try {
            VacationDivision.valueOf(prefs.getString("summerDivision", "HALF") ?: "HALF")
        } catch (e: Exception) {
            VacationDivision.HALF
        }

        // NUEVO: Cargar cambios de patrón
        val patternChangesCount = prefs.getInt("patternChangesCount", 0)
        viewModel.patternChanges.clear()
        for (i in 0 until patternChangesCount) {
            val startDateStr = prefs.getString("patternChange_${i}_startDate", null)
            val patternType = prefs.getInt("patternChange_${i}_patternType", 0)
            val changeDayOfWeek = prefs.getInt("patternChange_${i}_changeDayOfWeek", 1)
            val startsWithParent = prefs.getInt("patternChange_${i}_startsWithParent", 1)
            val description = prefs.getString("patternChange_${i}_description", "") ?: ""

            if (startDateStr != null) {
                try {
                    val pattern: CustodyPattern = when (patternType) {
                        0 -> {
                            val swp = prefs.getInt("patternChange_${i}_startWithParent", 1)
                            AlternateWeeks(startWithParent = swp)
                        }
                        1 -> {
                            val swp = prefs.getInt("patternChange_${i}_startWithParent", 1)
                            AlternateDays(startWithParent = swp)
                        }
                        2 -> {
                            val wdp = prefs.getInt("patternChange_${i}_weekdaysParent", 1)
                            val wep = prefs.getInt("patternChange_${i}_weekendsParent", 2)
                            WeekdaysWeekends(weekdaysParent = wdp, weekendsParent = wep)
                        }
                        3 -> {
                            val cdp1 = prefs.getInt("patternChange_${i}_customDaysParent1", 7)
                            val cdp2 = prefs.getInt("patternChange_${i}_customDaysParent2", 7)
                            val cswp = prefs.getInt("patternChange_${i}_customStartWithParent", 1)
                            CustomDaysPattern(daysForParent1 = cdp1, daysForParent2 = cdp2, startWithParent = cswp)
                        }
                        else -> AlternateWeeks(startWithParent = 1)
                    }

                    val change = MainActivity.PatternChange(
                        startDate = LocalDate.parse(startDateStr),
                        pattern = pattern,
                        changeDayOfWeek = changeDayOfWeek,
                        startsWithParent = startsWithParent,
                        description = description
                    )
                    viewModel.patternChanges.add(change)
                } catch (e: Exception) {
                    // Ignorar cambios con formato inválido
                }
            }
        }

        // Cargar configuración antigua por compatibilidad (pero no se usa en la lógica)
        @Suppress("DEPRECATION")
        run {
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
            viewModel.easterDisabled = prefs.getBoolean("easterDisabled", true)
        }

        // Cargar períodos sin custodia
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

        // Cargar fechas especiales
        val specialDatesCount = prefs.getInt("specialDatesCount", 0)
        viewModel.specialDates.clear()
        for (i in 0 until specialDatesCount) {
            val dateStr = prefs.getString("specialDate_${i}_date", null)
            val parentStr = prefs.getString("specialDate_${i}_parent", null)
            val desc = prefs.getString("specialDate_${i}_desc", "")

            if (dateStr != null && parentStr != null) {
                try {
                    val date = SpecialDate(
                        date = LocalDate.parse(dateStr),
                        parent = ParentType.valueOf(parentStr),
                        description = desc ?: ""
                    )
                    viewModel.specialDates.add(date)
                } catch (e: Exception) {
                    // Ignorar fechas con formato inválido
                }
            }
        }

        // Cargar eventos de verano (incluye Navidad y Semana Santa configurados manualmente)
        val summerEventsCount = prefs.getInt("summerEventsCount", 0)
        viewModel.summerEvents.clear()
        for (i in 0 until summerEventsCount) {
            val startStr = prefs.getString("summerEvent_${i}_start", null)
            val endStr = prefs.getString("summerEvent_${i}_end", null)
            val parentStr = prefs.getString("summerEvent_${i}_parent", null)
            val desc = prefs.getString("summerEvent_${i}_desc", "")

            if (startStr != null && endStr != null && parentStr != null) {
                try {
                    val event = SummerEvent(
                        startDate = LocalDate.parse(startStr),
                        endDate = LocalDate.parse(endStr),
                        parent = ParentType.valueOf(parentStr),
                        description = desc ?: ""
                    )
                    viewModel.summerEvents.add(event)
                } catch (e: Exception) {
                    // Ignorar eventos con formato inválido
                }
            }
        }
    }

    fun hasConfiguration(): Boolean {
        return prefs.getBoolean("hasConfiguration", false)
    }
}