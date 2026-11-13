package com.bytewise.game.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bytewise.game.data.model.AnswerRecord
import com.bytewise.game.data.model.GameSession
import com.bytewise.game.data.model.LearningReportEntity
import com.bytewise.game.data.model.WeaknessSnapshot

/**
 * Historical data must never be lost: no destructive migration is configured.
 * Schema changes must ship with a Migration; exported schemas live in app/schemas/.
 */
@Database(
    entities = [GameSession::class, AnswerRecord::class, LearningReportEntity::class, WeaknessSnapshot::class],
    version = 1,
    exportSchema = true,
)
abstract class BytewiseDatabase : RoomDatabase() {
    abstract fun gameSessionDao(): GameSessionDao
    abstract fun answerRecordDao(): AnswerRecordDao
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile private var instance: BytewiseDatabase? = null

        fun getInstance(context: Context): BytewiseDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BytewiseDatabase::class.java,
                    "bytewise.db",
                ).build().also { instance = it }
            }
    }
}
