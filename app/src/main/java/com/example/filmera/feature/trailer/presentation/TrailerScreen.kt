package com.example.filmera.feature.trailer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.filmera.R
import com.example.filmera.core.designsystem.theme.filmeraColors
import com.example.filmera.core.model.CastMember
import com.example.filmera.core.model.MediaDetails
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaVideo
import com.example.filmera.core.model.WatchProvider
import com.example.filmera.core.model.WatchProviderResult
import com.example.filmera.core.ui.LazyLayoutKey
import com.example.filmera.core.ui.LoadState
import com.example.filmera.core.ui.component.ErrorState
import com.example.filmera.core.ui.component.TmdbImage
import com.example.filmera.feature.detail.domain.DetailContent
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import java.util.Locale

@Composable
fun VideoDetailRoute(
  onBack: () -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  viewModel: VideoDetailViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  VideoDetailScreen(
    state = state,
    onAction = viewModel::onAction,
    onBack = onBack,
    onMediaClick = onMediaClick,
    onPersonClick = onPersonClick,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
  state: VideoDetailUiState,
  onAction: (VideoDetailAction) -> Unit,
  onBack: () -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = stringResource(R.string.video_detail_title),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = stringResource(R.string.navigate_back),
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          scrolledContainerColor = MaterialTheme.colorScheme.surface,
        ),
      )
    },
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      val isExpanded = maxWidth >= 840.dp
      when (val contentState = state.contentState) {
        LoadState.Loading -> VideoLoadingLayout(isExpanded = isExpanded)
        LoadState.Empty -> VideoEmptyLayout()
        is LoadState.Error -> ErrorState(
          error = contentState.error,
          onRetry = { onAction(VideoDetailAction.Retried) },
          modifier = Modifier.fillMaxSize(),
        )
        is LoadState.Success -> {
          val activeVideo = state.activeVideo
          if (activeVideo == null || activeVideo.key.isBlank()) {
            VideoEmptyLayout()
          } else {
            VideoSuccessLayout(
              state = state,
              content = contentState.value,
              activeVideo = activeVideo,
              isExpanded = isExpanded,
              onAction = onAction,
              onMediaClick = onMediaClick,
              onPersonClick = onPersonClick,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun VideoSuccessLayout(
  state: VideoDetailUiState,
  content: DetailContent,
  activeVideo: MediaVideo,
  isExpanded: Boolean,
  onAction: (VideoDetailAction) -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
) {
  if (isExpanded) {
    Row(modifier = Modifier.fillMaxSize()) {
      Column(
        modifier = Modifier
          .widthIn(min = 460.dp, max = 680.dp)
          .fillMaxHeight()
          .background(MaterialTheme.colorScheme.surfaceContainerLow),
      ) {
        FilmeraVideoPlayer(
          videoKey = activeVideo.key,
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        )
        PlayingNowSummary(
          activeVideo = activeVideo,
          details = content.details,
          modifier = Modifier.padding(20.dp),
        )
      }
      VideoInformationFeed(
        state = state,
        content = content,
        activeVideo = activeVideo,
        onAction = onAction,
        onMediaClick = onMediaClick,
        onPersonClick = onPersonClick,
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(24.dp),
      )
    }
  } else {
    Column(modifier = Modifier.fillMaxSize()) {
      FilmeraVideoPlayer(
        videoKey = activeVideo.key,
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 9f),
      )
      VideoInformationFeed(
        state = state,
        content = content,
        activeVideo = activeVideo,
        onAction = onAction,
        onMediaClick = onMediaClick,
        onPersonClick = onPersonClick,
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(
          start = 16.dp,
          top = 18.dp,
          end = 16.dp,
          bottom = 32.dp,
        ),
      )
    }
  }
}

@Composable
private fun FilmeraVideoPlayer(
  videoKey: String,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var isReady by remember(videoKey) { mutableStateOf(false) }
  val playerView = remember(context, videoKey) {
    YouTubePlayerView(context).apply {
      addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
        override fun onReady(youTubePlayer: YouTubePlayer) {
          isReady = true
          youTubePlayer.loadVideo(videoKey, 0f)
        }
      })
    }
  }

  DisposableEffect(lifecycleOwner, playerView) {
    lifecycleOwner.lifecycle.addObserver(playerView)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(playerView)
      playerView.release()
    }
  }

  Box(
    modifier = modifier
      .background(MaterialTheme.filmeraColors.imageScrim),
    contentAlignment = Alignment.Center,
  ) {
    AndroidView(
      factory = { playerView },
      modifier = Modifier.fillMaxSize(),
    )
    if (!isReady) {
      CircularProgressIndicator(
        modifier = Modifier.size(30.dp),
        color = MaterialTheme.filmeraColors.onImage,
        strokeWidth = 2.dp,
      )
    }
  }
}

