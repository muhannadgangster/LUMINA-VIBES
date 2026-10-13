package com.example.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Video(
  @DocumentId val id: String = "",
  val title: String = "",
  val description: String = "",
  val videoUrl: String = "",
  val thumbnailUrl: String = "",
  val durationSeconds: Int = 0,
  val formattedDuration: String = "0:00",
  val views: Long = 0L,
  val formattedViews: String = "0 views",
  val uploadDate: String = "Recently",
  val channelId: String = "",
  val channelTitle: String = "Creator",
  val channelAvatarUrl: String = "",
  val isVerifiedChannel: Boolean = false,
  val isShort: Boolean = false,
  val category: String = "General",
  val likesCount: Long = 0L,
  val dislikesCount: Long = 0L,
  val isLiked: Boolean = false,
  val isDisliked: Boolean = false,
  val isSubscribed: Boolean = false,
  val creatorId: String = "",
  val status: String = "published",
  val visibility: String = "public",
  val chapters: List<Chapter> = emptyList(),
  val tags: List<String> = emptyList(),
  val keyPoints: List<String> = emptyList()
)

data class UserProfile(
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val avatarUrl: String = "",
  val subscribersCount: Long = 0L,
  val createdAt: String = ""
)

data class Chapter(
  val title: String = "",
  val startTimeSeconds: Int = 0,
  val formattedTime: String = "0:00"
)

data class Channel(
  val id: String = "",
  val name: String = "",
  val handle: String = "",
  val avatarUrl: String = "",
  val bannerUrl: String = "",
  val subscriberCount: Long = 0L,
  val formattedSubscribers: String = "0 subscribers",
  val videoCount: Int = 0,
  val description: String = "",
  val isVerified: Boolean = false,
  val isSubscribed: Boolean = false,
  val notificationLevel: NotificationLevel = NotificationLevel.PERSONALIZED
)

enum class NotificationLevel {
  ALL, PERSONALIZED, NONE
}

@IgnoreExtraProperties
data class Comment(
  @DocumentId val id: String = "",
  val videoId: String = "",
  val userId: String = "",
  val authorName: String = "",
  val authorAvatarUrl: String = "",
  val text: String = "",
  val timestamp: String = "Just now",
  val likesCount: Long = 0L,
  val isLiked: Boolean = false,
  val repliesCount: Int = 0,
  val isCreatorPinned: Boolean = false,
  val replies: List<Comment> = emptyList()
)

data class WatchCollection(
  val id: String = "",
  val name: String = "",
  val description: String = "",
  val videoIds: List<String> = emptyList(),
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
  val totalViews: Long = 0L,
  val totalWatchHours: Double = 0.0,
  val subscribersCount: Long = 0L,
  val estimatedRevenueUsd: Double = 0.0,
  val topVideos: List<Video> = emptyList(),
  val impressionsRate: Double = 0.0,
  val averageViewDuration: String = "0:00"
)
