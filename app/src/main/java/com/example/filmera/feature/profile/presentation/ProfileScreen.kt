package com.example.filmera.feature.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocalMovies
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.filmera.BuildConfig
import com.example.filmera.R
import com.example.filmera.core.common.AppError
import com.example.filmera.core.designsystem.theme.FilmeraTheme
import com.example.filmera.core.designsystem.theme.filmeraColors
import com.example.filmera.core.navigation.BottomNavigationBar
import com.example.filmera.core.navigation.defaultBottomNavigationDestinations
import com.example.filmera.core.ui.LoadState
import com.example.filmera.core.ui.component.TmdbImage
import com.example.filmera.feature.profile.domain.UserProfile

@Composable
fun ProfileRoute(
  navController: NavHostController,
  onOpenLibrary: () -> Unit,
  onOpenPreferences: () -> Unit,
  viewModel: ProfileViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  ProfileScreen(
    state = state,
    onOpenLibrary = onOpenLibrary,
    onOpenPreferences = onOpenPreferences,
    bottomBar = {
      BottomNavigationBar(
        navController = navController,
        destinations = defaultBottomNavigationDestinations,
      )
    },
  )
}

@Composable
fun ProfileScreen(
  state: ProfileUiState,
  onOpenLibrary: () -> Unit,
  onOpenPreferences: () -> Unit,
  modifier: Modifier = Modifier,
  bottomBar: @Composable () -> Unit = {},
) {
  Scaffold(
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    bottomBar = bottomBar,
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier
          .widthIn(max = 900.dp)
          .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        item(key = "profile-hero") {
          ProfileHero(
            identityState = state.identityState,
            posters = state.libraryState.headerPosters(),
            onOpenSettings = onOpenPreferences,
          )
        }
        item(key = "profile-statistics") {
          ProfileLibraryOverview(
            state = state.libraryState,
            onOpenLibrary = onOpenLibrary,
            modifier = Modifier.padding(horizontal = 16.dp),
          )
        }
        item(key = "profile-space-title") {
          SectionTitle(
            title = stringResource(R.string.profile_space_section),
            modifier = Modifier.padding(horizontal = 20.dp),
          )
        }
        item(key = "profile-library") {
          ProfileNavigationCard(
            icon = Icons.Default.VideoLibrary,
            title = stringResource(R.string.profile_library_title),
            message = stringResource(R.string.profile_library_message),
            onClick = onOpenLibrary,
            modifier = Modifier.padding(horizontal = 16.dp),
          )
        }
        item(key = "profile-content-preferences") {
          ProfileNavigationCard(
            icon = Icons.Outlined.Tune,
            title = stringResource(R.string.profile_preferences_title),
            message = stringResource(R.string.profile_preferences_message),
            onClick = onOpenPreferences,
            modifier = Modifier.padding(horizontal = 16.dp),
          )
        }
        item(key = "profile-about-title") {
          SectionTitle(
            title = stringResource(R.string.profile_about_section),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
          )
        }
        item(key = "profile-about-list") {
          ProfileAboutCard(
            modifier = Modifier.padding(horizontal = 16.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun ProfileHero(
  identityState: LoadState<UserProfile>,
  posters: List<ProfileHeaderPoster>,
  onOpenSettings: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(388.dp)
      .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
      .background(MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    ProfilePosterBackdrop(posters = posters)

    Box(
      modifier = Modifier
        .matchParentSize()
        .background(
          Brush.verticalGradient(
            colorStops = arrayOf(
              0f to MaterialTheme.filmeraColors.imageScrim.copy(alpha = 0.34f),
              0.42f to MaterialTheme.colorScheme.background.copy(alpha = 0.62f),
              0.72f to MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
              1f to MaterialTheme.colorScheme.background,
            ),
          ),
        ),
    )
    Box(
      modifier = Modifier
        .matchParentSize()
        .background(
          Brush.horizontalGradient(
            listOf(
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.26f),
              Color.Transparent,
              MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
            ),
          ),
        ),
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(
            text = stringResource(R.string.navigation_profile).uppercase(),
            color = MaterialTheme.filmeraColors.onImage,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
          )
          Text(
            text = stringResource(R.string.profile_journey_tagline).uppercase(),
            color = MaterialTheme.filmeraColors.onImage.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelMedium,
          )
          Spacer(
            modifier = Modifier
              .padding(top = 8.dp)
              .width(34.dp)
              .height(3.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary),
          )
        }
        Surface(
          onClick = onOpenSettings,
          modifier = Modifier.size(48.dp),
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
          border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
          ),
        ) {
          Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = stringResource(R.string.profile_open_settings),
            modifier = Modifier.padding(12.dp),
            tint = MaterialTheme.filmeraColors.onImage,
          )
        }
      }

      Spacer(modifier = Modifier.weight(1f))
      ProfileIdentity(identityState = identityState)
    }
  }
}

