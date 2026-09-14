package com.example.filmera.feature.discover.domain

import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaKey
import com.example.filmera.feature.preferences.domain.UserPreferenceProfile

enum class RecommendationSource {
  PREFERRED_GENRE,
  GLOBAL,
  TRENDING,
  CRITICALLY_ACCLAIMED,
  HIDDEN_GEM,
  SELECTED_TITLE,
  PREFERRED_PERSON,
  PREFERRED_COUNTRY,
  SERIES,
  ANIME,
}

data class RecommendationCandidate(
  val media: MediaItem,
  val originCountries: Set<String> = emptySet(),
  val sources: Set<RecommendationSource> = emptySet(),
)

data class RecommendedItem(
  val media: MediaItem,
  val score: Double,
  val matchPercentage: Int,
  val reasons: List<String>,
  val originCountries: Set<String>,
  val sources: Set<RecommendationSource>,
)

enum class RecommendationSectionType {
  TOP_PICKS,
  GENRE,
  GLOBAL,
  SERIES,
  ANIME,
  CRITICALLY_ACCLAIMED,
  HIDDEN_GEMS,
  PREFERRED_COUNTRIES,
  SELECTED_TITLE,
}

data class RecommendationSection(
  val id: String,
  val type: RecommendationSectionType,
  val title: String,
  val subtitle: String,
  val items: List<RecommendedItem>,
)

data class RecommendedPerson(
  val id: Int,
  val name: String,
  val profilePath: String?,
  val knownForDepartment: String,
  val knownFor: List<String>,
  val reason: String,
)

data class RecommendedContent(
  val heroItems: List<RecommendedItem>,
  val sections: List<RecommendationSection>,
  val people: List<RecommendedPerson>,
  val availableGenres: List<Int>,
) {
  val allItems: List<RecommendedItem>
    get() = (heroItems + sections.flatMap(RecommendationSection::items))
      .distinctBy { it.media.key }

  val hasContent: Boolean
    get() = heroItems.isNotEmpty() || sections.any { it.items.isNotEmpty() }
}

const val RECOMMENDATION_ALL_SECTION_ID = "all"
const val RECOMMENDATION_PEOPLE_SECTION_ID = "people"

data class OnboardingChoices(
  val titles: List<MediaItem>,
  val people: List<RecommendedPerson>,
)

enum class RecommendationFeedbackAction {
  MORE_LIKE_THIS,
  LESS_LIKE_THIS,
  NOT_INTERESTED,
  ALREADY_WATCHED,
  HIDE,
}

interface RecommendationRepository {
  suspend fun loadRecommendations(
    profile: UserPreferenceProfile,
    selectedGenreId: Int?,
    refreshGeneration: Int,
  ): DataResult<RecommendedContent>

  suspend fun loadOnboardingChoices(): DataResult<OnboardingChoices>

  suspend fun getFeedback(): Map<MediaKey, RecommendationFeedbackAction>

  suspend fun recordFeedback(
    media: MediaItem,
    action: RecommendationFeedbackAction,
  )

  suspend fun clearFeedback()
}
