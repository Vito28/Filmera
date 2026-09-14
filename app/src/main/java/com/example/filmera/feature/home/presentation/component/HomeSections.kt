package com.example.filmera.feature.home.presentation.component

import android.animation.ValueAnimator
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.filmera.R
import com.example.filmera.core.designsystem.theme.filmeraColors
import com.example.filmera.core.common.AppError
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.ui.LazyLayoutKey
import com.example.filmera.core.ui.component.TmdbImage
import com.example.filmera.core.ui.messageResource
import com.example.filmera.feature.home.domain.HomeCollection
import com.example.filmera.feature.home.domain.HomeCollectionKind
import com.example.filmera.feature.home.domain.HomeContent
import com.example.filmera.feature.home.domain.HomeGenre
import com.example.filmera.feature.home.domain.HomePerson
import com.example.filmera.feature.home.domain.HomeSection
import com.example.filmera.feature.home.domain.HomeSectionType
import com.example.filmera.feature.home.domain.HomeSpotlight
import com.example.filmera.feature.home.domain.HomeTrailer
import java.text.ParseException
import java.text.SimpleDateFormat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive

@Composable
fun TopPicksSection(
  content: HomeContent,
  preferredGenreIds: Set<Int>,
  onMediaClick: (MediaItem) -> Unit,
  onShowAll: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedCategory by rememberSaveable { mutableStateOf(TopPickCategory.GLOBAL) }
  val hapticFeedback = LocalHapticFeedback.current
  val candidates = remember(content, selectedCategory, preferredGenreIds) {
    content.topPickCandidates(selectedCategory, preferredGenreIds)
  }
  val isLoading = content.section(HomeSectionType.EDITORS_PICKS)?.isInitialLoading == true ||
    content.section(HomeSectionType.POPULAR_WORLDWIDE)?.isInitialLoading == true

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    SectionHeader(
      title = stringResource(R.string.home_top_picks),
      subtitle = stringResource(R.string.home_top_picks_subtitle),
      actionLabel = stringResource(R.string.show_all),
      onActionClick = onShowAll,
    )
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(end = 8.dp),
    ) {
      items(
        items = TopPickCategory.entries,
        key = { category -> LazyLayoutKey.of("home-top-pick-tab", category.name) },
      ) { category ->
        val selected = category == selectedCategory
        Surface(
          onClick = {
            if (selectedCategory != category) {
              hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
              selectedCategory = category
            }
          },
          shape = MaterialTheme.shapes.extraLarge,
          color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
          } else {
            Color.Transparent
          },
          contentColor = if (selected) {
            MaterialTheme.colorScheme.onSurface
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
          border = BorderStroke(
            1.dp,
            if (selected) {
              MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
            } else {
              MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f)
            },
          ),
        ) {
          Text(
            text = stringResource(category.labelResource),
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
          )
        }
      }
    }

    when {
      candidates.isNotEmpty() -> AnimatedContent(
        targetState = selectedCategory,
        transitionSpec = {
          (slideInHorizontally(tween(320)) { width -> width / 10 } + fadeIn(tween(260)))
            .togetherWith(
              slideOutHorizontally(tween(260)) { width -> -width / 12 } + fadeOut(tween(180)),
            )
        },
        label = "home top picks category",
      ) { category ->
        val animatedCandidates = content.topPickCandidates(category, preferredGenreIds)
        TopPickLayout(
          items = animatedCandidates,
          reason = stringResource(category.reasonResource),
          onMediaClick = onMediaClick,
        )
      }
      isLoading -> SectionLoadingPlaceholder(height = 392.dp)
      else -> SectionStatusMessage(
        error = null,
        emptyMessage = stringResource(R.string.home_editors_picks_empty),
        onRetry = {},
      )
    }
    content.cinemaPulse()?.let { pulse ->
      WorldCinemaPulseCard(
        pulse = pulse,
        onClick = onShowAll,
      )
    }
  }
}

@Composable
private fun WorldCinemaPulseCard(
  pulse: CinemaPulse,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
    ),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
        pulse.items.take(3).forEach { item ->
          TmdbImage(
            path = item.posterPath ?: item.backdropPath,
            contentDescription = null,
            modifier = Modifier
              .width(48.dp)
              .height(68.dp)
              .clip(MaterialTheme.shapes.medium),
            size = "w185",
          )
        }
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(
          text = stringResource(R.string.home_cinema_pulse),
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = stringResource(R.string.home_cinema_pulse_title, pulse.regionName),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = stringResource(R.string.home_cinema_pulse_signal),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.labelSmall,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = stringResource(R.string.home_cinema_pulse_explore),
        tint = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
private fun TopPickLayout(
  items: List<MediaItem>,
  reason: String,
  onMediaClick: (MediaItem) -> Unit,
) {
  val featured = items.firstOrNull() ?: return
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 392.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Card(
      onClick = { onMediaClick(featured) },
      modifier = Modifier
        .fillMaxWidth()
        .height(190.dp),
      shape = MaterialTheme.shapes.extraLarge,
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
      ),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        TmdbImage(
          path = featured.backdropPath ?: featured.posterPath,
          contentDescription = stringResource(
            R.string.backdrop_content_description,
            featured.title,
          ),
          modifier = Modifier.fillMaxSize(),
          size = "w780",
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(
                  Color.Transparent,
                  MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.94f),
                ),
              ),
            ),
        )
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Text(
            text = reason,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = featured.title,
            color = MaterialTheme.filmeraColors.onImage,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          MediaSupportingRow(
            item = featured,
            contentColor = MaterialTheme.filmeraColors.onImage.copy(alpha = 0.82f),
          )
        }
      }
    }
    items.drop(1).take(4).chunked(2).forEach { rowItems ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        rowItems.forEach { item ->
          Card(
            onClick = { onMediaClick(item) },
            modifier = Modifier
              .weight(1f)
              .height(96.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
          ) {
            Row(modifier = Modifier.fillMaxSize()) {
              TmdbImage(
                path = item.posterPath ?: item.backdropPath,
                contentDescription = null,
                modifier = Modifier
                  .width(64.dp)
                  .fillMaxHeight(),
                size = "w185",
              )
              Column(
                modifier = Modifier
                  .weight(1f)
                  .padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Text(
                  text = item.title,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
                MediaSupportingRow(item = item)
              }
            }
          }
        }
        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
      }
    }
  }
}

