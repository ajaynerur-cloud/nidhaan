package com.nidhaan.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Top level: Home, Garage, Office ... */
@Entity(tableName = "places")
data class Place(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val area: String = "",
    val notes: String = "",
    val photo: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/** Second level: Box, Rack, Wardrobe, Drawer, Shelf, Cabinet or a custom type. */
@Entity(
    tableName = "containers",
    foreignKeys = [ForeignKey(
        entity = Place::class,
        parentColumns = ["id"],
        childColumns = ["placeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("placeId")]
)
data class Container(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val placeId: Long,
    val name: String,
    val type: String = "Box",
    val notes: String = "",
    val photo: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/** Third level: a single item or a group of items (qty). */
@Entity(
    tableName = "items",
    foreignKeys = [ForeignKey(
        entity = Container::class,
        parentColumns = ["id"],
        childColumns = ["containerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("containerId")]
)
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val containerId: Long,
    val name: String,
    val isGroup: Boolean = false,
    val qty: Int = 1,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/** An item can have many photos; all files live in the app's private folder. */
@Entity(
    tableName = "item_photos",
    foreignKeys = [ForeignKey(
        entity = Item::class,
        parentColumns = ["id"],
        childColumns = ["itemId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("itemId")]
)
data class ItemPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val fileName: String
)

// ---------- query result types ----------

data class PlaceRow(
    @Embedded val place: Place,
    val containerCount: Int,
    val itemCount: Int
)

data class ContainerRow(
    @Embedded val container: Container,
    val itemCount: Int
)

data class ItemRow(
    @Embedded val item: Item,
    val thumb: String?
)

data class SearchRow(
    val itemId: Long,
    val itemName: String,
    val qty: Int,
    val isGroup: Boolean,
    val containerId: Long,
    val containerName: String,
    val containerType: String,
    val placeId: Long,
    val placeName: String,
    val thumb: String?
)

data class Crumb(
    val placeId: Long,
    val placeName: String,
    val containerId: Long,
    val containerName: String
)

data class ContainerChoice(
    val containerId: Long,
    val containerName: String,
    val placeName: String
)

// ---------- DAO ----------

@Dao
interface AppDao {

    // Places
    @Query(
        """SELECT p.*,
            (SELECT COUNT(*) FROM containers c WHERE c.placeId = p.id) AS containerCount,
            (SELECT COUNT(*) FROM items i JOIN containers c ON i.containerId = c.id WHERE c.placeId = p.id) AS itemCount
           FROM places p ORDER BY p.name COLLATE NOCASE"""
    )
    fun placeRows(): Flow<List<PlaceRow>>

    @Query("SELECT * FROM places WHERE id = :id")
    fun place(id: Long): Flow<Place?>

    @Query("SELECT * FROM places WHERE id = :id")
    suspend fun placeOnce(id: Long): Place?

    @Upsert
    suspend fun upsertPlace(p: Place): Long

    @Query("DELETE FROM places WHERE id = :id")
    suspend fun deletePlace(id: Long)

    // Containers
    @Query(
        """SELECT c.*,
            (SELECT COUNT(*) FROM items i WHERE i.containerId = c.id) AS itemCount
           FROM containers c WHERE c.placeId = :placeId ORDER BY c.name COLLATE NOCASE"""
    )
    fun containerRows(placeId: Long): Flow<List<ContainerRow>>

    @Query("SELECT * FROM containers WHERE id = :id")
    fun container(id: Long): Flow<Container?>

    @Query("SELECT * FROM containers WHERE id = :id")
    suspend fun containerOnce(id: Long): Container?

    @Upsert
    suspend fun upsertContainer(c: Container): Long

    @Query("DELETE FROM containers WHERE id = :id")
    suspend fun deleteContainer(id: Long)

    @Query(
        """SELECT p.id AS placeId, p.name AS placeName, c.id AS containerId, c.name AS containerName
           FROM containers c JOIN places p ON c.placeId = p.id WHERE c.id = :containerId"""
    )
    fun containerCrumb(containerId: Long): Flow<Crumb?>

    // Items
    @Query(
        """SELECT i.*,
            (SELECT fileName FROM item_photos WHERE itemId = i.id ORDER BY id LIMIT 1) AS thumb
           FROM items i WHERE i.containerId = :containerId ORDER BY i.name COLLATE NOCASE"""
    )
    fun itemsIn(containerId: Long): Flow<List<ItemRow>>

    @Query("SELECT * FROM items WHERE id = :id")
    fun item(id: Long): Flow<Item?>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun itemOnce(id: Long): Item?

    @Upsert
    suspend fun upsertItem(i: Item): Long

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("UPDATE items SET containerId = :containerId WHERE id = :id")
    suspend fun moveItem(id: Long, containerId: Long)

    @Query(
        """SELECT p.id AS placeId, p.name AS placeName, c.id AS containerId, c.name AS containerName
           FROM items i JOIN containers c ON i.containerId = c.id JOIN places p ON c.placeId = p.id
           WHERE i.id = :itemId"""
    )
    fun itemCrumb(itemId: Long): Flow<Crumb?>

    // Item photos
    @Query("SELECT * FROM item_photos WHERE itemId = :itemId ORDER BY id")
    fun photosOf(itemId: Long): Flow<List<ItemPhoto>>

    @Upsert
    suspend fun upsertItemPhoto(p: ItemPhoto): Long

    @Query("DELETE FROM item_photos WHERE id = :id")
    suspend fun deleteItemPhoto(id: Long)

    // Search / lists
    @Query(
        """SELECT i.id AS itemId, i.name AS itemName, i.qty AS qty, i.isGroup AS isGroup,
            c.id AS containerId, c.name AS containerName, c.type AS containerType,
            p.id AS placeId, p.name AS placeName,
            (SELECT fileName FROM item_photos WHERE itemId = i.id ORDER BY id LIMIT 1) AS thumb
           FROM items i
           JOIN containers c ON i.containerId = c.id
           JOIN places p ON c.placeId = p.id
           WHERE i.name LIKE '%' || :q || '%' OR i.notes LIKE '%' || :q || '%'
              OR c.name LIKE '%' || :q || '%' OR p.name LIKE '%' || :q || '%'
           ORDER BY i.name COLLATE NOCASE"""
    )
    fun search(q: String): Flow<List<SearchRow>>

    @Query(
        """SELECT c.id AS containerId, c.name AS containerName, p.name AS placeName
           FROM containers c JOIN places p ON c.placeId = p.id
           ORDER BY p.name COLLATE NOCASE, c.name COLLATE NOCASE"""
    )
    fun containerChoices(): Flow<List<ContainerChoice>>

    @Query("SELECT COUNT(*) FROM items")
    fun totalItems(): Flow<Int>

    // Housekeeping + backup
    @Query(
        """SELECT photo FROM places WHERE photo IS NOT NULL
           UNION SELECT photo FROM containers WHERE photo IS NOT NULL
           UNION SELECT fileName FROM item_photos"""
    )
    suspend fun allPhotoNames(): List<String>

    @Query("DELETE FROM places")
    suspend fun clearAll()

    @Query("SELECT * FROM places ORDER BY id")
    suspend fun allPlaces(): List<Place>

    @Query("SELECT * FROM containers ORDER BY id")
    suspend fun allContainers(): List<Container>

    @Query("SELECT * FROM items ORDER BY id")
    suspend fun allItems(): List<Item>

    @Query("SELECT * FROM item_photos ORDER BY id")
    suspend fun allItemPhotos(): List<ItemPhoto>
}

@Database(
    entities = [Place::class, Container::class, Item::class, ItemPhoto::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}
