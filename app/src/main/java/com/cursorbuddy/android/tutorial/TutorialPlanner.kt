package com.cursorbuddy.android.tutorial

import android.graphics.RectF
import com.cursorbuddy.android.model.*
import com.cursorbuddy.android.service.AppCategory

object TutorialPlanner {

    fun planTutorial(question: String, packageName: String?, uiTree: UiNode?): Tutorial? {
        val q = question.lowercase().trim()
        
        // Phase 1: Try to match known patterns using UI tree + question
        val matched = matchQuestionToUiTree(q, packageName, uiTree)
        if (matched != null) return matched
        
        // Phase 2: Generate from UI tree exploration
        return generateSmartTutorial(q, packageName, uiTree)
    }
    
    // Smarter version that uses the actual UI tree to find real elements
    fun planTutorialWithContext(question: String, packageName: String?, uiTree: UiNode?, category: AppCategory?): Tutorial? {
        val q = question.lowercase().trim()
        
        // Try pattern matching first
        val matched = matchQuestionToUiTree(q, packageName, uiTree)
        if (matched != null) return matched
        
        // Generate smart tutorial
        return generateSmartTutorial(q, packageName, uiTree)
    }
    
    private fun matchQuestionToUiTree(question: String, packageName: String?, uiTree: UiNode?): Tutorial? {
        if (uiTree == null) return null
        val allNodes = uiTree.flatten()
        
        // WiFi related
        if (question.containsAny("wifi", "wi-fi", "internet", "network", "connect")) {
            return findAndBuildTutorial(
                id = "wifi_connect",
                appPackage = packageName ?: "com.android.settings",
                question = question,
                uiTree = uiTree,
                allNodes = allNodes,
                searchTerms = listOf(
                    "Network" to "Tap Network & internet",
                    "Wi-Fi" to "Tap Wi-Fi",
                    "Internet" to "Tap Internet",
                    "Wi‑Fi" to "Tap Wi-Fi to see available networks"
                ),
                fallbackCaption = "Look for Network or Wi-Fi in Settings"
            )
        }
        
        // Bluetooth
        if (question.containsAny("bluetooth", "pair", "connect device")) {
            return findAndBuildTutorial(
                id = "bluetooth",
                appPackage = packageName ?: "com.android.settings",
                question = question,
                uiTree = uiTree,
                allNodes = allNodes,
                searchTerms = listOf(
                    "Bluetooth" to "Tap Bluetooth",
                    "Connected devices" to "Tap Connected devices",
                    "Pair new device" to "Tap Pair new device"
                ),
                fallbackCaption = "Look for Bluetooth or Connected devices"
            )
        }
        
        // Bookmark (browser)
        if (question.containsAny("bookmark", "save page", "favorite")) {
            return findAndBuildTutorial(
                id = "bookmark",
                appPackage = packageName ?: "com.android.chrome",
                question = question,
                uiTree = uiTree,
                allNodes = allNodes,
                searchTerms = listOf(
                    "More options" to "Tap the menu button",
                    "⋮" to "Tap the three-dot menu",
                    "Bookmark" to "Tap Bookmark to save this page",
                    "Star" to "Tap the star to bookmark"
                ),
                fallbackCaption = "Look for the menu or star icon"
            )
        }
        
        // New tab
        if (question.containsAny("new tab", "open tab", "tab")) {
            return findAndBuildTutorial(
                id = "new_tab",
                appPackage = packageName ?: "com.android.chrome",
                question = question,
                uiTree = uiTree,
                allNodes = allNodes,
                searchTerms = listOf(
                    "Switch or close tabs" to "Tap the tab switcher",
                    "New tab" to "Tap New tab",
                    "Tabs" to "Tap to see your tabs"
                ),
                fallbackCaption = "Look for the tab button (shows a number)"
            )
        }
        
        // Send message / photo in messaging
        if (question.containsAny("send photo", "send image", "send picture", "attach")) {
            return findAndBuildTutorial(
                id = "send_photo",
                appPackage = packageName ?: "",
                question = question,
                uiTree = uiTree,
                allNodes = allNodes,
                searchTerms = listOf(
                    "Attach" to "Tap the attachment button",
                    "📎" to "Tap the paperclip icon",
                    "Camera" to "Tap Camera to take a photo",
                    "Gallery" to "Tap Gallery to choose a photo",
                    "Photo" to "Tap to add a photo",
                    "Image" to "Tap to select an image"
                ),
                fallbackCaption = "Look for an attachment or camera button"
            )
        }
        
        // Search
        if (question.containsAny("search", "find", "look for")) {
            return findAndBuildTutorial(
                id = "search",
                appPackage = packageName ?: "",
                question = question,
                uiTree = uiTree,
                allNodes = allNodes,
                searchTerms = listOf(
                    "Search" to "Tap the search bar",
                    "🔍" to "Tap the search icon",
                    "Search or type" to "Tap here to search"
                ),
                fallbackCaption = "Look for a search bar or magnifying glass icon"
            )
        }
        
        // Settings / preferences in any app
        if (question.containsAny("settings", "preferences", "options", "configure")) {
            return findAndBuildTutorial(
                id = "app_settings",
                appPackage = packageName ?: "",
                question = question,
                uiTree = uiTree,
                allNodes = allNodes,
                searchTerms = listOf(
                    "Settings" to "Tap Settings",
                    "More options" to "Tap the menu for options",
                    "⋮" to "Tap the three-dot menu",
                    "⚙" to "Tap the gear icon",
                    "Preferences" to "Tap Preferences",
                    "Profile" to "Tap your profile for settings"
                ),
                fallbackCaption = "Look for a gear icon or menu button"
            )
        }
        
        // Show me around / overview
        if (question.containsAny("show me around", "overview", "what can", "tour", "explore")) {
            return generateAppTour(packageName, uiTree, allNodes)
        }
        
        // Help / navigate
        if (question.containsAny("help", "navigate", "how do i", "where is")) {
            // Try to extract the key noun from the question and find it
            val keywords = extractKeywords(question)
            for (keyword in keywords) {
                val node = findNodeByText(allNodes, keyword)
                if (node != null) {
                    return Tutorial(
                        id = "found_${keyword}",
                        appPackage = packageName ?: "",
                        question = question,
                        steps = listOf(
                            TutorialStep(
                                stepNumber = 1,
                                totalSteps = 1,
                                action = if (node.isClickable) StepAction.TAP else StepAction.WAIT,
                                targetBounds = RectF(node.bounds),
                                targetDescription = node.label,
                                caption = "Here it is! \"${node.label}\" — tap it"
                            )
                        )
                    )
                }
            }
        }
        
        return null
    }
    
