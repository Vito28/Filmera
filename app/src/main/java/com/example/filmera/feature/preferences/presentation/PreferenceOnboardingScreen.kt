package com.example.filmera.feature.preferences.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.LocalMovies
import androidx.compose.material.icons.outlined.LocalPolice
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.filmera.R
import com.example.filmera.core.designsystem.theme.FilmeraTheme
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.ui.LoadState
import com.example.filmera.core.ui.component.TmdbImage
import com.example.filmera.feature.discover.domain.OnboardingChoices
import com.example.filmera.feature.discover.domain.RecommendedPerson
import com.example.filmera.feature.preferences.domain.MediaPreference
import com.example.filmera.feature.preferences.domain.PreferenceCountry
import com.example.filmera.feature.preferences.domain.PreferenceGenre
import com.example.filmera.feature.preferences.domain.UserPreferenceProfile
import com.example.filmera.feature.preferences.domain.supportedPreferenceCountries
import com.example.filmera.feature.preferences.domain.supportedPreferenceGenres

@Composable
fun PreferenceOnboardingRoute(
  onCompleted: () -> Unit,
  onNavigateBack: () -> Unit,
  viewModel: PreferenceOnboardingViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(viewModel) {
    viewModel.completed.collect { onCompleted() }
  }

  PreferenceOnboardingScreen(
    state = state,
    onAction = viewModel::onAction,
    onNavigateBack = onNavigateBack,
  )
}

@Composable
fun PreferenceOnboardingScreen(
  state: PreferenceOnboardingUiState,
  onAction: (PreferenceOnboardingAction) -> Unit,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showResetConfirmation by remember { mutableStateOf(false) }
  val canMoveBack = state.step != PreferenceStep.CONTENT_TYPES

  BackHandler {
    when {
      canMoveBack -> onAction(PreferenceOnboardingAction.BackClicked)
      state.isEditing -> onNavigateBack()
    }
  }

  Scaffold(
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      PreferenceHeader(
        state = state,
        canMoveBack = canMoveBack,
        onBack = { onAction(PreferenceOnboardingAction.BackClicked) },
        onClose = onNavigateBack,
        onReset = { showResetConfirmation = true },
      )
      if (state.step != PreferenceStep.COMPLETE) {
        OnboardingProgress(
          currentStep = state.stepNumber,
          totalSteps = PreferenceOnboardingUiState.TOTAL_QUESTION_STEPS,
          modifier = Modifier.padding(horizontal = 20.dp),
        )
      }
      PreferenceQuestion(
        step = state.step,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp),
      )
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
      ) {
        PreferenceStepContent(
          state = state,
          onAction = onAction,
        )
      }
      PreferenceFooter(
        state = state,
        onAction = onAction,
      )
    }
  }

  if (showResetConfirmation) {
    AlertDialog(
      onDismissRequest = { showResetConfirmation = false },
      title = { Text(stringResource(R.string.preferences_reset_title)) },
      text = { Text(stringResource(R.string.preferences_reset_message)) },
      confirmButton = {
        TextButton(
          onClick = {
            showResetConfirmation = false
            onAction(PreferenceOnboardingAction.ResetConfirmed)
          },
        ) {
          Text(stringResource(R.string.preferences_reset_confirm))
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetConfirmation = false }) {
          Text(stringResource(R.string.cancel))
        }
      },
    )
  }
}

@Composable
private fun PreferenceHeader(
  state: PreferenceOnboardingUiState,
  canMoveBack: Boolean,
  onBack: () -> Unit,
  onClose: () -> Unit,
  onReset: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (canMoveBack) {
      IconButton(onClick = onBack) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.preferences_back),
        )
      }
    } else if (state.isEditing) {
      IconButton(onClick = onClose) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.navigate_back),
        )
      }
    } else {
      Spacer(modifier = Modifier.size(48.dp))
    }
    Text(
      text = stringResource(
        if (state.isEditing) {
          R.string.preferences_edit_title
        } else {
          R.string.app_name
        },
      ),
      modifier = Modifier.weight(1f),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
    )
    if (state.isEditing) {
      TextButton(onClick = onReset) {
        Text(stringResource(R.string.preferences_reset_short))
      }
    } else {
      Spacer(modifier = Modifier.size(48.dp))
    }
  }
}