private fun HomeContent.topPickCandidates(
  category: TopPickCategory,
  preferredGenreIds: Set<Int>,
): List<MediaItem> {
  val trending = section(HomeSectionType.TRENDING)?.items.orEmpty()
  val editorial = section(HomeSectionType.EDITORS_PICKS)?.items.orEmpty()
  val popular = section(HomeSectionType.POPULAR_WORLDWIDE)?.items.orEmpty()
  val upcoming = section(HomeSectionType.UPCOMING)?.items.orEmpty()
  val global = (editorial + trending + popular + upcoming)
    .distinctBy(MediaItem::key)
    .sortedWith(
      compareByDescending<MediaItem> { item ->
        item.genreIds.count(preferredGenreIds::contains)
      }
        .thenByDescending { item -> weightedAudienceScore(item) }
        .thenByDescending(MediaItem::popularity),
    )
  val filtered = when (category) {
    TopPickCategory.GLOBAL -> global
    TopPickCategory.SPORTS -> global.filter { item ->
      SPORTS_DRAMA_GENRE_ID in item.genreIds ||
        SPORTS_TERMS.any { term ->
          item.title.contains(term, ignoreCase = true) ||
            item.overview.contains(term, ignoreCase = true)
        }
    }
    TopPickCategory.K_DRAMA -> global.filter { item ->
      item.type == MediaType.TV_SHOW && item.originalLanguage == "ko"
    }
    TopPickCategory.MOVIES -> global.filter { it.type == MediaType.MOVIE }
    TopPickCategory.UPCOMING -> upcoming
    TopPickCategory.WESTERN -> global.filter { it.originalLanguage in WESTERN_LANGUAGES }
    TopPickCategory.ANIME -> global.filter { item ->
      ANIMATION_GENRE_ID in item.genreIds && item.originalLanguage == "ja"
    }
    TopPickCategory.HOT_SEARCH -> trending
  }
  return filtered.ifEmpty { global }.take(TOP_PICK_ITEM_LIMIT)
}

private fun weightedAudienceScore(item: MediaItem): Double {
  val confidence = item.voteCount.toDouble() / (item.voteCount + 750.0)
  return item.voteAverage * confidence
}

private fun HomeContent.cinemaPulse(): CinemaPulse? {
  val candidates = (
    section(HomeSectionType.TRENDING)?.items.orEmpty() +
      section(HomeSectionType.EDITORS_PICKS)?.items.orEmpty()
    )
    .filter { !it.posterPath.isNullOrBlank() || !it.backdropPath.isNullOrBlank() }
    .distinctBy(MediaItem::key)
  val group = candidates
    .groupBy { it.originalLanguage.ifBlank { "other" } }
    .maxByOrNull { (_, items) -> items.size }
    ?: return null
  if (group.value.size < 3) return null
  return CinemaPulse(
    regionName = LANGUAGE_REGION_NAMES[group.key] ?: "Global cinema",
    items = group.value.take(3),
  )
}

private data class CinemaPulse(
  val regionName: String,
  val items: List<MediaItem>,
)

private enum class TopPickCategory(
  @StringRes val labelResource: Int,
  @StringRes val reasonResource: Int,
) {
  GLOBAL(R.string.home_top_pick_global, R.string.home_top_pick_reason_global),
  SPORTS(R.string.home_top_pick_sports, R.string.home_top_pick_reason_sports),
  K_DRAMA(R.string.home_top_pick_k_drama, R.string.home_top_pick_reason_k_drama),
  MOVIES(R.string.home_top_pick_movies, R.string.home_top_pick_reason_movies),
  UPCOMING(R.string.home_top_pick_upcoming, R.string.home_top_pick_reason_upcoming),
  WESTERN(R.string.home_top_pick_western, R.string.home_top_pick_reason_western),
  ANIME(R.string.home_top_pick_anime, R.string.home_top_pick_reason_anime),
  HOT_SEARCH(R.string.home_top_pick_hot_search, R.string.home_top_pick_reason_hot_search),
}

@Composable
fun TrendingSection(
  section: HomeSection?,
  onMediaClick: (MediaItem) -> Unit,
  onShowAll: () -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  PaginatedHomeSection(
    title = stringResource(R.string.section_trending),
    subtitle = stringResource(R.string.section_trending_subtitle),
    section = section,
    emptyMessage = stringResource(R.string.home_trending_empty),
    onShowAll = onShowAll,
    onLoadMore = {},
    onRetry = onRetry,
    enablePagination = false,
    maxItemCount = HOME_TRENDING_LIMIT,
    modifier = modifier,
  ) { items, _ ->
    TrendingBentoRanking(
      items = items,
      onMediaClick = onMediaClick,
    )
  }
}

