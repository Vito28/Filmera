package com.example.filmera.feature.home.domain

enum class HomeBrowseKind(
  val sectionType: HomeSectionType? = null,
) {
  NOW_PLAYING(HomeSectionType.HERO),
  TRENDING(HomeSectionType.TRENDING),
  TOP_PICKS(HomeSectionType.EDITORS_PICKS),
  UPCOMING(HomeSectionType.UPCOMING),
  EDITORS_PICKS(HomeSectionType.EDITORS_PICKS),
  TOP_RATED(HomeSectionType.TOP_RATED),
  HIDDEN_GEMS(HomeSectionType.HIDDEN_GEMS),
  RECOMMENDATIONS(HomeSectionType.POPULAR_WORLDWIDE),
  GENRES,
  PEOPLE,
  SPOTLIGHTS,
  TRAILERS,
  COLLECTIONS,
  ;

  val isMediaList: Boolean
    get() = sectionType != null
}
