package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Main : Screen("main")
    object Admin : Screen("admin")
    object SnapEditor : Screen("snap_editor")
    object SnapViewer : Screen("snap_viewer/{snapId}") {
        fun createRoute(snapId: Long) = "snap_viewer/$snapId"
    }
    object StoryViewer : Screen("story_viewer/{userId}") {
        fun createRoute(userId: Long) = "story_viewer/$userId"
    }
    object ChatDetail : Screen("chat_detail/{userId}") {
        fun createRoute(userId: Long) = "chat_detail/$userId"
    }
}

enum class MainTab(val label: String) {
    CHAT("Chat"),
    CAMERA("Camera"),
    STORIES("Stories"),
    PROFILE("Profile")
}
