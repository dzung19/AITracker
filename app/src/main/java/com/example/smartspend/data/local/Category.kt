package com.example.smartspend.data.local

import androidx.compose.ui.graphics.Color

enum class Category(val displayName: String, val color: Color) {
    FOOD("Food", Color(0xFFFF6B6B)),
    TRANSPORT("Transport", Color(0xFF4ECDC4)),
    SHOPPING("Shopping", Color(0xFFFFE66D)),
    ENTERTAINMENT("Entertainment", Color(0xFFA78BFA)),
    BILLS("Bills", Color(0xFFF472B6)),
    INVESTMENT("Investment", Color(0xFF22C55E)),
    OTHER("Other", Color(0xFF94A3B8));

    companion object {
        fun fromString(value: String): Category {
            return entries.find { it.displayName.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}
