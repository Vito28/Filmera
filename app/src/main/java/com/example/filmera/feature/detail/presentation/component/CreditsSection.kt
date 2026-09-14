package com.example.filmera.feature.detail.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.filmera.R
import com.example.filmera.core.model.CastMember
import com.example.filmera.core.model.Credits
import com.example.filmera.core.ui.LazyLayoutKey
import com.example.filmera.core.ui.component.TmdbImage

@Composable
fun CreditsSection(
  credits: Credits,
  onCastClick: (CastMember) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (credits.cast.isEmpty() && credits.crew.isEmpty()) return

  Column(modifier = modifier.fillMaxWidth()) {
    if (credits.cast.isNotEmpty()) {
      SectionTitle(stringResource(R.string.main_cast))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(
          items = credits.cast.take(12),
          key = { castMember ->
            LazyLayoutKey.of(
              "detail-cast",
              castMember.id.toString(),
              castMember.order.toString(),
            )
          },
        ) { castMember ->
          CastMemberItem(
            castMember = castMember,
            onClick = { onCastClick(castMember) },
          )
        }
      }
    }

    val importantJobs = setOf("Director", "Writer", "Screenplay", "Producer", "Executive Producer")
    val highlightedCrew = credits.crew
      .filter { it.job in importantJobs }
      .distinctBy { "${it.id}-${it.job}" }
      .take(12)

    if (highlightedCrew.isNotEmpty()) {
      Spacer(modifier = Modifier.height(24.dp))
      SectionTitle(stringResource(R.string.key_crew))
      highlightedCrew.forEach { crewMember ->
        Text(
          text = stringResource(R.string.crew_member_format, crewMember.job, crewMember.name),
          modifier = Modifier.padding(vertical = 4.dp),
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
  }
}

@Composable
private fun CastMemberItem(
  castMember: CastMember,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .width(104.dp)
      .clip(RoundedCornerShape(14.dp))
      .clickable(
        role = Role.Button,
        onClick = onClick,
      ),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    TmdbImage(
      path = castMember.profilePath,
      contentDescription = stringResource(R.string.profile_image_description, castMember.name),
      modifier = Modifier
        .size(width = 104.dp, height = 136.dp)
        .clip(RoundedCornerShape(14.dp)),
      size = "w185",
    )
    Text(
      text = castMember.name,
      modifier = Modifier.padding(top = 8.dp),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      style = MaterialTheme.typography.labelLarge,
    )
    Text(
      text = castMember.character,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.labelSmall,
    )
  }
}

@Composable
private fun SectionTitle(text: String) {
  Text(
    text = text,
    modifier = Modifier.padding(bottom = 12.dp),
    style = MaterialTheme.typography.titleLarge,
    fontWeight = FontWeight.SemiBold,
  )
}
