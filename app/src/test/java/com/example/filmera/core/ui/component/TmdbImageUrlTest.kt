package com.example.filmera.core.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbImageUrlTest {
  @Test
  fun `relative TMDB path resolves against trusted image host`() {
    assertEquals(
      "https://image.tmdb.org/t/p/w342/poster.jpg",
      resolveTmdbImageUrl("/poster.jpg", "w342"),
    )
  }

  @Test
  fun `external and insecure image hosts are rejected`() {
    assertNull(resolveTmdbImageUrl("http://image.tmdb.org/t/p/w500/poster.jpg", "w500"))
    assertNull(resolveTmdbImageUrl("https://tracker.example/poster.jpg", "w500"))
  }

  @Test
  fun `unsupported size falls back to a known TMDB size`() {
    assertEquals(
      "https://image.tmdb.org/t/p/w500/poster.jpg",
      resolveTmdbImageUrl("/poster.jpg", "../../original"),
    )
  }
}
