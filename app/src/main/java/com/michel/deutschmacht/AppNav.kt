package com.michel.deutschmacht

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.michel.deutschmacht.ui.screens.*

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") { HomeScreen(nav) }
        composable("lesson/{num}") { back ->
            val n = back.arguments?.getString("num")?.toIntOrNull() ?: 1
            LessonScreen(nav, n)
        }
        composable("quiz/{num}") { back ->
            val n = back.arguments?.getString("num")?.toIntOrNull() ?: 1
            QuizScreen(nav, n)
        }
        composable("search") { SearchScreen(nav) }
    }
}
