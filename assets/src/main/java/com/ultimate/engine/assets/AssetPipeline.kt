package com.ultimate.engine.assets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.util.concurrent.ConcurrentHashMap

class AssetPipeline(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val textureCache = ConcurrentHashMap<String, Bitmap>()
    private val textCache = ConcurrentHashMap<String, String>()

    suspend fun loadTexture(assetPath: String): Bitmap = withContext(ioDispatcher) {
        textureCache[assetPath] ?: context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream).also { textureCache[assetPath] = it }
        }
    }

    suspend fun loadText(assetPath: String): String = withContext(ioDispatcher) {
        textCache[assetPath] ?: context.assets.open(assetPath).bufferedReader().use(BufferedReader::readText)
            .also { textCache[assetPath] = it }
    }

    fun clear() {
        textureCache.values.forEach(Bitmap::recycle)
        textureCache.clear()
        textCache.clear()
    }
}