@Composable
private fun TrendingBentoRanking(
  items: List<MediaItem>,
  onMediaClick: (MediaItem) -> Unit,
) {
  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val expanded = maxWidth >= TRENDING_EXPANDED_BREAKPOINT

    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (expanded && items.size > 1) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          TrendingFeaturedCard(
            item = items.first(),
            onClick = { onMediaClick(items.first()) },
            imageHeight = TRENDING_FEATURED_IMAGE_HEIGHT_EXPANDED,
            modifier = Modifier
              .weight(1.35f)
              .fillMaxHeight(),
          )
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items
              .drop(1)
              .take(2)
              .forEachIndexed { index, item ->
                TrendingSecondaryCard(
                  rank = index + 2,
                  item = item,
                  horizontal = true,
                  onClick = { onMediaClick(item) },
                  modifier = Modifier.fillMaxWidth(),
                )
              }
          }
        }
      } else {
        TrendingFeaturedCard(
          item = items.first(),
          onClick = { onMediaClick(items.first()) },
          imageHeight = TRENDING_FEATURED_IMAGE_HEIGHT_COMPACT,
          modifier = Modifier.fillMaxWidth(),
        )
        if (items.size > 1) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items
              .drop(1)
              .take(2)
              .forEachIndexed { index, item ->
                TrendingSecondaryCard(
                  rank = index + 2,
                  item = item,
                  horizontal = false,
                  onClick = { onMediaClick(item) },
                  modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                )
              }
          }
        }
      }

      if (items.size > TRENDING_BENTO_LEAD_COUNT) {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(end = 8.dp),
        ) {
          itemsIndexed(
            items = items.drop(TRENDING_BENTO_LEAD_COUNT),
            key = { _, item -> LazyLayoutKey.media("home-trending-compact", item) },
          ) { index, item ->
            TrendingCompactCard(
              rank = index + TRENDING_BENTO_LEAD_COUNT + 1,
              item = item,
              onClick = { onMediaClick(item) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TrendingFeaturedCard(
  item: MediaItem,
  imageHeight: Dp,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val rank = 1
  val rankDescription = stringResource(R.string.browse_rank_description, rank, item.title)
  Card(
    onClick = onClick,
    modifier = modifier.semantics(mergeDescendants = true) {
      contentDescription = rankDescription
    },
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    TmdbImage(
      path = item.backdropPath ?: item.posterPath,
      contentDescription = stringResource(
        R.string.backdrop_content_description,
        item.title,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(imageHeight),
      size = "w780",
    )
    Row(
      modifier = Modifier.padding(10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.Top,
    ) {
      TrendingRankBadge(
        rank = rank,
        prominent = true,
      )
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = item.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        MediaSupportingRow(item = item)
      }
    }
  }
}

@Composable
private fun TrendingSecondaryCard(
  rank: Int,
  item: MediaItem,
  onClick: () -> Unit,
  horizontal: Boolean,
  modifier: Modifier = Modifier,
) {
  val rankDescription = stringResource(R.string.browse_rank_description, rank, item.title)
  Card(
    onClick = onClick,
    modifier = modifier.semantics(mergeDescendants = true) {
      contentDescription = rankDescription
    },
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    if (horizontal) {
      Row(modifier = Modifier.fillMaxWidth()) {
        TmdbImage(
          path = item.backdropPath ?: item.posterPath,
          contentDescription = stringResource(
            R.string.backdrop_content_description,
            item.title,
          ),
          modifier = Modifier
            .width(TRENDING_SECONDARY_IMAGE_WIDTH_EXPANDED)
            .height(TRENDING_SECONDARY_CARD_HEIGHT_EXPANDED),
          size = "w500",
        )
        TrendingCardDetails(
          rank = rank,
          item = item,
          modifier = Modifier
            .weight(1f)
            .padding(10.dp),
        )
      }
    } else {
      Column(modifier = Modifier.fillMaxWidth()) {
        TmdbImage(
          path = item.backdropPath ?: item.posterPath,
          contentDescription = stringResource(
            R.string.backdrop_content_description,
            item.title,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(TRENDING_SECONDARY_IMAGE_HEIGHT_COMPACT),
          size = "w500",
        )
        TrendingCardDetails(
          rank = rank,
          item = item,
          modifier = Modifier.padding(8.dp),
        )
      }
    }
  }
}

@Composable
private fun TrendingCompactCard(
  rank: Int,
  item: MediaItem,
  onClick: () -> Unit,
) {
  val rankDescription = stringResource(R.string.browse_rank_description, rank, item.title)
  Card(
    onClick = onClick,
    modifier = Modifier
      .width(TRENDING_COMPACT_CARD_WIDTH)
      .semantics(mergeDescendants = true) {
        contentDescription = rankDescription
      },
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    TmdbImage(
      path = item.backdropPath ?: item.posterPath,
      contentDescription = stringResource(
        R.string.backdrop_content_description,
        item.title,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(TRENDING_COMPACT_IMAGE_HEIGHT),
      size = "w342",
    )
    TrendingCardDetails(
      rank = rank,
      item = item,
      modifier = Modifier.padding(8.dp),
    )
  }
}

@Composable
private fun TrendingCardDetails(
  rank: Int,
  item: MediaItem,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.Top,
    ) {
      TrendingRankBadge(rank = rank)
      Text(
        text = item.title,
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        minLines = 2,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
    MediaSupportingRow(item = item)
  }
}

@Composable
private fun TrendingRankBadge(
  rank: Int,
  prominent: Boolean = false,
) {
  Surface(
    modifier = Modifier
      .size(
        width = if (prominent) 44.dp else 34.dp,
        height = if (prominent) 36.dp else 28.dp,
      )
      .testTag("home_trending_rank_$rank"),
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f),
    contentColor = MaterialTheme.colorScheme.primary,
    border = BorderStroke(
      width = 1.dp,
      color = MaterialTheme.colorScheme.primary.copy(alpha = 0.48f),
    ),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = rank.toString().padStart(2, '0'),
        style = if (prominent) {
          MaterialTheme.typography.titleMedium
        } else {
          MaterialTheme.typography.labelLarge
        }.copy(
          fontFeatureSettings = "tnum",
        ),
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
fun MovieSpotlightSection(
  spotlights: List<HomeSpotlight>,
  title: String,
  isVisible: Boolean,
  onStoryClick: (HomeSpotlight) -> Unit,
  onShowAll: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (spotlights.isEmpty()) return
  val items = spotlights.take(SPOTLIGHT_LIMIT)
  val pagerState = rememberPagerState(pageCount = items::size)
  AutoSlidePagerEffect(
    pagerState = pagerState,
    intervalMillis = SPOTLIGHT_AUTO_SLIDE_MILLIS,
    isVisible = isVisible,
  )

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    SectionHeader(
      title = title,
      actionLabel = stringResource(R.string.show_all),
      onActionClick = onShowAll,
    )
    HorizontalPager(
      state = pagerState,
      pageSpacing = 12.dp,
      key = { page -> LazyLayoutKey.of("home-spotlight", items[page].id) },
    ) {
      val story = items[it]
      Card(
        onClick = { onStoryClick(story) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 8.8f),
        ) {
          TmdbImage(
            path = story.media.backdropPath ?: story.media.posterPath,
            contentDescription = stringResource(
              R.string.backdrop_content_description,
              story.media.title,
            ),
            modifier = Modifier.fillMaxSize(),
            size = "w780",
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.horizontalGradient(
                  listOf(
                    MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.92f),
                    MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.3f),
                  ),
                ),
              ),
          )
          Column(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .fillMaxWidth(0.82f)
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "${story.category} • ${story.source}\n${story.publishedLabel} • ${
                stringResource(R.string.home_spotlight_read_time, story.readMinutes)
              }",
              color = MaterialTheme.colorScheme.primaryContainer,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = story.title,
              color = MaterialTheme.filmeraColors.onImage,
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = story.summary,
              color = MaterialTheme.filmeraColors.onImage.copy(alpha = 0.82f),
              style = MaterialTheme.typography.bodyMedium,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = stringResource(R.string.home_read_story),
              color = MaterialTheme.filmeraColors.onImage,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }
    PagerIndicator(
      pageCount = items.size,
      currentPage = pagerState.currentPage,
      modifier = Modifier.align(Alignment.CenterHorizontally),
    )
  }
}

@Composable
fun UpcomingSection(
  section: HomeSection?,
  onMediaClick: (MediaItem) -> Unit,
  onShowAll: () -> Unit,
  onLoadMore: () -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  PaginatedHomeSection(
    title = stringResource(R.string.section_upcoming),
    subtitle = stringResource(R.string.section_upcoming_subtitle),
    section = section,
    emptyMessage = stringResource(R.string.home_upcoming_empty),
    onShowAll = onShowAll,
    onLoadMore = onLoadMore,
    onRetry = onRetry,
    modifier = modifier,
  ) { items, listState ->
    LazyRow(
      state = listState,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(end = 8.dp),
    ) {
      items(
        items = items,
        key = { item -> LazyLayoutKey.media("home-upcoming", item) },
      ) { item ->
        UpcomingReleaseCard(
          item = item,
          onClick = { onMediaClick(item) },
        )
      }
      paginationTail(requireNotNull(section))
    }
  }
}

@Composable
private fun UpcomingReleaseCard(
  item: MediaItem,
  onClick: () -> Unit,
) {
  val locale = LocalLocale.current.platformLocale
  val dateToBeAnnounced = stringResource(R.string.date_to_be_announced_short)
  val date = remember(item.releaseDate, locale, dateToBeAnnounced) {
    formatReleaseDate(
      rawDate = item.releaseDate,
      locale = locale,
      dateToBeAnnounced = dateToBeAnnounced,
    )
  }

  Card(
    onClick = onClick,
    modifier = Modifier.width(272.dp),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(146.dp),
      ) {
        TmdbImage(
          path = item.backdropPath ?: item.posterPath,
          contentDescription = stringResource(
            R.string.backdrop_content_description,
            item.title,
          ),
          modifier = Modifier.fillMaxSize(),
          size = "w500",
        )
        Surface(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(10.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          shape = MaterialTheme.shapes.medium,
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = date.month,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = date.day,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = item.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = date.year,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.labelMedium,
        )
      }
    }
  }
}

@Composable
fun GenreSection(
  genres: List<HomeGenre>,
  error: AppError?,
  isLoading: Boolean,
  onGenreClick: () -> Unit,
  onShowAll: () -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    SectionHeader(
      title = stringResource(R.string.home_explore_genres),
      subtitle = stringResource(R.string.home_explore_genres_subtitle),
      actionLabel = stringResource(R.string.show_all),
      onActionClick = onShowAll,
    )
    when {
      genres.isNotEmpty() -> GenreGrid(genres = genres, onGenreClick = onGenreClick)
      isLoading -> SectionLoadingPlaceholder(height = 242.dp)
      else -> {
      SectionStatusMessage(
        error = error,
        emptyMessage = stringResource(R.string.home_genres_empty),
        onRetry = onRetry,
      )
      }
    }
  }
}

@Composable
private fun GenreGrid(
  genres: List<HomeGenre>,
  onGenreClick: () -> Unit,
) {
  val rows = remember(genres) { genres.take(6).chunked(2) }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    rows.forEach { rowGenres ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        rowGenres.forEach { genre ->
          Card(
            onClick = onGenreClick,
            modifier = Modifier
              .weight(1f)
              .height(116.dp),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
          ) {
            Box(modifier = Modifier.fillMaxSize()) {
              TmdbImage(
                path = genre.backdropPath,
                contentDescription = stringResource(
                  R.string.genre_image_description,
                  genre.name,
                ),
                modifier = Modifier.fillMaxSize(),
                size = "w500",
              )
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(
                    Brush.horizontalGradient(
                      listOf(
                        MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.82f),
                        MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.12f),
                      ),
                    ),
                  ),
              )
              Column(
                modifier = Modifier
                  .align(Alignment.BottomStart)
                  .padding(12.dp),
              ) {
                Text(
                  text = genre.name,
                  color = MaterialTheme.filmeraColors.onImage,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = pluralStringResource(
                    R.plurals.home_title_count,
                    genre.titleCount,
                    genre.titleCount,
                  ),
                  color = MaterialTheme.filmeraColors.onImage.copy(alpha = 0.74f),
                  style = MaterialTheme.typography.labelMedium,
                )
              }
            }
          }
        }
        if (rowGenres.size == 1) Spacer(modifier = Modifier.weight(1f))
      }
    }
  }
}

