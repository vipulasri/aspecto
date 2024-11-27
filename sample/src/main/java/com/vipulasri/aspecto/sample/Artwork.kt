package com.vipulasri.aspecto.sample

import java.util.UUID

/**
 * Created by Vipul Asri on 26/11/24.
 */

data class Artwork(
    val id: String = UUID.randomUUID().toString(),
    val aspectRatio: Float,
    val imageUrl: String,
    val title: String
)
