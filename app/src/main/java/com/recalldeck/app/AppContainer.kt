package com.recalldeck.app

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.recalldeck.app.data.backup.BackupManager
import com.recalldeck.app.data.backup.CsvExporter
import com.recalldeck.app.data.db.RecallDeckDatabase
import com.recalldeck.app.data.repo.DeckRepository
import com.recalldeck.app.data.repo.SettingsRepository
import com.recalldeck.app.data.repo.StudyRepository
import com.recalldeck.app.data.stats.StatsRepo
import com.recalldeck.app.importer.PdfTextExtractor

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Manual dependency injection container. Repositories and other app-wide
 * singletons are created lazily here and handed to ViewModels.
 */
class AppContainer(val applicationContext: Context) {

    val database: RecallDeckDatabase by lazy { RecallDeckDatabase.build(applicationContext) }

    val deckRepository: DeckRepository by lazy {
        DeckRepository(database.subjectDao(), database.categoryDao(), database.cardDao())
    }

    val studyRepository: StudyRepository by lazy {
        StudyRepository(database.cardDao(), database.reviewLogDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(applicationContext.dataStore)
    }

    val statsRepo: StatsRepo by lazy {
        StatsRepo(
            database.subjectDao(),
            database.categoryDao(),
            database.cardDao(),
            database.reviewLogDao(),
        )
    }

    val backupManager: BackupManager by lazy { BackupManager(database) }

    val csvExporter: CsvExporter by lazy { CsvExporter(database.cardDao()) }

    val pdfTextExtractor: PdfTextExtractor by lazy { PdfTextExtractor(applicationContext) }
}
