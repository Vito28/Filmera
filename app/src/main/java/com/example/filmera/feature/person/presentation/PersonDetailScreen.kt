package com.example.filmera.feature.person.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.filmera.R
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.navigation.FilmeraScaffold
import com.example.filmera.core.ui.LazyLayoutKey
import com.example.filmera.core.ui.LoadState
import com.example.filmera.core.ui.component.EmptyState
import com.example.filmera.core.ui.component.ErrorState
import com.example.filmera.core.ui.component.LoadingState
import com.example.filmera.core.ui.component.TmdbImage
import com.example.filmera.feature.person.domain.PersonCredit
import com.example.filmera.feature.person.domain.PersonDetails
import com.example.filmera.feature.person.domain.PersonExternalIds
import com.example.filmera.feature.person.domain.PersonGender
import java.net.URI

@Composable
fun PersonDetailRoute(
  navController: NavHostController,
  onMediaClick: (MediaItem) -> Unit,
  viewModel: PersonDetailViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val uriHandler = LocalUriHandler.current

  PersonDetailScreen(
    state = state,
    navController = navController,
    onNavigateBack = navController::popBackStack,
    onMediaClick = onMediaClick,
    onOpenExternalLink = { url -> runCatching { uriHandler.openUri(url) } },
    onRetry = viewModel::retry,
  )
}

@Composable
fun PersonDetailScreen(
  state: PersonDetailUiState,
  navController: NavHostController,
  onNavigateBack: () -> Unit,
  onMediaClick: (MediaItem) -> Unit,
  onOpenExternalLink: (String) -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val title = (state.contentState as? LoadState.Success)?.value?.name
    ?: stringResource(R.string.person_detail_title)

  FilmeraScaffold(
    title = title,
    navController = navController,
    modifier = modifier,
    showBottomBar = false,
    onNavigateBack = onNavigateBack,
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
    ) {
      when (val contentState = state.contentState) {
        LoadState.Loading -> LoadingState()
        LoadState.Empty -> EmptyState(
          title = stringResource(R.string.person_empty_title),
          message = stringResource(R.string.person_empty_message),
        )
        is LoadState.Error -> ErrorState(contentState.error, onRetry)
        is LoadState.Success -> PersonContent(
          person = contentState.value,
          onMediaClick = onMediaClick,
          onOpenExternalLink = onOpenExternalLink,
        )
      }
    }
  }
}

@Composable
private fun PersonContent(
  person: PersonDetails,
  onMediaClick: (MediaItem) -> Unit,
  onOpenExternalLink: (String) -> Unit,
) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxHeight()
        .widthIn(max = 960.dp)
        .fillMaxWidth(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      item(key = "person-hero") {
        PersonHero(person)
      }
      item(key = "person-biography") {
        BiographySection(person.biography)
      }
      item(key = "person-information") {
        PersonInformationSection(person)
      }

      val externalLinks = person.externalIds.toExternalLinks(person.homepage)
      if (externalLinks.isNotEmpty()) {
        item(key = "person-external-links") {
          ExternalLinksSection(
            links = externalLinks,
            onOpenExternalLink = onOpenExternalLink,
          )
        }
      }
      if (person.photos.size > 1) {
        item(key = "person-photo-gallery") {
          PhotoGallery(person)
        }
      }
      if (person.movieCredits.isNotEmpty()) {
        item(key = "person-movie-credits") {
          FilmographySection(
            title = stringResource(R.string.person_movie_credits),
            credits = person.movieCredits,
            onMediaClick = onMediaClick,
          )
        }
      }
      if (person.tvCredits.isNotEmpty()) {
        item(key = "person-tv-credits") {
          FilmographySection(
            title = stringResource(R.string.person_tv_credits),
            credits = person.tvCredits,
            onMediaClick = onMediaClick,
          )
        }
      }
    }
  }
}

@Composable
private fun PersonHero(person: PersonDetails) {
  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    if (maxWidth < 600.dp) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        PersonPortrait(person)
        PersonIdentity(person, horizontalAlignment = Alignment.CenterHorizontally)
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        PersonPortrait(person)
        PersonIdentity(
          person = person,
          horizontalAlignment = Alignment.Start,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun PersonPortrait(person: PersonDetails) {
  TmdbImage(
    path = person.profilePath,
    contentDescription = stringResource(R.string.profile_image_description, person.name),
    modifier = Modifier
      .size(width = 184.dp, height = 264.dp)
      .clip(RoundedCornerShape(24.dp)),
    size = "h632",
  )
}

@Composable
private fun PersonIdentity(
  person: PersonDetails,
  horizontalAlignment: Alignment.Horizontal,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = horizontalAlignment,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = person.name,
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
    )
    person.knownForDepartment?.let {
      Text(
        text = it,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleMedium,
      )
    }
    val summary = listOfNotNull(
      person.gender.label(),
      person.birthday?.take(4),
      person.placeOfBirth,
    ).joinToString(" • ")
    if (summary.isNotBlank()) {
      Text(
        text = summary,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}

@Composable
private fun BiographySection(biography: String) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    SectionTitle(stringResource(R.string.person_biography))
    Text(
      text = biography.ifBlank { stringResource(R.string.person_biography_unavailable) },
      style = MaterialTheme.typography.bodyLarge,
    )
  }
}

@Composable
private fun PersonInformationSection(person: PersonDetails) {
  val rows = listOfNotNull(
    person.gender.label().let { stringResource(R.string.person_gender) to it },
    person.birthday?.let { stringResource(R.string.person_birthday) to it },
    person.deathday?.let { stringResource(R.string.person_deathday) to it },
    person.placeOfBirth?.let { stringResource(R.string.person_place_of_birth) to it },
    person.knownForDepartment?.let { stringResource(R.string.person_known_for) to it },
    person.alsoKnownAs.takeIf(List<String>::isNotEmpty)
      ?.let { stringResource(R.string.person_also_known_as) to it.joinToString() },
  )

  Card(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp)) {
      SectionTitle(stringResource(R.string.person_information))
      rows.forEachIndexed { index, (label, value) ->
        if (index > 0) {
          HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
        }
        Text(
          text = label,
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.labelMedium,
        )
        Text(
          text = value,
          modifier = Modifier.padding(top = 2.dp),
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
  }
}

@Composable
private fun ExternalLinksSection(
  links: List<ExternalLinkUi>,
  onOpenExternalLink: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SectionTitle(stringResource(R.string.person_external_links))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(
        items = links,
        key = { link -> LazyLayoutKey.of("person-external-link", link.url) },
      ) { link ->
        AssistChip(
          onClick = { onOpenExternalLink(link.url) },
          label = { Text(stringResource(link.labelRes)) },
          leadingIcon = {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
          },
        )
      }
    }
  }
}

