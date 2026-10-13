package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "watch_history")
data class HistoryEntity(
  @PrimaryKey val videoId: String,
  val watchedAt: Long,
  val playbackPositionMs: Long,
  val durationMs: Long,
  val isCompleted: Boolean = false
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
  @PrimaryKey val id: String,
  val videoId: String,
  val timestampMs: Long,
  val formattedTime: String,
  val note: String,
  val createdAt: Long
)

@Entity(tableName = "watch_collections")
data class CollectionEntity(
  @PrimaryKey val id: String,
  val name: String,
  val description: String,
  val videoIdsJson: String, // comma separated or json list
  val updatedAt: Long,
  val isPrivate: Boolean = true
)

@Entity(tableName = "downloads")
data class DownloadEntity(
  @PrimaryKey val videoId: String,
  val title: String,
  val thumbnailUrl: String,
  val videoUrl: String,
  val progressPercent: Int,
  val sizeMb: Double,
  val status: String,
  val downloadedAt: Long
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
  @PrimaryKey val channelId: String,
  val channelName: String,
  val channelAvatar: String,
  val notificationLevel: String,
  val subscribedAt: Long
)

@Entity(tableName = "search_history")
data class SearchQueryEntity(
  @PrimaryKey val query: String,
  val timestamp: Long
)

@Entity(tableName = "custom_videos")
data class CustomVideoEntity(
  @PrimaryKey val id: String,
  val title: String,
  val description: String,
  val videoUrl: String,
  val thumbnailUrl: String,
  val durationSeconds: Int,
  val category: String,
  val isShort: Boolean,
  val uploadDate: String,
  val views: Long = 0
)

@Dao
interface HistoryDao {
  @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC")
  fun getAllHistory(): Flow<List<HistoryEntity>>

  @Query("SELECT * FROM watch_history WHERE videoId = :videoId LIMIT 1")
  suspend fun getHistoryForVideo(videoId: String): HistoryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(entity: HistoryEntity)

  @Query("DELETE FROM watch_history WHERE videoId = :videoId")
  suspend fun deleteByVideoId(videoId: String)

  @Query("DELETE FROM watch_history")
  suspend fun clearHistory()
}

@Dao
interface BookmarkDao {
  @Query("SELECT * FROM bookmarks WHERE videoId = :videoId ORDER BY timestampMs ASC")
  fun getBookmarksForVideo(videoId: String): Flow<List<BookmarkEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBookmark(entity: BookmarkEntity)

  @Delete
  suspend fun deleteBookmark(entity: BookmarkEntity)
}

@Dao
interface CollectionDao {
  @Query("SELECT * FROM watch_collections ORDER BY updatedAt DESC")
  fun getAllCollections(): Flow<List<CollectionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(entity: CollectionEntity)

  @Delete
  suspend fun delete(entity: CollectionEntity)
}

@Dao
interface DownloadDao {
  @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
  fun getAllDownloads(): Flow<List<DownloadEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(entity: DownloadEntity)

  @Query("DELETE FROM downloads WHERE videoId = :videoId")
  suspend fun deleteDownload(videoId: String)

  @Query("DELETE FROM downloads")
  suspend fun clearAllDownloads()
}

@Dao
interface SubscriptionDao {
  @Query("SELECT * FROM subscriptions ORDER BY subscribedAt DESC")
  fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(entity: SubscriptionEntity)

  @Query("DELETE FROM subscriptions WHERE channelId = :channelId")
  suspend fun delete(channelId: String)

  @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE channelId = :channelId)")
  suspend fun isSubscribed(channelId: String): Boolean
}

@Dao
interface SearchDao {
  @Query("SELECT query FROM search_history ORDER BY timestamp DESC LIMIT 20")
  fun getRecentQueries(): Flow<List<String>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertQuery(entity: SearchQueryEntity)

  @Query("DELETE FROM search_history WHERE query = :query")
  suspend fun deleteQuery(query: String)

  @Query("DELETE FROM search_history")
  suspend fun clearAllQueries()
}

@Dao
interface CustomVideoDao {
  @Query("SELECT * FROM custom_videos ORDER BY uploadDate DESC")
  fun getAllCustomVideos(): Flow<List<CustomVideoEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVideo(entity: CustomVideoEntity)

  @Query("DELETE FROM custom_videos WHERE id = :id")
  suspend fun deleteVideo(id: String)
}

@Database(
  entities = [
    HistoryEntity::class,
    BookmarkEntity::class,
    CollectionEntity::class,
    DownloadEntity::class,
    SubscriptionEntity::class,
    SearchQueryEntity::class,
    CustomVideoEntity::class
  ],
  version = 1,
  exportSchema = false
)
abstract class LuminaDatabase : RoomDatabase() {
  abstract fun historyDao(): HistoryDao
  abstract fun bookmarkDao(): BookmarkDao
  abstract fun collectionDao(): CollectionDao
  abstract fun downloadDao(): DownloadDao
  abstract fun subscriptionDao(): SubscriptionDao
  abstract fun searchDao(): SearchDao
  abstract fun customVideoDao(): CustomVideoDao
}
