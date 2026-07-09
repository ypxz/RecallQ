package com.recalldeck.app.data.backup

import androidx.room.withTransaction
import com.recalldeck.app.data.db.RecallDeckDatabase
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Versioned JSON export/import of all tables. Restore is replace-all:
 * every existing row is deleted before the backup rows (with their
 * original ids, preserving all FSRS scheduling state) are inserted.
 */
class BackupManager(private val database: RecallDeckDatabase) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportToJson(now: Long = System.currentTimeMillis()): String {
        val backup = database.withTransaction {
            BackupFile(
                schemaVersion = BACKUP_SCHEMA_VERSION,
                exportedAt = now,
                subjects = database.subjectDao().getAll().map { it.toBackup() },
                categories = database.categoryDao().getAll().map { it.toBackup() },
                cards = database.cardDao().getAll().map { it.toBackup() },
                reviewLogs = database.reviewLogDao().getAll().map { it.toBackup() },
            )
        }
        return json.encodeToString(BackupFile.serializer(), backup)
    }

    suspend fun restoreFromJson(content: String) {
        val backup = try {
            json.decodeFromString(BackupFile.serializer(), content)
        } catch (e: SerializationException) {
            throw BackupFormatException("Not a valid RecallDeck backup file", e)
        } catch (e: IllegalArgumentException) {
            throw BackupFormatException("Not a valid RecallDeck backup file", e)
        }
        if (backup.schemaVersion > BACKUP_SCHEMA_VERSION) {
            throw BackupFormatException(
                "Backup schema version ${backup.schemaVersion} is newer than supported " +
                    "version $BACKUP_SCHEMA_VERSION",
            )
        }
        database.withTransaction {
            database.reviewLogDao().deleteAll()
            database.cardDao().deleteAll()
            database.categoryDao().deleteAll()
            database.subjectDao().deleteAll()

            database.subjectDao().insertAll(backup.subjects.map { it.toEntity() })
            database.categoryDao().insertAll(backup.categories.map { it.toEntity() })
            database.cardDao().insertAll(backup.cards.map { it.toEntity() })
            database.reviewLogDao().insertAll(backup.reviewLogs.map { it.toEntity() })
        }
    }
}

class BackupFormatException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
