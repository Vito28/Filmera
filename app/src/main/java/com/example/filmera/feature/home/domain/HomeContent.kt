package com.example.filmera.feature.home.domain

import com.example.filmera.core.common.AppError
import com.example.filmera.core.model.MediaItem

enum class HomeChannel(
  val countryCode: String?,
  val originalLanguage: String?,
  val isAnime: Boolean = false,
) {
  FOR_YOU(countryCode = null, originalLanguage = null),
  INDONESIA(countryCode = "ID", originalLanguage = "id"),
  CHINA(countryCode = "CN", originalLanguage = "zh"),
  INDIA(countryCode = "IN", originalLanguage = null),
  AMERICA(countryCode = "US", originalLanguage = "en"),
  KOREA(countryCode = "KR", originalLanguage = "ko"),
  ANIME(countryCode = "JP", originalLanguage = "ja", isAnime = true),
}

enum class AnimeTopic(
  val movieGenreId: Int?,
  val tvGenreId: Int?,
) {
  ALL(movieGenreId = null, tvGenreId = null),
  ACTION(movieGenreId = 28, tvGenreId = 10759),
  ADVENTURE(movieGenreId = 12, tvGenreId = 10759),
  FANTASY(movieGenreId = 14, tvGenreId = 10765),
  ROMANCE(movieGenreId = 10749, tvGenreId = 18),
  COMEDY(movieGenreId = 35, tvGenreId = 35),
  SPORTS(movieGenreId = 18, tvGenreId = 18),
  SLICE_OF_LIFE(movieGenreId = 18, tvGenreId = 18),
}

data class HomeFeedKey(
  val channel: HomeChannel,
  val animeTopic: AnimeTopic = AnimeTopic.ALL,
) {
  val normalizedAnimeTopic: AnimeTopic
    get() = if (channel.isAnime) animeTopic else AnimeTopic.ALL
}

enum class HomeSectionType {
  HERO,
  TRENDING,
  UPCOMING,
  TOP_RATED,
  EDITORS_PICKS,
  HIDDEN_GEMS,
  POPULAR_WORLDWIDE,
}

data class HomeSection(
  val type: HomeSectionType,
  val items: List<MediaItem> = emptyList(),
  val isInitialLoading: Boolean = false,
  val page: Int = 1,
  val totalPages: Int = 1,
  val error: AppError? = null,
  val isLoadingMore: Boolean = false,
  val paginationError: AppError? = null,
) {
  val canLoadMore: Boolean
    get() = !isLoadingMore && page < totalPages
}

enum class HomeSupplementalType {
  GENRES,
  PEOPLE,
  TRAILERS,
  COLLECTIONS,
}

data class HomeSectionPage(
  val type: HomeSectionType,
  val items: List<MediaItem>,
  val page: Int,
  val totalPages: Int,
)

data class HomeGenre(
  val id: Int,
  val name: String,
  val titleCount: Int,
  val backdropPath: String?,
)

data class HomePerson(
  val id: Int,
  val name: String,
  val profilePath: String?,
  val knownFor: List<String>,
)

data class HomeTrailer(
  val media: MediaItem,
  val videoKey: String,
  val name: String,
  val type: String,
)

data class HomeSpotlight(
  val id: String,
  val media: MediaItem,
  val category: String,
  val title: String,
  val summary: String,
  val readMinutes: Int,
  val source: String = "Filmera Editorial",
  val publishedLabel: String = "Curated this week",
)

enum class HomeCollectionKind {
  OFFICIAL_COLLECTION,
  CINEMATIC_UNIVERSE,
}

data class HomeCollection(
  val id: Int,
  val name: String,
  val posterPath: String?,
  val backdropPath: String?,
  val overview: String,
  val itemCount: Int,
  val kind: HomeCollectionKind,
  val featuredMedia: MediaItem,
)

data class HomeContent(
  val feedKey: HomeFeedKey,
  val sections: List<HomeSection>,
  val genres: List<HomeGenre>,
  val allGenres: List<HomeGenre> = genres,
  val people: List<HomePerson>,
  val trailers: List<HomeTrailer> = emptyList(),
  val collections: List<HomeCollection> = emptyList(),
  val spotlights: List<HomeSpotlight> = emptyList(),
  val genresError: AppError? = null,
  val peopleError: AppError? = null,
  val trailersError: AppError? = null,
  val collectionsError: AppError? = null,
  val loadingSupplementals: Set<HomeSupplementalType> = emptySet(),
  val hasPartialFailures: Boolean,
) {
  fun section(type: HomeSectionType): HomeSection? =
    sections.firstOrNull { it.type == type }

  val hasVisibleContent: Boolean
    get() = sections.any { it.items.isNotEmpty() } ||
      genres.isNotEmpty() ||
      people.isNotEmpty() ||
      trailers.isNotEmpty() ||
      collections.isNotEmpty() ||
      spotlights.isNotEmpty()

  val hasPendingContent: Boolean
    get() = sections.any(HomeSection::isInitialLoading) || loadingSupplementals.isNotEmpty()

  fun withSection(updatedSection: HomeSection): HomeContent =
    copy(
      sections = sections.map { section ->
        if (section.type == updatedSection.type) updatedSection else section
      },
    )
}

/**
 * Limits repeated titles inside one rendered Home snapshot. The order of [sections]
 * is the presentation priority, so Hero and Trending keep the strongest candidates.
 */
internal fun HomeContent.enforceSessionMediaLimit(
  maximumAppearances: Int = 2,
): HomeContent {
  require(maximumAppearances > 0)
  val appearances = mutableMapOf<com.example.filmera.core.model.MediaKey, Int>()
  val filteredSections = sections.map { section ->
    section.copy(
      items = section.items.filter { item ->
        val count = appearances.getOrDefault(item.key, 0)
        if (count >= maximumAppearances) {
          false
        } else {
          appearances[item.key] = count + 1
          true
        }
      },
    )
  }
  return copy(sections = filteredSections)
}

internal fun selectHeroMedia(
  vararg sources: List<MediaItem>,
  limit: Int = 12,
): List<MediaItem> =
  selectDiverseMedia(
    sources = sources,
    limit = limit,
    requireBackdrop = true,
    requireOverview = true,
  )

internal fun selectDiverseMedia(
  vararg sources: List<MediaItem>,
  limit: Int,
  maxPerLanguage: Int = 3,
  requireBackdrop: Boolean = false,
  requireOverview: Boolean = false,
): List<MediaItem> {
  val candidates = sources
    .asSequence()
    .flatten()
    .filter { item ->
      item.id > 0 &&
        item.title.isNotBlank() &&
        !item.adult &&
        (!requireBackdrop || !item.backdropPath.isNullOrBlank()) &&
        (!requireOverview || item.overview.isNotBlank())
    }
    .distinctBy(MediaItem::key)
    .toList()

  val selected = mutableListOf<MediaItem>()
  val languageCounts = mutableMapOf<String, Int>()
  candidates.forEach { item ->
    val language = item.originalLanguage.ifBlank { "unknown" }
    if (languageCounts.getOrDefault(language, 0) < maxPerLanguage) {
      selected += item
      languageCounts[language] = languageCounts.getOrDefault(language, 0) + 1
    }
  }

  if (selected.size < limit) {
    selected += candidates.filter { candidate ->
      selected.none { it.key == candidate.key }
    }
  }
  return selected.take(limit)
}
