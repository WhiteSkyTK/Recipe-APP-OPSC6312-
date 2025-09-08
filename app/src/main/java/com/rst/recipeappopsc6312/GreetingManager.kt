package com.rst.recipeappopsc6312

import java.util.Calendar

object GreetingManager {

    private val morningGreetings = listOf(
        "☀️ Good Morning!",
        "Rise and shine! 🌅",
        "Time for some breakfast? 🍳",
        "What's cooking this morning? 🥞",
        "A great day starts with a great meal!",
        "Hello, sunshine! ☀️",
        "New day, new flavors! 🍯",
        "Coffee first, then conquer the day! ☕",
        "Morning vibes only! 🌸",
        "Breakfast is served! 🍞🥓",
        "Wake up and smell the coffee! ☕🌄",
        "Good morning, food explorer! 🥐",
        "Fuel up for the day ahead! 🍌",
        "Bright mornings, tasty mornings! 🌞",
        "A morning without pancakes is just sad. 🥞",
        "Start your day deliciously! 🍯🥖",
        "Hello, early bird! 🐦",
        "Eggs, toast, coffee… repeat! 🍳☕",
        "Smiles and breakfast all around! 😄🥯",
        "Rise, shine, and dine! 🍴",
        "Let's get this bread! (Literally) 🍞",
        "Today's forecast: 100% chance of delicious.",
        "Sip, sip, hooray! It's a new day! 🍊"
    )

    private val lunchGreetings = listOf(
        "🥗 Lunchtime!",
        "Ready for a midday meal? 🥪",
        "Refuel for the afternoon! 💪",
        "What's on the menu for lunch? 🍱",
        "Time for a delicious break.",
        "Midday munchies? 🍛",
        "Keep your energy up! 🌯",
        "Lunch o'clock! ⏰🍴",
        "A tasty break for a busy day! 🥗",
        "Fuel your afternoon adventures! 🥙",
        "Bon appétit! 😋",
        "Sandwiches, salads, or soup? 🥗🥪🍜",
        "Time to power up! ⚡🥪",
        "Lunch with a side of smiles! 😊",
        "Eat well, work well! 🍛💻",
        "Take a break and enjoy your food! 🍲",
        "Midday delight for the foodie soul! 🥙",
        "Refuel like a champ! 🏆🍱",
        "Lunch never looked this good! 🍣",
        "Healthy, tasty, satisfying! 🥗✨",
        "Halfway through the day! You deserve this. 🎉",
        "Let's taco 'bout lunch! 🌮",
        "Soup-er to see you! 🍲"
    )

    private val afternoonGreetings = listOf(
        "☕ Afternoon Snack?",
        "A little treat to get you through the day? 🍪",
        "Feeling peckish? 🍎",
        "Time for a coffee break! ☕",
        "Need a little pick-me-up? ✨",
        "Snack attack time! 🍫",
        "Afternoon cravings? 🍩",
        "Take five and enjoy a bite! 🍇",
        "Sweet or savory? You decide! 🥨🍪",
        "A tasty pause to power through! ⚡",
        "Fuel your afternoon! 🥜",
        "Snack smart, stay happy! 🥯",
        "Cookies or fruits? 🍏🍪",
        "Time for a little indulgence! 🍫",
        "Bite-sized happiness! 🥨",
        "Keep your energy flowing! ⚡🍎",
        "A treat to brighten your day! 🌞🍫",
        "Snack o'clock is the best o'clock! ⏰",
        "Refuel, recharge, repeat! 🥜",
        "Afternoon vibes and delicious bites! 🍪",
        "Donut worry, be happy! 🍩",
        "You're one snack away from a good mood. 😉",
        "A little something to keep you going. 🍓"
    )

    private val dinnerGreetings = listOf(
        "🌙 Dinner is Served",
        "What's for dinner tonight? 🍝",
        "Hope you're hungry! 🍲",
        "Time to unwind with a good meal. 🍷",
        "Let's make something amazing for dinner!",
        "Evening delights await! 🍛",
        "Cook, eat, repeat! 🍽️",
        "Dinner vibes only! 🌟",
        "A cozy dinner for a cozy night. 🕯️",
        "Gather 'round, food is ready! 🥘",
        "Time for flavors that soothe the soul! 🍲",
        "Delicious evening ahead! 😋",
        "Dinner and chill! 🍷",
        "Savor every bite! 🥗",
        "Evening meals, happy feels! 🌙",
        "End the day on a tasty note! 🍝",
        "Family dinner time! 🥘👨‍👩‍👧‍👦",
        "Cook up some magic tonight! ✨🍛",
        "A plate full of happiness! 🍽️",
        "Cheers to a tasty evening! 🥂",
        "Let's eat, drink, and be merry! 🥳",
        "Dinner is better when we eat together. ❤️",
        "Time to feast! 🍗"
    )

    private val nightGreetings = listOf(
        "😋 Midnight Snack?",
        "Craving something sweet? 🍰",
        "A late-night bite? 🍕",
        "Something cozy for the evening? 🍿",
        "Sweet dreams are made of cheese... and this! 🧀",
        "Late-night cravings? 🌙",
        "Snack o'clock! ⏳🍫",
        "Nighttime nibbles? 🍩",
        "Moonlight munchies! 🌌",
        "A tiny treat before bed? 🥛🍪",
        "Quiet night, tasty bites. 🛌🍫",
        "Night owl? Treat yourself! 🦉🍫",
        "Sweet or salty, you deserve both! 🍿🍫",
        "Keep the fridge company! 🥒🥛",
        "Snack bliss under the stars! ✨🍪",
        "Midnight delight awaits! 🌙🍩",
        "A bite for the night warrior! 🥨🌌",
        "Nibble, relax, repeat! 🛌🍫",
        "Cravings approved! 🍫",
        "End the night deliciously! 🌙🍪",
        "The kitchen is calling... 🤫",
        "Just one more bite... I promise! 😉",
        "Shhh... it's snack time. 🤫"
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
