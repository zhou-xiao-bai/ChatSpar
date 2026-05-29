package com.chatspar.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chatspar.app.core.database.dao.MessageDao
import com.chatspar.app.core.database.dao.PhraseDao
import com.chatspar.app.core.database.dao.PracticeSessionDao
import com.chatspar.app.core.database.dao.ReviewDao
import com.chatspar.app.core.database.entity.MessageEntity
import com.chatspar.app.core.database.entity.PhraseEntity
import com.chatspar.app.core.database.entity.PracticeSessionEntity
import com.chatspar.app.core.database.entity.ReviewEntity

@Database(
    entities = [
        PracticeSessionEntity::class,
        MessageEntity::class,
        ReviewEntity::class,
        PhraseEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun practiceSessionDao(): PracticeSessionDao
    abstract fun messageDao(): MessageDao
    abstract fun reviewDao(): ReviewDao
    abstract fun phraseDao(): PhraseDao

    companion object {
        private const val DATABASE_NAME = "chatspar.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also {
                    instance = it
                }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reviews ADD COLUMN raw_response TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
