package com.example.data.model

data class Video(
  val id: String,
  val title: String,
  val description: String,
  val videoUrl: String,
  val thumbnailUrl: String,
  val durationSeconds: Int,
  val formattedDuration: String,
  val views: Long,
  val formattedViews: String,
  val uploadDate: String,
  val channelId: String,
  val channelTitle: String,
  val channelAvatarUrl: String,
  val isVerifiedChannel: Boolean = true,
  val isShort: Boolean = false,
  val category: String = "General",
  val likesCount: Long = 0,
  val dislikesCount: Long = 0,
  val isLiked: Boolean = false,
  val isDisliked: Boolean = false,
  val isSubscribed: Boolean = false,
  val chapters: List<Chapter> = emptyList(),
  val tags: List<String> = emptyList(),
  val keyPoints: List<String> = emptyList()
)

data class Chapter(
  val title: String,
  val startTimeSeconds: Int,
  val formattedTime: String
)

data class Channel(
  val id: String,
  val name: String,
  val handle: String,
  val avatarUrl: String,
  val bannerUrl: String,
  val subscriberCount: Long,
  val formattedSubscribers: String,
  val videoCount: Int,
  val description: String,
  val isVerified: Boolean = true,
  val isSubscribed: Boolean = false,
  val notificationLevel: NotificationLevel = NotificationLevel.PERSONALIZED
)

enum class NotificationLevel {
  ALL, PERSONALIZED, NONE
}

data class Comment(
  val id: String,
  val videoId: String,
  val authorName: String,
  val authorAvatarUrl: String,
  val text: String,
  val timestamp: String,
  val likesCount: Long = 0,
  val isLiked: Boolean = false,
  val repliesCount: Int = 0,
  val isCreatorPinned: Boolean = false,
  val replies: List<Comment> = emptyList()
)

data class WatchCollection(
  val id: String,
  val name: String,
  val description: String,
  val videoIds: List<String>,
  val updatedAt: Long = System.currentTimeMillis(),
  val isPrivate: Boolean = true
)

data class HistoryEntry(
  val videoId: String,
  val watchedAt: Long,
  val playbackPositionMs: Long,
  val durationMs: Long,
  val isCompleted: Boolean = false
)

data class OfflineDownload(
  val videoId: String,
  val title: String,
  val thumbnailUrl: String,
  val videoUrl: String,
  val progressPercent: Int = 100,
  val sizeMb: Double = 45.2,
  val status: DownloadStatus = DownloadStatus.COMPLETED,
  val downloadedAt: Long = System.currentTimeMillis()
)

enum class DownloadStatus {
  DOWNLOADING, PAUSED, COMPLETED, ERROR
}

data class VideoBookmark(
  val id: String,
  val videoId: String,
  val timestampMs: Long,
  val formattedTime: String,
  val note: String,
  val createdAt: Long = System.currentTimeMillis()
)

data class CreatorAnalytics(
  val totalViews: Long,
  val totalWatchHours: Double,
  val subscribersCount: Long,
  val estimatedRevenueUsd: Double,
  val topVideos: List<Video>,
  val impressionsRate: Double = 8.4,
  val averageViewDuration: String = "4:32"
)
