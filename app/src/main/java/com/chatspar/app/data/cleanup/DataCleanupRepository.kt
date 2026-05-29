package com.chatspar.app.data.cleanup

import com.chatspar.app.core.database.AppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataCleanupRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun clearPracticeRecords() {
        withContext(ioDispatcher) {
            database.runInTransaction {
                database.practiceSessionDao().deleteAll()
            }
        }
    }

    suspend fun clearPhrases() {
        withContext(ioDispatcher) {
            database.runInTransaction {
                database.phraseDao().deleteAll()
            }
        }
    }
}
