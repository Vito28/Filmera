package com.example.filmera.feature.community.presentation.review

import androidx.lifecycle.SavedStateHandle
import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.navigation.AppDestination
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.community.domain.CommentRealtimeEvent
import com.example.filmera.feature.community.domain.CommentRealtimeSubscription
import com.example.filmera.feature.community.domain.CommunityAuthor
import com.example.filmera.feature.community.domain.CommunityComment
import com.example.filmera.feature.community.domain.CommunityCursor
import com.example.filmera.feature.community.domain.CommunityPage
import com.example.filmera.feature.community.domain.CommunityRepository
import com.example.filmera.feature.community.domain.CommunityReview
import com.example.filmera.feature.community.domain.HelpfulState
import com.example.filmera.feature.community.domain.ReactionSummary
import com.example.filmera.feature.community.domain.ReviewReaction
import com.example.filmera.feature.community.domain.PublishReviewRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewDetailViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun `failed optimistic comment remains visible with retry state`() = runTest {
    val repository = FakeCommunityRepository(
      createResult = DataResult.Error(AppError.NetworkUnavailable),
    )
    val viewModel = createViewModel(repository)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect {}
    }
    advanceUntilIdle()

    viewModel.onAction(ReviewDetailAction.ComposerTextChanged("A thoughtful comment"))
    viewModel.onAction(ReviewDetailAction.SendComment)
    advanceUntilIdle()

    val thread = (viewModel.uiState.value.commentsState as LoadState.Success).value
    val pending = thread.rootComments().single()
    assertEquals("A thoughtful comment", pending.comment.body)
    assertEquals(CommentDeliveryState.FAILED, pending.deliveryState)
    assertTrue(viewModel.uiState.value.composerText.isEmpty())
  }

  @Test
  fun `reply mode attaches optimistic comment to root and clears mode`() = runTest {
    val root = sampleComment(id = ROOT_ID)
    val reply = sampleComment(id = REPLY_ID, parentId = ROOT_ID)
    val repository = FakeCommunityRepository(
      roots = listOf(root),
      createResult = DataResult.Success(reply),
    )
    val viewModel = createViewModel(repository)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect {}
    }
    advanceUntilIdle()

    viewModel.onAction(ReviewDetailAction.ReplySelected(ROOT_ID, "cinephile"))
    viewModel.onAction(ReviewDetailAction.ComposerTextChanged("I saw it differently"))
    viewModel.onAction(ReviewDetailAction.SendComment)
    advanceUntilIdle()

    val thread = (viewModel.uiState.value.commentsState as LoadState.Success).value
    assertEquals(listOf(REPLY_ID), thread.replyIdsByRootId[ROOT_ID])
    assertEquals(null, viewModel.uiState.value.replyTarget)
    assertEquals(ROOT_ID, repository.lastParentCommentId)
  }

  private fun createViewModel(repository: CommunityRepository) = ReviewDetailViewModel(
    savedStateHandle = SavedStateHandle(
      mapOf(AppDestination.ReviewDetail.REVIEW_ID_ARGUMENT to REVIEW_ID),
    ),
    repository = repository,
  )

  private class FakeCommunityRepository(
    private val roots: List<CommunityComment> = emptyList(),
    private val createResult: DataResult<CommunityComment>,
  ) : CommunityRepository {
    private val realtimeEvents = MutableSharedFlow<CommentRealtimeEvent>()
    var lastParentCommentId: String? = null
      private set

    override suspend fun getReviews(cursor: CommunityCursor?, limit: Int) =
      DataResult.Success(CommunityPage(listOf(sampleReview()), null))

    override suspend fun getReview(reviewId: String) = DataResult.Success(sampleReview())

    override suspend fun publishReview(request: PublishReviewRequest) =
      DataResult.Success(sampleReview())

    override suspend fun setHelpful(reviewId: String, helpful: Boolean) = DataResult.Success(Unit)

    override suspend fun setReaction(reviewId: String, reaction: ReviewReaction?) =
      DataResult.Success(Unit)

    override suspend fun setRating(mediaId: Long, rating: Int?) = DataResult.Success(Unit)

    override suspend fun getRootComments(
      reviewId: String,
      cursor: CommunityCursor?,
      limit: Int,
    ) = DataResult.Success(roots)

    override suspend fun getReplies(
      rootCommentId: String,
      cursor: CommunityCursor?,
      limit: Int,
    ) = DataResult.Success(emptyList<CommunityComment>())

    override suspend fun getComment(commentId: String): DataResult<CommunityComment> =
      DataResult.Error(AppError.NotFound)

    override suspend fun createComment(
      reviewId: String,
      body: String,
      parentCommentId: String?,
    ): DataResult<CommunityComment> {
      lastParentCommentId = parentCommentId
      return createResult
    }

    override suspend fun subscribeToComments(reviewId: String) = DataResult.Success(
      object : CommentRealtimeSubscription {
        override val events: Flow<CommentRealtimeEvent> = realtimeEvents
        override suspend fun close() = Unit
      },
    )
  }

  class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
  ) : TestWatcher() {
    override fun starting(description: Description) {
      Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
      Dispatchers.resetMain()
    }
  }

  private companion object {
    const val REVIEW_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
    const val ROOT_ID = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
    const val REPLY_ID = "cccccccc-cccc-4ccc-8ccc-cccccccccccc"

    fun sampleReview() = CommunityReview(
      id = REVIEW_ID,
      mediaId = 12,
      mediaKey = MediaKey(550, MediaType.MOVIE),
      mediaTitle = "Fight Club",
      posterPath = "/poster.jpg",
      releaseYear = "1999",
      author = CommunityAuthor("author", "cinephile", "Cinephile", null),
      headline = "A strong review",
      body = "A sufficiently long review body for testing the review detail flow.",
      containsSpoilers = false,
      authorRating = 9,
      viewerRating = null,
      filmeraRating = 8.5,
      ratingCount = 20,
      helpful = HelpfulState(false, 0),
      commentCount = 0,
      reactions = ReactionSummary(),
      createdAt = "2026-08-04T10:00:00Z",
      updatedAt = "2026-08-04T10:00:00Z",
    )

    fun sampleComment(id: String, parentId: String? = null) = CommunityComment(
      id = id,
      reviewId = REVIEW_ID,
      parentCommentId = parentId,
      author = CommunityAuthor("author", "cinephile", "Cinephile", null),
      body = "Comment body",
      replyCount = if (parentId == null) 1 else 0,
      isOwnedByViewer = false,
      createdAt = "2026-08-04T10:00:00Z",
      updatedAt = "2026-08-04T10:00:00Z",
    )
  }
}
