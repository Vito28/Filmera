package com.example.filmera.feature.home.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.filmera.R
import com.example.filmera.core.common.AppError
import com.example.filmera.core.designsystem.theme.filmeraColors
import com.example.filmera.core.ui.LoadState
import com.example.filmera.core.ui.component.TmdbImage
import com.example.filmera.core.ui.messageResource
import com.example.filmera.feature.community.domain.CommunityReview
import com.example.filmera.feature.community.domain.ReviewReaction
import java.time.Duration
import java.time.Instant
import kotlin.math.max

@Composable
fun CommunityPulseSection(
  state: LoadState<List<CommunityReview>>,
  onSeeAll: () -> Unit,
  onReviewClick: (String) -> Unit,
  onMediaClick: (CommunityReview) -> Unit,
  onHelpfulClick: (String) -> Unit,
  onCommentClick: (String) -> Unit,
  onRateClick: (String) -> Unit,
  onReactionClick: (String, ReviewReaction) -> Unit,
  onWriteReview: () -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    SectionHeader(
      title = stringResource(R.string.community_pulse_title),
      subtitle = stringResource(R.string.community_pulse_subtitle),
      actionLabel = stringResource(R.string.see_all),
      onActionClick = onSeeAll,
    )

    AnimatedContent(
      targetState = state,
      transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) },
      contentKey = { current -> current::class },
      label = "community pulse state",
    ) { current ->
      when (current) {
        LoadState.Loading -> CommunityPulseSkeleton()
        LoadState.Empty -> CommunityPulseEmpty(
          onWriteReview = onWriteReview,
          onExplore = onSeeAll,
        )
        is LoadState.Error -> CommunityPulseError(error = current.error, onRetry = onRetry)
        is LoadState.Success -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          current.value.firstOrNull()?.let { review ->
            FeaturedCommunityReviewCard(
              review = review,
              onOpen = { onReviewClick(review.id) },
              onMediaClick = { onMediaClick(review) },
              onHelpfulClick = { onHelpfulClick(review.id) },
              onCommentClick = { onCommentClick(review.id) },
              onRateClick = { onRateClick(review.id) },
              onReactionClick = { reaction -> onReactionClick(review.id, reaction) },
            )
          }
          current.value.drop(1).take(2).forEach { review ->
            CommunityConversationCard(
              review = review,
              onClick = { onReviewClick(review.id) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FeaturedCommunityReviewCard(
  review: CommunityReview,
  onOpen: () -> Unit,
  onMediaClick: () -> Unit,
  onHelpfulClick: () -> Unit,
  onCommentClick: () -> Unit,
  onRateClick: () -> Unit,
  onReactionClick: (ReviewReaction) -> Unit,
) {
  var spoilerRevealed by rememberSaveable(review.id) { mutableStateOf(false) }
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)),
  ) {
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(190.dp)
          .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
          .clickable(onClick = onMediaClick, role = Role.Button),
      ) {
        TmdbImage(
          path = review.posterPath,
          contentDescription = stringResource(R.string.poster_content_description, review.mediaTitle),
          modifier = Modifier.fillMaxSize(),
          size = "w780",
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.12f),
                0.54f to Color.Black.copy(alpha = 0.38f),
                1f to MaterialTheme.colorScheme.surfaceContainerLow,
              ),
            ),
        )
        Surface(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(14.dp),
          shape = RoundedCornerShape(50),
          color = Color.Black.copy(alpha = 0.66f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)),
        ) {
          Text(
            text = stringResource(R.string.community_review_badge),
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
          )
        }
        review.authorRating?.let { rating ->
          Surface(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(14.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.filmeraColors.rating.copy(alpha = 0.94f),
          ) {
            Text(
              text = stringResource(R.string.community_rating_value, rating),
              modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
              color = Color(0xFF1C1400),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.ExtraBold,
            )
          }
        }
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          Text(
            text = review.mediaTitle,
            color = MaterialTheme.filmeraColors.onImage,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = listOfNotNull(
              if (review.mediaKey.type.routeValue == "movie") {
                stringResource(R.string.media_type_movie)
              } else {
                stringResource(R.string.media_type_tv)
              },
              review.releaseYear,
            ).joinToString(" · "),
            color = MaterialTheme.filmeraColors.onImage.copy(alpha = 0.74f),
            style = MaterialTheme.typography.labelMedium,
          )
        }
      }

      Column(
        modifier = Modifier
          .clickable(onClick = onOpen, role = Role.Button)
          .padding(start = 16.dp, top = 14.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          AuthorInitial(review)
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = review.author.displayName,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = stringResource(
                R.string.community_reviewed_time,
                review.author.username,
                relativeTime(review.createdAt),
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.labelSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          review.filmeraRating?.let { rating ->
            Text(
              text = stringResource(R.string.community_filmera_score, rating),
              color = MaterialTheme.colorScheme.primary,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        review.headline?.let { headline ->
          Text(
            text = headline,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }

        if (review.containsSpoilers && !spoilerRevealed) {
          SpoilerCover(onReveal = { spoilerRevealed = true })
        } else {
          Text(
            text = review.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      ReactionRow(
        review = review,
        onReactionClick = onReactionClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      )
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 8.dp, end = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        HelpfulAction(
          review = review,
          onClick = onHelpfulClick,
          modifier = Modifier.weight(1f),
        )
        CommunityAction(
          label = compactCount(review.commentCount),
          contentDescription = stringResource(R.string.community_open_comments, review.commentCount),
          onClick = onCommentClick,
          modifier = Modifier.weight(1f),
        ) {
          Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null)
        }
        CommunityAction(
          label = review.viewerRating?.let { "$it/10" }
            ?: stringResource(R.string.community_rate),
          contentDescription = stringResource(R.string.community_rate_title, review.mediaTitle),
          onClick = onRateClick,
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            Icons.Outlined.StarOutline,
            contentDescription = null,
            tint = if (review.viewerRating != null) {
              MaterialTheme.filmeraColors.rating
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
          )
        }
      }
    }
  }
}

@Composable
private fun ReactionRow(
  review: CommunityReview,
  onReactionClick: (ReviewReaction) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(7.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(R.string.community_react),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.labelSmall,
    )
    ReviewReaction.entries.forEach { reaction ->
      val selected = review.reactions.selectedReaction == reaction
      val description = reactionDescription(reaction)
      val background by animateColorAsState(
        targetValue = if (selected) {
          reactionColor(reaction).copy(alpha = 0.2f)
        } else {
          MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(200),
        label = "reaction background",
      )
      Surface(
        onClick = {
          if (!review.reactions.isSubmitting) onReactionClick(reaction)
        },
        modifier = Modifier
          .size(width = 48.dp, height = 40.dp)
          .semantics {
            this.selected = selected
            contentDescription = description
          },
        enabled = !review.reactions.isSubmitting,
        shape = RoundedCornerShape(15.dp),
        color = background,
        border = BorderStroke(
          1.dp,
          if (selected) reactionColor(reaction).copy(alpha = 0.82f)
          else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
        ),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(text = reaction.emoji, style = MaterialTheme.typography.titleMedium)
        }
      }
    }
    if (review.reactions.totalCount > 0) {
      Text(
        text = compactCount(review.reactions.totalCount),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
      )
    }
  }
}

@Composable
private fun HelpfulAction(
  review: CommunityReview,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val scale by animateFloatAsState(
    targetValue = if (review.helpful.isHelpful) 1.06f else 1f,
    animationSpec = tween(140),
    label = "helpful icon scale",
  )
  CommunityAction(
    label = compactCount(review.helpful.count),
    contentDescription = if (review.helpful.isHelpful) {
      stringResource(R.string.community_helpful_selected, review.helpful.count)
    } else {
      stringResource(R.string.community_mark_helpful, review.helpful.count)
    },
    onClick = {
      haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
      onClick()
    },
    modifier = modifier,
    selected = review.helpful.isHelpful,
    enabled = !review.helpful.isSubmitting,
  ) {
    if (review.helpful.isSubmitting) {
      CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
    } else {
      Icon(
        imageVector = if (review.helpful.isHelpful) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
        contentDescription = null,
        modifier = Modifier.scale(scale),
      )
    }
  }
}

@Composable
private fun CommunityAction(
  label: String,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  selected: Boolean = false,
  enabled: Boolean = true,
  icon: @Composable () -> Unit,
) {
  val color by animateColorAsState(
    targetValue = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant,
    animationSpec = tween(200),
    label = "community action color",
  )
  Surface(
    onClick = onClick,
    modifier = modifier
      .height(48.dp)
      .semantics {
        this.contentDescription = contentDescription
        this.selected = selected
      },
    enabled = enabled,
    shape = RoundedCornerShape(16.dp),
    color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
    else Color.Transparent,
    contentColor = color,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 9.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      icon()
      Spacer(Modifier.width(6.dp))
      Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
private fun CommunityConversationCard(
  review: CommunityReview,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f)),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TmdbImage(
        path = review.posterPath,
        contentDescription = null,
        modifier = Modifier
          .size(width = 54.dp, height = 72.dp)
          .clip(RoundedCornerShape(14.dp)),
        size = "w185",
      )
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = if (review.containsSpoilers) {
            stringResource(R.string.community_spoiler_conversation)
          } else {
            stringResource(R.string.community_trending_conversation)
          },
          color = if (review.containsSpoilers) MaterialTheme.filmeraColors.warning
          else MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = review.headline ?: review.body,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = stringResource(
            R.string.community_conversation_meta,
            review.commentCount,
            relativeTime(review.updatedAt),
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.labelSmall,
        )
      }
      Icon(
        Icons.AutoMirrored.Outlined.ArrowForward,
        contentDescription = stringResource(R.string.community_open_review),
        tint = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
private fun SpoilerCover(onReveal: () -> Unit) {
  Surface(
    onClick = onReveal,
    modifier = Modifier
      .fillMaxWidth()
      .semantics {
        contentDescription = "Spoiler review hidden. Tap to reveal."
      },
    shape = RoundedCornerShape(17.dp),
    color = MaterialTheme.filmeraColors.warning.copy(alpha = 0.1f),
    border = BorderStroke(1.dp, MaterialTheme.filmeraColors.warning.copy(alpha = 0.36f)),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = stringResource(R.string.community_spoiler_hidden),
        color = MaterialTheme.filmeraColors.warning,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = stringResource(R.string.community_tap_reveal),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}

@Composable
private fun AuthorInitial(review: CommunityReview) {
  val initial = review.author.displayName.firstOrNull()?.uppercase() ?: "F"
  Box(
    modifier = Modifier
      .size(42.dp)
      .clip(CircleShape)
      .background(
        Brush.linearGradient(
          listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f),
          ),
        ),
      ),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = initial,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun CommunityPulseSkeleton() {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(390.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
    repeat(2) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(96.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(MaterialTheme.colorScheme.surfaceContainer),
      )
    }
  }
}

@Composable
private fun CommunityPulseEmpty(
  onWriteReview: () -> Unit,
  onExplore: () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = Icons.Outlined.Groups,
        contentDescription = null,
        modifier = Modifier.size(32.dp),
        tint = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = stringResource(R.string.community_empty_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = stringResource(R.string.community_empty_message),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = onWriteReview,
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        ) {
          Text(stringResource(R.string.community_write_first_review))
        }
        OutlinedButton(
          onClick = onExplore,
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        ) {
          Text(stringResource(R.string.community_explore_action))
        }
      }
    }
  }
}

