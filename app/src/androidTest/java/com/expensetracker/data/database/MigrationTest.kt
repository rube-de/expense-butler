package com.expensetracker.data.database

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import kotlinx.coroutines.test.runTest
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    @Test
    fun testDatabaseMigrationsExist() {
        // Test that our migration definitions are properly configured
        assertNotNull("MIGRATION_1_2 should be defined", DatabaseMigrations.MIGRATION_1_2)
        assertTrue("ALL_MIGRATIONS should contain at least one migration", 
                   DatabaseMigrations.ALL_MIGRATIONS.isNotEmpty())
        assertEquals("MIGRATION_1_2 should migrate from version 1 to 2", 
                     1, DatabaseMigrations.MIGRATION_1_2.startVersion)
        assertEquals("MIGRATION_1_2 should migrate from version 1 to 2", 
                     2, DatabaseMigrations.MIGRATION_1_2.endVersion)
    }

    @Test
    fun testDatabaseCanBeCreatedWithMigrations() = runTest {
        // Test that we can create a database with migrations enabled
        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
            TEST_DB
        ).addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
         .build()

        // Verify database can be used
        val categoryDao = database.categoryDao()
        assertNotNull("CategoryDao should be accessible", categoryDao)
        
        // Test basic database operations work
        val categoryCount = categoryDao.getCategoryCount()
        assertTrue("Category count should be retrievable", categoryCount >= 0)

        database.close()
    }

    @Test
    fun testMigrationLogicDirectly() {
        // Test the migration logic directly on a database
        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
            "$TEST_DB-direct"
        ).addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
         .build()

        // Get the underlying SQLite database
        val sqLiteDatabase = database.openHelper.writableDatabase

        // Manually execute migration to test indices creation
        DatabaseMigrations.MIGRATION_1_2.migrate(sqLiteDatabase)

        // Verify indices were created
        val indexCursor = sqLiteDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'index_%'"
        )
        
        val indexNames = mutableSetOf<String>()
        while (indexCursor.moveToNext()) {
            indexNames.add(indexCursor.getString(0))
        }
        indexCursor.close()

        // Verify expected indices exist
        assertTrue("Should have index on expenses date", 
                   indexNames.contains("index_expenses_date"))
        assertTrue("Should have index on expenses categoryId", 
                   indexNames.contains("index_expenses_categoryId"))
        assertTrue("Should have index on expenses createdAt", 
                   indexNames.contains("index_expenses_createdAt"))
        assertTrue("Should have index on categories name", 
                   indexNames.contains("index_categories_name"))

        database.close()
    }
}