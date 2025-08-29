package com.rst.recipeappopsc6312

import java.util.Calendar

object GreetingManager {

    private val morningGreetings = listOf(
        "☀️ Good Morning!",
        "Rise and shine! 🌅",
        "Time for some breakfast? 🍳",
        "What's cooking this morning? 🥞"
    )

    private val lunchGreetings = listOf(
        "🥗 Lunchtime!",
        "Ready for a midday meal? 🥪",
        "Refuel for the afternoon! 💪"
    )

    private val afternoonGreetings = listOf(
        "☕ Afternoon Snack?",
        "A little treat to get you through the day? 🍪",
        "Feeling peckish? 🍎"
    )

    private val dinnerGreetings = listOf(
        "🌙 Dinner is Served",
        "What's for dinner tonight? 🍝",
        "Hope you're hungry! 🍲"
    )

    private val nightGreetings = listOf(
        "😋 Midnight Snack",
        "Craving something sweet? 🍰",
        "A late-night bite? 🍕"
    )

    fun getRandomGreetingForCurrentTime(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> getRandomMorningGreeting()
            in 11..13 -> getRandomLunchGreeting()
            in 14..17 -> getRandomAfternoonGreeting()
            in 18..21 -> getRandomDinnerGreeting()
            else -> getRandomNightGreeting()
        }
    }

    fun getRandomMorningGreeting() = morningGreetings.random()
    fun getRandomLunchGreeting() = lunchGreetings.random()
    fun getRandomAfternoonGreeting() = afternoonGreetings.random()
    fun getRandomDinnerGreeting() = dinnerGreetings.random()
    fun getRandomNightGreeting() = nightGreetings.random()
}