@Composable
private fun CommunityPulseError(error: AppError, onRetry: () -> Unit) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.34f),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(error.messageResource()),
        modifier = Modifier.weight(1f),
        color = MaterialTheme.colorScheme.onErrorContainer,
        style = MaterialTheme.typography.bodyMedium,
      )
      Surface(
        onClick = onRetry,
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.retry))
        }
      }
    }
  }
}

private fun compactCount(value: Int): String = when {
  value >= 1_000_000 -> "${value / 1_000_000}M"
  value >= 1_000 -> "${value / 1_000}K"
  else -> value.toString()
}

@Composable
private fun reactionColor(reaction: ReviewReaction): Color = when (reaction) {
  ReviewReaction.LOVE -> Color(0xFFFF6685)
  ReviewReaction.FIRE -> Color(0xFFFFA24B)
  ReviewReaction.MIND_BLOWN -> Color(0xFFA986FF)
  ReviewReaction.MOVING -> Color(0xFF58D9FF)
}

@Composable
private fun reactionDescription(reaction: ReviewReaction): String = when (reaction) {
  ReviewReaction.LOVE -> stringResource(R.string.community_reaction_love)
  ReviewReaction.FIRE -> stringResource(R.string.community_reaction_fire)
  ReviewReaction.MIND_BLOWN -> stringResource(R.string.community_reaction_mind_blown)
  ReviewReaction.MOVING -> stringResource(R.string.community_reaction_moving)
}

@Composable
private fun relativeTime(value: String): String {
  val fallback = stringResource(R.string.community_recently)
  return remember(value, fallback) {
    runCatching {
      val duration = Duration.between(Instant.parse(value), Instant.now())
      when {
        duration.isNegative -> fallback
        duration.toMinutes() < 1 -> "now"
        duration.toHours() < 1 -> "${max(1, duration.toMinutes())}m"
        duration.toDays() < 1 -> "${duration.toHours()}h"
        duration.toDays() < 7 -> "${duration.toDays()}d"
        else -> "${duration.toDays() / 7}w"
      }
    }.getOrDefault(fallback)
  }
}
