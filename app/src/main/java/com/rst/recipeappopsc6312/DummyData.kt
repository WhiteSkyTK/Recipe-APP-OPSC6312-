package com.rst.recipeappopsc6312

import kotlin.math.min

object DummyData {

    // A master list of all possible recipes with full details
    private val allRecipes = listOf(
        Recipe(
            id = "1",
            title = "Classic Italian Lasagna",
            imageUrl = "https://images.unsplash.com/photo-1574894709920-31b29d1dc395",
            timeInMins = 90,
            isFavorite = true,
            author = "Chef Giovanni",
            description = "A rich and cheesy classic that's perfect for a family dinner.",
            servings = 8,
            nutrition = listOf(
                NutritionFact("Calories", "450"),
                NutritionFact("Fat", "25g"),
                NutritionFact("Carbs", "35g"),
                NutritionFact("Protein", "20g")
            ),
            ingredients = listOf(
                Ingredient("Ground Beef", "1 lb"),
                Ingredient("Lasagna Noodles", "9"),
                Ingredient("Ricotta Cheese", "15 oz"),
                Ingredient("Marinara Sauce", "24 oz"),
                Ingredient("Mozzarella Cheese", "1 lb")
            ),
            method = listOf(
                MethodStep("Preheat oven to 375°F (190°C). Cook the ground beef in a skillet until browned."),
                MethodStep("Boil the lasagna noodles for 8-10 minutes until al dente."),
                MethodStep("In a baking dish, layer marinara sauce, noodles, ricotta cheese, and meat sauce."),
                MethodStep("Repeat layers, finishing with a layer of mozzarella cheese on top."),
                MethodStep("Bake for 45 minutes, or until bubbly and golden brown.")
            )
        ),
        Recipe(
            id = "2",
            title = "Spicy Chicken Tacos",
            imageUrl = "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38",
            timeInMins = 25,
            isFavorite = false,
            author = "Maria Garcia",
            description = "Quick and easy chicken tacos with a spicy kick.",
            servings = 4,
            nutrition = listOf(
                NutritionFact("Calories", "320"),
                NutritionFact("Fat", "15g"),
                NutritionFact("Carbs", "20g"),
                NutritionFact("Protein", "25g")
            ),
            ingredients = listOf(
                Ingredient("Chicken Breast", "1 lb, diced"),
                Ingredient("Taco Shells", "8"),
                Ingredient("Salsa", "1 cup"),
                Ingredient("Lettuce", "shredded"),
                Ingredient("Cheddar Cheese", "shredded")
            ),
            method = listOf(
                MethodStep("In a skillet, cook diced chicken with your favorite taco seasoning."),
                MethodStep("Warm the taco shells in the oven or microwave."),
                MethodStep("Assemble the tacos with chicken, salsa, lettuce, and cheese.")
            )
        ),
        Recipe(
            id = "3",
            title = "Creamy Tomato Soup",
            imageUrl = "https://images.unsplash.com/photo-1598866594240-a92460641428",
            timeInMins = 30,
            isFavorite = false,
            author = "Chef Thomas",
            description = "A comforting and velvety smooth tomato soup, perfect with grilled cheese.",
            servings = 6,
            nutrition = listOf(
                NutritionFact("Calories", "150"),
                NutritionFact("Fat", "8g"),
                NutritionFact("Carbs", "15g"),
                NutritionFact("Protein", "5g")
            ),
            ingredients = listOf(
                Ingredient("Canned Tomatoes", "28 oz"),
                Ingredient("Vegetable Broth", "4 cups"),
                Ingredient("Heavy Cream", "1 cup"),
                Ingredient("Onion", "1, chopped"),
                Ingredient("Garlic", "2 cloves, minced")
            ),
            method = listOf(
                MethodStep("Sauté onion and garlic in a large pot until soft."),
                MethodStep("Add tomatoes and vegetable broth. Simmer for 20 minutes."),
                MethodStep("Use an immersion blender to puree the soup until smooth."),
                MethodStep("Stir in the heavy cream and season with salt and pepper.")
            )
        ),
        Recipe(
            id = "4",
            title = "Japanese Sushi Rolls",
            imageUrl = "https://images.unsplash.com/photo-1579584425555-c3ce17fd4351",
            timeInMins = 50,
            isFavorite = true,
            author = "Yuki Tanaka",
            description = "Learn to make your own delicious sushi rolls at home.",
            servings = 4,
            nutrition = listOf(
                NutritionFact("Calories", "150"),
                NutritionFact("Fat", "8g"),
                NutritionFact("Carbs", "15g"),
                NutritionFact("Protein", "5g")
            ),
            ingredients = listOf(
                Ingredient("Sushi Rice", "2 cups"),
                Ingredient("Nori Sheets", "4"),
                Ingredient("Cucumber", "1"), Ingredient("Avocado", "1"),
                Ingredient("Fresh Salmon", "1/2 lb")),
            method = listOf(
                MethodStep("Cook sushi rice according to package directions."),
                MethodStep("Lay a nori sheet on a bamboo mat, spread a thin layer of rice on top."),
                MethodStep("Place thin strips of salmon, cucumber, and avocado in the center."),
                MethodStep("Roll tightly and slice into 1-inch pieces."))
        ),
        Recipe(
            id = "5",
            title = "Indian Butter Chicken",
            imageUrl = "https://images.unsplash.com/photo-1588166524941-3bf61a9c41db",
            timeInMins = 45,
            isFavorite = false,
            author = "Priya Sharma",
            description = "A rich and creamy curry with tender chicken pieces.",
            servings = 6,
            nutrition = listOf(
                NutritionFact("Calories", "150"),
                NutritionFact("Fat", "8g"),
                NutritionFact("Carbs", "15g"),
                NutritionFact("Protein", "5g")
            ),
            ingredients = listOf(
                Ingredient("Chicken Thighs", "2 lbs"),
                Ingredient("Yogurt", "1 cup"),
                Ingredient("Tomato Puree", "15 oz"),
                Ingredient("Heavy Cream", "1 cup"),
                Ingredient("Garam Masala", "2 tsp")),
            method = listOf(
                MethodStep("Marinate chicken in yogurt and spices for at least 1 hour."),
                MethodStep("Sear the chicken in a hot pan until golden."),
                MethodStep("Add tomato puree and simmer until the chicken is cooked through."),
                MethodStep("Stir in heavy cream and garam masala. Serve with naan or rice."))
        ),
        Recipe(
            id = "9",
            title = "Greek Salad with Feta",
            imageUrl = "https://images.unsplash.com/photo-1551248429-4097c682f76b",
            timeInMins = 15,
            isFavorite = true,
            author = "Niko Papadakis",
            description = "A refreshing and healthy salad with classic Greek flavors.",
            servings = 4,
            nutrition = listOf(
                NutritionFact("Calories", "150"),
                NutritionFact("Fat", "8g"),
                NutritionFact("Carbs", "15g"),
                NutritionFact("Protein", "5g")
            ),
            ingredients = listOf(
                Ingredient("Cucumber", "1"),
                Ingredient("Tomatoes", "4"),
                Ingredient("Red Onion", "1/2"),
                Ingredient("Kalamata Olives", "1/2 cup"),
                Ingredient("Feta Cheese", "1 block")),
            method = listOf(
                MethodStep("Chop all vegetables into bite-sized pieces."),
                MethodStep("Combine vegetables and olives in a large bowl."),
                MethodStep("Crumble feta cheese over the top."),
                MethodStep("Drizzle with olive oil and a squeeze of lemon juice."))
        ),
        Recipe(
            id = "10",
            title = "American BBQ Ribs",
            imageUrl = "https://images.unsplash.com/photo-1544025162-d76694265947",
            timeInMins = 180,
            isFavorite = false,
            author = "Smokey Joe",
            description = "Fall-off-the-bone tender BBQ ribs with a sweet and tangy sauce.",
            servings = 4,
            nutrition = listOf(
                NutritionFact("Calories", "150"),
                NutritionFact("Fat", "8g"),
                NutritionFact("Carbs", "15g"),
                NutritionFact("Protein", "5g")
            ),
            ingredients = listOf(
                Ingredient("Pork Ribs", "1 rack"),
                Ingredient("BBQ Sauce", "2 cups"),
                Ingredient("Brown Sugar", "1/4 cup"),
                Ingredient("Paprika", "1 tbsp")),
            method = listOf(
                MethodStep("Preheat oven to 300°F (150°C). Rub ribs with a mix of brown sugar and paprika."),
                MethodStep("Wrap ribs tightly in foil and bake for 2.5 hours."),
                MethodStep("Unwrap, brush generously with BBQ sauce, and broil for 5-10 minutes until caramelized."))
        ),
        Recipe(
            id = "16",
            title = "Chocolate Lava Cake",
            imageUrl = "https://images.unsplash.com/photo-1586985289936-a8a763a23574",
            timeInMins = 25,
            isFavorite = true,
            author = "Chef Antoine",
            description = "A decadent chocolate cake with a gooey, molten center.",
            servings = 2,
            nutrition = listOf(
                NutritionFact("Calories", "150"),
                NutritionFact("Fat", "8g"),
                NutritionFact("Carbs", "15g"),
                NutritionFact("Protein", "5g")
            ),
            ingredients = listOf(
                Ingredient("Bittersweet Chocolate", "4 oz"),
                Ingredient("Butter", "1/2 cup"),
                Ingredient("Eggs", "2"),
                Ingredient("Sugar", "2 tbsp"),
                Ingredient("Flour", "1 tbsp")),
            method = listOf(
                MethodStep("Preheat oven to 450°F (230°C). Grease two ramekins."),
                MethodStep("Melt chocolate and butter together."),
                MethodStep("Whisk eggs and sugar until pale, then fold in the chocolate mixture and flour."),
                MethodStep("Pour into ramekins and bake for 12-14 minutes."),
                MethodStep("Let cool for a minute before inverting onto plates."))
        ),
        Recipe(
            id = "18",
            title = "Simple Pancake Breakfast",
            imageUrl = "https://images.unsplash.com/photo-1528207776546-365bb710ee93",
            timeInMins = 20,
            isFavorite = false,
            author = "Harmony Kitchen",
            description = "Fluffy and delicious pancakes, a perfect start to any day."
        )
    )

