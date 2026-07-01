package com.vikas.tryon.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GarmentBitmapLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cache = mutableMapOf<Pair<Int, Int>, Bitmap>()

    fun load(drawableRes: Int, tintColor: Color, width: Int = 300, height: Int = 400): Bitmap {
        val key = drawableRes to tintColor.toArgb()
        return cache.getOrPut(key) {
            val drawable = ContextCompat.getDrawable(context, drawableRes)!!
            val wrapped = DrawableCompat.wrap(drawable).mutate()
            DrawableCompat.setTint(wrapped, tintColor.toArgb())
            DrawableCompat.setTintMode(wrapped, PorterDuff.Mode.MULTIPLY)

            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            wrapped.setBounds(0, 0, width, height)
            wrapped.draw(canvas)
            bmp
        }
    }

    fun clearCache() = cache.clear()
}
