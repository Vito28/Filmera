package com.example.filmera.feature.person.data

import com.example.filmera.core.model.MediaType
import com.example.filmera.core.network.dto.ExternalIdsDto
import com.example.filmera.core.network.dto.ImageAssetDto
import com.example.filmera.core.network.dto.PersonCombinedCreditsResponseDto
import com.example.filmera.core.network.dto.PersonCreditDto
import com.example.filmera.core.network.dto.PersonDetailsDto
import com.example.filmera.core.network.dto.PersonImagesResponseDto
import com.example.filmera.core.network.dto.TranslationDataDto
import com.example.filmera.core.network.dto.TranslationDto
import com.example.filmera.core.network.dto.TranslationResponseDto
import com.example.filmera.feature.person.domain.PersonGender
import org.junit.Assert.assertEquals
import org.junit.Test

class PersonMappersTest {
  @Test
  fun `person details map biography social ids photos and split filmography`() {
    val person = PersonDetailsDto(
      id = 31,
      name = "Performer",
      biography = " ",
      gender = 3,
      profilePath = "/main.jpg",
      combinedCredits = PersonCombinedCreditsResponseDto(
        cast = listOf(
          PersonCreditDto(
            id = 10,
            mediaType = "movie",
            title = "Movie role",
            character = "Lead",
            creditId = "movie-credit",
            releaseDate = "2024-01-01",
          ),
        ),
        crew = listOf(
          PersonCreditDto(
            id = 20,
            mediaType = "tv",
            name = "TV work",
            job = "Director",
            department = "Directing",
            creditId = "tv-credit",
            firstAirDate = "2023-01-01",
          ),
        ),
      ),
      externalIds = ExternalIdsDto(
        instagramId = "performer",
        facebookId = "performer.page",
      ),
      images = PersonImagesResponseDto(
        profiles = listOf(
          ImageAssetDto(filePath = "/secondary.jpg", voteAverage = 5.0),
          ImageAssetDto(filePath = "/main.jpg", voteAverage = 4.0),
        ),
      ),
      translations = TranslationResponseDto(
        translations = listOf(
          TranslationDto(
            languageCode = "en",
            data = TranslationDataDto(biography = "English biography"),
          ),
          TranslationDto(
            languageCode = "id",
            data = TranslationDataDto(biography = "Biografi Indonesia"),
          ),
        ),
      ),
    ).toDomain()

    assertEquals(PersonGender.NON_BINARY, person.gender)
    assertEquals("Biografi Indonesia", person.biography)
    assertEquals("performer", person.externalIds.instagramId)
    assertEquals(listOf("/main.jpg", "/secondary.jpg"), person.photos)
    assertEquals(MediaType.MOVIE, person.movieCredits.single().media.type)
    assertEquals("Lead", person.movieCredits.single().contribution)
    assertEquals(MediaType.TV_SHOW, person.tvCredits.single().media.type)
    assertEquals("Director", person.tvCredits.single().contribution)
  }

  @Test
  fun `unknown combined credit media types are ignored`() {
    val person = PersonDetailsDto(
      id = 1,
      name = "Person",
      combinedCredits = PersonCombinedCreditsResponseDto(
        cast = listOf(
          PersonCreditDto(id = 99, mediaType = "person", name = "Invalid"),
        ),
      ),
    ).toDomain()

    assertEquals(emptyList<Any>(), person.movieCredits)
    assertEquals(emptyList<Any>(), person.tvCredits)
  }
}
