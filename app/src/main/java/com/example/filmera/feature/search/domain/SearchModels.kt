package com.example.filmera.feature.search.domain

import com.example.filmera.core.model.MediaItem

enum class SearchType {
  ALL,
  MOVIES,
  TV_SHOWS,
  PEOPLE,
  COLLECTIONS,
}

data class SearchRequest(
  val query: String,
  val type: SearchType = SearchType.ALL,
  val page: Int = 1,
)

data class SearchPerson(
  val id: Int,
  val name: String,
  val knownForDepartment: String,
  val profilePath: String?,
  val knownFor: List<String>,
  val popularity: Double,
)

data class SearchCollection(
  val id: Int,
  val name: String,
  val overview: String,
  val posterPath: String?,
  val backdropPath: String?,
  val popularity: Double,
)

data class SearchGenre(
  val id: Int,
  val name: String,
  val titleCount: Int,
  val backdropPath: String?,
)

data class SearchDiscovery(
  val trendingQueries: List<String>,
  val genres: List<SearchGenre>,
  val popularPeople: List<SearchPerson>,
  val hasPartialFailures: Boolean,
) {
  val hasVisibleContent: Boolean
    get() = trendingQueries.isNotEmpty() || genres.isNotEmpty() || popularPeople.isNotEmpty()
}

sealed interface SearchTopResult {
  val title: String

  data class Media(
    val item: MediaItem,
  ) : SearchTopResult {
    override val title: String = item.title
  }

  data class Person(
    val person: SearchPerson,
  ) : SearchTopResult {
    override val title: String = person.name
  }

  data class Collection(
    val collection: SearchCollection,
  ) : SearchTopResult {
    override val title: String = collection.name
  }
}

data class SearchContent(
  val query: String,
  val selectedType: SearchType,
  val movies: List<MediaItem> = emptyList(),
  val tvShows: List<MediaItem> = emptyList(),
  val people: List<SearchPerson> = emptyList(),
  val collections: List<SearchCollection> = emptyList(),
  val topResult: SearchTopResult? = null,
  val page: Int = 1,
  val totalPages: Int = 1,
  val hasPartialFailures: Boolean = false,
) {
  val isEmpty: Boolean
    get() = movies.isEmpty() &&
      tvShows.isEmpty() &&
      people.isEmpty() &&
      collections.isEmpty()

  val canLoadMore: Boolean
    get() = selectedType != SearchType.ALL && page < totalPages
}

