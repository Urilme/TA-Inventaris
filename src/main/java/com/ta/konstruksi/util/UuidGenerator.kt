package com.ta.konstruksi.util

import java.util.UUID

object UuidGenerator {
    /** Generate UUID-based SKU untuk barang tanpa serial fisik */
    fun generateSkuId(): String = "SKU-${UUID.randomUUID().toString().take(8).uppercase()}"

    fun generateId(): String = UUID.randomUUID().toString()
}
