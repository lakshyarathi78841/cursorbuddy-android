package com.cursorbuddy.android.model

import android.graphics.Rect

data class UiNode(
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val viewId: String?,
    val bounds: Rect,
    val isClickable: Boolean,
    val isScrollable: Boolean,
    val isEditable: Boolean,
    val isPassword: Boolean,
    val isChecked: Boolean?,
    val children: List<UiNode> = emptyList()
) {
    val label: String
        get() = text ?: contentDescription ?: viewId ?: className

    fun flatten(): List<UiNode> {
        return listOf(this) + children.flatMap { it.flatten() }
    }

    fun findByText(query: String, ignoreCase: Boolean = true): UiNode? {
        if (text?.contains(query, ignoreCase) == true) return this
        if (contentDescription?.contains(query, ignoreCase) == true) return this
        return children.firstNotNullOfOrNull { it.findByText(query, ignoreCase) }
    }
}
