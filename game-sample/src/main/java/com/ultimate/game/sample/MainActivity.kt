package com.ultimate.game.sample

import android.os.Bundle
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.ultimate.engine.core.EngineLoop
import com.ultimate.engine.core.Scene
import com.ultimate.engine.core.SceneManager
import com.ultimate.engine.core.TransformComponent
import com.ultimate.engine.input.GestureListener
import com.ultimate.engine.input.TouchInputProcessor
import com.ultimate.engine.network.LatencyInterpolator
import com.ultimate.engine.network.MultiplayerClient
import com.ultimate.engine.network.NetEntityState
import com.ultimate.engine.physics.Collider
import com.ultimate.engine.physics.PhysicsSystem
import com.ultimate.engine.physics.RigidBody
import com.ultimate.engine.rendering.EngineGlSurfaceView
import com.ultimate.engine.rendering.UltimateRenderer
import com.ultimate.engine.ui.HudController
import com.ultimate.engine.ui.MenuController
import kotlin.random.Random

class MainActivity : AppCompatActivity(), GestureListener {
    private val sceneManager = SceneManager()
    private val loop = EngineLoop(60)
    private val multiplayerClient = MultiplayerClient()
    private val interpolator = LatencyInterpolator()
    private var shotPower = 0f
    private lateinit var hud: HudController
    private lateinit var touch: TouchInputProcessor
    private lateinit var gameScene: MiniGolfScene

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val renderer = UltimateRenderer()
        val renderContainer = findViewById<FrameLayout>(R.id.renderContainer)
        renderContainer.addView(EngineGlSurfaceView(this, renderer))

        hud = HudController(
            findViewById<TextView>(R.id.scoreText),
            findViewById<TextView>(R.id.powerText),
            findViewById<TextView>(R.id.leaderboardText)
        )
        touch = TouchInputProcessor(this)

        gameScene = MiniGolfScene(hud)
        sceneManager.switch(gameScene)

        MenuController(
            findViewById(R.id.menuOverlay),
            findViewById<MaterialButton>(R.id.startButton)
        ) { startMultiplayer() }

        loop.start { dt ->
            sceneManager.update(dt)
            val state = gameScene.ballState()
            multiplayerClient.sendState(state)
        }
    }

    private fun startMultiplayer() {
        val playerId = "player-${Random.nextInt(1000, 9999)}"
        multiplayerClient.connect("ws://10.0.2.2:8080/multiplayer")
        multiplayerClient.join("room-1", playerId)
        hud.updateLeaderboard(listOf("Room: room-1", playerId))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = touch.onTouch(event)

    override fun onDrag(dx: Float, dy: Float) {
        shotPower = (shotPower + (dx + dy) * 0.0008f).coerceIn(0f, 1f)
        hud.updatePower(shotPower)
    }

    override fun onTap(x: Float, y: Float) {
        gameScene.shoot(shotPower)
        shotPower = 0f
        hud.updatePower(shotPower)
    }

    override fun onDestroy() {
        loop.release()
        multiplayerClient.close()
        super.onDestroy()
    }
}

private class MiniGolfScene(private val hud: HudController) : Scene("mini_golf") {
    private val physics = PhysicsSystem()
    private var strokes = 0
    private val ball = world.createEntity().add(TransformComponent(0f, 0.2f, 0f)).add(RigidBody()).add(Collider.Sphere(0.2f))

    override fun onLoad() {
        world.addSystem(physics)
        loadMap(0)
        hud.updateScore(strokes)
    }

    fun shoot(power: Float) {
        val body = ball.get(RigidBody::class.java) ?: return
        body.velocityX += 5f * power
        body.velocityZ += -7f * power
        strokes += 1
        hud.updateScore(strokes)
    }

    fun ballState(): NetEntityState {
        val t = ball.get(TransformComponent::class.java)!!
        return NetEntityState(ball.id, t.x, t.y, t.z, System.currentTimeMillis())
    }

    private fun loadMap(index: Int) {
        repeat(4) {
            world.createEntity().add(TransformComponent(x = it * 2f - 4f, y = 0f, z = -5f - index * 2f)).add(Collider.Box(0.6f, 0.4f, 0.6f))
        }
    }
}
