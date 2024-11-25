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

    internal val items = mutableListOf<AspectoLayoutInfo>()

    fun item(
        aspectRatio: Float,
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable () -> Unit
    ) {
        items.add(
            AspectoLayoutInfo(
                aspectRatio = aspectRatio,
                key = key,
                contentType = contentType,
                content = content
            )
        )
    }
}