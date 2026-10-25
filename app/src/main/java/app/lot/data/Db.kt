package app.lot.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import app.lot.domain.Sex
import app.lot.domain.Tx

/** A life lived to its end. Its seed and picks replay the whole book; the rest is kept for the atlas, the chest and the heirs. */
@Entity(tableName = "life")
data class LifeRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seed: Long,
    val heirOf: Long?,
    val witness: Boolean,
    val picks: String,
    val nameRu: String,
    val nameEn: String,
    val sex: String,
    val year: Int,
    val death: Int,
    val eraI: Int,
    val region: String,
    val cult: String,
    val estate: String,
    val rank: Int,
    val wealth: Int,
    val surRu: String?,
    val surEn: String?,
    val kids: Int,
    val lit: Boolean,
    val sawSea: Boolean,
    val epitaphRu: String,
    val epitaphEn: String,
    val bridgeRu: String,
    val bridgeEn: String,
    val answer: String = "",
    val thingId: Long,
    val storyRu: String?,
    val storyEn: String?,
    val meet: Int?,
    /** The seed of the first life in a family line, shared by every great-grandchild after it. */
    val dynasty: Long,
    val gen: Int,
    val endedAt: Long,
    /** The engine that wrote the life: 1.0.0 lives (1) kept no [book] and are read again by today's engine. */
    @ColumnInfo(defaultValue = "1") val ver: Int = 1,
    /** The book as it was written, so later versions of the lines never change a finished life. */
    @ColumnInfo(defaultValue = "") val book: String = "",
    /** Ids of the real events the life met, for the chronicle. */
    @ColumnInfo(defaultValue = "") val seen: String = "",
) {
    val name get() = Tx(nameRu, nameEn)
    val epitaph get() = Tx(epitaphRu, epitaphEn)
    val bridge get() = Tx(bridgeRu, bridgeEn)
    val sexOf get() = Sex.valueOf(sex)
    val sur get() = surRu?.let { Tx(it, surEn ?: it) }
    val story get() = storyRu?.let { Tx(it, storyEn ?: it) }
    val pickList get() = if (picks.isEmpty()) emptyList() else picks.split(',').map { it.toInt() }
    val seenIds get() = if (seen.isEmpty()) emptyList() else seen.split(',').map { it.toInt() }
}

/** A thing in the chest; who held it is the lives that point at it. */
@Entity(tableName = "thing")
data class ThingRow(@PrimaryKey(autoGenerate = true) val id: Long = 0, val key: String, val createdAt: Long)

@Entity(tableName = "meta")
data class MetaRow(@PrimaryKey val k: String, val v: String)

@Dao
interface LotDao {
    @Query("SELECT * FROM life ORDER BY id") suspend fun lives(): List<LifeRow>
    @Query("SELECT * FROM thing ORDER BY id") suspend fun things(): List<ThingRow>
    @Query("SELECT * FROM meta") suspend fun meta(): List<MetaRow>

    @Insert suspend fun insert(l: LifeRow): Long
    @Insert suspend fun insert(t: ThingRow): Long
    @Query("UPDATE life SET answer = :answer WHERE id = :id") suspend fun answer(id: Long, answer: String)
    @Upsert suspend fun meta(m: MetaRow)
}

@Database(entities = [LifeRow::class, ThingRow::class, MetaRow::class], version = 2, exportSchema = true, autoMigrations = [AutoMigration(from = 1, to = 2)])
abstract class LotDb : RoomDatabase() {
    abstract fun dao(): LotDao

    companion object {
        fun create(c: Context): LotDb = Room.databaseBuilder(c, LotDb::class.java, "lot.db").build()
        fun memory(c: Context): LotDb = Room.inMemoryDatabaseBuilder(c, LotDb::class.java).build()
    }
}