@Composable
private fun ProfilePosterBackdrop(posters: List<ProfileHeaderPoster>) {
  if (posters.isEmpty()) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(270.dp)
        .background(
          Brush.linearGradient(
            listOf(
              MaterialTheme.colorScheme.primaryContainer,
              MaterialTheme.colorScheme.surfaceContainerHigh,
              MaterialTheme.colorScheme.background,
            ),
          ),
        ),
      contentAlignment = Alignment.CenterEnd,
    ) {
      Icon(
        imageVector = Icons.Outlined.LocalMovies,
        contentDescription = null,
        modifier = Modifier
          .padding(end = 28.dp)
          .size(112.dp)
          .alpha(0.12f),
        tint = MaterialTheme.colorScheme.primary,
      )
    }
    return
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(282.dp),
  ) {
    posters.forEachIndexed { index, poster ->
      TmdbImage(
        path = poster.path,
        contentDescription = null,
        size = "w342",
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .padding(end = if (index == posters.lastIndex) 0.dp else 1.dp),
      )
    }
  }
}

@Composable
private fun ProfileIdentity(identityState: LoadState<UserProfile>) {
  val profile = (identityState as? LoadState.Success)?.value
  val displayName = profile?.displayName
    ?.takeIf(String::isNotBlank)
    ?: stringResource(R.string.profile_guest_title)
  val username = profile?.username
    ?.takeIf(String::isNotBlank)
    ?.let { "@$it" }
  val bio = profile?.bio
    ?.takeIf(String::isNotBlank)
    ?: stringResource(R.string.profile_identity_message)
  val initials = displayName
    .split(' ')
    .asSequence()
    .filter(String::isNotBlank)
    .take(2)
    .mapNotNull(String::firstOrNull)
    .joinToString("")
    .uppercase()
    .ifBlank { "F" }

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(18.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      modifier = Modifier.size(94.dp),
      shape = CircleShape,
      color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
      border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
      shadowElevation = 8.dp,
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(
          text = initials,
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      if (identityState == LoadState.Loading) {
        CircularProgressIndicator(
          modifier = Modifier.size(22.dp),
          strokeWidth = 2.dp,
          color = MaterialTheme.colorScheme.primary,
        )
      }
      Text(
        text = displayName,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      username?.let {
        Text(
          text = it,
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
        text = bio,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f),
        border = BorderStroke(
          1.dp,
          MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
        ),
      ) {
        Text(
          text = stringResource(R.string.profile_member_badge),
          modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          style = MaterialTheme.typography.labelMedium,
        )
      }
    }
  }
}

@Composable
private fun ProfileLibraryOverview(
  state: LoadState<ProfileLibraryContent>,
  onOpenLibrary: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .clickable(
        role = Role.Button,
        onClickLabel = stringResource(R.string.profile_open_library),
        onClick = onOpenLibrary,
      ),
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.82f),
    ),
  ) {
    when (state) {
      LoadState.Loading -> ProfileStatsLoading()
      LoadState.Empty -> ProfileStats(ProfileLibrarySummary())
      is LoadState.Error -> Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = stringResource(R.string.profile_library_error),
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodyMedium,
        )
        Text(
          text = stringResource(R.string.profile_open_library),
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.labelLarge,
        )
      }
      is LoadState.Success -> ProfileStats(state.value.summary)
    }
  }
}

