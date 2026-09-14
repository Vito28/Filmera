package com.example.filmera.feature.home.presentation.component

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.filmera.R
import com.example.filmera.core.designsystem.theme.filmeraColors
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.ui.LazyLayoutKey
import com.example.filmera.core.ui.component.TmdbImage
import com.example.filmera.feature.home.domain.HomeSection
import com.example.filmera.feature.home.domain.HomeTrailer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive

@Composable
fun ImmersiveHeroSection(
  section: HomeSection?,
  trailers: List<HomeTrailer>,
  favoriteKeys: Set<MediaKey>,
  favoriteMutations: Set<MediaKey>,
  watchlistKeys: Set<MediaKey>,
  watchlistMutations: Set<MediaKey>,
  isExpanded: Boolean,
  isVisible: Boolean,
  onMediaClick: (MediaItem) -> Unit,
  onPlayTrailer: (String) -> Unit,
  onToggleFavorite: (MediaItem) -> Unit,
  onToggleWatchlist: (MediaItem) -> Unit,
  onActiveItemChanged: (MediaItem?) -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val heroHeight = if (isExpanded) 620.dp else 560.dp

  Box(
    modifier = modifier.fillMaxWidth(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Box(
      modifier = Modifier
        .widthIn(max = 1120.dp)
        .fillMaxWidth()
        .height(heroHeight)
        .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
      val items = section?.items.orEmpty()
      when {
        items.isNotEmpty() -> FeaturedPager(
          section = requireNotNull(section),
          trailers = trailers,
          favoriteKeys = favoriteKeys,
          favoriteMutations = favoriteMutations,
          watchlistKeys = watchlistKeys,
          watchlistMutations = watchlistMutations,
          isVisible = isVisible,
          onMediaClick = onMediaClick,
          onPlayTrailer = onPlayTrailer,
          onToggleFavorite = onToggleFavorite,
          onToggleWatchlist = onToggleWatchlist,
          onActiveItemChanged = onActiveItemChanged,
        )

        section?.isInitialLoading == true -> {
          LaunchedEffect(Unit) { onActiveItemChanged(null) }
          HeroLoadingPlaceholder()
        }

        section?.error != null -> {
          LaunchedEffect(Unit) { onActiveItemChanged(null) }
          SectionStatusMessage(
            error = section.error,
            emptyMessage = stringResource(R.string.home_featured_empty),
            onRetry = onRetry,
            modifier = Modifier
              .align(Alignment.Center)
              .padding(horizontal = 24.dp),
          )
        }

        else -> {
          LaunchedEffect(Unit) { onActiveItemChanged(null) }
          SectionStatusMessage(
            error = null,
            emptyMessage = stringResource(R.string.home_featured_empty),
            onRetry = onRetry,
            modifier = Modifier
              .align(Alignment.Center)
              .padding(horizontal = 24.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun FeaturedPager(
  section: HomeSection,
  trailers: List<HomeTrailer>,
  favoriteKeys: Set<MediaKey>,
  favoriteMutations: Set<MediaKey>,
  watchlistKeys: Set<MediaKey>,
  watchlistMutations: Set<MediaKey>,
  isVisible: Boolean,
  onMediaClick: (MediaItem) -> Unit,
  onPlayTrailer: (String) -> Unit,
  onToggleFavorite: (MediaItem) -> Unit,
  onToggleWatchlist: (MediaItem) -> Unit,
  onActiveItemChanged: (MediaItem?) -> Unit,
) {
  val items = section.items.take(HERO_ITEM_LIMIT)
  val pagerState = rememberPagerState(pageCount = { items.size })
  HeroAutoSlideEffect(pagerState, isVisible)

  LaunchedEffect(pagerState, items) {
    snapshotFlow { pagerState.currentPage }
      .distinctUntilChanged()
      .collect { page ->
        onActiveItemChanged(items.getOrNull(page))
      }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    HorizontalPager(
      state = pagerState,
      contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 18.dp),
      pageSpacing = 10.dp,
      key = { page ->
        val item = items[page]
        LazyLayoutKey.media("home-hero", item)
      },
      modifier = Modifier.fillMaxSize(),
    ) { page ->
      items.getOrNull(page)?.let { item ->
        HeroPage(
          item = item,
          trailer = trailers.firstOrNull { it.media.key == item.key },
          isFavorite = item.key in favoriteKeys,
          isFavoriteMutating = item.key in favoriteMutations,
          isWatchlisted = item.key in watchlistKeys,
          isWatchlistMutating = item.key in watchlistMutations,
          onMediaClick = { onMediaClick(item) },
          onPlayTrailer = onPlayTrailer,
          onToggleFavorite = { onToggleFavorite(item) },
          onToggleWatchlist = { onToggleWatchlist(item) },
        )
      }
    }

    if (items.size > 1) {
      Row(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        val indicatorPages = items.indices
          .filter { page ->
            kotlin.math.abs(page - pagerState.currentPage) <= 2 ||
              page == items.lastIndex
          }
        indicatorPages.forEach { page ->
          val isSelected = page == pagerState.currentPage
          Box(
            modifier = Modifier
              .size(
                width = if (isSelected) 20.dp else 6.dp,
                height = 6.dp,
              )
              .background(
                color = if (isSelected) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.filmeraColors.onImage.copy(alpha = 0.56f)
                },
                shape = MaterialTheme.shapes.extraSmall,
              ),
          )
        }
      }
    }

    if (section.isLoadingMore) {
      CircularProgressIndicator(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(20.dp)
          .size(22.dp),
        color = MaterialTheme.filmeraColors.onImage,
        strokeWidth = 2.dp,
      )
    }
  }
}

@Composable
private fun HeroPage(
  item: MediaItem,
  trailer: HomeTrailer?,
  isFavorite: Boolean,
  isFavoriteMutating: Boolean,
  isWatchlisted: Boolean,
  isWatchlistMutating: Boolean,
  onMediaClick: () -> Unit,
  onPlayTrailer: (String) -> Unit,
  onToggleFavorite: () -> Unit,
  onToggleWatchlist: () -> Unit,
) {
  val background = MaterialTheme.colorScheme.background
  val hapticFeedback = LocalHapticFeedback.current

  Box(modifier = Modifier.fillMaxSize()) {
    TmdbImage(
      path = item.backdropPath ?: item.posterPath,
      contentDescription = stringResource(
        R.string.backdrop_content_description,
        item.title,
      ),
      size = "w1280",
      modifier = Modifier.fillMaxSize(),
    )
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.7f),
              Color.Transparent,
              MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.32f),
              background,
            ),
          ),
        ),
    )
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth(0.9f)
        .align(Alignment.CenterStart)
        .background(
          Brush.horizontalGradient(
            colors = listOf(
              MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.72f),
              Color.Transparent,
            ),
          ),
        ),
    )

    Column(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .widthIn(max = 660.dp)
        .padding(
          start = 16.dp,
          end = 16.dp,
          bottom = 44.dp,
        ),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = stringResource(R.string.home_featured_label),
        color = MaterialTheme.filmeraColors.onImage.copy(alpha = 0.84f),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = item.title,
        color = MaterialTheme.filmeraColors.onImage,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      HeroMetadata(item)
      Text(
        text = item.overview,
        color = MaterialTheme.filmeraColors.onImage.copy(alpha = 0.86f),
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
      )
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (trailer != null) {
          Button(onClick = { onPlayTrailer(trailer.videoKey) }) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = null,
            )
            Text(
              text = stringResource(R.string.play_trailer),
              modifier = Modifier.padding(start = 6.dp),
            )
          }
        }
        FilledTonalButton(onClick = onMediaClick) {
          Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
          )
          Text(
            text = stringResource(R.string.view_details),
            modifier = Modifier.padding(start = 6.dp),
          )
        }
        IconButton(
          onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggleWatchlist()
          },
          enabled = !isWatchlistMutating,
        ) {
          if (isWatchlistMutating) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = MaterialTheme.filmeraColors.onImage,
              strokeWidth = 2.dp,
            )
          } else {
            Icon(
              imageVector = if (isWatchlisted) {
                Icons.Default.Bookmark
              } else {
                Icons.Outlined.BookmarkBorder
              },
              contentDescription = stringResource(
                if (isWatchlisted) R.string.remove_from_watchlist else R.string.add_to_watchlist,
              ),
              tint = MaterialTheme.filmeraColors.onImage,
            )
          }
        }
        IconButton(
          onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggleFavorite()
          },
          enabled = !isFavoriteMutating,
        ) {
          if (isFavoriteMutating) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = MaterialTheme.filmeraColors.onImage,
              strokeWidth = 2.dp,
            )
          } else {
            Icon(
              imageVector = if (isFavorite) {
                Icons.Default.Favorite
              } else {
                Icons.Outlined.FavoriteBorder
              },
              contentDescription = stringResource(
                if (isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites,
              ),
              tint = MaterialTheme.filmeraColors.onImage,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HeroMetadata(
  item: MediaItem,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = Icons.Default.Star,
      contentDescription = stringResource(R.string.rating),
      modifier = Modifier.size(16.dp),
      tint = MaterialTheme.colorScheme.tertiary,
    )
    Text(
      text = stringResource(R.string.rating_value, item.voteAverage),
      color = MaterialTheme.filmeraColors.onImage,
      style = MaterialTheme.typography.labelLarge,
    )
    item.releaseDate
      ?.take(4)
      ?.takeIf { it.all(Char::isDigit) }
      ?.let { year ->
        MetadataDot()
        Text(
          text = year,
          color = MaterialTheme.filmeraColors.onImage.copy(alpha = 0.8f),
          style = MaterialTheme.typography.labelLarge,
        )
      }
    MetadataDot()
    Text(
      text = stringResource(
        if (item.type == MediaType.MOVIE) {
          R.string.media_type_movie
        } else {
          R.string.media_type_tv_show
        },
      ),
      color = MaterialTheme.filmeraColors.onImage.copy(alpha = 0.8f),
      style = MaterialTheme.typography.labelLarge,
    )
  }
}

@Composable
private fun MetadataDot() {
  Box(
    modifier = Modifier
      .size(4.dp)
      .background(
        color = MaterialTheme.filmeraColors.onImage.copy(alpha = 0.64f),
        shape = MaterialTheme.shapes.extraSmall,
      ),
  )
}

@Composable
private fun HeroAutoSlideEffect(
  pagerState: PagerState,
  isVisible: Boolean,
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  var isResumed by remember(lifecycleOwner) {
    mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
  }
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, _ ->
      isResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  LaunchedEffect(pagerState, pagerState.pageCount, isResumed, isVisible) {
    if (
      !isResumed ||
      !isVisible ||
      pagerState.pageCount <= 1 ||
      !areSystemAnimationsEnabled()
    ) {
      return@LaunchedEffect
    }
    while (currentCoroutineContext().isActive) {
      delay(HERO_AUTO_SLIDE_MILLIS)
      if (!pagerState.isScrollInProgress && pagerState.pageCount > 1) {
        pagerState.animateScrollToPage(
          page = (pagerState.currentPage + 1) % pagerState.pageCount,
        )
      }
    }
  }
}

@Composable
private fun HeroLoadingPlaceholder() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Column(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(start = 16.dp, end = 16.dp, bottom = 54.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      repeat(4) { index ->
        Surface(
          modifier = Modifier
            .fillMaxWidth(if (index == 1) 0.72f else 0.48f)
            .height(if (index == 1) 38.dp else 16.dp),
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {}
      }
    }
  }
}

private fun areSystemAnimationsEnabled(): Boolean =
  Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()

private const val HERO_ITEM_LIMIT = 8
private const val HERO_AUTO_SLIDE_MILLIS = 6_000L
