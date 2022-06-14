/*
 * Copyright 2022 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartregister.p2p.sync

import androidx.annotation.NonNull

data class DataType(@NonNull val name: String, @NonNull val type: Filetype, val position: Int) :
  Comparable<DataType> {

  enum class Filetype(s: String) {
    JSON("text/json"),
    PNG("image/png"),
    JPEG("image/jpeg")
  }

  override fun compareTo(other: DataType): Int = position.compareTo(other.position)
}
