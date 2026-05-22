package com.example.gymlab.ui

import com.example.gymlab.R

fun getExerciseImageRes(name: String): Int {
    val normalized = name
        .trim()
        .lowercase()
        .replace("-", " ")
        .replace(Regex("\\s+"), " ")

    return when (normalized) {
        "jumping jacks" -> R.drawable.jumping_jacks
        "burpees" -> R.drawable.burpees
        "push up" -> R.drawable.push_up
        "incline push up" -> R.drawable.incline_push_up
        "crunches" -> R.drawable.crunches
        "plank" -> R.drawable.plank
        "bodyweight squat" -> R.drawable.bodyweight_squat
        "lunges" -> R.drawable.lunges
        else -> R.drawable.exercise_placeholder
    }
}