@Composable
private fun VideoInformationFeed(
  state: VideoDetailUiState,
  content: DetailContent,
  activeVideo: MediaVideo,
  onAction: (VideoDetailAction) -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onPersonClick: (Int) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = contentPadding,
    verticalArrangement = Arrangement.spacedBy(28.dp),
  ) {
    item(key = "video-information") {
      VideoInformation(
        video = activeVideo,
        details = content.details,
        onOpenDetails = { onMediaClick(content.details.toMediaItem()) },
      )
    }
    item(key = "more-title-videos") {
      MoreTitleVideosSection(
        title = content.details.title,
        videos = content.videos,
        activeVideoKey = activeVideo.key,
        onVideoClick = { video ->
          onAction(VideoDetailAction.VideoSelected(video.key))
        },
      )
    }
    item(key = "watch-providers") {
      WatchProviderSection(
        providersByRegion = content.watchProviders,
        selectedRegionCode = state.selectedRegionCode,
        onRegionSelected = { regionCode ->
          onAction(VideoDetailAction.RegionSelected(regionCode))
        },
      )
    }
    if (content.credits.cast.isNotEmpty()) {
      item(key = "featured-cast") {
        FeaturedCastSection(
          cast = content.credits.cast.take(MAX_FEATURED_CAST),
          onPersonClick = onPersonClick,
        )
      }
    }
    if (content.recommendations.isNotEmpty()) {
      item(key = "similar-titles") {
        SimilarTitlesSection(
          items = content.recommendations,
          onMediaClick = onMediaClick,
        )
      }
    }
    item(key = "promotional-video-notice") {
      PromotionalVideoNotice()
    }
  }
}

@Composable
private fun PlayingNowSummary(
  activeVideo: MediaVideo,
  details: MediaDetails,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(7.dp),
  ) {
    Text(
      text = activeVideo.type.ifBlank { stringResource(R.string.video_type_video) }
        .uppercase(Locale.ENGLISH),
      color = MaterialTheme.colorScheme.primary,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = activeVideo.name.ifBlank { details.title },
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = details.title,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodyMedium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun VideoInformation(
  video: MediaVideo,
  details: MediaDetails,
  onOpenDetails: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        color = if (video.isOfficial) {
          MaterialTheme.colorScheme.primaryContainer
        } else {
          MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (video.isOfficial) {
          MaterialTheme.colorScheme.onPrimaryContainer
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = MaterialTheme.shapes.small,
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
          horizontalArrangement = Arrangement.spacedBy(5.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (video.isOfficial) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
            )
          }
          Text(
            text = stringResource(
              if (video.isOfficial) R.string.video_official
              else R.string.video_unofficial,
            ),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
          )
        }
      }
      Text(
        text = video.type.ifBlank { stringResource(R.string.video_type_video) },
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
      )
    }
    Text(
      text = video.name.ifBlank { details.title },
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = buildVideoMetadata(details, video),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodyMedium,
    )
    FilledTonalButton(onClick = onOpenDetails) {
      Icon(
        imageVector = Icons.Outlined.Movie,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(Modifier.width(8.dp))
      Text(stringResource(R.string.video_open_media_details))
    }
  }
}

@Composable
private fun MoreTitleVideosSection(
  title: String,
  videos: List<MediaVideo>,
  activeVideoKey: String,
  onVideoClick: (MediaVideo) -> Unit,
) {
  val availableTypes = remember(videos) {
    videos
      .map(MediaVideo::type)
      .filter(String::isNotBlank)
      .distinct()
  }
  var selectedCategory by rememberSaveable(videos.map(MediaVideo::key)) {
    mutableIntStateOf(0)
  }
  val filteredVideos = remember(videos, availableTypes, selectedCategory) {
    availableTypes
      .getOrNull(selectedCategory - 1)
      ?.let { selectedType ->
        videos.filter { it.type.equals(selectedType, ignoreCase = true) }
      }
      ?: videos
  }

  Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
    Text(
      text = stringResource(R.string.video_more_from, title),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
    )
    if (availableTypes.size > 1) {
      val categoryLabels = listOf(stringResource(R.string.browse_all)) + availableTypes
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        categoryLabels.forEachIndexed { index, label ->
          AssistChip(
            onClick = { selectedCategory = index },
            label = { Text(label) },
            leadingIcon = if (selectedCategory == index) {
              {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              }
            } else {
              null
            },
          )
        }
      }
    }
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(end = 8.dp),
    ) {
      items(
        items = filteredVideos.take(MAX_TITLE_VIDEOS),
        key = { video -> LazyLayoutKey.of("video-title-item", video.key) },
      ) { video ->
        VideoLandscapeCard(
          video = video,
          isActive = video.key == activeVideoKey,
          onClick = { onVideoClick(video) },
        )
      }
    }
  }
}