@Composable
fun EditorsPicksSection(
  section: HomeSection?,
  onMediaClick: (MediaItem) -> Unit,
  onShowAll: () -> Unit,
  onLoadMore: () -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  PaginatedHomeSection(
    title = stringResource(R.string.home_editors_picks),
    subtitle = stringResource(R.string.home_editors_picks_subtitle),
    section = section,
    emptyMessage = stringResource(R.string.home_editors_picks_empty),
    onShowAll = onShowAll,
    onLoadMore = onLoadMore,
    onRetry = onRetry,
    modifier = modifier,
  ) { items, listState ->
    LazyRow(
      state = listState,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = PaddingValues(end = 8.dp),
    ) {
      items(
        items = items,
        key = { item -> LazyLayoutKey.media("home-editors-pick", item) },
      ) { item ->
        EditorialMediaCard(
          item = item,
          onClick = { onMediaClick(item) },
        )
      }
      paginationTail(requireNotNull(section))
    }
  }
}

@Composable
private fun EditorialMediaCard(
  item: MediaItem,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    modifier = Modifier.width(310.dp),
    shape = MaterialTheme.shapes.extraLarge,
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 10f),
    ) {
      TmdbImage(
        path = item.backdropPath ?: item.posterPath,
        contentDescription = stringResource(
          R.string.backdrop_content_description,
          item.title,
        ),
        modifier = Modifier.fillMaxSize(),
        size = "w780",
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(
                Color.Transparent,
                MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.88f),
              ),
            ),
          ),
      )
      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Text(
          text = stringResource(R.string.home_spotlight_label),
          color = MaterialTheme.colorScheme.primaryContainer,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = item.title,
          color = MaterialTheme.filmeraColors.onImage,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        MediaSupportingRow(
          item = item,
          contentColor = MaterialTheme.filmeraColors.onImage.copy(alpha = 0.82f),
        )
      }
    }
  }
}

