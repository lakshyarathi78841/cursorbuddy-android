package com.cursorbuddy.android.service

import android.content.Context
import android.content.pm.PackageManager

data class AppInfo(
    val packageName: String,
    val appName: String,
    val category: AppCategory
)

enum class AppCategory {
    SETTINGS, BROWSER, MESSAGING, SOCIAL, EMAIL, MAPS, CAMERA, PHONE,
    FILES, MUSIC, VIDEO, GALLERY, CALENDAR, CLOCK, CALCULATOR,
    STORE, FINANCE, HEALTH, FOOD, TRAVEL, PRODUCTIVITY, UNKNOWN
}

object AppDetector {
    
    private var cachedApp: AppInfo? = null
    private var lastPackage: String? = null
    
    fun detect(packageName: String, context: Context): AppInfo {
        if (packageName == lastPackage && cachedApp != null) return cachedApp!!
        lastPackage = packageName
        
        val appName = try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName.substringAfterLast(".")
        }
        
        val category = categorize(packageName, appName)
        val info = AppInfo(packageName, appName, category)
        cachedApp = info
        return info
    }
    
    private fun categorize(pkg: String, name: String): AppCategory {
        val p = pkg.lowercase()
        val n = name.lowercase()
        
        return when {
            // Settings
            p.contains("settings") || p.contains("systemui") -> AppCategory.SETTINGS
            
            // Browsers
            p.contains("chrome") || p.contains("browser") || p.contains("firefox") || 
            p.contains("opera") || p.contains("brave") || p.contains("edge") || 
            p.contains("samsung.sbrowser") || p.contains("duckduckgo") -> AppCategory.BROWSER
            
            // Messaging
            p.contains("whatsapp") || p.contains("telegram") || p.contains("messenger") ||
            p.contains("signal") || p.contains("viber") || p.contains("wechat") ||
            p.contains("discord") || p.contains("slack") || p.contains("mms") ||
            p.contains("messaging") || p.contains("sms") || n.contains("messages") -> AppCategory.MESSAGING
            
            // Social
            p.contains("instagram") || p.contains("facebook") || p.contains("tiktok") ||
            p.contains("twitter") || p.contains("snapchat") || p.contains("reddit") ||
            p.contains("pinterest") || p.contains("linkedin") || p.contains("threads") -> AppCategory.SOCIAL
            
            // Email
            p.contains("gmail") || p.contains("email") || p.contains("mail") ||
            p.contains("outlook") || p.contains("yahoo.mobile") -> AppCategory.EMAIL
            
            // Maps
            p.contains("maps") || p.contains("waze") || p.contains("navigation") -> AppCategory.MAPS
            
            // Camera
            p.contains("camera") || p.contains("gcam") -> AppCategory.CAMERA
            
            // Phone
            p.contains("dialer") || p.contains("phone") || p.contains("contacts") ||
            p.contains("incallui") -> AppCategory.PHONE
            
            // Files
            p.contains("files") || p.contains("filemanager") || p.contains("documentsui") -> AppCategory.FILES
            
            // Music
            p.contains("spotify") || p.contains("music") || p.contains("pandora") ||
            p.contains("soundcloud") || p.contains("deezer") || p.contains("tidal") -> AppCategory.MUSIC
            
            // Video
            p.contains("youtube") || p.contains("netflix") || p.contains("video") ||
            p.contains("disney") || p.contains("hulu") || p.contains("primevideo") -> AppCategory.VIDEO
            
            // Gallery / Photos
            p.contains("gallery") || p.contains("photos") -> AppCategory.GALLERY
            
            // Calendar
            p.contains("calendar") -> AppCategory.CALENDAR
            
            // Clock
            p.contains("clock") || p.contains("alarm") || p.contains("deskclock") -> AppCategory.CLOCK
            
            // Calculator
            p.contains("calculator") || p.contains("calc") -> AppCategory.CALCULATOR
            
            // Store
            p.contains("vending") || p.contains("playstore") || p.contains("appstore") -> AppCategory.STORE
            
            // Finance
            p.contains("bank") || p.contains("pay") || p.contains("wallet") || 
            p.contains("finance") || p.contains("venmo") || p.contains("cashapp") -> AppCategory.FINANCE
            
            // Productivity
            p.contains("docs") || p.contains("sheets") || p.contains("drive") ||
            p.contains("notion") || p.contains("evernote") || p.contains("keep") ||
            p.contains("notes") || p.contains("todo") || p.contains("trello") -> AppCategory.PRODUCTIVITY
            
            // Food
            p.contains("ubereats") || p.contains("doordash") || p.contains("grubhub") ||
            p.contains("deliveroo") -> AppCategory.FOOD
            
            // Travel
            p.contains("uber") || p.contains("lyft") || p.contains("booking") ||
            p.contains("airbnb") || p.contains("tripadvisor") -> AppCategory.TRAVEL
            
            else -> AppCategory.UNKNOWN
        }
    }
    
    fun getQuickPrompts(appInfo: AppInfo): List<String> {
        return when (appInfo.category) {
            AppCategory.SETTINGS -> listOf(
                "Connect to WiFi",
                "Turn on Bluetooth",
                "Change wallpaper",
                "Check storage"
            )
            AppCategory.BROWSER -> listOf(
                "Bookmark this page",
                "Open a new tab",
                "Clear browsing data",
                "Find on page"
            )
            AppCategory.MESSAGING -> listOf(
                "Send a photo",
                "Create a group chat",
                "Send a voice message",
                "Change chat wallpaper"
            )
            AppCategory.SOCIAL -> listOf(
                "Post a photo",
                "Edit my profile",
                "Change privacy settings",
                "Find friends"
            )
            AppCategory.EMAIL -> listOf(
                "Compose an email",
                "Add an attachment",
                "Search my emails",
                "Create a label"
            )
            AppCategory.MAPS -> listOf(
                "Get directions home",
                "Find restaurants nearby",
                "Download offline maps",
                "Share my location"
            )
            AppCategory.CAMERA -> listOf(
                "Take a panorama",
                "Switch to video mode",
                "Use portrait mode",
                "Set a timer"
            )
            AppCategory.PHONE -> listOf(
                "Block a number",
                "Check voicemail",
                "Add a contact",
                "View call history"
            )
            AppCategory.MUSIC -> listOf(
                "Create a playlist",
                "Download for offline",
                "Find similar songs",
                "Adjust equalizer"
            )
            AppCategory.VIDEO -> listOf(
                "Search for a video",
                "Turn on subtitles",
                "Change video quality",
                "Save to watch later"
            )
            AppCategory.GALLERY -> listOf(
                "Edit a photo",
                "Create an album",
                "Share photos",
                "Free up space"
            )
            AppCategory.STORE -> listOf(
                "Update my apps",
                "Check subscriptions",
                "Redeem a gift card",
                "Change payment method"
            )
            AppCategory.FINANCE -> listOf(
                "Check my balance",
                "Send money",
                "View transactions",
                "Set up notifications"
            )
            AppCategory.PRODUCTIVITY -> listOf(
                "Create a new document",
                "Share with someone",
                "Search my files",
                "Organize folders"
            )
            else -> listOf(
                "Show me around",
                "What can I do here?",
                "Find the settings",
                "Help me navigate"
            )
        }
    }
    
    fun getAppGreeting(appInfo: AppInfo): String {
        return when (appInfo.category) {
            AppCategory.SETTINGS -> "I see you're in Settings. What do you need help with?"
            AppCategory.BROWSER -> "Browsing in ${appInfo.appName}. Need help with anything?"
            AppCategory.MESSAGING -> "In ${appInfo.appName}. How can I help you chat?"
            AppCategory.SOCIAL -> "On ${appInfo.appName}. What would you like to do?"
            AppCategory.EMAIL -> "In ${appInfo.appName}. Need help with your email?"
            AppCategory.MAPS -> "In ${appInfo.appName}. Where do you need to go?"
            AppCategory.CAMERA -> "Camera's open! Want some photography tips?"
            AppCategory.PHONE -> "In the Phone app. What do you need?"
            AppCategory.MUSIC -> "Listening on ${appInfo.appName}. What can I help with?"
            AppCategory.VIDEO -> "Watching on ${appInfo.appName}. Need help?"
            AppCategory.FINANCE -> "In ${appInfo.appName}. What do you need help with?"
            else -> "I see ${appInfo.appName}. What would you like help with?"
        }
    }
}