    fun getRecipeById(id: String): Recipe? {
        return allRecipes.find { it.id == id }
    }

    fun getFeaturedRecipes(): List<Recipe> {
        return allRecipes.filter { it.id in listOf("1", "10", "16") }
    }

    fun getRecommendedRecipes(): List<Recipe> {
        return allRecipes.filter { it.id in listOf("2", "5", "9") }
    }

    fun getDiscoverRecipes(page: Int, pageSize: Int): List<Recipe> {
        val start = page * pageSize
        val end = min(start + pageSize, allRecipes.size)
        return if (start < end) allRecipes.subList(start, end) else emptyList()
    }

    fun getCategories(): List<Category> {
        return listOf(
            Category("Breakfast", isSelected = true),
            Category("Lunch"),
            Category("Dinner"),
            Category("Dessert"),
            Category("Appetizer")
        )
    }

    fun getAllCategories(): List<Category> {
        // This is the full list for the CategoryFragment
        return listOf(
            Category("Breakfast", isSelected = true),
            Category("Lunch"),
            Category("Dinner"),
            Category("Fruits"),
            Category("Dairy product"),
            Category("Protein"),
            Category("Cereal"),
            Category("Grain"),
            Category("Egg"),
            Category("Vegetables"),
            Category("Dairy"),
            Category("Sweets"),
            Category("Beverages"),
            Category("Spices"),
            Category("Snacks")
        )
    }

    fun getNotifications(): List<Notification> {
        return listOf(
            Notification("New Update Available", "Today / 10:52 PM", R.drawable.ic_alert),
            Notification("New Recipes Added", "Yesterday / 04:01 PM", R.drawable.ic_new_recipe)
        )
    }
}
