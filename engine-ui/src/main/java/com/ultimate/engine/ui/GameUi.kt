package com.ultimate.engine.ui

import android.view.View
import android.widget.TextView
import com.google.android.material.button.MaterialButton

class HudController(
    private val scoreText: TextView,
    private val powerText: TextView,
    private val leaderboardText: TextView,
) {
    fun updateScore(score: Int) { scoreText.text = "Score: $score" }
    fun updatePower(power: Float) { powerText.text = "Power: ${(power * 100).toInt()}%" }
    fun updateLeaderboard(lines: List<String>) { leaderboardText.text = lines.joinToString("\n") }
}

class MenuController(
    private val menuRoot: View,
    startButton: MaterialButton,
    onStart: () -> Unit,
) {
    init {
        startButton.setOnClickListener {
            menuRoot.visibility = View.GONE
            onStart()
        }
    }
}