    private fun findAndBuildTutorial(
        id: String,
        appPackage: String,
        question: String,
        uiTree: UiNode,
        allNodes: List<UiNode>,
        searchTerms: List<Pair<String, String>>,
        fallbackCaption: String
    ): Tutorial? {
        val steps = mutableListOf<TutorialStep>()
        
        for ((term, caption) in searchTerms) {
            val node = findNodeByText(allNodes, term)
            if (node != null) {
                steps.add(TutorialStep(
                    stepNumber = steps.size + 1,
                    totalSteps = 0, // updated after
                    action = if (node.isClickable) StepAction.TAP 
                             else if (node.isScrollable) StepAction.SCROLL 
                             else StepAction.TAP,
                    targetBounds = RectF(node.bounds),
                    targetDescription = node.label,
                    caption = caption
                ))
            }
        }
        
        if (steps.isEmpty()) return null
        
        // Update totalSteps
        val total = steps.size
        val finalSteps = steps.mapIndexed { i, step ->
            step.copy(stepNumber = i + 1, totalSteps = total)
        }
        
        return Tutorial(
            id = id,
            appPackage = appPackage,
            question = question,
            steps = finalSteps
        )
    }
    
    private fun generateAppTour(packageName: String?, uiTree: UiNode?, allNodes: List<UiNode>): Tutorial? {
        if (allNodes.isEmpty()) return null
        
        // Find the most interesting clickable elements for a tour
        val interesting = allNodes
            .filter { it.isClickable && it.label.isNotBlank() && it.label != "Unknown" }
            .filter { it.bounds.width() > 20 && it.bounds.height() > 20 } // skip tiny elements
            .distinctBy { it.label }
            .take(6)
        
        if (interesting.isEmpty()) return null
        
        val steps = interesting.mapIndexed { index, node ->
            val caption = when (index) {
                0 -> "Let's start here — \"${node.label}\""
                interesting.size - 1 -> "And finally, \"${node.label}\""
                else -> "Here's \"${node.label}\""
            }
            TutorialStep(
                stepNumber = index + 1,
                totalSteps = interesting.size,
                action = StepAction.TAP,
                targetBounds = RectF(node.bounds),
                targetDescription = node.label,
                caption = caption
            )
        }
        
        return Tutorial(
            id = "tour_${System.currentTimeMillis()}",
            appPackage = packageName ?: "unknown",
            question = "Show me around",
            steps = steps,
            source = TutorialSource.AI_GENERATED
        )
    }
    