@Composable
private fun PhotoGallery(person: PersonDetails) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SectionTitle(stringResource(R.string.person_photos))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      items(
        items = person.photos.take(20),
        key = { photoPath -> LazyLayoutKey.of("person-photo", photoPath) },
      ) { photoPath ->
        TmdbImage(
          path = photoPath,
          contentDescription = stringResource(R.string.person_gallery_photo, person.name),
          modifier = Modifier
            .size(width = 140.dp, height = 196.dp)
            .clip(RoundedCornerShape(16.dp)),
          size = "h632",
        )
      }
    }
  }
}

@Composable
private fun FilmographySection(
  title: String,
  credits: List<PersonCredit>,
  onMediaClick: (MediaItem) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SectionTitle(title)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      items(
        items = credits.take(30),
        key = { credit -> LazyLayoutKey.of("person-credit", credit.stableKey) },
      ) { credit ->
        PersonCreditCard(
          credit = credit,
          onClick = { onMediaClick(credit.media) },
        )
      }
    }
  }
}

@Composable
private fun PersonCreditCard(
  credit: PersonCredit,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    modifier = Modifier.width(152.dp),
    shape = RoundedCornerShape(18.dp),
  ) {
    Column {
      TmdbImage(
        path = credit.media.posterPath,
        contentDescription = stringResource(
          R.string.poster_content_description,
          credit.media.title,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(220.dp),
      )
      Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = credit.media.title,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        credit.contribution.takeIf(String::isNotBlank)?.let {
          Text(
            text = it,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
          )
        }
        Text(
          text = credit.media.releaseDate?.take(4)
            ?: stringResource(R.string.not_available_short),
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.labelMedium,
        )
      }
    }
  }
}

@Composable
private fun PersonGender.label(): String =
  stringResource(
    when (this) {
      PersonGender.NOT_SPECIFIED -> R.string.person_gender_not_specified
      PersonGender.FEMALE -> R.string.person_gender_female
      PersonGender.MALE -> R.string.person_gender_male
      PersonGender.NON_BINARY -> R.string.person_gender_non_binary
    },
  )

@Composable
private fun SectionTitle(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleLarge,
    fontWeight = FontWeight.SemiBold,
  )
}

private fun PersonExternalIds.toExternalLinks(homepage: String?): List<ExternalLinkUi> =
  buildList {
    homepage.validHttpsUrl()?.let { add(ExternalLinkUi(R.string.external_website, it)) }
    imdbId.safeExternalId(Regex("nm\\d+"))
      ?.let { add(ExternalLinkUi(R.string.external_imdb, "https://www.imdb.com/name/$it")) }
    instagramId.safeExternalId()
      ?.let {
        add(ExternalLinkUi(R.string.external_instagram, "https://www.instagram.com/$it"))
      }
    facebookId.safeExternalId()
      ?.let { add(ExternalLinkUi(R.string.external_facebook, "https://www.facebook.com/$it")) }
    twitterId.safeExternalId()
      ?.let { add(ExternalLinkUi(R.string.external_x, "https://x.com/$it")) }
    tiktokId.safeExternalId()
      ?.let { add(ExternalLinkUi(R.string.external_tiktok, "https://www.tiktok.com/@$it")) }
    youtubeId.safeExternalId()
      ?.let {
        add(ExternalLinkUi(R.string.external_youtube, "https://www.youtube.com/channel/$it"))
      }
    wikidataId.safeExternalId(Regex("Q\\d+"))
      ?.let {
        add(ExternalLinkUi(R.string.external_wikidata, "https://www.wikidata.org/wiki/$it"))
      }
  }

private fun String?.safeExternalId(
  pattern: Regex = Regex("[A-Za-z0-9._-]+"),
): String? = this?.takeIf(pattern::matches)

private fun String?.validHttpsUrl(): String? {
  val uri = runCatching { URI(this.orEmpty()) }.getOrNull() ?: return null
  return toString().takeIf {
    uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
  }
}

private data class ExternalLinkUi(
  @param:StringRes val labelRes: Int,
  val url: String,
)
