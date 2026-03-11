package com.ultimate.engine.physics

import com.ultimate.engine.core.Component
import com.ultimate.engine.core.Entity
import com.ultimate.engine.core.System
import com.ultimate.engine.core.TransformComponent
import com.ultimate.engine.core.World
import kotlin.math.max
import kotlin.math.min

data class RigidBody(
    var mass: Float = 1f,
    var restitution: Float = 0.4f,
    var friction: Float = 0.96f,
    var velocityX: Float = 0f,
    var velocityY: Float = 0f,
    var velocityZ: Float = 0f,
    var useGravity: Boolean = true,
) : Component

sealed class Collider : Component {
    data class Sphere(val radius: Float) : Collider()
    data class Box(val halfX: Float, val halfY: Float, val halfZ: Float) : Collider()
}

data class RaycastHit(val entity: Entity, val distance: Float)

class PhysicsSystem(private val gravity: Float = -9.8f) : System {
    override fun update(world: World, deltaSeconds: Float) {
        val entities = world.entities()
        entities.forEach { entity ->
            val transform = entity.get(TransformComponent::class.java) ?: return@forEach
            val body = entity.get(RigidBody::class.java) ?: return@forEach
            if (body.useGravity) body.velocityY += gravity * deltaSeconds
            transform.x += body.velocityX * deltaSeconds
            transform.y += body.velocityY * deltaSeconds
            transform.z += body.velocityZ * deltaSeconds
            body.velocityX *= body.friction
            body.velocityY *= body.friction
            body.velocityZ *= body.friction
            if (transform.y < 0f) {
                transform.y = 0f
                body.velocityY = -body.velocityY * body.restitution
            }
        }

        for (i in entities.indices) {
            for (j in i + 1 until entities.size) {
                resolveCollision(entities[i], entities[j])
            }
        }
    }

    private fun resolveCollision(a: Entity, b: Entity) {
        val ta = a.get(TransformComponent::class.java) ?: return
        val tb = b.get(TransformComponent::class.java) ?: return
        val ca = a.get(Collider::class.java)
        val cb = b.get(Collider::class.java)
        if (ca is Collider.Sphere && cb is Collider.Sphere) {
            val dx = tb.x - ta.x
            val dz = tb.z - ta.z
            val distanceSquared = dx * dx + dz * dz
            val radius = ca.radius + cb.radius
            if (distanceSquared < radius * radius && distanceSquared > 0f) {
                val distance = kotlin.math.sqrt(distanceSquared)
                val overlap = radius - distance
                val nx = dx / distance
                val nz = dz / distance
                ta.x -= nx * overlap * 0.5f
                ta.z -= nz * overlap * 0.5f
                tb.x += nx * overlap * 0.5f
                tb.z += nz * overlap * 0.5f
            }
        }
    }

    fun raycast(origin: FloatArray, direction: FloatArray, maxDistance: Float, world: World): RaycastHit? {
        var nearest: RaycastHit? = null
        world.entities().forEach { entity ->
            val transform = entity.get(TransformComponent::class.java) ?: return@forEach
            val collider = entity.get(Collider::class.java) ?: return@forEach
            if (collider is Collider.Sphere) {
                val ocx = origin[0] - transform.x
                val ocy = origin[1] - transform.y
                val ocz = origin[2] - transform.z
                val b = ocx * direction[0] + ocy * direction[1] + ocz * direction[2]
                val c = ocx * ocx + ocy * ocy + ocz * ocz - collider.radius * collider.radius
                val discriminant = b * b - c
                if (discriminant >= 0f) {
                    val distance = -b - kotlin.math.sqrt(discriminant)
                    if (distance in 0f..maxDistance) {
                        nearest = if (nearest == null) RaycastHit(entity, distance)
                        else RaycastHit(entity, min(nearest!!.distance, distance))
                    }
                }
            }
        }
        return nearest
    }
}