@Composable
private fun VideoLandscapeCard(
  video: MediaVideo,
  isActive: Boolean,
  onClick: () -> Unit,
) {
  Column(
    modifier = Modifier
      .width(244.dp)
      .clickable(onClick = onClick),
    verticalArrangement = Arrangement.spacedBy(7.dp),
  ) {
    Card(
      colors = CardDefaults.cardColors(
        containerColor = if (isActive) {
          MaterialTheme.colorScheme.primaryContainer
        } else {
          MaterialTheme.colorScheme.surfaceContainer
        },
      ),
      shape = MaterialTheme.shapes.large,
    ) {
      Box {
        TmdbImage(
          path = video.youtubeThumbnailUrl(),
          contentDescription = stringResource(R.string.video_thumbnail_description, video.name),
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        )
        Surface(
          modifier = Modifier
            .align(Alignment.Center)
            .size(42.dp),
          shape = CircleShape,
          color = MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.72f),
          contentColor = MaterialTheme.filmeraColors.onImage,
        ) {
          Icon(
            imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.padding(10.dp),
          )
        }
      }
    }
    Text(
      text = video.name,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = video.type.ifBlank { stringResource(R.string.video_type_video) },
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.labelMedium,
    )
  }
}

@Composable
private fun WatchProviderSection(
  providersByRegion: Map<String, WatchProviderResult>,
  selectedRegionCode: String,
  onRegionSelected: (String) -> Unit,
) {
  val context = LocalContext.current
  val availableRegions = remember(providersByRegion) {
    (listOf(VideoDetailUiState.DEFAULT_WATCH_REGION) + providersByRegion.keys)
      .distinct()
      .sorted()
  }
  val activeProviders = providersByRegion[selectedRegionCode]
  var regionMenuExpanded by remember { mutableStateOf(false) }
  val openProviderLink: (String) -> Unit = { link ->
    runCatching {
      context.startActivity(
          android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            link.toUri(),
          ),
      )
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(
          text = stringResource(R.string.where_to_watch),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = stringResource(
            R.string.video_available_in,
            selectedRegionCode.regionDisplayName(),
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall,
        )
      }
      Box {
        TextButton(onClick = { regionMenuExpanded = true }) {
          Text(selectedRegionCode.regionDisplayName())
        }
        DropdownMenu(
          expanded = regionMenuExpanded,
          onDismissRequest = { regionMenuExpanded = false },
        ) {
          availableRegions.forEach { regionCode ->
            DropdownMenuItem(
              text = { Text(regionCode.regionDisplayName()) },
              onClick = {
                regionMenuExpanded = false
                onRegionSelected(regionCode)
              },
            )
          }
        }
      }
    }

    if (activeProviders == null || activeProviders.isEmpty) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.Top,
        ) {
          Icon(
            imageVector = Icons.Outlined.CollectionsBookmark,
            contentDescription = null,
          )
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = stringResource(
                R.string.provider_not_available,
                selectedRegionCode.regionDisplayName(),
              ),
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = stringResource(R.string.provider_not_available_message),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      }
    } else {
      ProviderCategory(
        label = stringResource(R.string.provider_stream),
        providers = activeProviders.stream,
        onProviderClick = activeProviders.link?.let { link ->
          { openProviderLink(link) }
        },
      )
      ProviderCategory(
        label = stringResource(R.string.provider_free),
        providers = activeProviders.free,
        onProviderClick = activeProviders.link?.let { link ->
          { openProviderLink(link) }
        },
      )
      ProviderCategory(
        label = stringResource(R.string.provider_ads),
        providers = activeProviders.ads,
        onProviderClick = activeProviders.link?.let { link ->
          { openProviderLink(link) }
        },
      )
      ProviderCategory(
        label = stringResource(R.string.provider_rent),
        providers = activeProviders.rent,
        onProviderClick = activeProviders.link?.let { link ->
          { openProviderLink(link) }
        },
      )
      ProviderCategory(
        label = stringResource(R.string.provider_buy),
        providers = activeProviders.buy,
        onProviderClick = activeProviders.link?.let { link ->
          { openProviderLink(link) }
        },
      )
      activeProviders.link?.let { link ->
        TextButton(
          onClick = { openProviderLink(link) },
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
          )
          Spacer(Modifier.width(7.dp))
          Text(stringResource(R.string.provider_view_options))
        }
      }
    }
    Text(
      text = stringResource(R.string.justwatch_attribution),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.labelSmall,
    )
  }
}

