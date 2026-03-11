package com.ultimate.engine.rendering

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.ultimate.engine.core.TransformComponent
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sqrt

data class Camera(
    val isPerspective: Boolean = true,
    var fov: Float = 60f,
    var near: Float = 0.1f,
    var far: Float = 200f,
    var left: Float = -10f,
    var right: Float = 10f,
    var top: Float = 10f,
    var bottom: Float = -10f,
)

data class Light(
    val type: LightType,
    val color: FloatArray,
    var intensity: Float,
    val position: FloatArray = floatArrayOf(0f, 5f, 0f),
    val direction: FloatArray = floatArrayOf(-0.4f, -1f, -0.3f),
)

enum class LightType { DIRECTIONAL, POINT, AMBIENT }

class SceneNode(val transform: TransformComponent = TransformComponent()) {
    val children = mutableListOf<SceneNode>()
    fun add(node: SceneNode): SceneNode = node.also { children += it }
}

class ShaderManager {
    private val programs = mutableMapOf<String, Int>()

    fun createProgram(name: String, vertex: String, fragment: String): Int {
        val vertexShader = compile(GLES30.GL_VERTEX_SHADER, vertex)
        val fragmentShader = compile(GLES30.GL_FRAGMENT_SHADER, fragment)
        val program = GLES30.glCreateProgram().also {
            GLES30.glAttachShader(it, vertexShader)
            GLES30.glAttachShader(it, fragmentShader)
            GLES30.glLinkProgram(it)
        }
        programs[name] = program
        return program
    }

    fun program(name: String): Int = programs.getValue(name)

    private fun compile(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        return shader
    }
}

class EngineGlSurfaceView(context: Context, renderer: UltimateRenderer) : GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(3)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}

class UltimateRenderer : GLSurfaceView.Renderer {
    private val shaderManager = ShaderManager()
    private val viewProjection = FloatArray(16)
    private val view = FloatArray(16)
    private val projection = FloatArray(16)
    private var width = 1
    private var height = 1

    val camera = Camera()
    val lights = mutableListOf(
        Light(LightType.AMBIENT, floatArrayOf(1f, 1f, 1f), 0.4f),
        Light(LightType.DIRECTIONAL, floatArrayOf(1f, 1f, 0.95f), 1f)
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.2f, 0.4f, 0.7f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        shaderManager.createProgram("basic", BASIC_VERTEX, BASIC_FRAGMENT)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.width = width
        this.height = height
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        Matrix.setLookAtM(view, 0, 0f, 7f, 10f, 0f, 0f, 0f, 0f, 1f, 0f)
        if (camera.isPerspective) {
            Matrix.perspectiveM(projection, 0, camera.fov, width.toFloat() / height, camera.near, camera.far)
        } else {
            Matrix.orthoM(projection, 0, camera.left, camera.right, camera.bottom, camera.top, camera.near, camera.far)
        }
        Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0)
    }

    fun frustumCull(position: FloatArray, radius: Float): Boolean {
        val distance = sqrt(position[0] * position[0] + position[1] * position[1] + position[2] * position[2])
        return distance - radius <= camera.far
    }

    companion object {
        private const val BASIC_VERTEX = """
            #version 300 es
            layout(location = 0) in vec3 aPos;
            uniform mat4 uMvp;
            void main() { gl_Position = uMvp * vec4(aPos, 1.0); }
        """

        private const val BASIC_FRAGMENT = """
            #version 300 es
            precision mediump float;
            out vec4 FragColor;
            void main() { FragColor = vec4(0.8, 0.8, 0.9, 1.0); }
        """
    }
}
