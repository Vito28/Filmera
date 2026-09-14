package com.example.filmera.feature.home.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.ui.LoadState
import com.example.filmera.feature.home.domain.AnimeTopic
import com.example.filmera.feature.home.domain.HomeChannel
import com.example.filmera.feature.home.domain.HomeContent
import com.example.filmera.feature.home.domain.HomeFeedKey
import com.example.filmera.feature.home.domain.HomeRepository
import com.example.filmera.feature.home.domain.HomeSection
import com.example.filmera.feature.home.domain.HomeSectionType
import com.example.filmera.feature.community.domain.CommunityRepository
import com.example.filmera.feature.community.domain.CommunityReview
import com.example.filmera.feature.community.domain.ReviewReaction
import com.example.filmera.feature.community.domain.PublishReviewRequest
import com.example.filmera.feature.library.domain.LibraryRepository
import com.example.filmera.feature.library.domain.WatchStatus
import com.example.filmera.feature.preferences.domain.PreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
  private val homeRepository: HomeRepository,
  private val libraryRepository: LibraryRepository,
  private val preferenceRepository: PreferenceRepository,
  private val communityRepository: CommunityRepository,
  private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
  private val contentState = MutableStateFlow<LoadState<HomeContent>>(LoadState.Loading)
  private val selectedChannel = MutableStateFlow(
    savedStateHandle.get<String>(SELECTED_CHANNEL_KEY).toHomeChannel(),
  )
  private val selectedAnimeTopic = MutableStateFlow(
    savedStateHandle.get<String>(SELECTED_ANIME_TOPIC_KEY).toAnimeTopic(),
  )
  private val isFeedRefreshing = MutableStateFlow(false)
  private val feedRefreshError = MutableStateFlow<AppError?>(null)
  private val favoriteMutations = MutableStateFlow<Set<MediaKey>>(emptySet())
  private val favoriteWriteError = MutableStateFlow<AppError?>(null)
  private val watchlistMutations = MutableStateFlow<Set<MediaKey>>(emptySet())
  private val libraryWriteError = MutableStateFlow<AppError?>(null)
  private val communityPulseState = MutableStateFlow<LoadState<List<CommunityReview>>>(
    LoadState.Loading,
  )
  private val ratingSheet = MutableStateFlow(HomeRatingSheetState())
  private val communityWriteError = MutableStateFlow<AppError?>(null)
  private val reviewComposer = MutableStateFlow(HomeReviewComposerState())
  private val contentCache = mutableMapOf<HomeFeedKey, HomeContent>()
  private val paginationJobs = mutableMapOf<HomeSectionType, Job>()
  private val libraryItems = libraryRepository.observeLibrary()
    .catch { emit(emptyList()) }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = emptyList(),
    )
  private val preferenceProfile = preferenceRepository.observeProfile()
    .catch { emit(null) }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = null,
    )
  private val libraryState = combine(
    libraryItems,
    favoriteMutations,
    favoriteWriteError,
    watchlistMutations,
    libraryWriteError,
  ) { items, favoriteChanges, favoriteError, watchlistChanges, libraryError ->
    val favorites = items.filter { it.isFavorite }.map { it.media }
    val watchlist = items
      .filter { it.watchStatus == WatchStatus.WATCHLIST }
      .map { it.media }
    LibraryState(
      favoriteItems = favorites,
      watchlistItems = watchlist,
      favoriteKeys = favorites.map(MediaItem::key).toSet(),
      watchlistKeys = items
        .filter { it.watchStatus != WatchStatus.NONE }
        .map { it.media.key }
        .toSet(),
      favoriteMutations = favoriteChanges,
      favoriteError = favoriteError,
      watchlistMutations = watchlistChanges,
      libraryError = libraryError,
    )
  }

  private val communityPresentationState = combine(
    communityPulseState,
    ratingSheet,
    communityWriteError,
    reviewComposer,
  ) { community, rating, communityError, composer ->
    CommunityPresentationState(
      community = community,
      ratingSheet = rating,
      writeError = communityError,
      reviewComposer = composer,
    )
  }

  private val loadPresentationState = combine(
    contentState,
    isFeedRefreshing,
    feedRefreshError,
    communityPresentationState,
  ) { content, refreshing, refreshError, communityState ->
    LoadPresentationState(
      content = content,
      isRefreshing = refreshing,
      refreshError = refreshError,
      community = communityState.community,
      ratingSheet = communityState.ratingSheet,
      communityWriteError = communityState.writeError,
      reviewComposer = communityState.reviewComposer,
    )
  }

  val uiState: StateFlow<HomeUiState> = combine(
    loadPresentationState,
    selectedChannel,
    selectedAnimeTopic,
    libraryState,
    preferenceProfile,
  ) { loadState, channel, animeTopic, library, profile ->
    HomeUiState(
      contentState = loadState.content,
      selectedChannel = channel,
      selectedAnimeTopic = animeTopic,
      isFeedRefreshing = loadState.isRefreshing,
      feedRefreshError = loadState.refreshError,
      preferredGenreIds = profile?.preferredGenreIds.orEmpty(),
      favoriteItems = library.favoriteItems,
      watchlistItems = library.watchlistItems,
      favoriteKeys = library.favoriteKeys,
      favoriteMutations = library.favoriteMutations,
      favoriteWriteError = library.favoriteError,
      watchlistKeys = library.watchlistKeys,
      watchlistMutations = library.watchlistMutations,
      libraryWriteError = library.libraryError,
      communityPulseState = loadState.community,
      ratingSheet = loadState.ratingSheet,
      communityWriteError = loadState.communityWriteError,
      reviewComposer = loadState.reviewComposer,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
    initialValue = HomeUiState(
      selectedChannel = selectedChannel.value,
      selectedAnimeTopic = selectedAnimeTopic.value,
    ),
  )

  private var loadJob: Job? = null

  init {
    loadContent(preserveCurrentContent = false)
    loadCommunityPulse()
  }

  fun onAction(action: HomeAction) {
    when (action) {
      is HomeAction.ChannelSelected -> selectChannel(action.channel)
      is HomeAction.AnimeTopicSelected -> selectAnimeTopic(action.topic)
      is HomeAction.LoadMore -> loadMore(action.sectionType)
      is HomeAction.RetrySection -> retrySection(action.sectionType)
      is HomeAction.FavoriteToggled -> toggleFavorite(action.item)
      is HomeAction.WatchlistToggled -> toggleWatchlist(action.item)
      is HomeAction.HelpfulToggled -> toggleHelpful(action.reviewId)
      is HomeAction.ReactionSelected -> selectReaction(action.reviewId, action.reaction)
      is HomeAction.RatingRequested -> showRatingSheet(action.reviewId)
      is HomeAction.RatingSelected -> ratingSheet.update { sheet ->
        if (sheet.isSubmitting) sheet else sheet.copy(selectedRating = action.rating.coerceIn(1, 10))
      }
      is HomeAction.ReviewComposerRequested -> reviewComposer.value = HomeReviewComposerState(
        media = action.media,
      )
      is HomeAction.ReviewRatingSelected -> reviewComposer.update {
        if (it.isSubmitting) it else it.copy(rating = action.rating.coerceIn(1, 10))
      }
      is HomeAction.ReviewHeadlineChanged -> reviewComposer.update {
        if (it.isSubmitting) it else it.copy(
          headline = action.value.take(REVIEW_HEADLINE_MAX),
          headlineInvalid = false,
        )
      }
      is HomeAction.ReviewBodyChanged -> reviewComposer.update {
        if (it.isSubmitting) it else it.copy(
          body = action.value.take(REVIEW_BODY_MAX),
          bodyInvalid = false,
        )
      }
      is HomeAction.ReviewSpoilerChanged -> reviewComposer.update {
        if (it.isSubmitting) it else it.copy(containsSpoilers = action.value)
      }
      is HomeAction.ScrollPositionChanged -> updateScrollPosition(
        feedKey = action.feedKey,
        position = action.position,
      )
      HomeAction.Retried -> loadContent(
        preserveCurrentContent = contentState.value is LoadState.Success,
      )
      HomeAction.RefreshErrorDismissed -> feedRefreshError.value = null
      HomeAction.FavoriteErrorDismissed -> favoriteWriteError.value = null
      HomeAction.LibraryErrorDismissed -> libraryWriteError.value = null
      HomeAction.CommunityRetried -> loadCommunityPulse()
      HomeAction.RatingSubmitted -> submitRating()
      HomeAction.RatingRemoved -> removeRating()
      HomeAction.RatingDismissed -> {
        if (!ratingSheet.value.isSubmitting) ratingSheet.value = HomeRatingSheetState()
      }
      HomeAction.CommunityErrorDismissed -> communityWriteError.value = null
      HomeAction.ReviewSubmitted -> publishReview()
      HomeAction.ReviewDismissed -> {
        if (!reviewComposer.value.isSubmitting) reviewComposer.value = HomeReviewComposerState()
      }
    }
  }

  private fun loadCommunityPulse() {
    viewModelScope.launch {
      communityPulseState.value = LoadState.Loading
      communityPulseState.value = when (val result = communityRepository.getReviews(limit = 3)) {
        is DataResult.Success -> if (result.value.reviews.isEmpty()) {
          LoadState.Empty
        } else {
          LoadState.Success(result.value.reviews)
        }
        is DataResult.Error -> LoadState.Error(result.error)
      }
    }
  }

  private fun toggleHelpful(reviewId: String) {
    val review = currentCommunityReviews().firstOrNull { it.id == reviewId } ?: return
    if (review.helpful.isSubmitting) return
    val previous = review.helpful
    val optimistic = previous.copy(
      isHelpful = !previous.isHelpful,
      count = (previous.count + if (previous.isHelpful) -1 else 1).coerceAtLeast(0),
      isSubmitting = true,
    )
    updateCommunityReview(reviewId) { it.copy(helpful = optimistic) }

    viewModelScope.launch {
      when (val result = communityRepository.setHelpful(reviewId, optimistic.isHelpful)) {
        is DataResult.Success -> updateCommunityReview(reviewId) {
          it.copy(helpful = optimistic.copy(isSubmitting = false))
        }
        is DataResult.Error -> {
          updateCommunityReview(reviewId) { it.copy(helpful = previous) }
          communityWriteError.value = result.error
        }
      }
    }
  }

  private fun selectReaction(reviewId: String, selected: ReviewReaction) {
    val review = currentCommunityReviews().firstOrNull { it.id == reviewId } ?: return
    if (review.reactions.isSubmitting) return
    val previous = review.reactions
    val nextReaction = selected.takeUnless { it == previous.selectedReaction }
    val counts = previous.counts.toMutableMap().apply {
      previous.selectedReaction?.let { reaction ->
        this[reaction] = (getOrDefault(reaction, 0) - 1).coerceAtLeast(0)
      }
      nextReaction?.let { reaction ->
        this[reaction] = getOrDefault(reaction, 0) + 1
      }
    }
    val optimistic = previous.copy(
      selectedReaction = nextReaction,
      counts = counts,
      isSubmitting = true,
    )
    updateCommunityReview(reviewId) { it.copy(reactions = optimistic) }

    viewModelScope.launch {
      when (val result = communityRepository.setReaction(reviewId, nextReaction)) {
        is DataResult.Success -> updateCommunityReview(reviewId) {
          it.copy(reactions = optimistic.copy(isSubmitting = false))
        }
        is DataResult.Error -> {
          updateCommunityReview(reviewId) { it.copy(reactions = previous) }
          communityWriteError.value = result.error
        }
      }
    }
  }

  private fun showRatingSheet(reviewId: String) {
    val review = currentCommunityReviews().firstOrNull { it.id == reviewId } ?: return
    ratingSheet.value = HomeRatingSheetState(
      reviewId = review.id,
      mediaId = review.mediaId,
      mediaTitle = review.mediaTitle,
      selectedRating = review.viewerRating,
      existingRating = review.viewerRating,
    )
  }

  private fun submitRating() {
    val sheet = ratingSheet.value
    val mediaId = sheet.mediaId ?: return
    val reviewId = sheet.reviewId ?: return
    val selectedRating = sheet.selectedRating ?: return
    if (sheet.isSubmitting) return
    ratingSheet.value = sheet.copy(isSubmitting = true)

    viewModelScope.launch {
      when (val result = communityRepository.setRating(mediaId, selectedRating)) {
        is DataResult.Success -> {
          updateCommunityReview(reviewId) { it.copy(viewerRating = selectedRating) }
          ratingSheet.value = HomeRatingSheetState()
        }
        is DataResult.Error -> {
          ratingSheet.value = sheet.copy(isSubmitting = false)
          communityWriteError.value = result.error
        }
      }
    }
  }

  private fun removeRating() {
    val sheet = ratingSheet.value
    val mediaId = sheet.mediaId ?: return
    val reviewId = sheet.reviewId ?: return
    if (sheet.isSubmitting) return
    ratingSheet.value = sheet.copy(isSubmitting = true)

    viewModelScope.launch {
      when (val result = communityRepository.setRating(mediaId, null)) {
        is DataResult.Success -> {
          updateCommunityReview(reviewId) { it.copy(viewerRating = null) }
          ratingSheet.value = HomeRatingSheetState()
        }
        is DataResult.Error -> {
          ratingSheet.value = sheet.copy(isSubmitting = false)
          communityWriteError.value = result.error
        }
      }
    }
  }

  private fun publishReview() {
    val composer = reviewComposer.value
    val media = composer.media ?: return
    val rating = composer.rating
    val normalizedBody = composer.body.trim()
    val headlineInvalid = composer.headline.length > REVIEW_HEADLINE_MAX
    val bodyInvalid = normalizedBody.length !in REVIEW_BODY_MIN..REVIEW_BODY_MAX
    if (rating == null || headlineInvalid || bodyInvalid || composer.isSubmitting) {
      reviewComposer.value = composer.copy(
        headlineInvalid = headlineInvalid,
        bodyInvalid = bodyInvalid,
      )
      return
    }
    reviewComposer.value = composer.copy(isSubmitting = true)
    viewModelScope.launch {
      when (
        val result = communityRepository.publishReview(
          PublishReviewRequest(
            mediaKey = media.key,
            rating = rating,
            headline = composer.headline,
            body = normalizedBody,
            containsSpoilers = composer.containsSpoilers,
          ),
        )
      ) {
        is DataResult.Success -> {
          val current = currentCommunityReviews()
          communityPulseState.value = LoadState.Success(
            (listOf(result.value) + current).distinctBy(CommunityReview::id).take(3),
          )
          reviewComposer.value = HomeReviewComposerState()
        }
        is DataResult.Error -> {
          reviewComposer.value = composer.copy(isSubmitting = false)
          communityWriteError.value = result.error
        }
      }
    }
  }

  private fun currentCommunityReviews(): List<CommunityReview> =
    (communityPulseState.value as? LoadState.Success)?.value.orEmpty()

  private fun updateCommunityReview(
    reviewId: String,
    transform: (CommunityReview) -> CommunityReview,
  ) {
    val state = communityPulseState.value as? LoadState.Success ?: return
    communityPulseState.value = LoadState.Success(
      state.value.map { review -> if (review.id == reviewId) transform(review) else review },
    )
  }

  private fun selectChannel(channel: HomeChannel) {
    if (channel == selectedChannel.value) return

    savedStateHandle[SELECTED_CHANNEL_KEY] = channel.name
    selectedChannel.value = channel
    if (!channel.isAnime) {
      selectedAnimeTopic.value = AnimeTopic.ALL
    }
    showCachedContentOrLoad()
  }

  private fun selectAnimeTopic(topic: AnimeTopic) {
    if (!selectedChannel.value.isAnime || topic == selectedAnimeTopic.value) return

    savedStateHandle[SELECTED_ANIME_TOPIC_KEY] = topic.name
    selectedAnimeTopic.value = topic
    showCachedContentOrLoad()
  }

  private fun showCachedContentOrLoad() {
    val cached = contentCache[currentFeedKey()]
    if (cached != null) {
      contentState.value = LoadState.Success(cached)
    }
    loadContent(preserveCurrentContent = cached != null)
  }

  private fun toggleFavorite(item: MediaItem) {
    if (item.key in favoriteMutations.value) return

    viewModelScope.launch {
      favoriteMutations.update { it + item.key }
      favoriteWriteError.value = null
      try {
        val shouldBeFavorite = libraryItems.value
          .firstOrNull { it.media.key == item.key }
          ?.isFavorite != true
        libraryRepository.setFavorite(item, shouldBeFavorite)
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
        favoriteWriteError.value = AppError.Unknown
      } finally {
        favoriteMutations.update { it - item.key }
      }
    }
  }

  private fun toggleWatchlist(item: MediaItem) {
    if (item.key in watchlistMutations.value) return

    viewModelScope.launch {
      watchlistMutations.update { it + item.key }
      libraryWriteError.value = null
      try {
        val current = libraryItems.value.firstOrNull { it.media.key == item.key }?.watchStatus
          ?: WatchStatus.NONE
        libraryRepository.setWatchStatus(
          item,
          if (current == WatchStatus.NONE) WatchStatus.WATCHLIST else WatchStatus.NONE,
        )
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
        libraryWriteError.value = AppError.Unknown
      } finally {
        watchlistMutations.update { it - item.key }
      }
    }
  }

  private fun loadContent(
    preserveCurrentContent: Boolean,
  ) {
    val requestedFeed = currentFeedKey()
    loadJob?.cancel()
    paginationJobs.values.forEach(Job::cancel)
    paginationJobs.clear()
    loadJob = viewModelScope.launch {
      feedRefreshError.value = null
      if (preserveCurrentContent && contentState.value is LoadState.Success) {
        isFeedRefreshing.value = true
      } else {
        contentState.value = LoadState.Loading
      }

      try {
        var receivedUsableSnapshot = false
        homeRepository.observeHomeContent(requestedFeed).collect { result ->
          if (currentFeedKey() != requestedFeed) return@collect
          when (result) {
            is DataResult.Success -> {
              val content = result.value
              val nextState = when {
                content.hasVisibleContent || content.hasPendingContent -> {
                  if (content.hasVisibleContent) contentCache[requestedFeed] = content
                  receivedUsableSnapshot = true
                  LoadState.Success(content)
                }
                else -> LoadState.Empty
              }
              contentState.value = nextState
              isFeedRefreshing.value = false
            }
            is DataResult.Error -> {
              val cached = contentCache[requestedFeed]
              if (preserveCurrentContent && cached != null) {
                contentState.value = LoadState.Success(cached)
                feedRefreshError.value = result.error
              } else if (!receivedUsableSnapshot) {
                contentState.value = LoadState.Error(result.error)
              } else {
                feedRefreshError.value = result.error
              }
              isFeedRefreshing.value = false
            }
          }
        }
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
        if (currentFeedKey() == requestedFeed) {
          val cached = contentCache[requestedFeed]
          if (preserveCurrentContent && cached != null) {
            contentState.value = LoadState.Success(cached)
            feedRefreshError.value = AppError.Unknown
          } else {
            contentState.value = LoadState.Error(AppError.Unknown)
          }
        }
      } finally {
        if (currentFeedKey() == requestedFeed) {
          isFeedRefreshing.value = false
        }
      }
    }
  }

  private fun loadMore(sectionType: HomeSectionType) {
    val feedKey = currentFeedKey()
    val content = (contentState.value as? LoadState.Success)?.value ?: return
    if (content.feedKey != feedKey) return
    val section = content.section(sectionType) ?: return
    if (!section.canLoadMore || paginationJobs[sectionType]?.isActive == true) return

    updateSection(
      content = content,
      section = section.copy(
        isLoadingMore = true,
        paginationError = null,
      ),
    )
    paginationJobs[sectionType] = viewModelScope.launch {
      try {
        when (
          val result = homeRepository.loadSectionPage(
            feedKey = feedKey,
            sectionType = sectionType,
            page = section.page + 1,
          )
        ) {
          is DataResult.Success -> {
            val latest = currentContentFor(feedKey) ?: return@launch
            val currentSection = latest.section(sectionType) ?: return@launch
            updateSection(
              content = latest,
              section = currentSection.copy(
                items = (currentSection.items + result.value.items)
                  .distinctBy(MediaItem::key),
                page = result.value.page,
                totalPages = result.value.totalPages.coerceAtLeast(result.value.page),
                isLoadingMore = false,
                paginationError = null,
              ),
            )
          }
          is DataResult.Error -> {
            val latest = currentContentFor(feedKey) ?: return@launch
            val currentSection = latest.section(sectionType) ?: return@launch
            updateSection(
              content = latest,
              section = currentSection.copy(
                isLoadingMore = false,
                paginationError = result.error,
              ),
            )
          }
        }
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
        val latest = currentContentFor(feedKey) ?: return@launch
        val currentSection = latest.section(sectionType) ?: return@launch
        updateSection(
          content = latest,
          section = currentSection.copy(
            isLoadingMore = false,
            paginationError = AppError.Unknown,
          ),
        )
      } finally {
        paginationJobs.remove(sectionType)
      }
    }
  }

  private fun retrySection(sectionType: HomeSectionType) {
    val feedKey = currentFeedKey()
    val content = currentContentFor(feedKey) ?: return
    val section = content.section(sectionType) ?: return
    if (paginationJobs[sectionType]?.isActive == true) return

    updateSection(
      content = content,
      section = section.copy(
        isInitialLoading = true,
        error = null,
        paginationError = null,
      ),
    )
    paginationJobs[sectionType] = viewModelScope.launch {
      try {
        when (
          val result = homeRepository.loadSectionPage(
            feedKey = feedKey,
            sectionType = sectionType,
            page = 1,
          )
        ) {
          is DataResult.Success -> {
            val latest = currentContentFor(feedKey) ?: return@launch
            val currentSection = latest.section(sectionType) ?: return@launch
            updateSection(
              content = latest,
              section = currentSection.copy(
                items = result.value.items.distinctBy(MediaItem::key),
                isInitialLoading = false,
                page = result.value.page,
                totalPages = result.value.totalPages.coerceAtLeast(result.value.page),
                error = null,
              ),
            )
          }
          is DataResult.Error -> {
            val latest = currentContentFor(feedKey) ?: return@launch
            val currentSection = latest.section(sectionType) ?: return@launch
            updateSection(
              content = latest,
              section = currentSection.copy(
                isInitialLoading = false,
                error = result.error,
              ),
            )
          }
        }
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
        val latest = currentContentFor(feedKey) ?: return@launch
        val currentSection = latest.section(sectionType) ?: return@launch
        updateSection(
          content = latest,
          section = currentSection.copy(
            isInitialLoading = false,
            error = AppError.Unknown,
          ),
        )
      } finally {
        paginationJobs.remove(sectionType)
      }
    }
  }

  private fun updateSection(
    content: HomeContent,
    section: HomeSection,
  ) {
    val updated = content.withSection(section)
    contentCache[updated.feedKey] = updated
    if (currentFeedKey() == updated.feedKey) {
      contentState.value = LoadState.Success(updated)
    }
  }

  private fun currentContentFor(feedKey: HomeFeedKey): HomeContent? =
    (contentState.value as? LoadState.Success)
      ?.value
      ?.takeIf { it.feedKey == feedKey }
      ?: contentCache[feedKey]

  private fun currentFeedKey(): HomeFeedKey =
    HomeFeedKey(
      channel = selectedChannel.value,
      animeTopic = if (selectedChannel.value.isAnime) {
        selectedAnimeTopic.value
      } else {
        AnimeTopic.ALL
      },
    )

  fun scrollPosition(feedKey: HomeFeedKey): HomeScrollPosition = HomeScrollPosition(
    index = savedStateHandle[scrollIndexKey(feedKey)] ?: 0,
    offset = savedStateHandle[scrollOffsetKey(feedKey)] ?: 0,
  )

  private fun updateScrollPosition(
    feedKey: HomeFeedKey,
    position: HomeScrollPosition,
  ) {
    if (position.index < 0 || position.offset < 0) return
    savedStateHandle[scrollIndexKey(feedKey)] = position.index
    savedStateHandle[scrollOffsetKey(feedKey)] = position.offset
  }

  private data class LibraryState(
    val favoriteItems: List<MediaItem>,
    val watchlistItems: List<MediaItem>,
    val favoriteKeys: Set<MediaKey>,
    val watchlistKeys: Set<MediaKey>,
    val favoriteMutations: Set<MediaKey>,
    val favoriteError: AppError?,
    val watchlistMutations: Set<MediaKey>,
    val libraryError: AppError?,
  )

  private data class LoadPresentationState(
    val content: LoadState<HomeContent>,
    val isRefreshing: Boolean,
    val refreshError: AppError?,
    val community: LoadState<List<CommunityReview>>,
    val ratingSheet: HomeRatingSheetState,
    val communityWriteError: AppError?,
    val reviewComposer: HomeReviewComposerState,
  )

  private data class CommunityPresentationState(
    val community: LoadState<List<CommunityReview>>,
    val ratingSheet: HomeRatingSheetState,
    val writeError: AppError?,
    val reviewComposer: HomeReviewComposerState,
  )

  private fun String?.toHomeChannel(): HomeChannel =
    HomeChannel.entries.firstOrNull { it.name == this } ?: HomeChannel.FOR_YOU

  private fun String?.toAnimeTopic(): AnimeTopic =
    AnimeTopic.entries.firstOrNull { it.name == this } ?: AnimeTopic.ALL

  private companion object {
    const val SELECTED_CHANNEL_KEY = "home_selected_channel"
    const val SELECTED_ANIME_TOPIC_KEY = "home_selected_anime_topic"
    const val REVIEW_HEADLINE_MAX = 120
    const val REVIEW_BODY_MIN = 20
    const val REVIEW_BODY_MAX = 5_000

    fun scrollIndexKey(feedKey: HomeFeedKey): String =
      "home_scroll_index_${feedKey.channel.name}_${feedKey.normalizedAnimeTopic.name}"

    fun scrollOffsetKey(feedKey: HomeFeedKey): String =
      "home_scroll_offset_${feedKey.channel.name}_${feedKey.normalizedAnimeTopic.name}"
  }
}
