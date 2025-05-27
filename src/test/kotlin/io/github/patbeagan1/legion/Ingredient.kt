package io.github.patbeagan1.legion

sealed class Ingredient {
    class Egg : Ingredient()
    class Sugar : Ingredient()
    class Flour : Ingredient()
    class Milk : Ingredient()
    class Cake : Ingredient()
}