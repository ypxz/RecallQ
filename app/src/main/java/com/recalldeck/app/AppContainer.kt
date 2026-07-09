package com.recalldeck.app

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.recalldeck.app.data.db.RecallDeckDatabase
import com.recalldeck.app.data.repo.DeckRepository
import com.recalldeck.app.data.repo.SettingsRepository
import com.recalldeck.app.data.repo.StudyRepository

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Manual dependency injection container. Repositories and other app-wide
 * singletons are created lazily here and handed to ViewModels.
 */
class AppContainer(private val applicationContext: Context) {

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
}
