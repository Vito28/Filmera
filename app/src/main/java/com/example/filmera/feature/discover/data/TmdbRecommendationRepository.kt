package com.example.filmera.feature.discover.data

import com.example.filmera.core.common.AppError
import com.example.filmera.core.common.DataResult
import com.example.filmera.core.database.RecommendationFeedbackDao
import com.example.filmera.core.database.RecommendationFeedbackEntity
import com.example.filmera.core.model.MediaItem
import com.example.filmera.core.model.MediaKey
import com.example.filmera.core.model.MediaType
import com.example.filmera.core.network.FilterCombination
import com.example.filmera.core.network.IdFilter
import com.example.filmera.core.network.MovieDiscoverQuery
import com.example.filmera.core.network.MovieSort
import com.example.filmera.core.network.TmdbCatalogApi
import com.example.filmera.core.network.TmdbConfig
import com.example.filmera.core.network.TmdbMovieApi
import com.example.filmera.core.network.TmdbPersonApi
import com.example.filmera.core.network.TmdbTvSeriesApi
import com.example.filmera.core.network.TvDiscoverQuery
import com.example.filmera.core.network.TvSort
import com.example.filmera.core.network.dto.MovieDto
import com.example.filmera.core.network.dto.PersonDto
import com.example.filmera.core.network.dto.PersonCreditDto
import com.example.filmera.core.network.dto.TvShowDto
import com.example.filmera.core.network.mapper.toDomain
import com.example.filmera.core.network.safeNetworkCall
import com.example.filmera.feature.discover.domain.OnboardingChoices
import com.example.filmera.feature.discover.domain.RecommendationCandidate
import com.example.filmera.feature.discover.domain.RecommendationEngine
import com.example.filmera.feature.discover.domain.RecommendationFeedbackAction
import com.example.filmera.feature.discover.domain.RecommendationRepository
import com.example.filmera.feature.discover.domain.RecommendationSection
import com.example.filmera.feature.discover.domain.RecommendationSectionType
import com.example.filmera.feature.discover.domain.RecommendationSource
import com.example.filmera.feature.discover.domain.RecommendedContent
import com.example.filmera.feature.discover.domain.RecommendedItem
import com.example.filmera.feature.discover.domain.RecommendedPerson
import com.example.filmera.feature.preferences.domain.MediaPreference
import com.example.filmera.feature.preferences.domain.UserPreferenceProfile
import com.example.filmera.feature.preferences.domain.supportedPreferenceGenres
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

