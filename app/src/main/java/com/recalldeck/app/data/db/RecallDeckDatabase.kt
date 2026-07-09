package com.recalldeck.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        SubjectEntity::class,
        CategoryEntity::class,
        CardEntity::class,
        ReviewLogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class RecallDeckDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun categoryDao(): CategoryDao
    abstract fun cardDao(): CardDao
    abstract fun reviewLogDao(): ReviewLogDao

    companion object {
        fun build(context: Context): RecallDeckDatabase =
            Room.databaseBuilder(context, RecallDeckDatabase::class.java, "recalldeck.db")
                .build()
    }
}