@Composable
private fun ProfileStats(summary: ProfileLibrarySummary) {
  BoxWithConstraints(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 18.dp),
  ) {
    val stats = listOf(
      ProfileStatData(summary.savedCount, R.string.profile_saved, Icons.Default.VideoLibrary),
      ProfileStatData(summary.favoriteCount, R.string.profile_favorites, Icons.Default.Favorite),
      ProfileStatData(summary.watchlistCount, R.string.profile_watchlist, Icons.Default.Bookmarks),
      ProfileStatData(summary.watchingCount, R.string.profile_watching, Icons.Default.PlayCircle),
    )
    if (maxWidth >= 360.dp) {
      Row(modifier = Modifier.fillMaxWidth()) {
        stats.forEachIndexed { index, stat ->
          ProfileStat(
            data = stat,
            modifier = Modifier.weight(1f),
          )
          if (index != stats.lastIndex) {
            VerticalDivider(
              modifier = Modifier
                .height(70.dp),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
            )
          }
        }
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        stats.chunked(2).forEach { rowStats ->
          Row(modifier = Modifier.fillMaxWidth()) {
            rowStats.forEach { stat ->
              ProfileStat(data = stat, modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ProfileStat(
  data: ProfileStatData,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Icon(
      imageVector = data.icon,
      contentDescription = null,
      modifier = Modifier.size(25.dp),
      tint = MaterialTheme.colorScheme.primary,
    )
    Text(
      text = data.value.toString(),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = stringResource(data.labelRes),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.labelSmall,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun ProfileStatsLoading() {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 24.dp, horizontal = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    repeat(4) {
      Spacer(
        modifier = Modifier
          .weight(1f)
          .height(62.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHighest),
      )
    }
  }
}

@Composable
private fun SectionTitle(
  title: String,
  modifier: Modifier = Modifier,
) {
  Text(
    text = title,
    modifier = modifier.semantics { heading() },
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.SemiBold,
  )
}

@Composable
private fun ProfileNavigationCard(
  icon: ImageVector,
  title: String,
  message: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    border = BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    ),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        modifier = Modifier.size(44.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.padding(10.dp),
          tint = MaterialTheme.colorScheme.primary,
        )
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = message,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Icon(
        imageVector = Icons.Outlined.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ProfileAboutCard(modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    border = BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    ),
  ) {
    ProfileInformationRow(
      icon = Icons.Outlined.Info,
      title = stringResource(R.string.about_app_title),
      message = stringResource(R.string.about_app_message),
      supportingText = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
    )
    HorizontalDivider(
      modifier = Modifier.padding(start = 70.dp),
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
    )
    ProfileInformationRow(
      icon = Icons.Outlined.Storage,
      title = stringResource(R.string.about_data_title),
      message = stringResource(R.string.about_data_message),
    )
    HorizontalDivider(
      modifier = Modifier.padding(start = 70.dp),
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
    )
    ProfileInformationRow(
      icon = Icons.Outlined.Lock,
      title = stringResource(R.string.about_privacy_title),
      message = stringResource(R.string.about_privacy_message),
    )
  }
}

@Composable
private fun ProfileInformationRow(
  icon: ImageVector,
  title: String,
  message: String,
  supportingText: String? = null,
) {
  Row(
    modifier = Modifier.padding(18.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(24.dp),
      tint = MaterialTheme.colorScheme.primary,
    )
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        supportingText?.let {
          Text(
            text = it,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelSmall,
          )
        }
      }
      Text(
        text = message,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}

private fun LoadState<ProfileLibraryContent>.headerPosters(): List<ProfileHeaderPoster> =
  (this as? LoadState.Success)?.value?.headerPosters.orEmpty()

private data class ProfileStatData(
  val value: Int,
  val labelRes: Int,
  val icon: ImageVector,
)

@Preview(showBackground = true, widthDp = 412, heightDp = 1100)
@Composable
private fun ProfileScreenPreview() {
  FilmeraTheme {
    ProfileScreen(
      state = ProfileUiState(
        identityState = LoadState.Success(
          UserProfile(
            displayName = "Raka Wijaya",
            username = "rakawijaya",
            avatarUrl = null,
            bio = "Movies are more than stories, they are experiences.",
          ),
        ),
        libraryState = LoadState.Success(
          ProfileLibraryContent(
            summary = ProfileLibrarySummary(
              savedCount = 56,
              watchlistCount = 24,
              watchingCount = 7,
              favoriteCount = 12,
            ),
            headerPosters = listOf(
              ProfileHeaderPoster(
                path = "/6FfCtAuVAW8XJjZ7eWeLibRLWTw.jpg",
                title = "Star Wars",
              ),
              ProfileHeaderPoster(
                path = "/1E5baAaEse26fej7uHcjOgEE2t2.jpg",
                title = "Movie poster",
              ),
            ),
          ),
        ),
      ),
      onOpenLibrary = {},
      onOpenPreferences = {},
    )
  }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 900)
@Composable
private fun ProfileLoadingPreview() {
  FilmeraTheme {
    ProfileScreen(
      state = ProfileUiState(),
      onOpenLibrary = {},
      onOpenPreferences = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun ProfileErrorPreview() {
  FilmeraTheme {
    ProfileLibraryOverview(
      state = LoadState.Error(AppError.Unknown),
      onOpenLibrary = {},
    )
  }
}
