package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.VideoRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Lumina", appName)
  }

  @Test
  fun `repository loads seed videos and handles likes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = VideoRepository(context)

    val videos = repo.videos.value
    assertTrue("Should have seed videos loaded", videos.isNotEmpty())

    val firstVideo = videos.first()
    assertNotNull(firstVideo.videoUrl)
    assertNotNull(firstVideo.thumbnailUrl)

    val initialLikes = firstVideo.likesCount
    repo.toggleLike(firstVideo.id)
    val updatedVideo = repo.videos.value.first { it.id == firstVideo.id }
    assertTrue(updatedVideo.isLiked)
    assertEquals(initialLikes + 1, updatedVideo.likesCount)
  }
}