    private fun generateSmartTutorial(question: String, packageName: String?, uiTree: UiNode?): Tutorial? {
        if (uiTree == null) return null
        val allNodes = uiTree.flatten()
        
        // Try to find elements matching keywords in the question
        val keywords = extractKeywords(question)
        val matchedNodes = mutableListOf<UiNode>()
        
        for (keyword in keywords) {
            val node = findNodeByText(allNodes, keyword)
            if (node != null && node !in matchedNodes) {
                matchedNodes.add(node)
            }
        }
        
        // If we found matches, build a tutorial from them
        if (matchedNodes.isNotEmpty()) {
            val steps = matchedNodes.mapIndexed { index, node ->
                TutorialStep(
                    stepNumber = index + 1,
                    totalSteps = matchedNodes.size,
                    action = if (node.isClickable) StepAction.TAP else StepAction.WAIT,
                    targetBounds = RectF(node.bounds),
                    targetDescription = node.label,
                    caption = if (index == 0) "I found \"${node.label}\" — tap here"
                              else "Then \"${node.label}\""
                )
            }
            
            return Tutorial(
                id = "smart_${System.currentTimeMillis()}",
                appPackage = packageName ?: "unknown",
                question = question,
                steps = steps,
                source = TutorialSource.AI_GENERATED
            )
        }
        
        // Fallback: show clickable elements
        val clickables = allNodes
            .filter { it.isClickable && it.label.isNotBlank() && it.label != "Unknown" }
            .filter { it.bounds.width() > 20 && it.bounds.height() > 20 }
            .distinctBy { it.label }
            .take(5)
        
        if (clickables.isEmpty()) return null
        
        val steps = clickables.mapIndexed { index, node ->
            TutorialStep(
                stepNumber = index + 1,
                totalSteps = clickables.size,
                action = StepAction.TAP,
                targetBounds = RectF(node.bounds),
                targetDescription = node.label,
                caption = if (index == 0) "I see \"${node.label}\" — you could try this"
                          else "Also \"${node.label}\""
            )
        }
        
        return Tutorial(
            id = "explore_${System.currentTimeMillis()}",
            appPackage = packageName ?: "unknown",
            question = question,
            steps = steps,
            source = TutorialSource.AI_GENERATED
        )
    }
    
    private fun findNodeByText(nodes: List<UiNode>, query: String): UiNode? {
        // Exact match first
        nodes.firstOrNull { 
            it.text?.equals(query, ignoreCase = true) == true ||
            it.contentDescription?.equals(query, ignoreCase = true) == true
        }?.let { return it }
        
        // Contains match
        return nodes.firstOrNull {
            it.text?.contains(query, ignoreCase = true) == true ||
            it.contentDescription?.contains(query, ignoreCase = true) == true
        }
    }
    
    private fun extractKeywords(question: String): List<String> {
        val stopWords = setOf(
            "how", "do", "i", "can", "the", "a", "an", "to", "in", "on", "for",
            "is", "it", "my", "me", "this", "that", "what", "where", "when",
            "show", "help", "find", "get", "set", "up", "make", "use", "open",
            "please", "want", "need", "would", "like", "could", "should"
        )
        
        return question.lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .split(" +".toRegex())
            .filter { it.length > 2 && it !in stopWords }
            .distinct()
    }
    
    private fun String.containsAny(vararg terms: String): Boolean {
        return terms.any { this.contains(it, ignoreCase = true) }
    }
}
