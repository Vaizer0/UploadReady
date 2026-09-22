package com.vaizero.uploadready.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vaizero.uploadready.data.model.AppProfile
import com.vaizero.uploadready.data.model.RecentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [AppProfile::class, RecentFile::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun recentFileDao(): RecentFileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "uploadready_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialProfiles(database.profileDao())
                    }
                }
            }

            suspend fun populateInitialProfiles(dao: ProfileDao) {
                val presets = listOf(
                    AppProfile(
                        name = "SSC Exams / Govt Jobs",
                        description = "Standard specifications for SSC CGL, CHSL, MTS & State Staff Selection",
                        photoWidthPx = 200,
                        photoHeightPx = 230,
                        photoMinKb = 20,
                        photoMaxKb = 50,
                        photoFormat = "JPEG",
                        signatureWidthPx = 140,
                        signatureHeightPx = 60,
                        signatureMinKb = 10,
                        signatureMaxKb = 20,
                        signatureFormat = "JPEG",
                        pdfMaxKb = 1024,
                        isPreset = true
                    ),
                    AppProfile(
                        name = "Passport & Visa Application",
                        description = "Standard 2x2 inch square photo specifications with crisp clarity",
                        photoWidthPx = 600,
                        photoHeightPx = 600,
                        photoMinKb = 50,
                        photoMaxKb = 200,
                        photoFormat = "JPEG",
                        signatureWidthPx = 300,
                        signatureHeightPx = 150,
                        signatureMinKb = 20,
                        signatureMaxKb = 50,
                        signatureFormat = "JPEG",
                        pdfMaxKb = 2048,
                        isPreset = true
                    ),
                    AppProfile(
                        name = "UPSC Civil Services",
                        description = "Civil Services examination portal specifications",
                        photoWidthPx = 350,
                        photoHeightPx = 350,
                        photoMinKb = 20,
                        photoMaxKb = 300,
                        photoFormat = "JPEG",
                        signatureWidthPx = 350,
                        signatureHeightPx = 350,
                        signatureMinKb = 20,
                        signatureMaxKb = 300,
                        signatureFormat = "JPEG",
                        pdfMaxKb = 1024,
                        isPreset = true
                    ),
                    AppProfile(
                        name = "College / University Admissions",
                        description = "General academic admissions and scholarship portals",
                        photoWidthPx = 300,
                        photoHeightPx = 400,
                        photoMinKb = 30,
                        photoMaxKb = 100,
                        photoFormat = "JPEG",
                        signatureWidthPx = 200,
                        signatureHeightPx = 80,
                        signatureMinKb = 10,
                        signatureMaxKb = 30,
                        signatureFormat = "JPEG",
                        pdfMaxKb = 2048,
                        isPreset = true
                    )
                )
                dao.insertAll(presets)
            }
        }
    }
}
