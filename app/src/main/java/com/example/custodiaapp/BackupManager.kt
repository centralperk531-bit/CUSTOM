package com.example.custodiaapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(private val context: Context) {

    companion object {
        private const val BACKUP_FILE_PREFIX = "custodiaapp_backup_"
        private const val BACKUP_FILE_EXTENSION = ".json"
    }

    /**
     * Crea un backup con todos los datos de SharedPreferences
     * @return Result con el archivo creado o error
     */
    fun createBackup(): Result<File> {
        return try {
            val prefs = context.getSharedPreferences("custody_preferences", Context.MODE_PRIVATE)
            val allPrefs = prefs.all

            // Crear objeto JSON con todos los datos
            val jsonBackup = JSONObject()
            jsonBackup.put("app_version", "1.0")
            jsonBackup.put("backup_date", System.currentTimeMillis())

            val dataObject = JSONObject()
            allPrefs.forEach { (key, value) ->
                when (value) {
                    is String -> dataObject.put(key, value)
                    is Int -> dataObject.put(key, value)
                    is Boolean -> dataObject.put(key, value)
                    is Float -> dataObject.put(key, value)
                    is Long -> dataObject.put(key, value)
                    is Set<*> -> dataObject.put(key, value.joinToString(","))
                }
            }
            jsonBackup.put("data", dataObject)

            // Crear archivo
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "$BACKUP_FILE_PREFIX$timestamp$BACKUP_FILE_EXTENSION"
            val backupFile = File(context.getExternalFilesDir(null), fileName)

            backupFile.writeText(jsonBackup.toString(2))

            Result.success(backupFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restaura un backup desde un archivo JSON
     * @param uri URI del archivo seleccionado
     * @return Result con éxito o error
     */
    fun restoreBackup(uri: Uri): Result<Unit> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("No se pudo leer el archivo"))

            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonBackup = JSONObject(jsonString)

            // Validar estructura
            if (!jsonBackup.has("data")) {
                return Result.failure(Exception("Archivo de backup inválido"))
            }

            val dataObject = jsonBackup.getJSONObject("data")
            val prefs = context.getSharedPreferences("custody_preferences", Context.MODE_PRIVATE)
            val editor = prefs.edit()

            // Limpiar preferencias actuales
            editor.clear()

            // Restaurar todos los valores
            val keys = dataObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = dataObject.get(key)

                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is Long -> editor.putLong(key, value)
                }
            }

            editor.apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Comparte el archivo de backup por WhatsApp, Telegram, email, etc.
     * @param file Archivo a compartir
     */
    fun shareBackup(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Backup Mi Turno Family")
            putExtra(Intent.EXTRA_TEXT, "Backup de Mi Turno Family creado el ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Compartir backup"))
    }
}
