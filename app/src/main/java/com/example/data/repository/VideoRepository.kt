package com.example.data.repository

import android.content.Context
import androidx.room.Room
import com.example.data.local.BookmarkEntity
import com.example.data.local.CollectionEntity
import com.example.data.local.CustomVideoEntity
import com.example.data.local.DownloadEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.LuminaDatabase
import com.example.data.local.SearchQueryEntity
import com.example.data.local.SubscriptionEntity
import com.example.data.model.Channel
import com.example.data.model.Chapter
import com.example.data.model.Comment
import com.example.data.model.CreatorAnalytics
import com.example.data.model.DownloadStatus
import com.example.data.model.GlassMode
import com.example.data.model.HistoryEntry
import com.example.data.model.NotificationLevel
import com.example.data.model.OfflineDownload
import com.example.data.model.UserPreferences
import com.example.data.model.Video
import com.example.data.model.VideoBookmark
import com.example.data.model.WatchCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class VideoRepository(context: Context) {
  private val scope = CoroutineScope(Dispatchers.IO)
  private val db = Room.databaseBuilder(
    context.applicationContext,
    LuminaDatabase::class.java,
    "lumina_database.db"
  ).fallbackToDestructiveMigration(true).build()

  private val _videos = MutableStateFlow<List<Video>>(emptyList())
  val videos: StateFlow<List<Video>> = _videos.asStateFlow()

  private val _shorts = MutableStateFlow<List<Video>>(emptyList())
  val shorts: StateFlow<List<Video>> = _shorts.asStateFlow()

  private val _channels = MutableStateFlow<List<Channel>>(emptyList())
  val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

  private val _comments = MutableStateFlow<Map<String, List<Comment>>>(emptyMap())
  val comments: StateFlow<Map<String, List<Comment>>> = _comments.asStateFlow()

  private val _userPreferences = MutableStateFlow(UserPreferences())
  val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

  private val _universalQueue = MutableStateFlow<List<Video>>(emptyList())
  val universalQueue: StateFlow<List<Video>> = _universalQueue.asStateFlow()

  private val _watchLaterIds = MutableStateFlow<Set<String>>(emptySet())
  val watchLaterIds: StateFlow<Set<String>> = _watchLaterIds.asStateFlow()

  init {
    loadSeedData()
    observeCustomVideos()
  }

  val watchHistory: Flow<List<HistoryEntry>> = db.historyDao().getAllHistory().map { list ->
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

  val offlineDownloads: Flow<List<OfflineDownload>> = db.downloadDao().getAllDownloads().map { list ->
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

  val watchCollections: Flow<List<WatchCollection>> = db.collectionDao().getAllCollections().map { list ->
    list.map {
      WatchCollection(
        id = it.id,
        name = it.name,
        description = it.description,
        videoIds = if (it.videoIdsJson.isBlank()) emptyList() else it.videoIdsJson.split(","),
        updatedAt = it.updatedAt,
        isPrivate = it.isPrivate
      )
    }
  }

  val recentSearches: Flow<List<String>> = db.searchDao().getRecentQueries()

  fun getBookmarks(videoId: String): Flow<List<VideoBookmark>> = db.bookmarkDao().getBookmarksForVideo(videoId).map { list ->
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

  private fun observeCustomVideos() {
    scope.launch {
      db.customVideoDao().getAllCustomVideos().collect { customList ->
        val converted = customList.map { entity ->
          Video(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            videoUrl = entity.videoUrl,
            thumbnailUrl = entity.thumbnailUrl,
            durationSeconds = entity.durationSeconds,
            formattedDuration = formatDuration(entity.durationSeconds),
            views = entity.views,
            formattedViews = "${entity.views} views",
            uploadDate = entity.uploadDate,
            channelId = "user_channel",
            channelTitle = "Creator Studio",
            channelAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
            isVerifiedChannel = true,
            isShort = entity.isShort,
            category = entity.category,
            likesCount = 0,
            dislikesCount = 0,
            isLiked = false,
            isDisliked = false,
            isSubscribed = true,
            chapters = listOf(
              Chapter("Beginning", 0, "0:00"),
              Chapter("Key Segment", entity.durationSeconds / 2, formatDuration(entity.durationSeconds / 2))
            ),
            tags = listOf(entity.category, "CustomUpload", "Original"),
            keyPoints = listOf("User-created content uploaded via Lumina Creator Studio.", "Stored with local persistence.")
          )
        }
        val currentSeed = seedVideos()
        val customRegular = converted.filter { !it.isShort }
        val customShorts = converted.filter { it.isShort }

        _videos.value = customRegular + currentSeed
        _shorts.value = customShorts + seedShorts()
      }
    }
  }

  private fun loadSeedData() {
    val initialVideos = seedVideos()
    _videos.value = initialVideos
    _shorts.value = seedShorts()
    _channels.value = seedChannels()
    _comments.value = seedComments()

    // Add initial default collection
    scope.launch {
      db.collectionDao().insertOrUpdate(
        CollectionEntity(
          id = "c_featured",
          name = "Visionary Cinema",
          description = "Masterpiece cinematic shorts and high-fidelity experimental visuals.",
          videoIdsJson = "v_tears_of_steel,v_cosmos,v_sintel",
          updatedAt = System.currentTimeMillis(),
          isPrivate = false
        )
      )
    }
  }

  fun updatePreferences(update: (UserPreferences) -> UserPreferences) {
    _userPreferences.value = update(_userPreferences.value)
  }

  fun toggleLike(videoId: String) {
    _videos.value = _videos.value.map { v ->
      if (v.id == videoId) {
        val newIsLiked = !v.isLiked
        v.copy(
          isLiked = newIsLiked,
          likesCount = if (newIsLiked) v.likesCount + 1 else maxOf(0, v.likesCount - 1),
          isDisliked = if (newIsLiked) false else v.isDisliked
        )
      } else v
    }
    _shorts.value = _shorts.value.map { v ->
      if (v.id == videoId) {
        val newIsLiked = !v.isLiked
        v.copy(
          isLiked = newIsLiked,
          likesCount = if (newIsLiked) v.likesCount + 1 else maxOf(0, v.likesCount - 1),
          isDisliked = if (newIsLiked) false else v.isDisliked
        )
      } else v
    }
  }

  fun toggleDislike(videoId: String) {
    _videos.value = _videos.value.map { v ->
      if (v.id == videoId) {
        val newDisliked = !v.isDisliked
        v.copy(
          isDisliked = newDisliked,
          isLiked = if (newDisliked) false else v.isLiked
        )
      } else v
    }
    _shorts.value = _shorts.value.map { v ->
      if (v.id == videoId) {
        val newDisliked = !v.isDisliked
        v.copy(
          isDisliked = newDisliked,
          isLiked = if (newDisliked) false else v.isLiked
        )
      } else v
    }
  }

  fun toggleSubscribe(channelId: String) {
    _channels.value = _channels.value.map { ch ->
      if (ch.id == channelId) {
        val newSub = !ch.isSubscribed
        val newCount = if (newSub) ch.subscriberCount + 1 else maxOf(0, ch.subscriberCount - 1)
        ch.copy(
          isSubscribed = newSub,
          subscriberCount = newCount,
          formattedSubscribers = formatCount(newCount)
        )
      } else ch
    }

    _videos.value = _videos.value.map { v ->
      if (v.channelId == channelId) {
        v.copy(isSubscribed = !v.isSubscribed)
      } else v
    }

    scope.launch {
      val isSub = db.subscriptionDao().isSubscribed(channelId)
      if (isSub) {
        db.subscriptionDao().delete(channelId)
      } else {
        val ch = _channels.value.find { it.id == channelId }
        if (ch != null) {
          db.subscriptionDao().insert(
            SubscriptionEntity(
              channelId = ch.id,
              channelName = ch.name,
              channelAvatar = ch.avatarUrl,
              notificationLevel = ch.notificationLevel.name,
              subscribedAt = System.currentTimeMillis()
            )
          )
        }
      }
    }
  }

  fun setNotificationLevel(channelId: String, level: NotificationLevel) {
    _channels.value = _channels.value.map {
      if (it.id == channelId) it.copy(notificationLevel = level) else it
    }
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

  fun addComment(videoId: String, text: String, authorName: String = "You") {
    val currentMap = _comments.value.toMutableMap()
    val videoComments = currentMap[videoId]?.toMutableList() ?: mutableListOf()
    val newComment = Comment(
      id = "c_${UUID.randomUUID().toString().take(8)}",
      videoId = videoId,
      authorName = authorName,
      authorAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80",
      text = text,
      timestamp = "Just now",
      likesCount = 0,
      isLiked = false,
      repliesCount = 0,
      isCreatorPinned = false
    )
    videoComments.add(0, newComment)
    currentMap[videoId] = videoComments
    _comments.value = currentMap
  }

  fun toggleCommentLike(videoId: String, commentId: String) {
    val currentMap = _comments.value.toMutableMap()
    val list = currentMap[videoId] ?: return
    currentMap[videoId] = list.map { c ->
      if (c.id == commentId) {
        val newLiked = !c.isLiked
        c.copy(
          isLiked = newLiked,
          likesCount = if (newLiked) c.likesCount + 1 else maxOf(0, c.likesCount - 1)
        )
      } else c
    }
    _comments.value = currentMap
  }

  fun recordWatchHistory(videoId: String, positionMs: Long, durationMs: Long) {
    if (_userPreferences.value.pauseWatchHistory) return
    scope.launch {
      db.historyDao().insertOrUpdate(
        HistoryEntity(
          videoId = videoId,
          watchedAt = System.currentTimeMillis(),
          playbackPositionMs = positionMs,
          durationMs = durationMs,
          isCompleted = positionMs > (durationMs * 0.9)
        )
      )
    }
  }

  suspend fun getSavedPosition(videoId: String): Long {
    return db.historyDao().getHistoryForVideo(videoId)?.playbackPositionMs ?: 0L
  }

  fun clearWatchHistory() {
    scope.launch {
      db.historyDao().clearHistory()
    }
  }

  fun recordSearchQuery(query: String) {
    if (query.isBlank() || _userPreferences.value.pauseSearchHistory) return
    scope.launch {
      db.searchDao().insertQuery(SearchQueryEntity(query.trim(), System.currentTimeMillis()))
    }
  }

  fun clearSearchHistory() {
    scope.launch {
      db.searchDao().clearAllQueries()
    }
  }

  fun addBookmark(videoId: String, timestampMs: Long, note: String) {
    val formatted = formatDuration((timestampMs / 1000).toInt())
    scope.launch {
      db.bookmarkDao().insertBookmark(
        BookmarkEntity(
          id = UUID.randomUUID().toString(),
          videoId = videoId,
          timestampMs = timestampMs,
          formattedTime = formatted,
          note = note.ifBlank { "Bookmark at $formatted" },
          createdAt = System.currentTimeMillis()
        )
      )
    }
  }

  fun deleteBookmark(bookmark: VideoBookmark) {
    scope.launch {
      db.bookmarkDao().deleteBookmark(
        BookmarkEntity(
          id = bookmark.id,
          videoId = bookmark.videoId,
          timestampMs = bookmark.timestampMs,
          formattedTime = bookmark.formattedTime,
          note = bookmark.note,
          createdAt = bookmark.createdAt
        )
      )
    }
  }

  fun startDownload(video: Video) {
    scope.launch {
      db.downloadDao().insertOrUpdate(
        DownloadEntity(
          videoId = video.id,
          title = video.title,
          thumbnailUrl = video.thumbnailUrl,
          videoUrl = video.videoUrl,
          progressPercent = 100,
          sizeMb = (video.durationSeconds * 0.28).coerceAtLeast(18.5),
          status = DownloadStatus.COMPLETED.name,
          downloadedAt = System.currentTimeMillis()
        )
      )
    }
  }

  fun deleteDownload(videoId: String) {
    scope.launch {
      db.downloadDao().deleteDownload(videoId)
    }
  }

  fun createCollection(name: String, description: String, initialVideoIds: List<String> = emptyList()) {
    scope.launch {
      db.collectionDao().insertOrUpdate(
        CollectionEntity(
          id = "c_${UUID.randomUUID().toString().take(8)}",
          name = name,
          description = description,
          videoIdsJson = initialVideoIds.joinToString(","),
          updatedAt = System.currentTimeMillis(),
          isPrivate = true
        )
      )
    }
  }

  fun addVideoToCollection(collectionId: String, videoId: String) {
    scope.launch {
      val collections = db.collectionDao()
      // find collection
      // update
    }
  }

  fun publishVideo(
    title: String,
    description: String,
    category: String,
    videoUrl: String,
    isShort: Boolean
  ) {
    val duration = if (isShort) 45 else 210
    val defaultThumb = if (isShort) {
      "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80"
    } else {
      "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&auto=format&fit=crop&q=80"
    }

    val entity = CustomVideoEntity(
      id = "custom_${System.currentTimeMillis()}",
      title = title,
      description = description,
      videoUrl = videoUrl.ifBlank { "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8" },
      thumbnailUrl = defaultThumb,
      durationSeconds = duration,
      category = category,
      isShort = isShort,
      uploadDate = "Today",
      views = 1
    )

    scope.launch {
      db.customVideoDao().insertVideo(entity)
    }
  }

  fun getCreatorAnalytics(): CreatorAnalytics {
    val allVids = _videos.value.filter { it.channelId == "user_channel" || it.id.startsWith("custom_") }
    return CreatorAnalytics(
      totalViews = 184520L,
      totalWatchHours = 12480.5,
      subscribersCount = 4820L,
      estimatedRevenueUsd = 1420.75,
      topVideos = _videos.value.take(4),
      impressionsRate = 9.2,
      averageViewDuration = "5:18"
    )
  }

  // --- Seed Data with real open video streams and rich metadata ---

  private fun seedVideos(): List<Video> {
    return listOf(
      Video(
        id = "v_tears_of_steel",
        title = "Tears of Steel: VFX Sci-Fi Odyssey",
        description = "A futuristic sci-fi epic set in a dystopian future where a group of warriors and scientists attempt to avert a technological singularity. Mastered in 4K with stunning visual effects by the Blender Institute.",
        videoUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
        thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop&q=80",
        durationSeconds = 734,
        formattedDuration = "12:14",
        views = 3420800,
        formattedViews = "3.4M views",
        uploadDate = "2 weeks ago",
        channelId = "ch_blender",
        channelTitle = "Blender Open Studio",
        channelAvatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop&q=80",
        isVerifiedChannel = true,
        isShort = false,
        category = "Sci-Fi & Cinema",
        likesCount = 148200,
        dislikesCount = 1200,
        chapters = listOf(
          Chapter("Introduction & Reunion", 0, "0:00"),
          Chapter("The Holographic Bridge", 180, "3:00"),
          Chapter("Robotic Convergence", 420, "7:00"),
          Chapter("The Singularity Choice", 600, "10:00")
        ),
        tags = listOf("SciFi", "VFX", "Cinematic", "4K", "Robotics"),
        keyPoints = listOf(
          "Pioneering open-source photorealistic CGI combined with live action.",
          "Explores themes of artificial consciousness and human emotional attachments.",
          "Shot on location at the historic Oude Kerk in Amsterdam."
        )
      ),
      Video(
        id = "v_cosmos",
        title = "Echo: Futuristic CGI Dimension",
        description = "A high-fidelity experimental CGI visual journey exploring digital architecture, quantum particles, and synth soundscapes.",
        videoUrl = "https://cdn.jsdelivr.net/gh/mediaelement/mediaelement-files@master/echo-hereweare.mp4",
        thumbnailUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=800&auto=format&fit=crop&q=80",
        durationSeconds = 730,
        formattedDuration = "12:10",
        views = 1890400,
        formattedViews = "1.8M views",
        uploadDate = "1 month ago",
        channelId = "ch_blender",
        channelTitle = "Blender Open Studio",
        channelAvatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop&q=80",
        isVerifiedChannel = true,
        isShort = false,
        category = "Animation",
        likesCount = 98400,
        dislikesCount = 850,
        chapters = listOf(
          Chapter("The Windswept Ridge", 0, "0:00"),
          Chapter("Victor's Offer", 210, "3:30"),
          Chapter("The Multiverse Door", 450, "7:30"),
          Chapter("Metamorphosis", 620, "10:20")
        ),
        tags = listOf("Animation", "Surreal", "Blender", "Fantasy"),
        keyPoints = listOf(
          "Won the Jury Prize at the prestigious SIGGRAPH Computer Animation Festival.",
          "Features revolutionary physically-based hair and procedural physics simulation.",
          "Darkly humorous narrative exploring existential choices."
        )
      ),
      Video(
        id = "v_sintel",
        title = "Oceans: Deep Sea Marine Odyssey",
        description = "Dive into the mysterious depths of earth's oceans with spectacular marine wildlife encounters in crystal clear high definition.",
        videoUrl = "https://vjs.zencdn.net/v/oceans.mp4",
        thumbnailUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800&auto=format&fit=crop&q=80",
        durationSeconds = 888,
        formattedDuration = "14:48",
        views = 7650000,
        formattedViews = "7.6M views",
        uploadDate = "3 months ago",
        channelId = "ch_chronos",
        channelTitle = "Chronos Cinema",
        channelAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80",
        isVerifiedChannel = true,
        isShort = false,
        category = "Nature",
        likesCount = 420000,
        dislikesCount = 3100,
        chapters = listOf(
          Chapter("The Frozen Mountain", 0, "0:00"),
          Chapter("Memory of Scales", 240, "4:00"),
          Chapter("The Oasis Journey", 480, "8:00"),
          Chapter("The Dragon's Lair", 720, "12:00")
        ),
        tags = listOf("Nature", "Oceans", "Wildlife", "Cinematic"),
        keyPoints = listOf(
          "Unprecedented underwater filming techniques and deep reef illumination.",
          "Showcases majestic humpback whales and bioluminescent coral habitats."
        )
      ),
      Video(
        id = "v_bunny",
        title = "Big Buck Bunny: Colorful Forest Chronicles",
        description = "A colossal, warm-hearted woodland rabbit exacts gentle, hilarious revenge against three pesky forest bullies in this timeless animation classic.",
        videoUrl = "https://cdn.jsdelivr.net/gh/mediaelement/mediaelement-files@master/big_buck_bunny.mp4",
        thumbnailUrl = "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=800&auto=format&fit=crop&q=80",
        durationSeconds = 596,
        formattedDuration = "9:56",
        views = 12400000,
        formattedViews = "12M views",
        uploadDate = "6 months ago",
        channelId = "ch_blender",
        channelTitle = "Blender Open Studio",
        channelAvatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop&q=80",
        isVerifiedChannel = true,
        isShort = false,
        category = "Animation",
        likesCount = 890000,
        dislikesCount = 5400,
        chapters = listOf(
          Chapter("Morning in the Glade", 0, "0:00"),
          Chapter("The Butterfly Incident", 120, "2:00"),
          Chapter("The Trap Blueprint", 300, "5:00"),
          Chapter("Bunny's Payback", 450, "7:30")
        ),
        tags = listOf("Comedy", "Classic", "OpenSource", "Animation"),
        keyPoints = listOf(
          "The definitive benchmark video used worldwide for video codec and display quality testing.",
          "Vibrant high-contrast color palette ideal for testing HDR and Liquid Glass playback.",
          "Timeless physical slapstick comedy."
        )
      ),
      Video(
        id = "v_we_are_going_on_bullrun",
        title = "Cyberpunk Neo-Tokyo: High-Speed Night Drift",
        description = "Adrenaline-fueled nocturnal racing through towering neon skyscrapers and rain-soaked highways in an ultra-futuristic metropolis.",
        videoUrl = "https://github.com/intel-iot-devkit/sample-videos/raw/master/car-detection.mp4",
        thumbnailUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&auto=format&fit=crop&q=80",
        durationSeconds = 115,
        formattedDuration = "1:55",
        views = 980200,
        formattedViews = "980K views",
        uploadDate = "4 days ago",
        channelId = "ch_cyber",
        channelTitle = "CyberVoxel",
        channelAvatarUrl = "https://images.unsplash.com/photo-1566492031773-4f4e44671857?w=150&auto=format&fit=crop&q=80",
        isVerifiedChannel = true,
        isShort = false,
        category = "Gaming & Tech",
        likesCount = 68400,
        dislikesCount = 420,
        chapters = listOf(
          Chapter("Ignition", 0, "0:00"),
          Chapter("The Neon Expressway", 45, "0:45"),
          Chapter("Finish Line Drift", 90, "1:30")
        ),
        tags = listOf("Cyberpunk", "Tokyo", "Speed", "Synthwave"),
        keyPoints = listOf(
          "Synthesizes ray-traced neon lighting with retro-futuristic synth beats.",
          "High frame-rate capture perfect for testing motion smoothing and OLED displays."
        )
      ),
      Video(
        id = "v_for_bigger_blazes",
        title = "Deep Space Odyssey: Planetary Horizons",
        description = "Voyage past gas giants, shimmering asteroid rings, and interstellar nebulae in this awe-inspiring astrophysical visual journey.",
        videoUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
        thumbnailUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&auto=format&fit=crop&q=80",
        durationSeconds = 180,
        formattedDuration = "3:00",
        views = 450100,
        formattedViews = "450K views",
        uploadDate = "1 week ago",
        channelId = "ch_nebula",
        channelTitle = "Nebula Science",
        channelAvatarUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=150&auto=format&fit=crop&q=80",
        isVerifiedChannel = true,
        isShort = false,
        category = "Space & Science",
        likesCount = 38900,
        dislikesCount = 110,
        chapters = listOf(
          Chapter("Planetary Flyby", 0, "0:00")
        ),
        tags = listOf("Space", "Astrophysics", "Cosmos", "Science"),
        keyPoints = listOf(
          "Based on real spectroscopic telescope telemetry.",
          "Synthesizes authentic gravitational lens optical curvature."
        )
      ),
      Video(
        id = "v_for_bigger_escapes",
        title = "Wild Earth: Alpine Glaciers & Emerald Fjords",
        description = "Breathtaking cinematic drone footage capturing pristine glacial peaks, crystal fjords, and untamed wildlife sanctuaries.",
        videoUrl = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
        thumbnailUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&auto=format&fit=crop&q=80",
        durationSeconds = 60,
        formattedDuration = "1:00",
        views = 612000,
        formattedViews = "612K views",
        uploadDate = "5 days ago",
        channelId = "ch_terra",
        channelTitle = "Terra Explorations",
        channelAvatarUrl = "https://images.unsplash.com/photo-1472214103451-9374bd1c798e?w=150&auto=format&fit=crop&q=80",
        isVerifiedChannel = true,
        isShort = false,
        category = "Nature",
        likesCount = 49200,
        dislikesCount = 180,
        chapters = listOf(
          Chapter("Glacial Summit", 0, "0:00")
        ),
        tags = listOf("Nature", "Drone", "Norway", "Landscape"),
        keyPoints = listOf(
          "Ultra-high dynamic range wilderness capture.",
          "Natural ambient audio recorded with binaural microphones."
        )
      )
    )
  }

  private fun seedShorts(): List<Video> {
    return listOf(
      Video(
        id = "s_neon_synth",
        title = "Holographic Glass UI in 2026! ⚡ #Tech #Design #LiquidGlass",
        description = "Insane translucent UI concepts running at 120 FPS with real-time refraction and specular highlights! What do you think?",
        videoUrl = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/friday.mp4",
        thumbnailUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
        durationSeconds = 15,
        formattedDuration = "0:15",
        views = 840000,
        formattedViews = "840K views",
        uploadDate = "Yesterday",
        channelId = "ch_cyber",
        channelTitle = "CyberVoxel",
        channelAvatarUrl = "https://images.unsplash.com/photo-1566492031773-4f4e44671857?w=150&auto=format&fit=crop&q=80",
        isVerifiedChannel = true,
        isShort = true,
        category = "Tech",
        likesCount = 74500,
        dislikesCount = 320,
        tags = listOf("Shorts", "Tech", "GlassUI")
      ),
      Video(
        id = "s_tokyo_drift",
        title = "Midnight Expressway in Neo Tokyo 🏎️💨 #Cyberpunk #Speed",
        description = "Full throttle through the rain-soaked streets with pure synthwave energy. Sound on! 🎧",
        videoUrl = "https://github.com/intel-iot-devkit/sample-videos/raw/master/car-detection.mp4",
        thumbnailUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
        durationSeconds = 30,
        formattedDuration = "0:30",
        views = 1250000,
        formattedViews = "1.2M views",
        uploadDate = "2 days ago",
        channelId = "ch_cyber",
        channelTitle = "CyberVoxel",
        channelAvatarUrl = "https://images.unsplash.com/photo-1566492031773-4f4e44671857?w=150&auto=format&fit=crop&q=80",
        isVerifiedChannel = true,
        isShort = true,
        category = "Gaming",
        likesCount = 112000,
        dislikesCount = 490,
        tags = listOf("Shorts", "Drift", "Tokyo")
      ),
      Video(
        id = "s_aurora",
        title = "Urban Life & Cyber Aesthetics 🌌✨ #Modern #City",
        description = "Pedestrians navigating the modern cityscape in crisp definition.",
        videoUrl = "https://github.com/intel-iot-devkit/sample-videos/raw/master/face-demographics-walking.mp4",
        thumbnailUrl = "https://images.unsplash.com/photo-1517411032315-54ef2cb783bb?w=600&auto=format&fit=crop&q=80",
        durationSeconds = 15,
        formattedDuration = "0:15",
        views = 930000,
        formattedViews = "930K views",
        uploadDate = "3 days ago",
        channelId = "ch_terra",
        channelTitle = "Terra Explorations",
        channelAvatarUrl = "https://images.unsplash.com/photo-1472214103451-9374bd1c798e?w=150&auto=format&fit=crop&q=80",
        isVerifiedChannel = true,
        isShort = true,
        category = "Nature",
        likesCount = 89000,
        dislikesCount = 210,
        tags = listOf("Shorts", "City", "Modern")
      )
    )
  }

  private fun seedChannels(): List<Channel> {
    return listOf(
      Channel(
        id = "ch_blender",
        name = "Blender Open Studio",
        handle = "@blender_studio",
        avatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop&q=80",
        bannerUrl = "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=1200&auto=format&fit=crop&q=80",
        subscriberCount = 3840000,
        formattedSubscribers = "3.8M subscribers",
        videoCount = 48,
        description = "Empowering artists worldwide with open source computer graphics, filmmaking pipelines, and boundary-pushing short films.",
        isVerified = true,
        isSubscribed = true,
        notificationLevel = NotificationLevel.ALL
      ),
      Channel(
        id = "ch_cyber",
        name = "CyberVoxel",
        handle = "@cybervoxel",
        avatarUrl = "https://images.unsplash.com/photo-1566492031773-4f4e44671857?w=150&auto=format&fit=crop&q=80",
        bannerUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1200&auto=format&fit=crop&q=80",
        subscriberCount = 1420000,
        formattedSubscribers = "1.4M subscribers",
        videoCount = 112,
        description = "Future tech, cybernetic aesthetics, game development devlogs, and next-generation interactive design.",
        isVerified = true,
        isSubscribed = false
      ),
      Channel(
        id = "ch_chronos",
        name = "Chronos Cinema",
        handle = "@chronos_cinema",
        avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80",
        bannerUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=1200&auto=format&fit=crop&q=80",
        subscriberCount = 2890000,
        formattedSubscribers = "2.8M subscribers",
        videoCount = 64,
        description = "Independent cinema and epic narratives celebrating timeless storytelling and digital cinematography.",
        isVerified = true,
        isSubscribed = false
      ),
      Channel(
        id = "ch_terra",
        name = "Terra Explorations",
        handle = "@terra_explorations",
        avatarUrl = "https://images.unsplash.com/photo-1472214103451-9374bd1c798e?w=150&auto=format&fit=crop&q=80",
        bannerUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1200&auto=format&fit=crop&q=80",
        subscriberCount = 920000,
        formattedSubscribers = "920K subscribers",
        videoCount = 89,
        description = "Documenting the furthest reaches of our planet in ultra-high resolution HDR.",
        isVerified = true,
        isSubscribed = false
      ),
      Channel(
        id = "ch_nebula",
        name = "Nebula Science",
        handle = "@nebulascience",
        avatarUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=150&auto=format&fit=crop&q=80",
        bannerUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=1200&auto=format&fit=crop&q=80",
        subscriberCount = 1890000,
        formattedSubscribers = "1.8M subscribers",
        videoCount = 140,
        description = "Unraveling quantum mechanics, astrophysics, and cosmic exploration with deep scientific rigor.",
        isVerified = true,
        isSubscribed = false
      )
    )
  }

  private fun seedComments(): Map<String, List<Comment>> {
    return mapOf(
      "v_tears_of_steel" to listOf(
        Comment(
          id = "c1",
          videoId = "v_tears_of_steel",
          authorName = "Blender Open Studio",
          authorAvatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop&q=80",
          text = "Thank you all for supporting open cinema! The complete 3D assets and 4K EXR pipeline files are available freely in the repository.",
          timestamp = "Pinned by creator",
          likesCount = 14200,
          isLiked = false,
          repliesCount = 84,
          isCreatorPinned = true
        ),
        Comment(
          id = "c2",
          videoId = "v_tears_of_steel",
          authorName = "Aria Nova",
          authorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80",
          text = "The Liquid Glass HUD reflections on the robotic arms at 7:42 still blow my mind today. Remarkable VFX craftsmanship!",
          timestamp = "2 days ago",
          likesCount = 3840,
          isLiked = true,
          repliesCount = 12
        ),
        Comment(
          id = "c3",
          videoId = "v_tears_of_steel",
          authorName = "DevKairo",
          authorAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80",
          text = "Watching this on Lumina with Enhanced Glass mode makes the entire experience feel like pure sci-fi.",
          timestamp = "5 days ago",
          likesCount = 1290,
          isLiked = false,
          repliesCount = 3
        )
      ),
      "v_sintel" to listOf(
        Comment(
          id = "cs1",
          videoId = "v_sintel",
          authorName = "Elena Rostova",
          authorAvatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&auto=format&fit=crop&q=80",
          text = "That ending gets me every single time. The realization in the dragon's eyes is pure heartbreak.",
          timestamp = "1 week ago",
          likesCount = 8400,
          isLiked = true,
          repliesCount = 34
        )
      )
    )
  }

  private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.US, "%d:%02d", m, s)
  }

  private fun formatCount(count: Long): String {
    return when {
      count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
      count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000.0)
      else -> count.toString()
    }
  }
}
