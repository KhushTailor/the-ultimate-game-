package com.ultimate.engine.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

interface Component

data class TransformComponent(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var rx: Float = 0f,
    var ry: Float = 0f,
    var rz: Float = 0f,
    var sx: Float = 1f,
    var sy: Float = 1f,
    var sz: Float = 1f,
) : Component

class Entity(val id: Int) {
    private val components = ConcurrentHashMap<Class<*>, Component>()

    fun <T : Component> add(component: T): Entity {
        components[component.javaClass] = component
        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Component> get(type: Class<T>): T? = components[type] as? T
}

interface System {
    fun update(world: World, deltaSeconds: Float)
}

class World {
    private val ids = AtomicInteger(0)
    private val entities = CopyOnWriteArrayList<Entity>()
    private val systems = CopyOnWriteArrayList<System>()

    fun createEntity(): Entity = Entity(ids.incrementAndGet()).also { entities.add(it) }
    fun entities(): List<Entity> = entities
    fun addSystem(system: System) { systems += system }
    fun update(deltaSeconds: Float) = systems.forEach { it.update(this, deltaSeconds) }
}

abstract class Scene(val name: String) {
    protected val world = World()
    open fun onLoad() {}
    open fun onUnload() {}
    open fun update(deltaSeconds: Float) { world.update(deltaSeconds) }
}

class SceneManager {
    private var currentScene: Scene? = null
    fun switch(scene: Scene) {
        currentScene?.onUnload()
        currentScene = scene
        scene.onLoad()
    }
    fun update(dt: Float) = currentScene?.update(dt)
}

class EngineLoop(private val tickRateHz: Int = 60) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    fun start(onTick: (Float) -> Unit) {
        stop()
        val tickMs = 1000L / tickRateHz
        job = scope.launch {
            var previous = System.nanoTime()
            while (isActive) {
                val now = System.nanoTime()
                val dt = (now - previous) / 1_000_000_000f
                previous = now
                onTick(dt)
                delay(tickMs)
            }
        }
    }

    fun stop() = job?.cancel()
    fun release() { stop(); scope.cancel() }
}
