package com.vipulasri.aspecto

import androidx.compose.runtime.Composable

/**
 * Created by Vipul Asri on 23/11/24.
 */

data class AspectoLayoutInfo(
    val aspectRatio: Float,
    val key: Any?,
    val contentType: Any?,
    val content: @Composable () -> Unit,
    var width: Int = 0,
    var height: Int = 0
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
}