@Composable
fun PopularPeopleSection(
  people: List<HomePerson>,
  error: AppError?,
  isLoading: Boolean,
  title: String,
  subtitle: String,
  onPersonClick: (Int) -> Unit,
  onShowAll: () -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    SectionHeader(
      title = title,
      subtitle = subtitle,
      actionLabel = stringResource(R.string.show_all),
      onActionClick = onShowAll,
    )
    if (people.isNotEmpty()) {
      RegionalPeopleMosaic(
        people = people.take(4),
        onPersonClick = onPersonClick,
      )
    } else if (isLoading) {
      SectionLoadingPlaceholder(height = 218.dp)
    } else {
      SectionStatusMessage(
        error = error,
        emptyMessage = stringResource(R.string.home_people_empty),
        onRetry = onRetry,
      )
    }
  }
}

@Composable
private fun RegionalPeopleMosaic(
  people: List<HomePerson>,
  onPersonClick: (Int) -> Unit,
) {
  val featured = people.firstOrNull() ?: return
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(284.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Card(
      onClick = { onPersonClick(featured.id) },
      modifier = Modifier
        .weight(1.25f)
        .fillMaxHeight(),
      shape = MaterialTheme.shapes.extraLarge,
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
      ),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        TmdbImage(
          path = featured.profilePath,
          contentDescription = stringResource(
            R.string.profile_image_description,
            featured.name,
          ),
          modifier = Modifier.fillMaxSize(),
          size = "h632",
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(
                  Color.Transparent,
                  MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.94f),
                ),
              ),
            ),
        )
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = stringResource(R.string.home_star_rising_label),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = featured.name,
            color = MaterialTheme.filmeraColors.onImage,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          featured.knownFor.firstOrNull()?.let { knownFor ->
            Text(
              text = knownFor,
              color = MaterialTheme.filmeraColors.onImage.copy(alpha = 0.76f),
              style = MaterialTheme.typography.labelSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      people.drop(1).take(3).forEachIndexed { index, person ->
        Card(
          onClick = { onPersonClick(person.id) },
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
          Row(modifier = Modifier.fillMaxSize()) {
            TmdbImage(
              path = person.profilePath,
              contentDescription = stringResource(
                R.string.profile_image_description,
                person.name,
              ),
              modifier = Modifier
                .width(72.dp)
                .fillMaxHeight(),
              size = "w185",
            )
            Column(
              modifier = Modifier
                .weight(1f)
                .padding(9.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = if (index == 0) {
                  stringResource(R.string.home_star_global_label)
                } else {
                  stringResource(R.string.home_star_new_release_label)
                },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
              )
              Text(
                text = person.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun NewTrailersSection(
  trailers: List<HomeTrailer>,
  error: AppError?,
  isLoading: Boolean,
  title: String,
  onPlayTrailer: (String) -> Unit,
  onShowAll: () -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    SectionHeader(
      title = title,
      subtitle = stringResource(R.string.home_new_trailers_subtitle),
      actionLabel = stringResource(R.string.show_all),
      onActionClick = onShowAll,
    )
    if (trailers.isNotEmpty()) {
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(end = 8.dp),
      ) {
        items(
          items = trailers,
          key = { trailer -> LazyLayoutKey.of("home-trailer", trailer.videoKey) },
        ) { trailer ->
          TrailerCard(
            trailer = trailer,
            onClick = { onPlayTrailer(trailer.videoKey) },
          )
        }
      }
    } else if (isLoading) {
      SectionLoadingPlaceholder(height = 214.dp)
    } else {
      SectionStatusMessage(
        error = error,
        emptyMessage = stringResource(R.string.home_trailers_empty),
        onRetry = onRetry,
      )
    }
  }
}

@Composable
private fun TrailerCard(
  trailer: HomeTrailer,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    modifier = Modifier.width(296.dp),
    shape = MaterialTheme.shapes.extraLarge,
    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)
        .clip(MaterialTheme.shapes.large),
    ) {
      TmdbImage(
        path = trailer.media.backdropPath ?: trailer.media.posterPath,
        contentDescription = stringResource(
          R.string.backdrop_content_description,
          trailer.media.title,
        ),
        modifier = Modifier.fillMaxSize(),
        size = "w780",
      )
      Surface(
        modifier = Modifier
          .align(Alignment.Center)
          .size(52.dp),
        shape = CircleShape,
        color = MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.72f),
        contentColor = MaterialTheme.filmeraColors.onImage,
      ) {
        Icon(
          imageVector = Icons.Default.PlayArrow,
          contentDescription = stringResource(R.string.play_trailer),
          modifier = Modifier.padding(12.dp),
        )
      }
      Surface(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(10.dp),
        color = MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.7f),
        contentColor = MaterialTheme.filmeraColors.onImage,
        shape = MaterialTheme.shapes.small,
      ) {
        Text(
          text = trailer.type,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          style = MaterialTheme.typography.labelSmall,
        )
      }
    }
    Text(
      text = trailer.media.title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = trailer.name,
      modifier = Modifier.padding(bottom = 8.dp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.labelMedium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    }
  }
}

@Composable
fun TopRatedSection(
  section: HomeSection?,
  title: String,
  onMediaClick: (MediaItem) -> Unit,
  onShowAll: () -> Unit,
  onLoadMore: () -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  PaginatedHomeSection(
    title = title,
    section = section,
    emptyMessage = stringResource(R.string.home_top_rated_empty),
    onShowAll = onShowAll,
    onLoadMore = onLoadMore,
    onRetry = onRetry,
    modifier = modifier,
  ) { items, listState ->
    PosterRail(
      section = requireNotNull(section),
      items = items,
      listState = listState,
      scope = "home-top-rated",
      onMediaClick = onMediaClick,
    )
  }
}

@Composable
fun CollectionsSection(
  collections: List<HomeCollection>,
  error: AppError?,
  isLoading: Boolean,
  title: String,
  isVisible: Boolean,
  onCollectionClick: (HomeCollection) -> Unit,
  onShowAll: () -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (collections.isEmpty() && error == null && !isLoading) return
  val items = collections.take(MAXIMUM_HOME_COLLECTIONS)
  val pagerState = rememberPagerState(pageCount = items::size)
  AutoSlidePagerEffect(
    pagerState = pagerState,
    intervalMillis = COLLECTION_AUTO_SLIDE_MILLIS,
    isVisible = isVisible,
  )

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    SectionHeader(
      title = title,
      subtitle = stringResource(R.string.home_collections_subtitle),
      actionLabel = stringResource(R.string.show_all),
      onActionClick = onShowAll,
    )
    if (items.isNotEmpty()) {
      HorizontalPager(
        state = pagerState,
        pageSpacing = 12.dp,
        key = { page ->
          val collection = items[page]
          LazyLayoutKey.of(
            "home-collection",
            collection.kind.name,
            collection.id.toString(),
          )
        },
      ) { page ->
        val collection = items[page]
        CollectionBanner(
          collection = collection,
          onClick = { onCollectionClick(collection) },
          modifier = Modifier.fillMaxWidth(),
        )
      }
      PagerIndicator(
        pageCount = items.size,
        currentPage = pagerState.currentPage,
        modifier = Modifier.align(Alignment.CenterHorizontally),
      )
    } else if (isLoading) {
      SectionLoadingPlaceholder(height = 220.dp)
    } else {
      SectionStatusMessage(
        error = error,
        emptyMessage = "",
        onRetry = onRetry,
      )
    }
  }
}

