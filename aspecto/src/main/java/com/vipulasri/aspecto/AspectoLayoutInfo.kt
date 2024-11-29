package com.vipulasri.aspecto

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Created by Vipul Asri on 23/11/24.
 */

@Stable
data class AspectoLayoutInfo(
    val aspectRatio: Float,
    val key: Any?,
    val contentType: Any?,
    val content: @Composable () -> Unit,
    val width: Int = 0,
    val height: Int = 0
)

class AspectoLayoutScope {
    private val _items = mutableListOf<AspectoLayoutInfo>()
    val items: List<AspectoLayoutInfo> = _items

    fun item(
        aspectRatio: Float,
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable () -> Unit
    ) {
        validate(aspectRatio)
        _items.add(
            AspectoLayoutInfo(
                aspectRatio = aspectRatio,
                key = key,
                contentType = contentType,
                content = content
            )
        )
    }

    fun <T> items(
        items: List<T>,
        key: ((item: T) -> Any)? = null,
        aspectRatio: (T) -> Float,
        contentType: ((item: T) -> Any)? = null,
        itemContent: @Composable (T) -> Unit
    ) {
        items.forEach { item ->
            validate(aspectRatio(item))
            _items.add(
                AspectoLayoutInfo(
                    key = key?.invoke(item),
                    aspectRatio = aspectRatio(item),
                    contentType = contentType?.invoke(item),
                    content = { itemContent(item) }
                )
            )
        }
    }

    private fun validate(aspectRatio: Float, key: Any? = null) {
        require(aspectRatio > 0f) {
            buildString {
                append("Aspect ratio must be positive and greater than zero (got $aspectRatio)")
                if (key != null) append(" for key: $key")
            }
        }
    }
}