class TmdbRecommendationRepository @Inject constructor(
  private val catalogApi: TmdbCatalogApi,
  private val movieApi: TmdbMovieApi,
  private val tvSeriesApi: TmdbTvSeriesApi,
  private val personApi: TmdbPersonApi,
  private val feedbackDao: RecommendationFeedbackDao,
  private val config: TmdbConfig,
) : RecommendationRepository {
  private val engine = RecommendationEngine()

  override suspend fun loadRecommendations(
    profile: UserPreferenceProfile,
    selectedGenreId: Int?,
    refreshGeneration: Int,
  ): DataResult<RecommendedContent> {
    if (!config.isConfigured) return DataResult.Error(AppError.MissingApiToken)

    val page = (refreshGeneration % REFRESH_PAGE_COUNT) + 1
    val preferredGenres = selectedGenreId?.let(::setOf)
      ?: profile.preferredGenreIds.takeIf(Set<Int>::isNotEmpty)
      ?: DEFAULT_GENRES
    val genreFilter = IdFilter(preferredGenres, FilterCombination.ANY)
    val preferredCountries = profile.preferredCountries
      .filter { it != UserPreferenceProfile.DEFAULT_COUNTRY && it != EUROPE_CODE }
      .take(PREFERRED_COUNTRY_REQUEST_LIMIT)

    return supervisorScope {
      val requests = buildList {
        add(async {
          movieCandidates(
            query = MovieDiscoverQuery(
              page = page,
              sortBy = MovieSort.POPULARITY_DESC,
              withGenres = genreFilter,
              minimumVoteCount = BASE_MINIMUM_VOTES,
            ),
            sources = setOf(RecommendationSource.PREFERRED_GENRE),
          )
        })
        add(async {
          tvCandidates(
            query = TvDiscoverQuery(
              page = page,
              sortBy = TvSort.POPULARITY_DESC,
              withGenres = genreFilter,
              minimumVoteCount = BASE_MINIMUM_VOTES,
            ),
            sources = setOf(
              RecommendationSource.PREFERRED_GENRE,
              RecommendationSource.SERIES,
            ),
          )
        })
        add(async {
          safeNetworkCall {
            catalogApi.getTopRatedMovies(page = page)
          }.toMovieCandidates(
            sources = setOf(
              RecommendationSource.GLOBAL,
              RecommendationSource.CRITICALLY_ACCLAIMED,
            ),
          )
        })
        add(async {
          safeNetworkCall {
            catalogApi.getTopRatedTvShows(page = page)
          }.toTvCandidates(
            sources = setOf(
              RecommendationSource.GLOBAL,
              RecommendationSource.SERIES,
              RecommendationSource.CRITICALLY_ACCLAIMED,
            ),
          )
        })
        add(async {
          safeNetworkCall {
            catalogApi.getTrendingMovies(page = page)
          }.toMovieCandidates(
            sources = setOf(
              RecommendationSource.GLOBAL,
              RecommendationSource.TRENDING,
            ),
          )
        })
        add(async {
          movieCandidates(
            query = MovieDiscoverQuery(
              page = page,
              sortBy = MovieSort.VOTE_AVERAGE_DESC,
              minimumVoteAverage = HIDDEN_GEM_MINIMUM_RATING,
              minimumVoteCount = HIDDEN_GEM_MINIMUM_VOTES,
              maximumVoteCount = HIDDEN_GEM_MAXIMUM_VOTES,
            ),
            sources = setOf(RecommendationSource.HIDDEN_GEM),
          )
        })
        if (MediaPreference.ANIME in profile.preferredMediaTypes) {
          add(async {
            tvCandidates(
              query = TvDiscoverQuery.japaneseAnime(page = page),
              sources = setOf(
                RecommendationSource.ANIME,
                RecommendationSource.SERIES,
              ),
            )
          })
        }
        preferredCountries.forEach { preferredCountry ->
          add(async {
            movieCandidates(
              query = MovieDiscoverQuery(
                page = page,
                sortBy = MovieSort.POPULARITY_DESC,
                withOriginCountry = preferredCountry,
                minimumVoteCount = COUNTRY_MINIMUM_VOTES,
              ),
              sources = setOf(RecommendationSource.PREFERRED_COUNTRY),
            )
          })
          add(async {
            tvCandidates(
              query = TvDiscoverQuery(
                page = page,
                sortBy = TvSort.POPULARITY_DESC,
                withOriginCountry = preferredCountry,
                minimumVoteCount = COUNTRY_MINIMUM_VOTES,
              ),
              sources = setOf(
                RecommendationSource.PREFERRED_COUNTRY,
                RecommendationSource.SERIES,
              ),
            )
          })
        }
        profile.preferredMovieIds.take(SEED_REQUEST_LIMIT).forEach { movieId ->
          add(async {
            safeNetworkCall {
              movieApi.getMovieRecommendations(movieId = movieId, page = page)
            }.toMovieCandidates(setOf(RecommendationSource.SELECTED_TITLE))
          })
        }
        profile.preferredTvIds.take(SEED_REQUEST_LIMIT).forEach { seriesId ->
          add(async {
            safeNetworkCall {
              tvSeriesApi.getTvRecommendations(seriesId = seriesId, page = page)
            }.toTvCandidates(
              setOf(
                RecommendationSource.SELECTED_TITLE,
                RecommendationSource.SERIES,
              ),
            )
          })
        }
        profile.preferredPersonIds.take(SEED_REQUEST_LIMIT).forEach { personId ->
          add(async { personCandidates(personId) })
        }
      }

      val candidateResults = requests.awaitAll()
      val candidates = candidateResults
        .filterIsInstance<DataResult.Success<List<RecommendationCandidate>>>()
        .flatMap { it.value }
      if (candidates.isEmpty()) {
        return@supervisorScope candidateResults
          .filterIsInstance<DataResult.Error>()
          .firstOrNull()
          ?: DataResult.Error(AppError.Unknown)
      }

      val feedback = getFeedback()
      val ranked = engine.rank(
        candidates = candidates,
        profile = profile,
        feedback = feedback,
        selectedGenreId = selectedGenreId,
      )
      val people = loadPeople(profile).valueOrEmpty()
      val content = buildContent(
        ranked = ranked,
        people = people,
        profile = profile,
        selectedGenreId = selectedGenreId,
      )
      if (content.hasContent) {
        DataResult.Success(content)
      } else {
        DataResult.Error(AppError.NotFound)
      }
    }
  }

  override suspend fun loadOnboardingChoices(): DataResult<OnboardingChoices> {
    if (!config.isConfigured) return DataResult.Error(AppError.MissingApiToken)

    return supervisorScope {
      val movies = async {
        safeNetworkCall { catalogApi.getTopRatedMovies(page = 1) }
      }
      val series = async {
        safeNetworkCall { catalogApi.getTopRatedTvShows(page = 1) }
      }
      val anime = async {
        safeNetworkCall {
          catalogApi.discoverTvShows(TvDiscoverQuery.japaneseAnime().toQueryMap())
        }
      }
      val people = async {
        safeNetworkCall { personApi.getPopularPeople(page = 1) }
      }

      val titleResults = listOf(
        movies.await().mapResults(MovieDto::toDomain),
        series.await().mapResults(TvShowDto::toDomain),
        anime.await().mapResults(TvShowDto::toDomain),
      )
      val titleGroups = titleResults
        .filterIsInstance<DataResult.Success<List<MediaItem>>>()
        .map { result ->
          result.value
            .filter { item -> item.isValidChoice() }
            .sortedWith(
              compareByDescending<MediaItem> { it.voteAverage }
                .thenByDescending(MediaItem::voteCount),
            )
        }
      val titles = interleaveMedia(titleGroups, ONBOARDING_TITLE_LIMIT)
      val peopleResult = people.await().mapResults { dto ->
        dto.toRecommendedPerson("Popular with movie and series fans")
      }

      if (titles.isEmpty()) {
        titleResults.filterIsInstance<DataResult.Error>().firstOrNull()
          ?: DataResult.Error(AppError.NotFound)
      } else {
        DataResult.Success(
          OnboardingChoices(
            titles = titles,
            people = peopleResult.valueOrEmpty()
              .filter { it.profilePath != null }
              .take(ONBOARDING_PERSON_LIMIT),
          ),
        )
      }
    }
  }

  override suspend fun getFeedback(): Map<MediaKey, RecommendationFeedbackAction> =
    feedbackDao.getAll().mapNotNull { entity ->
      val type = runCatching { MediaType.valueOf(entity.mediaType) }.getOrNull()
        ?: return@mapNotNull null
      val action = runCatching { RecommendationFeedbackAction.valueOf(entity.action) }
        .getOrNull()
        ?: return@mapNotNull null
      MediaKey(entity.mediaId, type) to action
    }.toMap()

  override suspend fun recordFeedback(
    media: MediaItem,
    action: RecommendationFeedbackAction,
  ) {
    feedbackDao.upsert(
      RecommendationFeedbackEntity(
        mediaId = media.id,
        mediaType = media.type.name,
        action = action.name,
        updatedAt = System.currentTimeMillis(),
      ),
    )
  }

  override suspend fun clearFeedback() {
    feedbackDao.clear()
  }

  private suspend fun movieCandidates(
    query: MovieDiscoverQuery,
    sources: Set<RecommendationSource>,
  ): DataResult<List<RecommendationCandidate>> =
    safeNetworkCall {
      catalogApi.discoverMovies(query.toQueryMap())
    }.toMovieCandidates(sources)

  private suspend fun tvCandidates(
    query: TvDiscoverQuery,
    sources: Set<RecommendationSource>,
  ): DataResult<List<RecommendationCandidate>> =
    safeNetworkCall {
      catalogApi.discoverTvShows(query.toQueryMap())
    }.toTvCandidates(sources)

  private suspend fun loadPeople(
    profile: UserPreferenceProfile,
  ): DataResult<List<RecommendedPerson>> =
    safeNetworkCall { personApi.getPopularPeople(page = 1) }
      .mapResults { person ->
        person.toRecommendedPerson(
          reason = if (person.id in profile.preferredPersonIds) {
            "One of the people you selected"
          } else {
            "Popular across stories you may enjoy"
          },
        )
      }

  private suspend fun personCandidates(
    personId: Int,
  ): DataResult<List<RecommendationCandidate>> =
    when (
      val result = safeNetworkCall {
        personApi.getPersonCombinedCredits(personId = personId)
      }
    ) {
      is DataResult.Success -> DataResult.Success(
        (result.value.cast + result.value.crew)
          .mapNotNull { credit -> credit.toCandidateOrNull() }
          .distinctBy { it.media.key },
      )
      is DataResult.Error -> result
    }

  private fun buildContent(
    ranked: List<RecommendedItem>,
    people: List<RecommendedPerson>,
    profile: UserPreferenceProfile,
    selectedGenreId: Int?,
  ): RecommendedContent {
    val topPicks = engine.diversify(ranked, TOP_PICK_LIMIT)
    val genreIds = selectedGenreId?.let(::listOf)
      ?: profile.preferredGenreIds.take(MAXIMUM_GENRE_SECTIONS)
    val genreSections = genreIds.mapNotNull { genreId ->
      val genre = supportedPreferenceGenres.firstOrNull { it.id == genreId } ?: return@mapNotNull null
      ranked
        .filter { genreId in it.media.genreIds }
        .take(SECTION_ITEM_LIMIT)
        .takeIf(List<RecommendedItem>::isNotEmpty)
        ?.let { items ->
          RecommendationSection(
            id = "genre-$genreId",
            type = RecommendationSectionType.GENRE,
            title = "Because you like ${genre.name}",
            subtitle = "Stories connected to one of your favorite genres",
            items = items,
          )
        }
    }

    val sections = buildList {
      addSection(
        id = "top-picks",
        type = RecommendationSectionType.TOP_PICKS,
        title = "Top picks for you",
        subtitle = "Selected from your preferences and quality signals",
        items = topPicks,
      )
      addAll(genreSections)
      addSection(
        id = "global",
        type = RecommendationSectionType.GLOBAL,
        title = "Global stories you may love",
        subtitle = "Quality picks from different cinema worlds",
        items = engine.diversify(
          ranked.filter { RecommendationSource.GLOBAL in it.sources },
          SECTION_ITEM_LIMIT,
        ),
      )
      if (MediaPreference.TV_SERIES in profile.preferredMediaTypes) {
        addSection(
          id = "series",
          type = RecommendationSectionType.SERIES,
          title = "Recommended series",
          subtitle = "Series matched to what you enjoy",
          items = ranked
            .filter { it.media.type == MediaType.TV_SHOW }
            .filterNot { item -> item.isAnime() }
            .take(SECTION_ITEM_LIMIT),
        )
      }
      if (MediaPreference.ANIME in profile.preferredMediaTypes) {
        addSection(
          id = "anime",
          type = RecommendationSectionType.ANIME,
          title = "Recommended anime",
          subtitle = "Animation from Japan based on your taste",
          items = ranked.filter { item -> item.isAnime() }.take(SECTION_ITEM_LIMIT),
        )
      }
      addSection(
        id = "critically-acclaimed",
        type = RecommendationSectionType.CRITICALLY_ACCLAIMED,
        title = "Critically acclaimed",
        subtitle = "High ratings backed by meaningful audience votes",
        items = ranked
          .filter { RecommendationSource.CRITICALLY_ACCLAIMED in it.sources }
          .take(SECTION_ITEM_LIMIT),
      )
      addSection(
        id = "hidden-gems",
        type = RecommendationSectionType.HIDDEN_GEMS,
        title = "Hidden gems for you",
        subtitle = "Excellent stories beyond the main spotlight",
        items = ranked
          .filter { RecommendationSource.HIDDEN_GEM in it.sources }
          .take(SECTION_ITEM_LIMIT),
      )
      addSection(
        id = "countries",
        type = RecommendationSectionType.PREFERRED_COUNTRIES,
        title = "From cinema worlds you like",
        subtitle = "A balanced mix of your country preferences and global discovery",
        items = ranked
          .filter { RecommendationSource.PREFERRED_COUNTRY in it.sources }
          .take(SECTION_ITEM_LIMIT),
      )
      addSection(
        id = "selected-title",
        type = RecommendationSectionType.SELECTED_TITLE,
        title = "Inspired by titles you love",
        subtitle = "Recommendations connected to your onboarding choices",
        items = ranked
          .filter { RecommendationSource.SELECTED_TITLE in it.sources }
          .take(SECTION_ITEM_LIMIT),
      )
    }

    return RecommendedContent(
      heroItems = topPicks.filter { it.media.backdropPath != null }.take(HERO_ITEM_LIMIT),
      sections = sections,
      people = people.take(PERSON_SECTION_LIMIT),
      availableGenres = profile.preferredGenreIds.sorted(),
    )
  }

  private fun MutableList<RecommendationSection>.addSection(
    id: String,
    type: RecommendationSectionType,
    title: String,
    subtitle: String,
    items: List<RecommendedItem>,
  ) {
    if (items.isNotEmpty()) {
      add(
        RecommendationSection(
          id = id,
          type = type,
          title = title,
          subtitle = subtitle,
          items = items,
        ),
      )
    }
  }

  private fun DataResult<com.example.filmera.core.network.dto.CatalogResponseDto<MovieDto>>
    .toMovieCandidates(
      sources: Set<RecommendationSource>,
    ): DataResult<List<RecommendationCandidate>> =
    mapResults { dto ->
      val media = dto.toDomain()
      RecommendationCandidate(
        media = media,
        originCountries = media.originCountries.toSet(),
        sources = sources,
      )
    }

  private fun DataResult<com.example.filmera.core.network.dto.CatalogResponseDto<TvShowDto>>
    .toTvCandidates(
      sources: Set<RecommendationSource>,
    ): DataResult<List<RecommendationCandidate>> =
    mapResults { dto ->
      val media = dto.toDomain()
      RecommendationCandidate(
        media = media,
        originCountries = media.originCountries.toSet(),
        sources = sources,
      )
    }

  private inline fun <T, R> DataResult<com.example.filmera.core.network.dto.CatalogResponseDto<T>>
    .mapResults(
      transform: (T) -> R,
    ): DataResult<List<R>> =
    when (this) {
      is DataResult.Success -> DataResult.Success(value.results.map(transform))
      is DataResult.Error -> this
    }

  private fun PersonDto.toRecommendedPerson(reason: String): RecommendedPerson =
    RecommendedPerson(
      id = id,
      name = name,
      profilePath = profilePath,
      knownForDepartment = knownForDepartment.orEmpty().ifBlank { "Artist" },
      knownFor = knownFor.mapNotNull { item -> item.title ?: item.name }.take(2),
      reason = reason,
    )

  private fun PersonCreditDto.toCandidateOrNull(): RecommendationCandidate? {
    val media = when (mediaType) {
      "movie" -> MovieDto(
        id = id,
        title = title,
        originalTitle = originalTitle,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        voteCount = voteCount,
        popularity = popularity,
        adult = adult,
        originalLanguage = originalLanguage,
        genreIds = genreIds,
      ).toDomain()
      "tv" -> TvShowDto(
        id = id,
        name = name,
        originalName = originalName,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        firstAirDate = firstAirDate,
        voteAverage = voteAverage,
        voteCount = voteCount,
        popularity = popularity,
        adult = adult,
        originalLanguage = originalLanguage,
        genreIds = genreIds,
      ).toDomain()
      else -> return null
    }
    if (media.id <= 0) return null
    return RecommendationCandidate(
      media = media,
      originCountries = media.originCountries.toSet(),
      sources = setOf(RecommendationSource.PREFERRED_PERSON),
    )
  }

  private fun MediaItem.isValidChoice(): Boolean =
    id > 0 && title.isNotBlank() && posterPath != null && !adult && voteCount >= BASE_MINIMUM_VOTES

  private fun interleaveMedia(
    groups: List<List<MediaItem>>,
    limit: Int,
  ): List<MediaItem> {
    val selected = mutableListOf<MediaItem>()
    var index = 0
    while (selected.size < limit && groups.any { index < it.size }) {
      groups.forEach { group ->
        group.getOrNull(index)?.let { item ->
          if (selected.none { it.key == item.key } && selected.size < limit) {
            selected += item
          }
        }
      }
      index += 1
    }
    return selected
  }

  private fun RecommendedItem.isAnime(): Boolean =
    media.type == MediaType.TV_SHOW &&
      ANIMATION_GENRE_ID in media.genreIds &&
      media.originalLanguage == JAPANESE_LANGUAGE

  private fun <T> DataResult<List<T>>.valueOrEmpty(): List<T> =
    (this as? DataResult.Success)?.value.orEmpty()

  private companion object {
    const val REFRESH_PAGE_COUNT = 3
    const val BASE_MINIMUM_VOTES = 100
    const val COUNTRY_MINIMUM_VOTES = 50
    const val HIDDEN_GEM_MINIMUM_RATING = 7.0
    const val HIDDEN_GEM_MINIMUM_VOTES = 150
    const val HIDDEN_GEM_MAXIMUM_VOTES = 5_000
    const val SEED_REQUEST_LIMIT = 2
    const val PREFERRED_COUNTRY_REQUEST_LIMIT = 2
    const val TOP_PICK_LIMIT = 10
    const val SECTION_ITEM_LIMIT = 10
    const val HERO_ITEM_LIMIT = 5
    const val PERSON_SECTION_LIMIT = 12
    const val MAXIMUM_GENRE_SECTIONS = 2
    const val ONBOARDING_TITLE_LIMIT = 30
    const val ONBOARDING_PERSON_LIMIT = 20
    const val ANIMATION_GENRE_ID = 16
    const val JAPANESE_LANGUAGE = "ja"
    const val EUROPE_CODE = "EU"

    val DEFAULT_GENRES = setOf(18, 28, 35)
  }
}
