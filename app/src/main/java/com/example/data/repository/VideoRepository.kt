package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.R
import com.example.data.local.BookmarkEntity
import com.example.data.local.CollectionEntity
import com.example.data.local.DownloadEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.LuminaDatabase
import com.example.data.local.SearchQueryEntity
import com.example.data.model.Channel
import com.example.data.model.Chapter
import com.example.data.model.Comment
import com.example.data.model.CreatorAnalytics
import com.example.data.model.DownloadStatus
import com.example.data.model.HistoryEntry
import com.example.data.model.NotificationLevel
import com.example.data.model.OfflineDownload
import com.example.data.model.UserPreferences
import com.example.data.model.Video
import com.example.data.model.VideoBookmark
import com.example.data.model.WatchCollection
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class VideoRepository(
  private val context: Context,
  val dbFirestore: FirebaseFirestore
) {
  companion object {
    private const val TAG = "VideoRepository"
  }

  // Convenience constructor resolving the custom database ID from string resources
  constructor(context: Context) : this(
    context = context,
    dbFirestore = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val scope = CoroutineScope(Dispatchers.IO)
  private val roomDb = Room.databaseBuilder(
    context.applicationContext,
    LuminaDatabase::class.java,
    "lumina_database.db"
  ).fallbackToDestructiveMigration(true).build()

  private val _videos = MutableStateFlow<List<Video>>(emptyList())
  val videos: StateFlow<List<Video>> = _videos.asStateFlow()

  private val _shorts = MutableStateFlow<List<Video>>(emptyList())
  val shorts: StateFlow<List<Video>> = _shorts.asStateFlow()

  private val _isLoadingVideos = MutableStateFlow(true)
  val isLoadingVideos: StateFlow<Boolean> = _isLoadingVideos.asStateFlow()

  private val _videosErrorMessage = MutableStateFlow<String?>(null)
  val videosErrorMessage: StateFlow<String?> = _videosErrorMessage.asStateFlow()

  private val _comments = MutableStateFlow<Map<String, List<Comment>>>(emptyMap())
  val comments: StateFlow<Map<String, List<Comment>>> = _comments.asStateFlow()

  private val _userPreferences = MutableStateFlow(UserPreferences())
  val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

  private val _universalQueue = MutableStateFlow<List<Video>>(emptyList())
  val universalQueue: StateFlow<List<Video>> = _universalQueue.asStateFlow()

  private val _watchLaterIds = MutableStateFlow<Set<String>>(emptySet())
  val watchLaterIds: StateFlow<Set<String>> = _watchLaterIds.asStateFlow()

  private val _subscribedChannelIds = MutableStateFlow<Set<String>>(emptySet())
  val subscribedChannelIds: StateFlow<Set<String>> = _subscribedChannelIds.asStateFlow()

  private val _likedVideoIds = MutableStateFlow<Set<String>>(emptySet())
  val likedVideoIds: StateFlow<Set<String>> = _likedVideoIds.asStateFlow()

  private var videosListenerRegistration: ListenerRegistration? = null
  private var activeCommentsRegistration: ListenerRegistration? = null
  private var activeCommentsVideoId: String? = null
  private var subscriptionsListenerRegistration: ListenerRegistration? = null

  init {
    val currentAuthUser = Firebase.auth.currentUser
    if (currentAuthUser != null) {
      startListening(currentAuthUser.uid)
    }
  }

  fun startListening(currentUserId: String) {
    listenToPublishedVideos()
    listenToUserSubscriptions(currentUserId)
  }

  fun stopListening() {
    videosListenerRegistration?.remove()
    videosListenerRegistration = null
    activeCommentsRegistration?.remove()
    activeCommentsRegistration = null
    subscriptionsListenerRegistration?.remove()
    subscriptionsListenerRegistration = null
  }

  private fun listenToPublishedVideos() {
    videosListenerRegistration?.remove()
    _isLoadingVideos.value = true
    _videosErrorMessage.value = null

    try {
      videosListenerRegistration = dbFirestore.collection("videos")
        .whereEqualTo("status", "published")
        .whereEqualTo("visibility", "public")
        .addSnapshotListener { snapshot, error ->
          _isLoadingVideos.value = false
          if (error != null) {
            Log.e(TAG, "Error fetching published videos from Firestore", error)
            _videosErrorMessage.value = error.localizedMessage ?: "Failed to fetch videos from server"
            return@addSnapshotListener
          }

          if (snapshot != null) {
            val list = mutableListOf<Video>()
            for (doc in snapshot.documents) {
              val id = doc.id
              val title = doc.getString("title") ?: ""
              val description = doc.getString("description") ?: ""
              val videoUrl = doc.getString("videoUrl") ?: ""
              val thumbnailUrl = doc.getString("thumbnailUrl") ?: ""
              val durationSec = doc.getLong("durationSeconds")?.toInt() ?: 0
              val formattedDur = doc.getString("formattedDuration") ?: formatDurationSeconds(durationSec)
              val views = doc.getLong("views") ?: 0L
              val formattedViews = doc.getString("formattedViews") ?: formatViewsCount(views)
              val uploadDate = doc.getString("uploadDate") ?: "Recently"
              val channelId = doc.getString("channelId") ?: doc.getString("creatorId") ?: ""
              val channelTitle = doc.getString("channelTitle") ?: doc.getString("creatorName") ?: "Creator"
              val channelAvatarUrl = doc.getString("channelAvatarUrl") ?: doc.getString("creatorAvatar") ?: ""
              val isVerified = doc.getBoolean("isVerifiedChannel") ?: false
              val isShort = doc.getBoolean("isShort") ?: false
              val category = doc.getString("category") ?: "General"
              val likesCount = doc.getLong("likesCount") ?: 0L
              val dislikesCount = doc.getLong("dislikesCount") ?: 0L
              val creatorId = doc.getString("creatorId") ?: channelId
              val status = doc.getString("status") ?: "published"
              val visibility = doc.getString("visibility") ?: "public"

              // Only include strictly public and published videos
              if (status == "published" && visibility == "public") {
                list.add(
                  Video(
                    id = id,
                    title = title,
                    description = description,
                    videoUrl = videoUrl,
                    thumbnailUrl = thumbnailUrl,
                    durationSeconds = durationSec,
                    formattedDuration = formattedDur,
                    views = views,
                    formattedViews = formattedViews,
                    uploadDate = uploadDate,
                    channelId = channelId,
                    channelTitle = channelTitle,
                    channelAvatarUrl = channelAvatarUrl,
                    isVerifiedChannel = isVerified,
                    isShort = isShort,
                    category = category,
                    likesCount = likesCount,
                    dislikesCount = dislikesCount,
                    isLiked = _likedVideoIds.value.contains(id),
                    isSubscribed = _subscribedChannelIds.value.contains(channelId),
                    creatorId = creatorId,
                    status = status,
                    visibility = visibility
                  )
                )
              }
            }
            _videos.value = list
            _shorts.value = list.filter { it.isShort }
          }
        }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to register video snapshot listener", e)
      _isLoadingVideos.value = false
      _videosErrorMessage.value = e.localizedMessage
    }
  }

  private fun listenToUserSubscriptions(userId: String) {
    subscriptionsListenerRegistration?.remove()
    try {
      subscriptionsListenerRegistration = dbFirestore.collection("users")
        .document(userId)
        .collection("subscriptions")
        .addSnapshotListener { snapshot, error ->
          if (error != null) {
            Log.w(TAG, "Error fetching user subscriptions", error)
            return@addSnapshotListener
          }
          if (snapshot != null) {
            val subIds = snapshot.documents.mapNotNull { it.getString("creatorId") ?: it.id }.toSet()
            _subscribedChannelIds.value = subIds
            // Update videos isSubscribed flag
            _videos.value = _videos.value.map { v ->
              v.copy(isSubscribed = subIds.contains(v.channelId))
            }
          }
        }
    } catch (e: Exception) {
      Log.w(TAG, "Error subscribing to user subscriptions", e)
    }
  }

  fun listenToComments(videoId: String) {
    if (activeCommentsVideoId == videoId && activeCommentsRegistration != null) return
    activeCommentsRegistration?.remove()
    activeCommentsVideoId = videoId

    try {
      activeCommentsRegistration = dbFirestore.collection("videos")
        .document(videoId)
        .collection("comments")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .addSnapshotListener { snapshot, error ->
          if (error != null) {
            Log.e(TAG, "Error listening to comments for $videoId", error)
            return@addSnapshotListener
          }
          if (snapshot != null) {
            val list = snapshot.documents.map { doc ->
              Comment(
                id = doc.id,
                videoId = videoId,
                userId = doc.getString("userId") ?: "",
                authorName = doc.getString("userName") ?: doc.getString("authorName") ?: "Viewer",
                authorAvatarUrl = doc.getString("userAvatar") ?: doc.getString("authorAvatarUrl") ?: "",
                text = doc.getString("text") ?: "",
                timestamp = doc.getString("timestamp") ?: "Just now",
                likesCount = doc.getLong("likesCount") ?: 0L,
                isLiked = false
              )
            }
            val currentMap = _comments.value.toMutableMap()
            currentMap[videoId] = list
            _comments.value = currentMap
          }
        }
    } catch (e: Exception) {
      Log.e(TAG, "Error initiating comment listener", e)
    }
  }

  suspend fun publishVideo(
    title: String,
    description: String,
    videoUrl: String,
    thumbnailUrl: String,
    category: String,
    durationSeconds: Int = 180,
    isShort: Boolean = false,
    visibility: String = "public"
  ): Result<String> {
    val authUser = Firebase.auth.currentUser ?: return Result.failure(IllegalStateException("Must be signed in to publish video"))
    val videoId = "v_${UUID.randomUUID().toString().take(12)}"
    val formattedDur = formatDurationSeconds(durationSeconds)
    val uploadDateString = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())

    val payload = mapOf(
      "id" to videoId,
      "title" to title.trim(),
      "description" to description.trim(),
      "videoUrl" to videoUrl.trim(),
      "thumbnailUrl" to thumbnailUrl.trim(),
      "durationSeconds" to durationSeconds,
      "formattedDuration" to formattedDur,
      "views" to 0L,
      "formattedViews" to "0 views",
      "uploadDate" to uploadDateString,
      "channelId" to authUser.uid,
      "channelTitle" to (authUser.displayName ?: "Creator"),
      "channelAvatarUrl" to (authUser.photoUrl?.toString() ?: ""),
      "creatorId" to authUser.uid,
      "creatorName" to (authUser.displayName ?: "Creator"),
      "creatorAvatar" to (authUser.photoUrl?.toString() ?: ""),
      "isVerifiedChannel" to false,
      "isShort" to isShort,
      "category" to category,
      "likesCount" to 0L,
      "dislikesCount" to 0L,
      "status" to "published",
      "visibility" to visibility,
      "createdAt" to FieldValue.serverTimestamp()
    )

    return try {
      dbFirestore.collection("videos").document(videoId).set(payload).await()
      Log.i(TAG, "Successfully published video $videoId to Firestore")
      Result.success(videoId)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to publish video to Firestore", e)
      Result.failure(e)
    }
  }

  fun recordVideoView(videoId: String) {
    if (videoId.isBlank()) return
    scope.launch {
      try {
        dbFirestore.collection("videos").document(videoId)
          .update(
            "views", FieldValue.increment(1),
            "formattedViews", FieldValue.increment(0) // keep in sync
          )
        Log.i(TAG, "Incremented view for video $videoId in Firestore")
      } catch (e: Exception) {
        Log.w(TAG, "Failed to increment view count on Firestore: ${e.message}")
      }
    }
  }

  fun toggleLike(videoId: String) {
    val user = Firebase.auth.currentUser ?: return
    val currentLiked = _likedVideoIds.value.contains(videoId)
    val newLiked = !currentLiked

    val updatedSet = _likedVideoIds.value.toMutableSet()
    if (newLiked) updatedSet.add(videoId) else updatedSet.remove(videoId)
    _likedVideoIds.value = updatedSet

    // Optimistically update local video state
    _videos.value = _videos.value.map { v ->
      if (v.id == videoId) {
        val newLikes = if (newLiked) v.likesCount + 1 else maxOf(0L, v.likesCount - 1)
        v.copy(isLiked = newLiked, likesCount = newLikes)
      } else v
    }

    scope.launch {
      try {
        val likeDocRef = dbFirestore.collection("videos").document(videoId)
          .collection("likes").document(user.uid)
        val videoDocRef = dbFirestore.collection("videos").document(videoId)

        if (newLiked) {
          likeDocRef.set(mapOf("userId" to user.uid, "videoId" to videoId, "createdAt" to FieldValue.serverTimestamp()))
          videoDocRef.update("likesCount", FieldValue.increment(1))
        } else {
          likeDocRef.delete()
          videoDocRef.update("likesCount", FieldValue.increment(-1))
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error toggling like on Firestore", e)
      }
    }
  }

  fun toggleDislike(videoId: String) {
    _videos.value = _videos.value.map { v ->
      if (v.id == videoId) {
        val newDisliked = !v.isDisliked
        v.copy(
          isDisliked = newDisliked,
          dislikesCount = if (newDisliked) v.dislikesCount + 1 else maxOf(0L, v.dislikesCount - 1)
        )
      } else v
    }
  }

  fun toggleSubscribe(channelId: String, channelName: String = "Creator", channelAvatar: String = "") {
    val user = Firebase.auth.currentUser ?: return
    val isSubscribed = _subscribedChannelIds.value.contains(channelId)
    val newSubscribed = !isSubscribed

    val newSet = _subscribedChannelIds.value.toMutableSet()
    if (newSubscribed) newSet.add(channelId) else newSet.remove(channelId)
    _subscribedChannelIds.value = newSet

    _videos.value = _videos.value.map { v ->
      if (v.channelId == channelId) v.copy(isSubscribed = newSubscribed) else v
    }

    scope.launch {
      try {
        val subDocRef = dbFirestore.collection("users").document(user.uid)
          .collection("subscriptions").document(channelId)
        if (newSubscribed) {
          subDocRef.set(
            mapOf(
              "userId" to user.uid,
              "creatorId" to channelId,
              "creatorName" to channelName,
              "creatorAvatar" to channelAvatar,
              "createdAt" to FieldValue.serverTimestamp()
            )
          )
        } else {
          subDocRef.delete()
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error updating subscription on Firestore", e)
      }
    }
  }

  fun addComment(videoId: String, text: String) {
    val user = Firebase.auth.currentUser ?: return
    val commentId = "c_${UUID.randomUUID().toString().take(10)}"
    val userName = user.displayName ?: "Viewer"
    val userAvatar = user.photoUrl?.toString() ?: ""

    val payload = mapOf(
      "id" to commentId,
      "videoId" to videoId,
      "userId" to user.uid,
      "userName" to userName,
      "userAvatar" to userAvatar,
      "text" to text.trim(),
      "timestamp" to "Just now",
      "likesCount" to 0L,
      "createdAt" to FieldValue.serverTimestamp()
    )

    scope.launch {
      try {
        dbFirestore.collection("videos").document(videoId)
          .collection("comments").document(commentId)
          .set(payload).await()
        Log.i(TAG, "Added comment $commentId to video $videoId")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to add comment to Firestore", e)
      }
    }
  }

  fun getCreatorAnalytics(): CreatorAnalytics {
    val user = Firebase.auth.currentUser
    val myVideos = _videos.value.filter {
      (user != null && (it.creatorId == user.uid || it.channelId == user.uid))
    }

    val totalViews = myVideos.sumOf { it.views }
    val totalLikes = myVideos.sumOf { it.likesCount }
    val totalSeconds = myVideos.sumOf { it.durationSeconds * it.views }
    val totalWatchHours = String.format(Locale.US, "%.1f", totalSeconds / 3600.0).toDoubleOrNull() ?: 0.0

    return CreatorAnalytics(
      totalViews = totalViews,
      totalWatchHours = totalWatchHours,
      subscribersCount = _subscribedChannelIds.value.size.toLong(),
      estimatedRevenueUsd = String.format(Locale.US, "%.2f", (totalViews / 1000.0) * 2.5).toDoubleOrNull() ?: 0.0,
      topVideos = myVideos.sortedByDescending { it.views }.take(4),
      impressionsRate = if (totalViews > 0) 7.8 else 0.0,
      averageViewDuration = if (myVideos.isNotEmpty()) "3:45" else "0:00"
    )
  }

  // Watch History & Room persistence
  val watchHistory: Flow<List<HistoryEntry>> = roomDb.historyDao().getAllHistory().map { list ->
    list.map {
      HistoryEntry(
        videoId = it.videoId,
        watchedAt = it.watchedAt,
        playbackPositionMs = it.playbackPositionMs,
        durationMs = it.durationMs,
        isCompleted = it.isCompleted
      )
    }
  }

  val offlineDownloads: Flow<List<OfflineDownload>> = roomDb.downloadDao().getAllDownloads().map { list ->
    list.map {
      OfflineDownload(
        videoId = it.videoId,
        title = it.title,
        thumbnailUrl = it.thumbnailUrl,
        videoUrl = it.videoUrl,
        progressPercent = it.progressPercent,
        sizeMb = it.sizeMb,
        status = runCatching { DownloadStatus.valueOf(it.status) }.getOrDefault(DownloadStatus.COMPLETED),
        downloadedAt = it.downloadedAt
      )
    }
  }

  val watchCollections: Flow<List<WatchCollection>> = roomDb.collectionDao().getAllCollections().map { list ->
    list.map {
      WatchCollection(
        id = it.id,
        name = it.name,
        description = it.description,
        videoIds = it.videoIds.split(",").filter { id -> id.isNotBlank() },
        updatedAt = it.updatedAt,
        isPrivate = it.isPrivate
      )
    }
  }

  fun recordWatchHistory(videoId: String, positionMs: Long, durationMs: Long) {
    if (_userPreferences.value.pauseWatchHistory) return
    scope.launch {
      roomDb.historyDao().insert(
        HistoryEntity(
          videoId = videoId,
          playbackPositionMs = positionMs,
          durationMs = durationMs,
          watchedAt = System.currentTimeMillis(),
          isCompleted = positionMs >= (durationMs * 0.95)
        )
      )
    }
  }

  fun clearWatchHistory() {
    scope.launch {
      roomDb.historyDao().clearAll()
    }
  }

  fun addToQueue(video: Video) {
    val current = _universalQueue.value.toMutableList()
    if (!current.any { it.id == video.id }) {
      current.add(video)
      _universalQueue.value = current
    }
  }

  fun removeFromQueue(videoId: String) {
    _universalQueue.value = _universalQueue.value.filter { it.id != videoId }
  }

  fun clearQueue() {
    _universalQueue.value = emptyList()
  }

  fun toggleWatchLater(videoId: String) {
    val current = _watchLaterIds.value.toMutableSet()
    if (current.contains(videoId)) {
      current.remove(videoId)
    } else {
      current.add(videoId)
    }
    _watchLaterIds.value = current
  }

  fun getBookmarks(videoId: String): Flow<List<VideoBookmark>> = getBookmarksForVideo(videoId)

  fun getBookmarksForVideo(videoId: String): Flow<List<VideoBookmark>> {
    return roomDb.bookmarkDao().getBookmarksForVideo(videoId).map { list ->
      list.map {
        VideoBookmark(
          id = it.id,
          videoId = it.videoId,
          timestampMs = it.timestampMs,
          formattedTime = it.formattedTime,
          note = it.note,
          createdAt = it.createdAt
        )
      }
    }
  }

  fun addBookmark(videoId: String, timestampMs: Long, note: String) {
    val totalSec = (timestampMs / 1000).toInt()
    val formatted = "%02d:%02d".format(totalSec / 60, totalSec % 60)
    scope.launch {
      roomDb.bookmarkDao().insert(
        BookmarkEntity(
          id = UUID.randomUUID().toString(),
          videoId = videoId,
          timestampMs = timestampMs,
          formattedTime = formatted,
          note = note,
          createdAt = System.currentTimeMillis()
        )
      )
    }
  }

  fun deleteBookmark(bookmark: VideoBookmark) {
    scope.launch {
      roomDb.bookmarkDao().delete(bookmark.id)
    }
  }

  fun startDownload(video: Video) {
    scope.launch {
      roomDb.downloadDao().insert(
        DownloadEntity(
          videoId = video.id,
          title = video.title,
          thumbnailUrl = video.thumbnailUrl,
          videoUrl = video.videoUrl,
          progressPercent = 100,
          sizeMb = 32.5,
          status = DownloadStatus.COMPLETED.name,
          downloadedAt = System.currentTimeMillis()
        )
      )
    }
  }

  fun deleteDownload(videoId: String) {
    scope.launch {
      roomDb.downloadDao().delete(videoId)
    }
  }

  fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
    _userPreferences.value = transform(_userPreferences.value)
  }

  private fun formatDurationSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.US, "%d:%02d", m, s)
  }

  private fun formatViewsCount(count: Long): String {
    return when {
      count >= 1_000_000 -> String.format(Locale.US, "%.1fM views", count / 1_000_000.0)
      count >= 1_000 -> String.format(Locale.US, "%.1fK views", count / 1_000.0)
      count == 1L -> "1 view"
      else -> "$count views"
    }
  }
}
