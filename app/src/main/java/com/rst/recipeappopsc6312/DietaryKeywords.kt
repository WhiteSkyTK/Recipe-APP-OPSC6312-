package com.rst.recipeappopsc6312

object DietaryKeywords {
    // === MEAT & SEAFOOD (for Vegetarian/Vegan/Pescetarian) ===
    val meatKeywords = setOf(
        "beef", "steak", "veal", "pork", "bacon", "ham", "prosciutto", "salami", "chorizo",
        "chicken", "poultry", "duck", "goose", "turkey", "lamb", "mutton", "venison", "rabbit",
        "sausage", "meatball", "jerky", "offal", "liver", "kidney", "tripe", "giblet",
        "pepperoni", "bresaola", "pastrami", "mortadella", "biltong", "kielbasa"
    )

    val seafoodKeywords = setOf(
        "fish", "salmon", "tuna", "cod", "sardine", "anchovy", "trout", "mackerel", "halibut",
        "haddock", "bass", "snapper", "catfish", "tilapia", "sole", "plaice", "swordfish",
        "shrimp", "prawn", "lobster", "crab", "oyster", "clam", "mussel", "scallop", "shellfish",
        "squid", "octopus", "cuttlefish", "eel", "roe", "caviar"
    )

    // === DAIRY (for Vegan & Dairy-Free) ===
    val dairyKeywords = setOf(
        "milk", "cheese", "cheddar", "mozzarella", "parmesan", "feta", "brie", "gouda",
        "cream", "butter", "ghee", "yogurt", "curd", "custard", "whey", "casein", "kefir", "paneer",
        "sour cream", "buttermilk", "evaporated milk", "condensed milk", "milk powder",
        "ricotta", "mascarpone", "halloumi"
    )

    // === EGGS (for Vegan) ===
    val eggKeywords = setOf(
        "egg", "eggs", "mayonnaise", "mayo", "aioli", "meringue",
        "custard", "egg yolk", "egg white", "omelette", "frittata", "quiche"
    )

    // === GLUTEN (for Gluten-Free) ===
    val glutenKeywords = setOf(
        "wheat", "flour", "bread", "pasta", "spaghetti", "noodle", "biscuit", "cracker",
        "barley", "rye", "malt", "bulgur", "seitan", "farro", "couscous",
        "pastry", "cake", "cookie", "dough", "bagel", "pretzel", "tortilla (flour)"
    )

    // === HIGH-CARB (for Keto/Low-Carb) ===
    val highCarbKeywords = setOf(
        "sugar", "honey", "syrup", "molasses", "maple syrup", "corn syrup",
        "rice", "noodle", "pasta", "bread", "potato", "yam", "sweet potato",
        "flour", "tortilla", "bun", "cake", "pastry", "cookie", "cracker",
        "oats", "granola", "quinoa", "millet", "barley"
    )

    // === NON-PALEO (for Paleo) ===
    val nonPaleoKeywords = setOf(
        // legumes
        "bean", "lentil", "chickpea", "soy", "tofu", "edamame", "peanut",
        // dairy
        "milk", "cheese", "butter", "cream", "yogurt",
        // grains
        "wheat", "corn", "oats", "rice", "quinoa", "barley", "rye",
        // processed
        "sugar", "refined flour", "soda"
    )

    // === NUTS (for Nut Allergy) ===
    val nutKeywords = setOf(
        "nut", "peanut", "almond", "walnut", "cashew", "pecan", "pistachio", "hazelnut",
        "macadamia", "brazil nut", "pine nut", "chestnut", "coconut"
    )

    // === HIGH-FODMAP (for Low-FODMAP) ===
    val highFodmapKeywords = setOf(
        // dairy
        "milk", "soft cheese", "cream", "ice cream",
        // legumes
        "lentil", "chickpea", "kidney bean", "black bean",
        // fruits
        "apple", "pear", "mango", "watermelon", "cherry", "plum",
        // sweeteners
        "honey", "high-fructose corn syrup",
        // vegetables
        "garlic", "onion", "cauliflower", "mushroom"
    )

    // === HARAM (for Halal) ===
    val haramKeywords = setOf(
        "pork", "bacon", "ham", "prosciutto", "salami", "lard",
        "alcohol", "wine", "beer", "whiskey", "brandy", "rum", "vodka", "gin", "liqueur",
        "champagne", "cognac", "tequila", "sake", "port", "sherry"
    )

    // === NON-KOSHER (for Kosher) ===
    val nonKosherKeywords = setOf(
        "pork", "bacon", "ham", "shellfish", "shrimp", "crab", "lobster", "clam", "oyster",
        "mussels", "scallops", "squid", "octopus", "cuttlefish",
        // mixing dairy & meat is tricky → handled case-by-case in logic
    )
}
