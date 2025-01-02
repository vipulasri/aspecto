/*
 * Copyright 2024 Vipul Asri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vipulasri.aspecto

/**
 * Represents a row in the AspectGrid layout.
 * Contains a list of items with their calculated dimensions.
 *
 * @param items List of layout items in this row
 */
internal data class AspectoRow(
    val items: List<AspectoLayoutInfo> = emptyList()
) {
    /** Unique identifier for the row, derived from item keys */
    val key: String
        get() = items.joinToString("-") { it.key?.toString() ?: it.hashCode().toString() }
}