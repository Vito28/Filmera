package com.example.filmera.core.network

import java.lang.reflect.Method
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.QueryMap

class TmdbApiContractTest {
  @Test
  fun `resource interfaces expose the expected TMDB paths`() {
    assertEndpoint(TmdbMovieApi::class.java, "getMovieDetails", "movie/{movie_id}")
    assertEndpoint(
      TmdbMovieApi::class.java,
      "getMovieTranslations",
      "movie/{movie_id}/translations",
    )
    assertEndpoint(
      TmdbTvSeriesApi::class.java,
      "getTvAggregateCredits",
      "tv/{series_id}/aggregate_credits",
    )
    assertEndpoint(
      TmdbTvSeriesApi::class.java,
      "getTvContentRatings",
      "tv/{series_id}/content_ratings",
    )
    assertEndpoint(
      TmdbTvSeriesApi::class.java,
      "getTvEpisodeGroups",
      "tv/{series_id}/episode_groups",
    )
    assertEndpoint(
      TmdbTvSeasonApi::class.java,
      "getSeasonDetails",
      "tv/{series_id}/season/{season_number}",
    )
    assertEndpoint(
      TmdbTvEpisodeApi::class.java,
      "getEpisodeDetails",
      "tv/{series_id}/season/{season_number}/episode/{episode_number}",
    )
    assertEndpoint(
      TmdbTvEpisodeApi::class.java,
      "getEpisodeGroupDetails",
      "tv/episode_group/{episode_group_id}",
    )
    assertEndpoint(
      TmdbPersonApi::class.java,
      "getPersonDetails",
      "person/{person_id}",
    )
    assertEndpoint(
      TmdbPersonApi::class.java,
      "getPersonCombinedCredits",
      "person/{person_id}/combined_credits",
    )
    assertEndpoint(
      TmdbPersonApi::class.java,
      "getPersonExternalIds",
      "person/{person_id}/external_ids",
    )
  }

  @Test
  fun `discover endpoints accept typed query maps`() {
    assertHasQueryMap(method(TmdbCatalogApi::class.java, "discoverMovies"))
    assertHasQueryMap(method(TmdbCatalogApi::class.java, "discoverTvShows"))
  }

  @Test
  fun `language configuration and translations are modeled separately`() {
    assertEndpoint(
      TmdbCatalogApi::class.java,
      "getLanguages",
      "configuration/languages",
    )
    assertEndpoint(
      TmdbCatalogApi::class.java,
      "getPrimaryTranslations",
      "configuration/primary_translations",
    )
    assertEndpoint(
      TmdbTvSeasonApi::class.java,
      "getSeasonTranslations",
      "tv/{series_id}/season/{season_number}/translations",
    )
  }

  @Test
  fun `detail append groups stay below the TMDB limit`() {
    val groups = listOf(
      TmdbAppendToResponse.MOVIE_DETAILS,
      TmdbAppendToResponse.TV_DETAILS,
      TmdbAppendToResponse.SEASON_DETAILS,
      TmdbAppendToResponse.EPISODE_DETAILS,
      TmdbAppendToResponse.PERSON_DETAILS,
    )

    assertTrue(groups.all { group -> group.split(',').size <= 20 })
  }

  private fun assertEndpoint(
    service: Class<*>,
    methodName: String,
    expectedPath: String,
  ) {
    val get = requireNotNull(
      method(service, methodName).getAnnotation(GET::class.java),
    ) {
      "Missing @GET on $methodName"
    }
    assertEquals(expectedPath, get.value)
  }

  private fun assertHasQueryMap(method: Method) {
    val hasQueryMap = method.parameterAnnotations
      .flatMap(Array<Annotation>::asIterable)
      .any { annotation -> annotation is QueryMap }
    assertTrue("Missing @QueryMap on ${method.name}", hasQueryMap)
  }

  private fun method(service: Class<*>, name: String): Method =
    service.methods.single { method -> method.name == name }
}
