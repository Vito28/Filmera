package com.example.filmera.feature.home.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.ui.LoadState
import com.example.filmera.core.common.AppError
import com.example.filmera.feature.community.domain.CommentRealtimeSubscription
import com.example.filmera.feature.community.domain.CommunityComment
import com.example.filmera.feature.community.domain.CommunityAuthor
import com.example.filmera.feature.community.domain.CommunityCursor
import com.example.filmera.feature.community.domain.CommunityPage
import com.example.filmera.feature.community.domain.CommunityRepository
import com.example.filmera.feature.community.domain.CommunityReview
import com.example.filmera.feature.community.domain.HelpfulState
import com.example.filmera.feature.community.domain.ReactionSummary
import com.example.filmera.feature.community.domain.ReviewReaction
import com.example.filmera.feature.community.domain.PublishReviewRequest
import com.example.filmera.feature.home.domain.AnimeTopic
import com.example.filmera.feature.home.domain.HomeChannel
import com.example.filmera.feature.home.domain.HomeContent
import com.example.filmera.feature.home.domain.HomeFeedKey
import com.example.filmera.feature.home.domain.HomeRepository
import com.example.filmera.feature.home.domain.HomeSection
import com.example.filmera.feature.home.domain.HomeSectionPage
import com.example.filmera.feature.home.domain.HomeSectionType
import com.example.filmera.feature.library.domain.LibraryItem
import com.example.filmera.feature.library.domain.LibraryRepository
import com.example.filmera.feature.library.domain.WatchStatus
import com.example.filmera.feature.preferences.domain.PreferenceRepository
import com.example.filmera.feature.preferences.domain.UserPreferenceProfile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun `home starts with global For You feed`() = runTest {
    val homeRepository = DeferredHomeRepository()
    val forYou = HomeFeedKey(HomeChannel.FOR_YOU)
    homeRepository.complete(forYou, DataResult.Success(content(forYou)))

    val viewModel = createViewModel(homeRepository)
    collect(viewModel)
    advanceUntilIdle()

    assertEquals(HomeChannel.FOR_YOU, viewModel.uiState.value.selectedChannel)
    assertEquals(listOf(forYou), homeRepository.requests)
  }

  @Test
  fun `latest channel response wins when previous request is cancelled`() = runTest {
    val homeRepository = DeferredHomeRepository()
    val viewModel = createViewModel(homeRepository)
    collect(viewModel)
    runCurrent()

    val korea = HomeFeedKey(HomeChannel.KOREA)
    viewModel.onAction(HomeAction.ChannelSelected(HomeChannel.KOREA))
    runCurrent()
    homeRepository.complete(korea, DataResult.Success(content(korea)))
    advanceUntilIdle()

    val loaded = viewModel.uiState.value.contentState as LoadState.Success
    assertEquals(HomeChannel.KOREA, viewModel.uiState.value.selectedChannel)
    assertEquals(korea, loaded.value.feedKey)
    assertEquals(
      listOf(HomeFeedKey(HomeChannel.FOR_YOU), korea),
      homeRepository.requests,
    )
  }

  @Test
  fun `channel refresh preserves cached content until replacement arrives`() = runTest {
    val homeRepository = DeferredHomeRepository()
    val forYou = HomeFeedKey(HomeChannel.FOR_YOU)
    val korea = HomeFeedKey(HomeChannel.KOREA)
    homeRepository.complete(forYou, DataResult.Success(content(forYou)))
    homeRepository.complete(korea, DataResult.Success(content(korea)))
    val viewModel = createViewModel(homeRepository)
    collect(viewModel)
    advanceUntilIdle()

    viewModel.onAction(HomeAction.ChannelSelected(HomeChannel.KOREA))
    advanceUntilIdle()
    homeRepository.reset(forYou)
    viewModel.onAction(HomeAction.ChannelSelected(HomeChannel.FOR_YOU))
    runCurrent()

    val visibleContent = viewModel.uiState.value.contentState as LoadState.Success
    assertEquals(forYou, visibleContent.value.feedKey)
    assertTrue(viewModel.uiState.value.isFeedRefreshing)
  }

  @Test
  fun `anime topic changes only the anime feed key`() = runTest {
    val homeRepository = DeferredHomeRepository()
    val forYou = HomeFeedKey(HomeChannel.FOR_YOU)
    val animeAll = HomeFeedKey(HomeChannel.ANIME)
    val animeFantasy = HomeFeedKey(HomeChannel.ANIME, AnimeTopic.FANTASY)
    homeRepository.complete(forYou, DataResult.Success(content(forYou)))
    homeRepository.complete(animeAll, DataResult.Success(content(animeAll)))
    homeRepository.complete(animeFantasy, DataResult.Success(content(animeFantasy)))
    val viewModel = createViewModel(homeRepository)
    collect(viewModel)
    advanceUntilIdle()

    viewModel.onAction(HomeAction.ChannelSelected(HomeChannel.ANIME))
    advanceUntilIdle()
    viewModel.onAction(HomeAction.AnimeTopicSelected(AnimeTopic.FANTASY))
    advanceUntilIdle()

    assertEquals(animeFantasy, viewModel.uiState.value.feedKey)
    assertEquals(animeFantasy, homeRepository.requests.last())
  }

  @Test
  fun `load more appends unique items and keeps current content`() = runTest {
    val homeRepository = DeferredHomeRepository()
    val forYou = HomeFeedKey(HomeChannel.FOR_YOU)
    homeRepository.complete(
      forYou,
      DataResult.Success(content(forYou, totalPages = 2)),
    )
    homeRepository.pageResult = DataResult.Success(
      HomeSectionPage(
        type = HomeSectionType.TRENDING,
        items = listOf(sampleMedia(1), sampleMedia(2)),
        page = 2,
        totalPages = 2,
      ),
    )
    val viewModel = createViewModel(homeRepository)
    collect(viewModel)
    advanceUntilIdle()

    viewModel.onAction(HomeAction.LoadMore(HomeSectionType.TRENDING))
    advanceUntilIdle()

    val loaded = (viewModel.uiState.value.contentState as LoadState.Success).value
    val section = loaded.section(HomeSectionType.TRENDING)!!
    assertEquals(listOf(1, 2), section.items.map(MediaItem::id))
    assertEquals(2, section.page)
  }

  @Test
  fun `duplicate favorite tap is ignored while mutation is running`() = runTest {
    val homeRepository = DeferredHomeRepository()
    val forYou = HomeFeedKey(HomeChannel.FOR_YOU)
    homeRepository.complete(forYou, DataResult.Success(content(forYou)))
    val libraryRepository = FakeLibraryRepository(pauseFavoriteWrites = true)
    val viewModel = HomeViewModel(
      homeRepository = homeRepository,
      libraryRepository = libraryRepository,
      preferenceRepository = FakePreferenceRepository(),
      communityRepository = FakeCommunityRepository(),
      savedStateHandle = SavedStateHandle(),
    )
    collect(viewModel)
    advanceUntilIdle()
    val item = sampleMedia()

    viewModel.onAction(HomeAction.FavoriteToggled(item))
    runCurrent()
    viewModel.onAction(HomeAction.FavoriteToggled(item))
    runCurrent()

    assertEquals(1, libraryRepository.favoriteWriteCount)
    assertTrue(item.key in viewModel.uiState.value.favoriteMutations)

    libraryRepository.finishFavoriteWrite.complete(Unit)
    advanceUntilIdle()

    assertTrue(item.key in viewModel.uiState.value.favoriteKeys)
    assertTrue(viewModel.uiState.value.favoriteMutations.isEmpty())
  }

  @Test
  fun `helpful is optimistic and ignores a second tap while submitting`() = runTest {
    val homeRepository = DeferredHomeRepository()
    val forYou = HomeFeedKey(HomeChannel.FOR_YOU)
    homeRepository.complete(forYou, DataResult.Success(content(forYou)))
    val communityRepository = FakeCommunityRepository(listOf(sampleCommunityReview()))
    communityRepository.pauseHelpfulWrite()
    val viewModel = createViewModel(
      homeRepository = homeRepository,
      communityRepository = communityRepository,
    )
    collect(viewModel)
    advanceUntilIdle()

    viewModel.onAction(HomeAction.HelpfulToggled("review-1"))
    runCurrent()
    viewModel.onAction(HomeAction.HelpfulToggled("review-1"))
    runCurrent()

    val optimistic = (
      viewModel.uiState.value.communityPulseState as LoadState.Success
    ).value.single().helpful
    assertTrue(optimistic.isHelpful)
    assertTrue(optimistic.isSubmitting)
    assertEquals(1, optimistic.count)
    assertEquals(1, communityRepository.helpfulWrites)

    communityRepository.completeHelpful(DataResult.Success(Unit))
    advanceUntilIdle()

    val saved = (
      viewModel.uiState.value.communityPulseState as LoadState.Success
    ).value.single().helpful
    assertTrue(saved.isHelpful)
    assertTrue(!saved.isSubmitting)
  }

  @Test
  fun `failed helpful mutation rolls back count and selection`() = runTest {
    val homeRepository = DeferredHomeRepository()
    val forYou = HomeFeedKey(HomeChannel.FOR_YOU)
    homeRepository.complete(forYou, DataResult.Success(content(forYou)))
    val communityRepository = FakeCommunityRepository(listOf(sampleCommunityReview()))
    communityRepository.helpfulResult = DataResult.Error(AppError.NetworkUnavailable)
    val viewModel = createViewModel(homeRepository, communityRepository = communityRepository)
    collect(viewModel)
    advanceUntilIdle()

    viewModel.onAction(HomeAction.HelpfulToggled("review-1"))
    advanceUntilIdle()

    val restored = (
      viewModel.uiState.value.communityPulseState as LoadState.Success
    ).value.single().helpful
    assertTrue(!restored.isHelpful)
    assertEquals(0, restored.count)
    assertTrue(viewModel.uiState.value.communityWriteError != null)
  }

  @Test
  fun `valid Home review draft publishes canonical review and closes composer`() = runTest {
    val homeRepository = DeferredHomeRepository()
    val forYou = HomeFeedKey(HomeChannel.FOR_YOU)
    homeRepository.complete(forYou, DataResult.Success(content(forYou)))
    val publishedReview = sampleCommunityReview()
    val communityRepository = FakeCommunityRepository(
      publishResult = DataResult.Success(publishedReview),
    )
    val viewModel = createViewModel(homeRepository, communityRepository = communityRepository)
    collect(viewModel)
    advanceUntilIdle()

    viewModel.onAction(HomeAction.ReviewComposerRequested(sampleMedia()))
    viewModel.onAction(HomeAction.ReviewRatingSelected(9))
    viewModel.onAction(HomeAction.ReviewHeadlineChanged("A vivid first impression"))
    viewModel.onAction(
      HomeAction.ReviewBodyChanged(
        "A thoughtful review body that is long enough to be published safely.",
      ),
    )
    viewModel.onAction(HomeAction.ReviewSpoilerChanged(true))
    viewModel.onAction(HomeAction.ReviewSubmitted)
    advanceUntilIdle()

    val request = communityRepository.publishedRequest!!
    assertEquals(sampleMedia().key, request.mediaKey)
    assertEquals(9, request.rating)
    assertTrue(request.containsSpoilers)
    assertTrue(!viewModel.uiState.value.reviewComposer.isVisible)
    val reviews = (viewModel.uiState.value.communityPulseState as LoadState.Success).value
    assertEquals(listOf(publishedReview), reviews)
  }

  private fun createViewModel(
    homeRepository: HomeRepository,
    libraryRepository: LibraryRepository = FakeLibraryRepository(),
    communityRepository: CommunityRepository = FakeCommunityRepository(),
  ) = HomeViewModel(
    homeRepository = homeRepository,
    libraryRepository = libraryRepository,
    preferenceRepository = FakePreferenceRepository(),
    communityRepository = communityRepository,
    savedStateHandle = SavedStateHandle(),
  )

  private fun kotlinx.coroutines.test.TestScope.collect(viewModel: HomeViewModel) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect {}
    }
  }

  private class DeferredHomeRepository : HomeRepository {
    val requests = mutableListOf<HomeFeedKey>()
    private val results = mutableMapOf<HomeFeedKey, CompletableDeferred<DataResult<HomeContent>>>()
    var pageResult: DataResult<HomeSectionPage> = DataResult.Success(
      HomeSectionPage(HomeSectionType.TRENDING, emptyList(), 1, 1),
    )

    override suspend fun loadHomeContent(feedKey: HomeFeedKey): DataResult<HomeContent> {
      requests += feedKey
      return results.getOrPut(feedKey) { CompletableDeferred() }.await()
    }

    override suspend fun loadSectionPage(
      feedKey: HomeFeedKey,
      sectionType: HomeSectionType,
      page: Int,
    ): DataResult<HomeSectionPage> = pageResult

    fun complete(
      feedKey: HomeFeedKey,
      result: DataResult<HomeContent>,
    ) {
      results.getOrPut(feedKey) { CompletableDeferred() }.complete(result)
    }

    fun reset(feedKey: HomeFeedKey) {
      results[feedKey] = CompletableDeferred()
    }
  }

  private class FakeLibraryRepository(
    pauseFavoriteWrites: Boolean = false,
  ) : LibraryRepository {
    private val items = MutableStateFlow<List<LibraryItem>>(emptyList())
    val finishFavoriteWrite = CompletableDeferred<Unit>().apply {
      if (!pauseFavoriteWrites) complete(Unit)
    }
    var favoriteWriteCount = 0
      private set

    override fun observeLibrary(): Flow<List<LibraryItem>> = items

    override suspend fun setWatchStatus(item: MediaItem, status: WatchStatus) = Unit

    override suspend fun setFavorite(item: MediaItem, isFavorite: Boolean) {
      favoriteWriteCount += 1
      finishFavoriteWrite.await()
      val existing = items.value.firstOrNull { it.media.key == item.key }
      val updated = LibraryItem(
        media = item,
        watchStatus = existing?.watchStatus ?: WatchStatus.NONE,
        isFavorite = isFavorite,
        addedAt = existing?.addedAt ?: 1L,
        updatedAt = 1L,
      )
      items.value = items.value.filterNot { it.media.key == item.key } + updated
    }

    override suspend fun restore(item: LibraryItem) = Unit
  }

  private class FakePreferenceRepository : PreferenceRepository {
    private val profile = MutableStateFlow<UserPreferenceProfile?>(null)

    override fun observeProfile(): Flow<UserPreferenceProfile?> = profile

    override suspend fun getProfile(): UserPreferenceProfile? = profile.value

    override suspend fun saveProfile(profile: UserPreferenceProfile) {
      this.profile.value = profile
    }

    override suspend fun clearProfile() {
      profile.value = null
    }
  }

  private class FakeCommunityRepository(
    private val reviews: List<CommunityReview> = emptyList(),
    private val publishResult: DataResult<CommunityReview> = DataResult.Error(AppError.Unknown),
  ) : CommunityRepository {
    var helpfulResult: DataResult<Unit> = DataResult.Success(Unit)
    var helpfulWrites: Int = 0
      private set
    private var helpfulGate: CompletableDeferred<DataResult<Unit>>? = null
    var publishedRequest: PublishReviewRequest? = null
      private set

    fun pauseHelpfulWrite() {
      helpfulGate = CompletableDeferred()
    }

    fun completeHelpful(result: DataResult<Unit>) {
      helpfulGate?.complete(result)
    }

    override suspend fun getReviews(
      cursor: CommunityCursor?,
      limit: Int,
    ): DataResult<CommunityPage> = DataResult.Success(CommunityPage(reviews.take(limit), null))

    override suspend fun getReview(reviewId: String): DataResult<CommunityReview> =
      DataResult.Error(AppError.NotFound)

    override suspend fun publishReview(
      request: PublishReviewRequest,
    ): DataResult<CommunityReview> {
      publishedRequest = request
      return publishResult
    }

    override suspend fun setHelpful(reviewId: String, helpful: Boolean): DataResult<Unit> {
      helpfulWrites += 1
      return helpfulGate?.await() ?: helpfulResult
    }

    override suspend fun setReaction(
      reviewId: String,
      reaction: ReviewReaction?,
    ): DataResult<Unit> = DataResult.Success(Unit)

    override suspend fun setRating(mediaId: Long, rating: Int?): DataResult<Unit> =
      DataResult.Success(Unit)

    override suspend fun getRootComments(
      reviewId: String,
      cursor: CommunityCursor?,
      limit: Int,
    ): DataResult<List<CommunityComment>> = DataResult.Success(emptyList())

    override suspend fun getReplies(
      rootCommentId: String,
      cursor: CommunityCursor?,
      limit: Int,
    ): DataResult<List<CommunityComment>> = DataResult.Success(emptyList())

    override suspend fun getComment(commentId: String): DataResult<CommunityComment> =
      DataResult.Error(AppError.NotFound)

    override suspend fun createComment(
      reviewId: String,
      body: String,
      parentCommentId: String?,
    ): DataResult<CommunityComment> = DataResult.Error(AppError.Unknown)

    override suspend fun subscribeToComments(
      reviewId: String,
    ): DataResult<CommentRealtimeSubscription> = DataResult.Error(AppError.NetworkUnavailable)
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
}