@Composable
private fun CollectionBanner(
  collection: HomeCollection,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    modifier = modifier
      .fillMaxWidth()
      .aspectRatio(16f / 8.2f),
    shape = MaterialTheme.shapes.extraLarge,
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      TmdbImage(
        path = collection.backdropPath ?: collection.featuredMedia.backdropPath,
        contentDescription = collection.name,
        modifier = Modifier.fillMaxSize(),
        size = "w780",
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.horizontalGradient(
              listOf(
                MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.88f),
                MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.24f),
              ),
            ),
          ),
      )
      collection.posterPath?.let { posterPath ->
        TmdbImage(
          path = posterPath,
          contentDescription = null,
          modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(12.dp)
            .width(92.dp)
            .height(138.dp)
            .clip(MaterialTheme.shapes.medium),
          size = "w185",
        )
      }
      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .fillMaxWidth(0.72f)
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = stringResource(
            if (collection.kind == HomeCollectionKind.OFFICIAL_COLLECTION) {
              R.string.home_collection_official
            } else {
              R.string.home_collection_universe
            },
          ),
          color = MaterialTheme.colorScheme.primaryContainer,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = collection.name,
          color = MaterialTheme.filmeraColors.onImage,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        val supportingText = collection.overview.ifBlank {
          if (collection.itemCount > 0) {
            pluralStringResource(
              R.plurals.home_title_count,
              collection.itemCount,
              collection.itemCount,
            )
          } else {
            stringResource(
              R.string.home_collection_featured,
              collection.featuredMedia.title,
            )
          }
        }
        Text(
          text = supportingText,
          color = MaterialTheme.filmeraColors.onImage.copy(alpha = 0.8f),
          style = MaterialTheme.typography.bodySmall,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = stringResource(R.string.home_explore_collection),
          color = MaterialTheme.filmeraColors.onImage,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }
}

@Composable
fun HiddenGemsSection(
  section: HomeSection?,
  onMediaClick: (MediaItem) -> Unit,
  onShowAll: () -> Unit,
  onLoadMore: () -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  PaginatedHomeSection(
    title = stringResource(R.string.home_hidden_gems),
    subtitle = stringResource(R.string.home_hidden_gems_subtitle),
    section = section,
    emptyMessage = stringResource(R.string.home_hidden_gems_empty),
    onShowAll = onShowAll,
    onLoadMore = onLoadMore,
    onRetry = onRetry,
    enablePagination = false,
    maxItemCount = 3,
    modifier = modifier,
  ) { items, _ ->
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      items.forEach { item ->
        HiddenGemCard(
          item = item,
          onClick = { onMediaClick(item) },
        )
      }
    }
  }
}

@Composable
private fun HiddenGemCard(
  item: MediaItem,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .height(112.dp),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Row(modifier = Modifier.fillMaxSize()) {
      TmdbImage(
        path = item.posterPath ?: item.backdropPath,
        contentDescription = stringResource(
          R.string.backdrop_content_description,
          item.title,
        ),
        modifier = Modifier
          .width(76.dp)
          .fillMaxHeight(),
        size = "w185",
      )
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = stringResource(R.string.home_hidden_gem_reason),
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = item.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        MediaSupportingRow(
          item = item,
        )
      }
    }
  }
}

@Composable
fun PopularRecommendationSection(
  section: HomeSection?,
  title: String,
  onMediaClick: (MediaItem) -> Unit,
  onShowAll: () -> Unit,
  onLoadMore: () -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  PaginatedHomeSection(
    title = title,
    section = section,
    emptyMessage = stringResource(R.string.home_popular_empty),
    onShowAll = onShowAll,
    onLoadMore = onLoadMore,
    onRetry = onRetry,
    modifier = modifier,
  ) { items, listState ->
    PosterRail(
      section = requireNotNull(section),
      items = items,
      listState = listState,
      scope = "home-popular-worldwide",
      onMediaClick = onMediaClick,
    )
  }
}

