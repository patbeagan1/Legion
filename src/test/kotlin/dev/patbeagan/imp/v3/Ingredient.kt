package dev.patbeagan.imp.v3

sealed class Ingredient {
    class Egg : Ingredient()
    class Sugar : Ingredient()
    class Flour : Ingredient()
    class Milk : Ingredient()
    class Cake : Ingredient()
}