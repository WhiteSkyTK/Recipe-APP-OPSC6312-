package com.rst.recipeappopsc6312

import android.content.Context
import java.util.Calendar

object GreetingManager {

    fun getRandomGreetingForCurrentTime(context: Context): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> getRandomGreeting(context, R.array.morning_greetings)
            in 11..13 -> getRandomGreeting(context, R.array.lunch_greetings)
            in 14..17 -> getRandomGreeting(context, R.array.afternoon_greetings)
            in 18..21 -> getRandomGreeting(context, R.array.dinner_greetings)
            else -> getRandomGreeting(context, R.array.night_greetings)
        }
    }

    private fun getRandomGreeting(context: Context, arrayResId: Int): String {
        val greetings = context.resources.getStringArray(arrayResId)
        return greetings.random()
    }
}