@Composable
fun WatchlistPreviewSection(
  items: List<MediaItem>,
  onMediaClick: (MediaItem) -> Unit,
  onViewAllClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (items.isEmpty()) return

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    SectionHeader(
      title = stringResource(R.string.home_my_watchlist),
      subtitle = stringResource(R.string.home_my_watchlist_subtitle),
      actionLabel = stringResource(R.string.view_all),
      onActionClick = onViewAllClick,
    )
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(end = 8.dp),
    ) {
      items(
        items = items,
        key = { item -> LazyLayoutKey.media("home-watchlist-preview", item) },
      ) { item ->
        Card(
          onClick = { onMediaClick(item) },
          modifier = Modifier.width(132.dp),
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = Color.Transparent),
          elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
          Box {
            TmdbImage(
              path = item.posterPath ?: item.backdropPath,
              contentDescription = stringResource(
                R.string.poster_content_description,
                item.title,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(MaterialTheme.shapes.large),
              size = "w342",
            )
            Surface(
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(32.dp),
              shape = CircleShape,
              color = MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.68f),
              contentColor = MaterialTheme.filmeraColors.onImage,
            ) {
              Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                modifier = Modifier.padding(7.dp),
              )
            }
          }
          Text(
            text = item.title,
            modifier = Modifier.padding(bottom = 6.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          }
        }
      }
    }
  }
}

@Composable
fun HomeLoadingFeed(
  isExpanded: Boolean,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(30.dp),
  ) {
    item(key = "loading-hero") {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(if (isExpanded) 620.dp else 560.dp)
          .background(MaterialTheme.colorScheme.surfaceContainer),
      ) {
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 16.dp, end = 16.dp, bottom = 48.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          SkeletonBlock(width = 100.dp, height = 18.dp)
          SkeletonBlock(width = 280.dp, height = 40.dp)
          SkeletonBlock(width = 190.dp, height = 18.dp)
          SkeletonBlock(width = 330.dp, height = 58.dp)
          SkeletonBlock(width = 220.dp, height = 48.dp)
        }
      }
    }
    items(
      count = 5,
      key = { index -> LazyLayoutKey.indexed("home-loading-section", index) },
    ) { index ->
      Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        SkeletonBlock(width = 210.dp, height = 28.dp)
        if (index % 2 == 0) {
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
              SkeletonBlock(modifier = Modifier.width(132.dp), height = 198.dp)
            }
          }
        } else {
          SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 190.dp)
        }
      }
    }
  }
}

@Composable
private fun PosterRail(
  section: HomeSection,
  items: List<MediaItem>,
  listState: LazyListState,
  scope: String,
  onMediaClick: (MediaItem) -> Unit,
) {
  LazyRow(
    state = listState,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(end = 8.dp),
  ) {
    items(
      items = items,
      key = { item -> LazyLayoutKey.media(scope, item) },
    ) { item ->
      Card(
        onClick = { onMediaClick(item) },
        modifier = Modifier.width(144.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        TmdbImage(
          path = item.posterPath ?: item.backdropPath,
          contentDescription = stringResource(
            R.string.poster_content_description,
            item.title,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clip(MaterialTheme.shapes.large),
          size = "w342",
        )
        Text(
          text = item.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        MediaSupportingRow(item = item)
        Spacer(modifier = Modifier.height(4.dp))
        }
      }
    }
    paginationTail(section)
  }
}

@Composable
private fun PaginatedHomeSection(
  title: String,
  section: HomeSection?,
  emptyMessage: String,
  onShowAll: () -> Unit,
  onLoadMore: () -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  enablePagination: Boolean = true,
  maxItemCount: Int = Int.MAX_VALUE,
  content: @Composable (List<MediaItem>, LazyListState) -> Unit,
) {
  val listState = rememberLazyListState()
  val displayedItems = section?.items.orEmpty().take(maxItemCount)
  LoadMoreEffect(
    listState = listState,
    itemCount = displayedItems.size,
    canLoadMore = enablePagination && section?.canLoadMore == true,
    onLoadMore = onLoadMore,
  )

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    SectionHeader(
      title = title,
      subtitle = subtitle,
      actionLabel = stringResource(R.string.show_all),
      onActionClick = onShowAll,
    )
    when {
      displayedItems.isNotEmpty() -> {
        content(displayedItems, listState)
        section?.paginationError?.let {
          Text(
            text = stringResource(R.string.load_more_failed),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelMedium,
          )
        }
      }
      section?.isInitialLoading == true -> SectionLoadingPlaceholder(height = 204.dp)
      else -> SectionStatusMessage(
        error = section?.error,
        emptyMessage = emptyMessage,
        onRetry = onRetry,
      )
    }
  }
}

@Composable
private fun LoadMoreEffect(
  listState: LazyListState,
  itemCount: Int,
  canLoadMore: Boolean,
  onLoadMore: () -> Unit,
) {
  val shouldLoadMore by remember(listState, itemCount, canLoadMore) {
    derivedStateOf {
      canLoadMore &&
        itemCount > 0 &&
        (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >=
        itemCount - PAGINATION_PREFETCH_DISTANCE
    }
  }
  LaunchedEffect(listState, itemCount, canLoadMore) {
    snapshotFlow { shouldLoadMore }
      .distinctUntilChanged()
      .collect { shouldLoad ->
        if (shouldLoad) onLoadMore()
      }
  }
}

@Composable
private fun AutoSlidePagerEffect(
  pagerState: PagerState,
  intervalMillis: Long,
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

  LaunchedEffect(pagerState, pagerState.pageCount, intervalMillis, isResumed, isVisible) {
    if (
      !isResumed ||
      !isVisible ||
      pagerState.pageCount <= 1 ||
      !areSystemAnimationsEnabled()
    ) {
      return@LaunchedEffect
    }
    while (currentCoroutineContext().isActive) {
      delay(intervalMillis)
      if (!pagerState.isScrollInProgress && pagerState.pageCount > 1) {
        pagerState.animateScrollToPage(
          page = (pagerState.currentPage + 1) % pagerState.pageCount,
        )
      }
    }
  }
}

@Composable
private fun PagerIndicator(
  pageCount: Int,
  currentPage: Int,
  modifier: Modifier = Modifier,
) {
  if (pageCount <= 1) return

  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    repeat(pageCount) { page ->
      val selected = page == currentPage
      Box(
        modifier = Modifier
          .size(
            width = if (selected) 22.dp else 7.dp,
            height = 7.dp,
          )
          .clip(CircleShape)
          .background(
            if (selected) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
            },
          ),
      )
    }
  }
}

