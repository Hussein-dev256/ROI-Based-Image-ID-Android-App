package com.example.objectid

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Custom view for drawing a bounding box annotation around objects.
 * Users can draw a rectangle to select the object they want to recognize.
 */
class AnnotationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paint for drawing the annotation rectangle
    private val boxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    
    // Rectangle representing the annotation area
    private val annotationRect = Rect()
    
    // Touch coordinates
    private var startX = 0f
    private var startY = 0f
    private var currentX = 0f
    private var currentY = 0f
    
    // Drawing state
    private var isDrawing = false
    
    // Callback for when annotation is complete
    private var onAnnotationCompleteListener: ((Rect) -> Unit)? = null
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Start drawing
                startX = event.x
                startY = event.y
                currentX = event.x
                currentY = event.y
                isDrawing = true
                updateAnnotationRect()
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // Update current position
                currentX = event.x
                currentY = event.y
                updateAnnotationRect()
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                // Finish drawing
                currentX = event.x
                currentY = event.y
                isDrawing = false
                updateAnnotationRect()
                
                // Notify listener if annotation is valid (non-zero area)
                if (annotationRect.width() > 10 && annotationRect.height() > 10) {
                    onAnnotationCompleteListener?.invoke(Rect(annotationRect))
                }
                
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw the annotation rectangle if we're drawing or have a valid rectangle
        if (isDrawing || (annotationRect.width() > 0 && annotationRect.height() > 0)) {
            canvas.drawRect(annotationRect, boxPaint)
        }
    }
    
    /**
     * Update the annotation rectangle based on current touch coordinates.
     */
    private fun updateAnnotationRect() {
        annotationRect.left = minOf(startX, currentX).toInt()
        annotationRect.top = minOf(startY, currentY).toInt()
        annotationRect.right = maxOf(startX, currentX).toInt()
        annotationRect.bottom = maxOf(startY, currentY).toInt()
    }
    
    /**
     * Clear the current annotation.
     */
    fun clearAnnotation() {
        annotationRect.setEmpty()
        invalidate()
    }
    
    /**
     * Get the current annotation rectangle.
     */
    fun getAnnotationRect(): Rect {
        return Rect(annotationRect)
    }
    
    /**
     * Set a listener to be called when annotation drawing is complete.
     */
    fun setOnAnnotationCompleteListener(listener: (Rect) -> Unit) {
        onAnnotationCompleteListener = listener
    }
}
