package com.tacticalconquest.game.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.tacticalconquest.game.R
import com.tacticalconquest.game.engine.*
import com.tacticalconquest.game.models.TerrainType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Пользовательский View для отображения гексагональной игровой карты
 */
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
    
    private var listener: GameViewListener? = null
    private var gameEngine: GameEngine? = null
    
    // Размеры и отступы
    private var hexSize = 64f
    private val hexWidth = hexSize * 2
    private val hexHeight = hexSize * sqrt(3f)
    private var offsetX = 0f
    private var offsetY = 0f
    
    // Выбранные элементы
    private var selectedUnit: Unit? = null
    private var selectedHex: HexCoordinate? = null
    private var selectedUnitType: UnitType? = null
    private var highlightedHexes = mutableSetOf<HexCoordinate>()
    
    // Паинты для рисования
    private val terrainPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = ContextCompat.getColor(context, R.color.hex_border)
    }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = ContextCompat.getColor(context, R.color.hex_selected)
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        alpha = 100
    }
    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = hexSize * 0.6f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val healthBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    // Пути для гексов
    private val hexPath = Path()
    
    // Анимации
    private var animatingUnits = mutableMapOf<Unit, PointF>()
    
    init {
        setWillNotDraw(false)
        setupHexPath()
    }
    
    fun setGameEngine(engine: GameEngine) {
        gameEngine = engine
        invalidate()
    }
    
    fun setListener(listener: GameViewListener) {
        this.listener = listener
    }
    
    fun setSelectedUnitType(type: UnitType?) {
        selectedUnitType = type
        updateHighlights()
        invalidate()
    }
    
    fun getSelectedUnitType(): UnitType? = selectedUnitType
    
    fun selectUnit(unit: Unit?) {
        selectedUnit = unit
        updateHighlights()
        invalidate()
    }
    
    fun getSelectedUnit(): Unit? = selectedUnit
    
    fun clearSelection() {
        selectedUnit = null
        selectedHex = null
        selectedUnitType = null
        highlightedHexes.clear()
        invalidate()
    }
    
    fun animateUnitMove(unit: Unit, from: HexCoordinate, to: HexCoordinate) {
        val fromPoint = hexToPixel(from)
        val toPoint = hexToPixel(to)
        
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                val currentX = fromPoint.x + (toPoint.x - fromPoint.x) * progress
                val currentY = fromPoint.y + (toPoint.y - fromPoint.y) * progress
                animatingUnits[unit] = PointF(currentX, currentY)
                invalidate()
            }
            start()
        }
    }
    
    fun animateAttack(attacker: Unit, defender: Unit, damage: Int) {
        // TODO: Реализовать анимацию атаки
        invalidate()
    }
    
    fun updateHexOwner(hex: HexCoordinate, newOwner: GameEngine.Player) {
        // TODO: Анимация захвата территории
        invalidate()
    }
    
    fun resetZoom() {
        hexSize = 64f
        offsetX = 0f
        offsetY = 0f
        setupHexPath()
        invalidate()
    }
    
    private fun setupHexPath() {
        hexPath.reset()
        for (i in 0..5) {
            val angle = 2.0 * Math.PI / 6 * i
            val x = hexSize * cos(angle).toFloat()
            val y = hexSize * sin(angle).toFloat()
            if (i == 0) {
                hexPath.moveTo(x, y)
            } else {
                hexPath.lineTo(x, y)
            }
        }
        hexPath.close()
    }
    
    private fun updateHighlights() {
        highlightedHexes.clear()
        
        selectedUnit?.let { unit ->
            // Подсвечиваем доступные для движения клетки
            val state = gameEngine?.getGameState() ?: return
            state.map.getAllHexes().forEach { hex ->
                if (gameEngine?.canMoveUnit(unit, hex) == true) {
                    highlightedHexes.add(hex)
                }
            }
            
            // Подсвечиваем врагов в радиусе атаки
            state.aiUnits.forEach { enemy ->
                if (gameEngine?.canAttack(unit, enemy.position) == true) {
                    highlightedHexes.add(enemy.position)
                }
            }
        }
        
        selectedUnitType?.let { type ->
            // Подсвечиваем места, где можно создать юнит
            val state = gameEngine?.getGameState() ?: return
            state.map.getAllHexes().forEach { hex ->
                if (gameEngine?.canCreateUnit(type, hex) == true) {
                    highlightedHexes.add(hex)
                }
            }
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val state = gameEngine?.getGameState() ?: return
        
        canvas.save()
        canvas.translate(offsetX, offsetY)
        
        // Рисуем гексы
        for (row in 0 until state.map.height) {
            for (col in 0 until state.map.width) {
                val hex = HexCoordinate(col, row)
                drawHex(canvas, hex, state.map)
            }
        }
        
        // Рисуем юнитов
        state.playerUnits.forEach { unit ->
            drawUnit(canvas, unit)
        }
        state.aiUnits.forEach { unit ->
            drawUnit(canvas, unit)
        }
        
        canvas.restore()
    }
    
    private fun drawHex(canvas: Canvas, hex: HexCoordinate, map: HexMap) {
        val center = hexToPixel(hex)
        
        canvas.save()
        canvas.translate(center.x, center.y)
        
        // Цвет местности
        val terrain = map.getTerrain(hex) ?: TerrainType.PLAINS
        terrainPaint.color = getTerrainColor(terrain)
        canvas.drawPath(hexPath, terrainPaint)
        
        // Цвет владельца (полупрозрачный оверлей)
        val owner = map.getHexOwner(hex)
        if (owner != GameEngine.Player.NEUTRAL) {
            terrainPaint.color = getOwnerColor(owner)
            terrainPaint.alpha = 80
            canvas.drawPath(hexPath, terrainPaint)
            terrainPaint.alpha = 255
        }
        
        // Подсветка
        if (hex in highlightedHexes) {
            val isAttackTarget = gameEngine?.getUnitAt(hex)?.owner == GameEngine.Player.AI
            highlightPaint.color = if (isAttackTarget) {
                ContextCompat.getColor(context, R.color.hex_attack)
            } else {
                ContextCompat.getColor(context, R.color.hex_movement)
            }
            canvas.drawPath(hexPath, highlightPaint)
        }
        
        // Граница
        canvas.drawPath(hexPath, borderPaint)
        
        // Выделение выбранного гекса
        if (hex == selectedHex || hex == selectedUnit?.position) {
            canvas.drawPath(hexPath, selectedPaint)
        }
        
        // Иконка местности (для городов)
        if (terrain == TerrainType.CITY) {
            textPaint.color = Color.BLACK
            textPaint.textSize = hexSize * 0.5f
            canvas.drawText("🏛", 0f, hexSize * 0.2f, textPaint)
        }
        
        canvas.restore()
    }
    
    private fun drawUnit(canvas: Canvas, unit: Unit) {
        val position = animatingUnits[unit] ?: hexToPixel(unit.position)
        
        canvas.save()
        canvas.translate(position.x, position.y)
        
        // Круг юнита
        val radius = hexSize * 0.6f
        unitPaint.color = unit.getDisplayColor()
        canvas.drawCircle(0f, 0f, radius, unitPaint)
        
        // Иконка юнита
        textPaint.color = Color.WHITE
        textPaint.textSize = hexSize * 0.8f
        val icon = unit.getIcon()
        val bounds = Rect()
        textPaint.getTextBounds(icon, 0, icon.length, bounds)
        canvas.drawText(icon, 0f, bounds.height() / 2f, textPaint)
        
        // Полоска здоровья
        val barWidth = radius * 1.5f
        val barHeight = 6f
        val barY = radius + 4f
        
        // Фон полоски
        healthBarPaint.color = Color.RED
        canvas.drawRect(-barWidth/2, barY, barWidth/2, barY + barHeight, healthBarPaint)
        
        // Текущее здоровье
        healthBarPaint.color = Color.GREEN
        val healthWidth = barWidth * unit.getHealthPercentage()
        canvas.drawRect(-barWidth/2, barY, -barWidth/2 + healthWidth, barY + barHeight, healthBarPaint)
        
        canvas.restore()
    }
    
    private fun hexToPixel(hex: HexCoordinate): PointF {
        val x = hexSize * (3f/2f * hex.col)
        val y = hexSize * sqrt(3f) * (hex.row + 0.5f * (hex.col and 1))
        return PointF(x + hexSize, y + hexSize)
    }
    
    private fun pixelToHex(x: Float, y: Float): HexCoordinate? {
        val adjustedX = x - offsetX - hexSize
        val adjustedY = y - offsetY - hexSize
        
        // Приблизительный расчет
        val col = (adjustedX * 2f/3f / hexSize).toInt()
        val row = ((adjustedY - (col and 1) * hexSize * sqrt(3f)/2f) / (hexSize * sqrt(3f))).toInt()
        
        val hex = HexCoordinate(col, row)
        
        // Проверяем, что гекс в пределах карты
        val map = gameEngine?.getGameState()?.map ?: return null
        return if (hex.isInBounds(map.width, map.height)) hex else null
    }
    
    private fun getTerrainColor(terrain: TerrainType): Int {
        return when (terrain) {
            TerrainType.PLAINS -> ContextCompat.getColor(context, R.color.terrain_plains)
            TerrainType.HILLS -> ContextCompat.getColor(context, R.color.terrain_hills)
            TerrainType.FOREST -> ContextCompat.getColor(context, R.color.terrain_forest)
            TerrainType.RIVER -> ContextCompat.getColor(context, R.color.terrain_river)
            TerrainType.CITY -> ContextCompat.getColor(context, R.color.terrain_city)
        }
    }
    
    private fun getOwnerColor(owner: GameEngine.Player?): Int {
        return when (owner) {
            GameEngine.Player.PLAYER -> ContextCompat.getColor(context, R.color.player_color)
            GameEngine.Player.AI -> ContextCompat.getColor(context, R.color.enemy_color)
            else -> ContextCompat.getColor(context, R.color.neutral_color)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val hex = pixelToHex(event.x, event.y)
                if (hex != null) {
                    handleHexClick(hex)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun handleHexClick(hex: HexCoordinate) {
        selectedHex = hex
        
        val unit = gameEngine?.getUnitAt(hex)
        if (unit != null) {
            listener?.onUnitClicked(unit)
        } else {
            listener?.onEmptyHexClicked(hex)
        }
        
        listener?.onHexClicked(hex)
    }
    
    override fun scrollBy(x: Int, y: Int) {
        offsetX -= x
        offsetY -= y
        
        // Ограничиваем прокрутку
        val map = gameEngine?.getGameState()?.map ?: return
        val maxX = map.width * hexSize * 1.5f - width
        val maxY = map.height * hexHeight - height
        
        offsetX = offsetX.coerceIn(-maxX, hexSize)
        offsetY = offsetY.coerceIn(-maxY, hexSize)
        
        invalidate()
    }
}

// Расширение для HexMap
fun HexMap.getAllHexes(): List<HexCoordinate> {
    val hexes = mutableListOf<HexCoordinate>()
    for (row in 0 until height) {
        for (col in 0 until width) {
            hexes.add(HexCoordinate(col, row))
        }
    }
    return hexes
}