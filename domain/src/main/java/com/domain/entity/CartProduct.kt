package com.domain.entity

data class CartProduct(
    val productVariant: ProductVariant,
    val title: String,
    val currency: String,
    val quantity: Int
)