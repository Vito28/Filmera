package com.example.filmera.feature.home.domain

import com.example.filmera.core.common.DataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface HomeRepository {
  suspend fun loadHomeContent(feedKey: HomeFeedKey): DataResult<HomeContent>

  /**
   * Emits usable Home snapshots as soon as each fetch phase completes. Repositories
   * that do not support progressive loading retain the single-result behavior.
   */
  fun observeHomeContent(feedKey: HomeFeedKey): Flow<DataResult<HomeContent>> = flow {
    emit(loadHomeContent(feedKey))
  }

  suspend fun loadSectionPage(
    feedKey: HomeFeedKey,
    sectionType: HomeSectionType,
    page: Int,
  ): DataResult<HomeSectionPage>
}