@Composable
private fun OnboardingProgress(
  currentStep: Int,
  totalSteps: Int,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = stringResource(
        R.string.preferences_progress,
        currentStep,
        totalSteps,
      ),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.labelMedium,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      repeat(totalSteps) { index ->
        Spacer(
          modifier = Modifier
            .weight(1f)
            .height(4.dp)
            .clip(CircleShape)
            .background(
              if (index < currentStep) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
              },
            ),
        )
      }
    }
  }
}

@Composable
private fun PreferenceQuestion(
  step: PreferenceStep,
  modifier: Modifier = Modifier,
) {
  val titleResource: Int
  val subtitleResource: Int
  when (step) {
    PreferenceStep.CONTENT_TYPES -> {
      titleResource = R.string.preferences_content_question
      subtitleResource = R.string.preferences_content_subtitle
    }
    PreferenceStep.GENRES -> {
      titleResource = R.string.preferences_genre_question
      subtitleResource = R.string.preferences_genre_subtitle
    }
    PreferenceStep.COUNTRIES -> {
      titleResource = R.string.preferences_country_question
      subtitleResource = R.string.preferences_country_subtitle
    }
    PreferenceStep.TITLES -> {
      titleResource = R.string.preferences_titles_question
      subtitleResource = R.string.preferences_titles_subtitle
    }
    PreferenceStep.PEOPLE -> {
      titleResource = R.string.preferences_people_question
      subtitleResource = R.string.preferences_people_subtitle
    }
    PreferenceStep.COMPLETE -> {
      titleResource = R.string.preferences_complete_title
      subtitleResource = R.string.preferences_complete_subtitle
    }
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = if (step == PreferenceStep.COMPLETE) {
      Alignment.CenterHorizontally
    } else {
      Alignment.Start
    },
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = stringResource(titleResource),
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      textAlign = if (step == PreferenceStep.COMPLETE) TextAlign.Center else TextAlign.Start,
    )
    Text(
      text = stringResource(subtitleResource),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodyLarge,
      textAlign = if (step == PreferenceStep.COMPLETE) TextAlign.Center else TextAlign.Start,
    )
  }
}

@Composable
private fun PreferenceStepContent(
  state: PreferenceOnboardingUiState,
  onAction: (PreferenceOnboardingAction) -> Unit,
) {
  val artworkTitles = (state.choicesState as? LoadState.Success)
    ?.value
    ?.titles
    .orEmpty()
  when (state.step) {
    PreferenceStep.CONTENT_TYPES -> ContentTypeGrid(
      selected = state.selectedMediaTypes,
      artworkTitles = artworkTitles,
      onToggle = {
        onAction(PreferenceOnboardingAction.MediaTypeToggled(it))
      },
    )
    PreferenceStep.GENRES -> GenreGrid(
      selected = state.selectedGenreIds,
      artworkTitles = artworkTitles,
      onToggle = { onAction(PreferenceOnboardingAction.GenreToggled(it)) },
    )
    PreferenceStep.COUNTRIES -> CountryGrid(
      selected = state.selectedCountries,
      artworkTitles = artworkTitles,
      onToggle = { onAction(PreferenceOnboardingAction.CountryToggled(it)) },
    )
    PreferenceStep.TITLES -> ChoicesContent(
      choicesState = state.choicesState,
      emptyMessage = stringResource(R.string.preferences_choices_error),
      onRetry = { onAction(PreferenceOnboardingAction.RetryChoicesClicked) },
    ) { choices ->
      TitleGrid(
        titles = choices.titles,
        selected = state.selectedTitles.keys,
        onToggle = { onAction(PreferenceOnboardingAction.TitleToggled(it)) },
      )
    }
    PreferenceStep.PEOPLE -> ChoicesContent(
      choicesState = state.choicesState,
      emptyMessage = stringResource(R.string.preferences_choices_error),
      onRetry = { onAction(PreferenceOnboardingAction.RetryChoicesClicked) },
    ) { choices ->
      PersonGrid(
        people = choices.people,
        selected = state.selectedPersonIds,
        onToggle = { onAction(PreferenceOnboardingAction.PersonToggled(it)) },
      )
    }
    PreferenceStep.COMPLETE -> CompletionArtwork()
  }
}

