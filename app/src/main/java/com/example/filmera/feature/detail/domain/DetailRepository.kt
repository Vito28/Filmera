package com.example.filmera.feature.detail.domain

import com.example.filmera.core.common.DataResult
import com.example.filmera.core.model.MediaKey

interface DetailRepository {
  suspend fun loadDetails(key: MediaKey): DataResult<DetailContent>
}
