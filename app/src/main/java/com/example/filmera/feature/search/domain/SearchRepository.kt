package com.example.filmera.feature.search.domain

import com.example.filmera.core.common.DataResult

interface SearchRepository {
  suspend fun loadDiscovery(): DataResult<SearchDiscovery>
  suspend fun search(request: SearchRequest): DataResult<SearchContent>
}

interface RecentSearchRepository {
  fun observeRecentSearches(): kotlinx.coroutines.flow.Flow<List<String>>
  suspend fun save(query: String)
  suspend fun remove(query: String)
  suspend fun clear()
}

