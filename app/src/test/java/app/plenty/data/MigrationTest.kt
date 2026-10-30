package app.plenty.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * v1 -> v2 keeps every row and marks journeys closed before the upgrade as already seen. The v1
 * database is built from the exported schema (Robolectric cannot read test assets, which
 * MigrationTestHelper needs); Room validates the migrated tables against v2 when it opens.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clean() {
        context.deleteDatabase(DB_NAME)
    }

    @After
    fun delete() {
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun v1JourneysSurviveAndClosedOnesCountAsSeen() {
        createV1Database { db ->
            db.execSQL(
                "INSERT INTO journeys (id, status, source, companionIds, startedAt, settledAt, distanceKm, rewardMaterialCount) " +
                    "VALUES (1, 'SETTLED', 'MOCK', '1', 100, 200, 12.5, 0), (2, 'ACTIVE', 'VEHICLE', '1', 300, NULL, NULL, 0)",
            )
            db.execSQL("INSERT INTO journey_growth VALUES (1, 1, 12, 'SEED', 'SEED')")
        }

        val room = Room.databaseBuilder(context, PlentyDatabase::class.java, DB_NAME).allowMainThreadQueries().build()
        val db = room.openHelper.readableDatabase
        db.query("SELECT id, resultSeenAt, arrivedAt FROM journeys ORDER BY id").use { rows ->
            rows.moveToFirst()
            assertEquals(200L, rows.getLong(1))
            rows.moveToNext()
            assertTrue(rows.isNull(1))
            assertTrue(rows.isNull(2))
        }
        db.query("SELECT xpGained, xpBefore, traitPoints FROM journey_growth").use { rows ->
            rows.moveToFirst()
            assertEquals(12, rows.getInt(0))
            assertTrue(rows.isNull(1))
            assertEquals(0, rows.getInt(2))
        }
        db.query("SELECT COUNT(*) FROM ready_selection").use { rows ->
            rows.moveToFirst()
            assertEquals(0, rows.getInt(0))
        }
        room.close()
    }

    private fun createV1Database(fill: (SQLiteDatabase) -> Unit) {
        // Unit tests run with the module directory as the working directory.
        val schema = JSONObject(File("schemas/app.plenty.data.PlentyDatabase/1.json").readText()).getJSONObject("database")
        val file = context.getDatabasePath(DB_NAME).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.version = 1
            fill(db)
        }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