@Composable
private fun ContentTypeGrid(
  selected: Set<MediaPreference>,
  artworkTitles: List<MediaItem>,
  onToggle: (MediaPreference) -> Unit,
) {
  val options = listOf(
    Triple(MediaPreference.MOVIE, R.string.preferences_content_movie, Icons.Outlined.LocalMovies),
    Triple(MediaPreference.TV_SERIES, R.string.preferences_content_series, Icons.Outlined.LiveTv),
    Triple(MediaPreference.ANIME, R.string.preferences_content_anime, Icons.Outlined.Animation),
    Triple(
      MediaPreference.DOCUMENTARY,
      R.string.preferences_content_documentary,
      Icons.Outlined.PhotoCamera,
    ),
    Triple(
      MediaPreference.ANIMATION,
      R.string.preferences_content_animation,
      Icons.Outlined.Animation,
    ),
  )
  LazyVerticalGrid(
    columns = GridCells.Adaptive(150.dp),
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(20.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(
      items = options,
      key = { it.first.name },
    ) { (type, label, icon) ->
      CinematicSelectionCard(
        title = stringResource(label),
        icon = icon,
        artworkPath = artworkTitles
          .artworkForContentType(type, type.ordinal)
          ?.artworkPath,
        selected = type in selected,
        onClick = { onToggle(type) },
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1.18f),
      )
    }
  }
}