private fun content(
  feedKey: HomeFeedKey,
  totalPages: Int = 1,
): HomeContent =
  HomeContent(
    feedKey = feedKey,
    sections = listOf(
      HomeSection(
        type = HomeSectionType.HERO,
        items = listOf(sampleMedia()),
      ),
      HomeSection(
        type = HomeSectionType.TRENDING,
        items = listOf(sampleMedia()),
        totalPages = totalPages,
      ),
    ),
    genres = emptyList(),
    people = emptyList(),
    hasPartialFailures = false,
  )

private fun sampleMedia(id: Int = 1): MediaItem =
  MediaItem(
    id = id,
    type = MediaType.MOVIE,
    title = "Movie $id",
    originalTitle = "Movie $id",
    overview = "Overview",
    posterPath = "/poster-$id.jpg",
    backdropPath = "/backdrop-$id.jpg",
    releaseDate = "2026-07-01",
    voteAverage = 8.0,
    voteCount = 500,
    popularity = 100.0,
    adult = false,
    originalLanguage = "en",
    genreIds = listOf(18),
  )

private fun sampleCommunityReview(): CommunityReview = CommunityReview(
  id = "review-1",
  mediaId = 42L,
  mediaKey = sampleMedia().key,
  mediaTitle = "Movie 1",
  posterPath = "/poster-1.jpg",
  releaseYear = "2026",
  author = CommunityAuthor(
    id = "author-1",
    username = "cinephile",
    displayName = "Cinephile",
    avatarUrl = null,
  ),
  headline = "A thoughtful review",
  body = "This review is long enough to represent a real community review in the test.",
  containsSpoilers = false,
  authorRating = 9,
  viewerRating = null,
  filmeraRating = 8.5,
  ratingCount = 12,
  helpful = HelpfulState(isHelpful = false, count = 0),
  commentCount = 3,
  reactions = ReactionSummary(),
  createdAt = "2026-08-04T10:00:00Z",
  updatedAt = "2026-08-04T10:00:00Z",
)
