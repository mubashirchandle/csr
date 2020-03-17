package com.csrapp.csr.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [StreamEntity::class, JobEntity::class, AptitudeQuestionEntity::class, PersonalityQuestionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class CSRDatabase : RoomDatabase() {
    abstract fun streamDao(): StreamDao
    abstract fun jobDao(): JobDao
    abstract fun aptitudeQuestionDao(): AptitudeQuestionDao
    abstract fun personalityQuestionDao(): PersonalityQuestionDao

    companion object {
        private var instance: CSRDatabase? = null

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: buildDatabase(context)
        }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(
            context,
            CSRDatabase::class.java,
            "csr_data.db"
        )
            .allowMainThreadQueries()
            .createFromAsset("database/csr_data.db")
            .build()
    }
}