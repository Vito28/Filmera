package com.example.filmera.core.network.mapper

import com.example.filmera.core.model.MediaType
import com.example.filmera.core.network.dto.AggregateCastMemberDto
import com.example.filmera.core.network.dto.AggregateCastRoleDto
import com.example.filmera.core.network.dto.AggregateCreditsResponseDto
import com.example.filmera.core.network.dto.MovieDto
import com.example.filmera.core.network.dto.MultiSearchItemDto
import com.example.filmera.core.network.dto.SpokenLanguageDto
import com.example.filmera.core.network.dto.TvShowDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbMappersTest {
  @Test
  fun `movie dto maps movie-specific fields`() {
    val result = MovieDto(
      id = 42,
      title = "Arrival",
      originalTitle = "Arrival",
      releaseDate = "2016-11-11",
      voteAverage = 7.6,
    ).toDomain()

    assertEquals(42, result.id)
    assertEquals(MediaType.MOVIE, result.type)
    assertEquals("Arrival", result.title)
    assertEquals("2016-11-11", result.releaseDate)
    assertEquals(7.6, result.voteAverage, 0.0)
  }

  @Test
  fun `tv dto maps tv-specific names and air date`() {
    val result = TvShowDto(
      id = 84,
      name = "Dark",
      originalName = "Dark",
      firstAirDate = "2017-12-01",
    ).toDomain()

    assertEquals(84, result.id)
    assertEquals(MediaType.TV_SHOW, result.type)
    assertEquals("Dark", result.title)
    assertEquals("2017-12-01", result.releaseDate)
  }

  @Test
  fun `missing display name uses a safe fallback`() {
    assertEquals("Untitled movie", MovieDto(id = 1).toDomain().title)
    assertEquals("Untitled TV show", TvShowDto(id = 2).toDomain().title)
  }

  @Test
  fun `multi search maps movie and tv but ignores people`() {
    val movie = MultiSearchItemDto(
      mediaType = "movie",
      id = 1,
      title = "Arrival",
    ).toMediaItemOrNull()
    val tvShow = MultiSearchItemDto(
      mediaType = "tv",
      id = 2,
      name = "Dark",
    ).toMediaItemOrNull()
    val person = MultiSearchItemDto(
      mediaType = "person",
      id = 3,
      name = "Actor",
    ).toMediaItemOrNull()

    assertEquals(MediaType.MOVIE, movie?.type)
    assertEquals(MediaType.TV_SHOW, tvShow?.type)
    assertNull(person)
  }

  @Test
  fun `aggregate TV credits merge distinct roles for a cast member`() {
    val credits = AggregateCreditsResponseDto(
      cast = listOf(
        AggregateCastMemberDto(
          id = 7,
          name = "Performer",
          roles = listOf(
            AggregateCastRoleDto(character = "Hero"),
            AggregateCastRoleDto(character = "Narrator"),
            AggregateCastRoleDto(character = "Hero"),
          ),
        ),
      ),
    ).toDomain()

    assertEquals("Hero, Narrator", credits.cast.single().character)
  }

  @Test
  fun `default language label never falls back to native endonym`() {
    val language = SpokenLanguageDto(
      languageCode = "zh",
      englishName = null,
      name = "普通话",
    )

    assertEquals("Chinese", language.defaultDisplayName())
  }
}
