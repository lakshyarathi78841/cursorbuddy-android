package com.cursorbuddy.android.service

import com.cursorbuddy.android.model.UiNode
import org.json.JSONArray
import org.json.JSONObject

object UiTreeSerializer {

    fun toJson(node: UiNode, maxDepth: Int = 5): String {
        val array = JSONArray()
        flattenToJson(node, array, 0, maxDepth)
        return array.toString(2)
    }

    fun toCompactJson(node: UiNode, maxElements: Int = 50): String {
        val array = JSONArray()
        val elements = node.flatten()
            .filter { isInteresting(it) }
            .take(maxElements)
        
        for (el in elements) {
            val obj = JSONObject().apply {
                if (el.text != null) put("text", el.text)
                if (el.contentDescription != null) put("desc", el.contentDescription)
                if (el.viewId != null) put("id", el.viewId.substringAfterLast("/"))
                put("type", el.className.substringAfterLast("."))
                put("bounds", JSONArray().apply {
                    put(el.bounds.left)
                    put(el.bounds.top)
                    put(el.bounds.right)
                    put(el.bounds.bottom)
                })
                if (el.isClickable) put("clickable", true)
                if (el.isScrollable) put("scrollable", true)
                if (el.isEditable) put("editable", true)
                if (el.isPassword) put("password", true)
                if (el.isChecked != null) put("checked", el.isChecked)
            }
            array.put(obj)
        }
        
        return array.toString()
    }

    private fun flattenToJson(node: UiNode, array: JSONArray, depth: Int, maxDepth: Int) {
        if (depth > maxDepth) return
        if (!isInteresting(node) && node.children.isEmpty()) return

        if (isInteresting(node)) {
            val obj = JSONObject().apply {
                if (node.text != null) put("text", node.text)
                if (node.contentDescription != null) put("desc", node.contentDescription)
                if (node.viewId != null) put("id", node.viewId.substringAfterLast("/"))
                put("type", node.className.substringAfterLast("."))
                put("bounds", JSONArray().apply {
                    put(node.bounds.left)
                    put(node.bounds.top)
                    put(node.bounds.right)
                    put(node.bounds.bottom)
                })
                if (node.isClickable) put("clickable", true)
                if (node.isScrollable) put("scrollable", true)
                if (node.isEditable) put("editable", true)
            }
            array.put(obj)
        }

        for (child in node.children) {
            flattenToJson(child, array, depth + 1, maxDepth)
        }
    }

    private fun isInteresting(node: UiNode): Boolean {
        // Skip invisible or zero-sized elements
        if (node.bounds.width() <= 0 || node.bounds.height() <= 0) return false
        
        // Has text or description
        if (node.text?.isNotBlank() == true) return true
        if (node.contentDescription?.isNotBlank() == true) return true
        
        // Is interactive
        if (node.isClickable || node.isScrollable || node.isEditable) return true
        
        return false
    }
}