@Composable
private fun ProviderCategory(
  label: String,
  providers: List<WatchProvider>,
  onProviderClick: (() -> Unit)?,
) {
  if (providers.isEmpty()) return

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      items(
        items = providers,
        key = { provider -> LazyLayoutKey.identified("watch-provider", provider.id) },
      ) { provider ->
        Column(
          modifier = Modifier
            .width(76.dp)
            .then(
              if (onProviderClick != null) {
                Modifier.clickable(onClick = onProviderClick)
              } else {
                Modifier
              },
            ),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          TmdbImage(
            path = provider.logoPath,
            contentDescription = provider.name,
            modifier = Modifier
              .size(58.dp)
              .clip(MaterialTheme.shapes.medium),
            size = "w92",
          )
          Text(
            text = provider.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun FeaturedCastSection(
  cast: List<CastMember>,
  onPersonClick: (Int) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
      text = stringResource(R.string.video_featured_cast),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      items(
        items = cast,
        key = { person -> LazyLayoutKey.identified("video-cast", person.id) },
      ) { person ->
        Column(
          modifier = Modifier
            .width(104.dp)
            .clickable { onPersonClick(person.id) },
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          TmdbImage(
            path = person.profilePath,
            contentDescription = stringResource(R.string.profile_image_description, person.name),
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(3f / 4f)
              .clip(MaterialTheme.shapes.large),
            size = "w185",
          )
          Text(
            text = person.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = person.character,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun SimilarTitlesSection(
  items: List<MediaItem>,
  onMediaClick: (MediaItem) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
      text = stringResource(R.string.video_more_like_this),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      items(
        items = items.take(MAX_SIMILAR_TITLES),
        key = { item -> LazyLayoutKey.media("video-similar", item) },
      ) { item ->
        Column(
          modifier = Modifier
            .width(126.dp)
            .clickable { onMediaClick(item) },
          verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          TmdbImage(
            path = item.posterPath ?: item.backdropPath,
            contentDescription = stringResource(R.string.poster_content_description, item.title),
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(2f / 3f)
              .clip(MaterialTheme.shapes.large),
            size = "w342",
          )
          Text(
            text = item.title,
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

@Composable
private fun PromotionalVideoNotice() {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    shape = MaterialTheme.shapes.large,
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Icon(
        imageVector = Icons.Outlined.Info,
        contentDescription = null,
        modifier = Modifier.size(19.dp),
      )
      Text(
        text = stringResource(R.string.video_promotional_notice),
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}

@Composable
private fun VideoLoadingLayout(
  isExpanded: Boolean,
) {
  if (isExpanded) {
    Row(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier = Modifier
          .width(620.dp)
          .aspectRatio(16f / 9f)
          .background(MaterialTheme.filmeraColors.imageScrim),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(color = MaterialTheme.filmeraColors.onImage)
      }
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight(),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator()
      }
    }
  } else {
    Column(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 9f)
          .background(MaterialTheme.filmeraColors.imageScrim),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(color = MaterialTheme.filmeraColors.onImage)
      }
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator()
      }
    }
  }
}

@Composable
private fun VideoEmptyLayout() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
  ) {
    Icon(
      imageVector = Icons.Outlined.Movie,
      contentDescription = null,
      modifier = Modifier.size(38.dp),
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = stringResource(R.string.video_empty_title),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = stringResource(R.string.video_empty_message),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodyMedium,
    )
  }
}

private fun buildVideoMetadata(
  details: MediaDetails,
  video: MediaVideo,
): String = buildList {
  add(details.title)
  details.releaseDate
    ?.take(4)
    ?.takeIf { it.all(Char::isDigit) }
    ?.let(::add)
  video.languageCode
    ?.languageDisplayName()
    ?.let(::add)
  add(video.site.ifBlank { "YouTube" })
}.joinToString(" • ")

private fun String.languageDisplayName(): String? {
  val language = Locale.forLanguageTag(this).getDisplayLanguage(Locale.ENGLISH)
  return language.takeIf {
    it.isNotBlank() && !it.equals(this, ignoreCase = true)
  }
}

private fun String.regionDisplayName(): String {
  val displayName = Locale.Builder()
    .setRegion(uppercase(Locale.ENGLISH))
    .build()
    .getDisplayCountry(Locale.ENGLISH)
  return displayName.takeIf(String::isNotBlank) ?: uppercase(Locale.ENGLISH)
}

private fun MediaVideo.youtubeThumbnailUrl(): String =
  "https://img.youtube.com/vi/$key/hqdefault.jpg"

private const val MAX_TITLE_VIDEOS = 10
private const val MAX_FEATURED_CAST = 8
private const val MAX_SIMILAR_TITLES = 12
