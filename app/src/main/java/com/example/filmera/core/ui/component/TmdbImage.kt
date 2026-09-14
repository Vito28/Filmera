package com.example.filmera.core.ui.component

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide
import java.net.URI

@Composable
fun TmdbImage(
  path: String?,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  size: String = "w500",
) {
  val imageContentDescription = contentDescription
  val imageUrl = remember(path, size) { resolveTmdbImageUrl(path, size) }
  val placeholderColor = MaterialTheme.colorScheme.surfaceContainerHighest
  val placeholderDrawable = remember(placeholderColor) {
    placeholderColor.toArgb().toDrawable()
  }

  AndroidView(
    modifier = modifier
      .background(placeholderColor)
      .semantics {
        imageContentDescription?.let { this.contentDescription = it }
      },
    factory = { context ->
      ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
      }
    },
    update = { imageView ->
      Glide.with(imageView)
        .load(imageUrl)
        .placeholder(placeholderDrawable)
        .fallback(placeholderDrawable)
        .error(placeholderDrawable)
        .centerCrop()
        .into(imageView)
    },
  )
}

internal fun resolveTmdbImageUrl(
  path: String?,
  requestedSize: String,
): String? {
  val value = path?.trim()?.takeIf(String::isNotEmpty) ?: return null
  val size = requestedSize.takeIf(TMDB_IMAGE_SIZE_PATTERN::matches) ?: DEFAULT_IMAGE_SIZE
  if (value.startsWith("/")) {
    return "$TMDB_IMAGE_BASE_URL$size$value"
  }

  val uri = runCatching { URI(value) }.getOrNull() ?: return null
  return value.takeIf {
    uri.scheme == "https" && uri.host == TMDB_IMAGE_HOST && uri.rawUserInfo == null
  }
}

private const val TMDB_IMAGE_HOST = "image.tmdb.org"
private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
private const val DEFAULT_IMAGE_SIZE = "w500"
private val TMDB_IMAGE_SIZE_PATTERN = Regex("(?:w|h)[1-9][0-9]{1,3}|original")
