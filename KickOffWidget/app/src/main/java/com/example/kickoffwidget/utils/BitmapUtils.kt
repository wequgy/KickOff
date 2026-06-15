package com.example.kickoffwidget.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.example.kickoffwidget.R
import kotlin.math.min

object BitmapUtils {
    fun getRoundedSquareBitmap(bitmap: Bitmap): Bitmap {
        val size = min(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val paint = Paint()
        val srcRect = Rect(
            (bitmap.width - size) / 2,
            (bitmap.height - size) / 2,
            (bitmap.width + size) / 2,
            (bitmap.height + size) / 2
        )
        val dstRect = Rect(0, 0, size, size)
        
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = 0xff424242.toInt()
        
        val rectF = RectF(0f, 0f, size.toFloat(), size.toFloat())
        val radius = size * 0.2f
        canvas.drawRoundRect(rectF, radius, radius, paint)
        
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        return output
    }

    fun createRotatedPill(context: Context, text: String, density: Float): Bitmap {
        val width = (112 * density).toInt()
        val height = (50 * density).toInt()
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        canvas.save()
        // Rotate around center by -2 degrees
        canvas.rotate(-2f, width / 2f, height / 2f)
        
        val isScore = text.contains("-") && text.split("-").size == 2
        
        if (isScore) {
            val parts = text.split("-")
            val homeScore = parts[0].trim()
            val awayScore = parts[1].trim()
            
            val blockWidth = 44 * density
            val blockHeight = 44 * density
            val blockRadius = 8 * density
            
            val shadowOffset = 3 * density
            
            // 1. Draw Shadows (for both blocks)
            paint.color = 0xFF1C0E00.toInt()
            paint.style = Paint.Style.FILL
            
            val homeShadowRect = RectF(
                4 * density + shadowOffset,
                3 * density + shadowOffset,
                4 * density + blockWidth + shadowOffset,
                3 * density + blockHeight + shadowOffset
            )
            val awayShadowRect = RectF(
                61 * density + shadowOffset,
                3 * density + shadowOffset,
                61 * density + blockWidth + shadowOffset,
                3 * density + blockHeight + shadowOffset
            )
            canvas.drawRoundRect(homeShadowRect, blockRadius, blockRadius, paint)
            canvas.drawRoundRect(awayShadowRect, blockRadius, blockRadius, paint)
            
            // 2. Draw Front Blocks Background (warm white/beige)
            paint.color = 0xFFFFF9F2.toInt()
            val homeFrontRect = RectF(
                4 * density,
                3 * density,
                4 * density + blockWidth,
                3 * density + blockHeight
            )
            val awayFrontRect = RectF(
                61 * density,
                3 * density,
                61 * density + blockWidth,
                3 * density + blockHeight
            )
            canvas.drawRoundRect(homeFrontRect, blockRadius, blockRadius, paint)
            canvas.drawRoundRect(awayFrontRect, blockRadius, blockRadius, paint)
            
            // 3. Draw Bold Borders (dark charcoal)
            paint.color = 0xFF2B1700.toInt() // @color/on_primary_fixed
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f * density
            canvas.drawRoundRect(homeFrontRect, blockRadius, blockRadius, paint)
            canvas.drawRoundRect(awayFrontRect, blockRadius, blockRadius, paint)
            
            // 4. Draw Score Numbers (dark charcoal)
            paint.style = Paint.Style.FILL
            paint.color = 0xFF2B1700.toInt() // @color/on_primary_fixed
            paint.textSize = 24 * density
            paint.textAlign = Paint.Align.CENTER
            
            try {
                val typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.jetbrains_mono_semibold)
                if (typeface != null) {
                     paint.typeface = typeface
                }
            } catch (e: Exception) {
                paint.typeface = android.graphics.Typeface.MONOSPACE
            }
            
            val fontMetrics = paint.fontMetrics
            val textY = 3 * density + blockHeight / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f
            
            canvas.drawText(homeScore, 4 * density + blockWidth / 2f, textY, paint)
            canvas.drawText(awayScore, 61 * density + blockWidth / 2f, textY, paint)
            
            // 5. Draw Colon Divider in the middle (dark charcoal)
            paint.color = 0xFF2B1700.toInt()
            paint.style = Paint.Style.FILL
            val colonX = 54.5f * density
            val dotRadius = 2f * density
            canvas.drawCircle(colonX, 21 * density, dotRadius, paint)
            canvas.drawCircle(colonX, 29 * density, dotRadius, paint)
            
        } else {
            // Draw Single Unified Dark Scoreboard Card:
            val shadowOffset = 3 * density
            val radius = 10 * density
            
            // 1. Draw Shadow
            paint.color = 0xFF1C0E00.toInt()
            paint.style = Paint.Style.FILL
            val shadowRect = RectF(
                shadowOffset,
                shadowOffset,
                (112 - 3) * density + shadowOffset,
                (50 - 3) * density + shadowOffset
            )
            canvas.drawRoundRect(shadowRect, radius, radius, paint)
            
            // 2. Draw Front Card Background (warm white/beige)
            paint.color = 0xFFFFF9F2.toInt()
            val frontRect = RectF(
                0f,
                0f,
                (112 - 3) * density,
                (50 - 3) * density
            )
            canvas.drawRoundRect(frontRect, radius, radius, paint)
            
            // 3. Draw Bold Border (dark charcoal)
            paint.color = 0xFF2B1700.toInt() // @color/on_primary_fixed
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f * density
            canvas.drawRoundRect(frontRect, radius, radius, paint)
            
            // 5. Draw Time / Status text (dark charcoal!)
            paint.style = Paint.Style.FILL
            paint.color = 0xFF2B1700.toInt() // @color/on_primary_fixed
            paint.textSize = 20 * density
            paint.textAlign = Paint.Align.CENTER
            
            try {
                val typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.jetbrains_mono_semibold)
                if (typeface != null) {
                    paint.typeface = typeface
                }
            } catch (e: Exception) {
                paint.typeface = android.graphics.Typeface.MONOSPACE
            }
            
            val textX = (112 - 3) * density / 2f
            val fontMetrics = paint.fontMetrics
            val textY = (50 - 3) * density / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f
            
            canvas.drawText(text, textX, textY, paint)
        }
        
        canvas.restore()
        return bitmap
    }
}