private fun areSystemAnimationsEnabled(): Boolean =
  Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()

private fun androidx.compose.foundation.lazy.LazyListScope.paginationTail(
  section: HomeSection,
) {
  if (section.isLoadingMore) {
    item(key = LazyLayoutKey.of("home-pagination", section.type.name)) {
      Box(
        modifier = Modifier
          .width(64.dp)
          .height(156.dp),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(24.dp),
          strokeWidth = 2.dp,
        )
      }
    }
  }
}

@Composable
internal fun SectionHeader(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  actionLabel: String? = null,
  onActionClick: () -> Unit = {},
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
      )
      subtitle?.let {
        Text(
          text = it,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    if (actionLabel != null) {
      TextButton(onClick = onActionClick) {
        Text(actionLabel)
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = null,
          modifier = Modifier
            .padding(start = 4.dp)
            .size(16.dp),
        )
      }
    }
  }
}

@Composable
private fun SectionLoadingPlaceholder(
  height: Dp,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(height),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    repeat(3) { index ->
      SkeletonBlock(
        modifier = Modifier
          .weight(if (index == 0) 1.35f else 1f)
          .fillMaxHeight(),
        height = height,
      )
    }
  }
}

@Composable
internal fun SectionStatusMessage(
  error: AppError?,
  emptyMessage: String,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val hasError = error != null
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = if (hasError) {
      MaterialTheme.colorScheme.errorContainer
    } else {
      MaterialTheme.colorScheme.surfaceContainer
    },
    contentColor = if (hasError) {
      MaterialTheme.colorScheme.onErrorContainer
    } else {
      MaterialTheme.colorScheme.onSurface
    },
    shape = MaterialTheme.shapes.large,
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = if (hasError) Icons.Outlined.CloudOff else Icons.Outlined.MovieFilter,
        contentDescription = null,
      )
      Text(
        text = if (error != null) {
          stringResource(error.messageResource())
        } else {
          emptyMessage
        },
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.bodyMedium,
      )
      if (hasError) {
        TextButton(onClick = onRetry) {
          Text(stringResource(R.string.retry))
        }
      }
    }
  }
}

@Composable
private fun MediaSupportingRow(
  item: MediaItem,
  modifier: Modifier = Modifier,
  contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = Icons.Default.Star,
      contentDescription = stringResource(R.string.rating),
      modifier = Modifier.size(14.dp),
      tint = MaterialTheme.colorScheme.tertiary,
    )
    Text(
      text = stringResource(R.string.rating_value, item.voteAverage),
      color = contentColor,
      style = MaterialTheme.typography.labelMedium,
    )
    Text(
      text = "·",
      color = contentColor,
      style = MaterialTheme.typography.labelMedium,
    )
    Text(
      text = stringResource(
        if (item.type == MediaType.MOVIE) R.string.media_type_movie
        else R.string.media_type_tv_show,
      ),
      color = contentColor,
      style = MaterialTheme.typography.labelMedium,
    )
  }
}

@Composable
private fun SkeletonBlock(
  height: Dp,
  modifier: Modifier = Modifier,
  width: Dp? = null,
) {
  Box(
    modifier = modifier
      .then(if (width != null) Modifier.width(width) else Modifier)
      .height(height)
      .clip(MaterialTheme.shapes.medium)
      .background(MaterialTheme.colorScheme.surfaceContainerHighest),
  )
}

private fun formatReleaseDate(
  rawDate: String?,
  locale: java.util.Locale,
  dateToBeAnnounced: String,
): ReleaseDateUi {
  val date = try {
    rawDate?.let {
      SimpleDateFormat("yyyy-MM-dd", locale)
        .apply { isLenient = false }
        .parse(it)
    }
  } catch (_: ParseException) {
    null
  }
  return if (date != null) {
    ReleaseDateUi(
      month = SimpleDateFormat("MMM", locale).format(date),
      day = SimpleDateFormat("d", locale).format(date),
      year = SimpleDateFormat("yyyy", locale).format(date),
    )
  } else {
    ReleaseDateUi(
      month = dateToBeAnnounced,
      day = "—",
      year = "",
    )
  }
}

private data class ReleaseDateUi(
  val month: String,
  val day: String,
  val year: String,
)

private const val PAGINATION_PREFETCH_DISTANCE = 4
private const val TOP_PICK_ITEM_LIMIT = 5
private const val SPORTS_DRAMA_GENRE_ID = 18
private const val ANIMATION_GENRE_ID = 16
private val SPORTS_TERMS = setOf("sport", "football", "basketball", "racing", "athlete")
private val WESTERN_LANGUAGES = setOf("en", "fr", "de", "es", "it", "pt")
private val LANGUAGE_REGION_NAMES = mapOf(
  "id" to "Indonesian cinema",
  "ko" to "Korean cinema",
  "ja" to "Japanese cinema",
  "zh" to "Chinese cinema",
  "hi" to "Indian cinema",
  "en" to "Global English-language cinema",
)
private const val HOME_TRENDING_LIMIT = 15
private const val SPOTLIGHT_LIMIT = 3
private const val MAXIMUM_HOME_COLLECTIONS = 10
private const val SPOTLIGHT_AUTO_SLIDE_MILLIS = 8_000L
private const val COLLECTION_AUTO_SLIDE_MILLIS = 10_000L
private const val TRENDING_BENTO_LEAD_COUNT = 3
private val TRENDING_EXPANDED_BREAKPOINT = 600.dp
private val TRENDING_FEATURED_IMAGE_HEIGHT_COMPACT = 152.dp
private val TRENDING_FEATURED_IMAGE_HEIGHT_EXPANDED = 184.dp
private val TRENDING_SECONDARY_IMAGE_HEIGHT_COMPACT = 88.dp
private val TRENDING_SECONDARY_IMAGE_WIDTH_EXPANDED = 128.dp
private val TRENDING_SECONDARY_CARD_HEIGHT_EXPANDED = 120.dp
private val TRENDING_COMPACT_CARD_WIDTH = 156.dp
private val TRENDING_COMPACT_IMAGE_HEIGHT = 84.dp
