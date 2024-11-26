package com.vipulasri.aspecto

/**
 * Created by Vipul Asri on 25/11/24.
 */

internal data class AspectoRow(
    val items: List<AspectoLayoutInfo> = emptyList()
) {
    val key: String
        get() = items.joinToString("-") { it.key?.toString() ?: it.hashCode().toString() }
}