@Composable
private fun GenreGrid(
  selected: Set<Int>,
  artworkTitles: List<MediaItem>,
  onToggle: (Int) -> Unit,
) {
  LazyVerticalGrid(
    columns = GridCells.Adaptive(130.dp),
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(20.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    items(
      items = supportedPreferenceGenres,
      key = PreferenceGenre::id,
    ) { genre ->
      CinematicSelectionCard(
        title = genre.name,
        icon = genreIcon(genre.id),
        artworkPath = artworkTitles
          .artworkForGenre(
            genreId = genre.id,
            fallbackIndex = supportedPreferenceGenres.indexOf(genre),
          )
          ?.artworkPath,
        selected = genre.id in selected,
        onClick = { onToggle(genre.id) },
        modifier = Modifier
          .fillMaxWidth()
          .height(108.dp),
      )
    }
  }
}

@Composable
private fun CountryGrid(
  selected: Set<String>,
  artworkTitles: List<MediaItem>,
  onToggle: (String) -> Unit,
) {
  LazyVerticalGrid(
    columns = GridCells.Adaptive(190.dp),
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(20.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(
      items = supportedPreferenceCountries,
      key = PreferenceCountry::code,
    ) { country ->
      CinematicSelectionCard(
        title = country.name,
        subtitle = country.description,
        icon = if (country.code == UserPreferenceProfile.DEFAULT_COUNTRY) {
          Icons.Outlined.TravelExplore
        } else {
          Icons.Outlined.LocationOn
        },
        artworkPath = artworkTitles
          .artworkForCountry(
            countryCode = country.code,
            fallbackIndex = supportedPreferenceCountries.indexOf(country),
          )
          ?.artworkPath,
        selected = country.code in selected,
        onClick = { onToggle(country.code) },
        modifier = Modifier
          .fillMaxWidth()
          .height(128.dp),
      )
    }
  }
}

@Composable
private fun TitleGrid(
  titles: List<MediaItem>,
  selected: Set<com.example.filmera.core.model.MediaKey>,
  onToggle: (MediaItem) -> Unit,
) {
  LazyVerticalGrid(
    columns = GridCells.Adaptive(112.dp),
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(20.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    items(
      items = titles,
      key = { item -> "${item.type}-${item.id}" },
    ) { item ->
      val isSelected = item.key in selected
      val interactionSource = remember { MutableInteractionSource() }
      val isPressed by interactionSource.collectIsPressedAsState()
      val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "title-card-scale",
      )
      Card(
        onClick = { onToggle(item) },
        interactionSource = interactionSource,
        modifier = Modifier
          .scale(scale)
          .selectionSemantics(isSelected, item.title),
        shape = RoundedCornerShape(16.dp),
        border = selectionCardBorder(isSelected),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f),
        ) {
          TmdbImage(
            path = item.posterPath,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
          )
          if (isSelected) {
            Surface(
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(7.dp),
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primary,
            ) {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.preferences_selected),
                modifier = Modifier.padding(5.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
              )
            }
          }
        }
        Text(
          text = item.title,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
          style = MaterialTheme.typography.labelLarge,
          minLines = 2,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun PersonGrid(
  people: List<RecommendedPerson>,
  selected: Set<Int>,
  onToggle: (Int) -> Unit,
) {
  LazyVerticalGrid(
    columns = GridCells.Adaptive(120.dp),
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(20.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    items(
      items = people,
      key = RecommendedPerson::id,
    ) { person ->
      val isSelected = person.id in selected
      val interactionSource = remember { MutableInteractionSource() }
      val isPressed by interactionSource.collectIsPressedAsState()
      val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "person-card-scale",
      )
      Card(
        onClick = { onToggle(person.id) },
        interactionSource = interactionSource,
        modifier = Modifier
          .scale(scale)
          .selectionSemantics(isSelected, person.name),
        shape = RoundedCornerShape(18.dp),
        border = selectionCardBorder(isSelected),
        colors = CardDefaults.cardColors(
          containerColor = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.44f)
          } else {
            MaterialTheme.colorScheme.surfaceContainerLow
          },
        ),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box {
            TmdbImage(
              path = person.profilePath,
              contentDescription = null,
              size = "w185",
              modifier = Modifier
                .size(104.dp)
                .clip(CircleShape),
            )
            if (isSelected) {
              Surface(
                modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .size(30.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
              ) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = stringResource(R.string.preferences_selected),
                  modifier = Modifier.padding(6.dp),
                  tint = MaterialTheme.colorScheme.onPrimary,
                )
              }
            }
          }
          Text(
            text = person.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = person.knownForDepartment,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}

@Composable
private fun ChoicesContent(
  choicesState: LoadState<OnboardingChoices>,
  emptyMessage: String,
  onRetry: () -> Unit,
  content: @Composable (OnboardingChoices) -> Unit,
) {
  when (choicesState) {
    LoadState.Loading -> CenteredMessage {
      CircularProgressIndicator()
      Text(
        text = stringResource(R.string.preferences_loading_choices),
        modifier = Modifier.padding(top = 16.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    LoadState.Empty -> CenteredMessage {
      Text(
        text = emptyMessage,
        textAlign = TextAlign.Center,
      )
      Button(
        onClick = onRetry,
        modifier = Modifier.padding(top = 16.dp),
      ) {
        Text(stringResource(R.string.retry))
      }
    }
    is LoadState.Error -> CenteredMessage {
      Text(
        text = emptyMessage,
        textAlign = TextAlign.Center,
      )
      Button(
        onClick = onRetry,
        modifier = Modifier.padding(top = 16.dp),
      ) {
        Text(stringResource(R.string.retry))
      }
    }
    is LoadState.Success -> content(choicesState.value)
  }
}

@Composable
private fun CenteredMessage(
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    content = content,
  )
}

@Composable
private fun CompletionArtwork() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .size(180.dp)
        .clip(CircleShape)
        .background(
          Brush.radialGradient(
            listOf(
              MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
              Color.Transparent,
            ),
          ),
        ),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        modifier = Modifier.size(96.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Icon(
          imageVector = Icons.Outlined.AutoAwesome,
          contentDescription = null,
          modifier = Modifier.padding(24.dp),
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    }
  }
}

@Composable
private fun CinematicSelectionCard(
  title: String,
  icon: ImageVector,
  artworkPath: String?,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.97f else 1f,
    animationSpec = tween(120),
    label = "preference-card-scale",
  )
  val shape = RoundedCornerShape(20.dp)
  Card(
    onClick = onClick,
    interactionSource = interactionSource,
    modifier = modifier
      .scale(scale)
      .selectionSemantics(selected, title),
    shape = shape,
    border = selectionCardBorder(selected),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.linearGradient(
            listOf(
              MaterialTheme.colorScheme.surfaceContainerHighest,
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f),
            ),
          ),
        ),
    ) {
      if (!artworkPath.isNullOrBlank()) {
        TmdbImage(
          path = artworkPath,
          contentDescription = null,
          size = "w500",
          modifier = Modifier.matchParentSize(),
        )
      }
      Box(
        modifier = Modifier
          .matchParentSize()
          .background(
            Brush.verticalGradient(
              colorStops = arrayOf(
                0f to MaterialTheme.colorScheme.surface.copy(alpha = 0.20f),
                0.42f to MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
                1f to MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
              ),
            ),
          ),
      )
      if (selected) {
        Box(
          modifier = Modifier
            .matchParentSize()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        )
      }
      Surface(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(10.dp)
          .size(36.dp),
        shape = CircleShape,
        color = if (selected) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
        },
        contentColor = if (selected) {
          MaterialTheme.colorScheme.onPrimary
        } else {
          MaterialTheme.colorScheme.primary
        },
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.padding(8.dp),
        )
      }
      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
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
      if (selected) {
        Surface(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(10.dp)
            .size(30.dp),
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = stringResource(R.string.preferences_selected),
            modifier = Modifier.padding(6.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun PreferenceFooter(
  state: PreferenceOnboardingUiState,
  onAction: (PreferenceOnboardingAction) -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface)
      .padding(horizontal = 20.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    state.validationError?.let { error ->
      Text(
        text = stringResource(error.messageResource()),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
      )
    }
    if (
      state.step == PreferenceStep.PEOPLE ||
      state.step == PreferenceStep.TITLES &&
      state.choicesState !is LoadState.Success
    ) {
      TextButton(
        onClick = { onAction(PreferenceOnboardingAction.SkipOptionalClicked) },
        enabled = !state.isSaving,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(R.string.preferences_skip))
      }
    }
    Button(
      onClick = {
        onAction(
          if (state.step == PreferenceStep.COMPLETE) {
            PreferenceOnboardingAction.SaveClicked
          } else {
            PreferenceOnboardingAction.ContinueClicked
          },
        )
      },
      enabled = !state.isSaving,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp),
    ) {
      if (state.isSaving) {
        CircularProgressIndicator(
          modifier = Modifier.size(22.dp),
          strokeWidth = 2.dp,
        )
      } else {
        Text(
          text = stringResource(
            when {
              state.step != PreferenceStep.COMPLETE -> R.string.preferences_continue
              state.isEditing -> R.string.preferences_save
              else -> R.string.preferences_start
            },
          ),
        )
      }
    }
  }
}

private fun PreferenceValidationError.messageResource(): Int =
  when (this) {
    PreferenceValidationError.CONTENT_TYPE_REQUIRED -> R.string.preferences_minimum_content
    PreferenceValidationError.GENRES_REQUIRED -> R.string.preferences_minimum_genres
    PreferenceValidationError.GENRES_LIMIT -> R.string.preferences_maximum_genres
    PreferenceValidationError.COUNTRIES_LIMIT -> R.string.preferences_maximum_countries
    PreferenceValidationError.TITLES_REQUIRED -> R.string.preferences_minimum_titles
    PreferenceValidationError.SAVE_FAILED -> R.string.preferences_save_error
  }

@Composable
private fun selectionCardBorder(selected: Boolean): BorderStroke {
  val borderColor by animateColorAsState(
    targetValue = if (selected) {
      MaterialTheme.colorScheme.primary
    } else {
      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
    },
    animationSpec = tween(160),
    label = "preference-card-border",
  )
  return BorderStroke(
    width = if (selected) 2.dp else 1.dp,
    color = borderColor,
  )
}

private val MediaItem.artworkPath: String?
  get() = backdropPath ?: posterPath

private fun List<MediaItem>.artworkForContentType(
  type: MediaPreference,
  fallbackIndex: Int,
): MediaItem? {
  val matching = when (type) {
    MediaPreference.MOVIE -> firstOrNull { it.type == MediaType.MOVIE && it.artworkPath != null }
    MediaPreference.TV_SERIES ->
      firstOrNull { it.type == MediaType.TV_SHOW && it.artworkPath != null }
    MediaPreference.ANIME -> firstOrNull {
      it.artworkPath != null &&
        ANIMATION_GENRE_ID in it.genreIds &&
        it.originalLanguage == JAPANESE_LANGUAGE_CODE
    }
    MediaPreference.DOCUMENTARY -> firstOrNull {
      it.artworkPath != null && DOCUMENTARY_GENRE_ID in it.genreIds
    }
    MediaPreference.ANIMATION -> firstOrNull {
      it.artworkPath != null && ANIMATION_GENRE_ID in it.genreIds
    }
  }
  return matching ?: artworkFallback(fallbackIndex)
}

private fun List<MediaItem>.artworkForGenre(
  genreId: Int,
  fallbackIndex: Int,
): MediaItem? =
  firstOrNull { genreId in it.genreIds && it.artworkPath != null }
    ?: artworkFallback(fallbackIndex)

private fun List<MediaItem>.artworkForCountry(
  countryCode: String,
  fallbackIndex: Int,
): MediaItem? {
  val matching = when (countryCode) {
    UserPreferenceProfile.DEFAULT_COUNTRY -> firstOrNull { it.artworkPath != null }
    EUROPE_REGION_CODE -> firstOrNull { item ->
      item.artworkPath != null &&
        item.originCountries.any(EUROPE_COUNTRY_CODES::contains)
    }
    else -> firstOrNull {
      it.artworkPath != null && countryCode in it.originCountries
    }
  }
  return matching ?: artworkFallback(fallbackIndex)
}

private fun List<MediaItem>.artworkFallback(index: Int): MediaItem? {
  val candidates = filter { it.artworkPath != null }
  return candidates.getOrNull(index.mod(candidates.size.coerceAtLeast(1)))
}

private fun genreIcon(genreId: Int): ImageVector =
  when (genreId) {
    28 -> Icons.Outlined.Bolt
    12 -> Icons.Outlined.Explore
    16 -> Icons.Outlined.Animation
    35 -> Icons.Outlined.SentimentSatisfied
    80 -> Icons.Outlined.LocalPolice
    DOCUMENTARY_GENRE_ID -> Icons.Outlined.PhotoCamera
    18 -> Icons.Outlined.TheaterComedy
    10751 -> Icons.Outlined.Groups
    14 -> Icons.Outlined.AutoAwesome
    36 -> Icons.Outlined.HistoryEdu
    27 -> Icons.Outlined.Visibility
    10402 -> Icons.Outlined.MusicNote
    9648 -> Icons.AutoMirrored.Outlined.HelpOutline
    10749 -> Icons.Outlined.FavoriteBorder
    878 -> Icons.Outlined.RocketLaunch
    53, 10752 -> Icons.Outlined.Security
    37 -> Icons.Outlined.Landscape
    else -> Icons.Outlined.Movie
  }

private fun Modifier.selectionSemantics(
  isSelected: Boolean,
  label: String,
): Modifier = semantics(mergeDescendants = true) {
  selected = isSelected
  role = Role.Checkbox
  contentDescription = label
}

private const val ANIMATION_GENRE_ID = 16
private const val DOCUMENTARY_GENRE_ID = 99
private const val JAPANESE_LANGUAGE_CODE = "ja"
private const val EUROPE_REGION_CODE = "EU"
private val EUROPE_COUNTRY_CODES = setOf(
  "FR",
  "DE",
  "ES",
  "IT",
  "SE",
  "NO",
  "DK",
  "NL",
  "BE",
  "PL",
)

@Preview(showBackground = true, widthDp = 412, heightDp = 900)
@Composable
private fun PreferenceGenresPreview() {
  FilmeraTheme {
    PreferenceOnboardingScreen(
      state = PreferenceOnboardingUiState(
        step = PreferenceStep.GENRES,
        selectedGenreIds = setOf(18, 28, 878),
      ),
      onAction = {},
      onNavigateBack = {},
    )
  }
}
