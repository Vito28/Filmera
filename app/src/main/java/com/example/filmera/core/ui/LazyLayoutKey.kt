package com.example.filmera.core.ui

import com.example.filmera.core.model.MediaItem

/**
 * Creates stable String keys for lazy layouts and pagers.
 *
 * Length-prefixed identity parts prevent ambiguous keys when a value contains
 * the separator itself. Returning [String] also keeps every key compatible
 * with Android saved-state Bundles.
 */
object LazyLayoutKey {
  fun media(
    scope: String,
    item: MediaItem,
  ): String = of(
    scope,
    item.type.routeValue,
    item.id.toString(),
  )

  fun indexed(
    scope: String,
    index: Int,
  ): String = of(scope, index.toString())

  fun identified(
    scope: String,
    id: Int,
  ): String = of(scope, id.toString())

  fun of(
    scope: String,
    vararg identityParts: String,
  ): String = buildString {
    appendPart(scope)
    identityParts.forEach { identityPart ->
      appendPart(identityPart)
    }
  }

  private fun StringBuilder.appendPart(value: String) {
    if (isNotEmpty()) append(KEY_PART_SEPARATOR)
    append(value.length)
    append(KEY_LENGTH_SEPARATOR)
    append(value)
  }

  private const val KEY_PART_SEPARATOR = '|'
  private const val KEY_LENGTH_SEPARATOR = ':'
}
