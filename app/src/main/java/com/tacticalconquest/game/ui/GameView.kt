package com.tacticalconquest.game.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import com.tacticalconquest.game.engine.*

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    interface GameViewListener {
        fun onHexClicked(hex: HexCoordinate)
        fun onUnitClicked(unit: Unit)
        fun onEmptyHexClicked(hex: HexCoordinate)
    }
    
    // TODO: Copy full implementation from Claude artifacts
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
    }
}
