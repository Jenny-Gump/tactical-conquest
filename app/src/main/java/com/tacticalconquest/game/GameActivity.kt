package com.tacticalconquest.game

import android.graphics.PointF
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.lifecycleScope
import com.tacticalconquest.game.databinding.ActivityGameBinding
import com.tacticalconquest.game.databinding.DialogUnitInfoBinding
import com.tacticalconquest.game.engine.*
import com.tacticalconquest.game.managers.GameManager
import com.tacticalconquest.game.managers.PreferencesManager
import com.tacticalconquest.game.managers.SoundManager
import com.tacticalconquest.game.ui.GameView
import com.tacticalconquest.game.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameActivity : AppCompatActivity(), GameView.GameViewListener {
    
    private lateinit var binding: ActivityGameBinding
    private lateinit var gameEngine: GameEngine
    private lateinit var gameManager: GameManager
    private lateinit var soundManager: SoundManager
    private lateinit var prefsManager: PreferencesManager
    private lateinit var gestureDetector: GestureDetectorCompat
    
    private var levelId: Int = 1
    private var isPaused = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Получаем ID уровня
        levelId = intent.getIntExtra(Constants.EXTRA_LEVEL_ID, 1)
        
        // Инициализация менеджеров
        gameManager = GameManager.getInstance(this)
        soundManager = SoundManager.getInstance(this)
        prefsManager = PreferencesManager.getInstance(this)
        
        // Инициализация игрового движка
        initGameEngine()
        
        // Настройка UI
        setupUI()
        
        // Настройка жестов
        setupGestures()
        
        // Запуск игры
        startGame()
    }
    
    private fun initGameEngine() {
        val level = gameManager.getLevel(levelId)
        gameEngine = GameEngine(level, GameEngine.Difficulty.values()[levelId / 4])
        
        // Устанавливаем слушателя событий
        gameEngine.setGameListener(object : GameEngine.GameListener {
            override fun onGameStateChanged(state: GameEngine.GameState) {
                runOnUiThread {
                    when (state) {
                        GameEngine.GameState.PLAYER_TURN -> {
                            binding.btnEndTurn.isEnabled = true
                            updateUI()
                        }
                        GameEngine.GameState.AI_TURN -> {
                            binding.btnEndTurn.isEnabled = false
                            binding.tvTurnInfo.text = "Ход противника..."
                            processAITurn()
                        }
                        GameEngine.GameState.VICTORY -> showVictory()
                        GameEngine.GameState.DEFEAT -> showDefeat()
                    }
                }
            }
            
            override fun onUnitMoved(unit: Unit, from: HexCoordinate, to: HexCoordinate) {
                soundManager.playSound(SoundManager.SOUND_MOVE)
                binding.gameView.animateUnitMove(unit, from, to)
            }
            
            override fun onUnitAttacked(attacker: Unit, defender: Unit, damage: Int) {
                soundManager.playSound(SoundManager.SOUND_ATTACK)
                binding.gameView.animateAttack(attacker, defender, damage)
            }
            
            override fun onTerritoryChanged(hex: HexCoordinate, newOwner: GameEngine.Player) {
                soundManager.playSound(SoundManager.SOUND_CAPTURE)
                binding.gameView.updateHexOwner(hex, newOwner)
            }
        })
        
        // Передаем движок в GameView
        binding.gameView.setGameEngine(gameEngine)
        binding.gameView.setListener(this)
    }
    
    private fun setupUI() {
        // Название уровня
        binding.tvLevelName.text = gameManager.getLevel(levelId).name
        
        // Кнопка паузы
        binding.btnPause.setOnClickListener {
            soundManager.playSound(SoundManager.SOUND_CLICK)
            showPauseDialog()
        }
        
        // Кнопка завершения хода
        binding.btnEndTurn.setOnClickListener {
            soundManager.playSound(SoundManager.SOUND_CLICK)
            gameEngine.endPlayerTurn()
        }
        
        // Кнопки юнитов
        binding.btnInfantry.setOnClickListener {
            selectUnit(UnitType.INFANTRY)
        }
        
        binding.btnArchers.setOnClickListener {
            selectUnit(UnitType.ARCHERS)
        }
        
        binding.btnCavalry.setOnClickListener {
            selectUnit(UnitType.CAVALRY)
        }
        
        updateUI()
    }
    
    private fun setupGestures() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                binding.gameView.scrollBy(distanceX.toInt(), distanceY.toInt())
                return true
            }
            
            override fun onDoubleTap(e: MotionEvent): Boolean {
                binding.gameView.resetZoom()
                return true
            }
        })
        
        binding.gameView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            binding.gameView.onTouchEvent(event)
            true
        }
    }
    
    private fun startGame() {
        // Воспроизводим музыку
        if (prefsManager.isMusicEnabled()) {
            soundManager.playMusic(SoundManager.MUSIC_GAME)
        }
        
        // Начинаем игру
        gameEngine.startGame()
        updateUI()
    }
    
    private fun updateUI() {
        val state = gameEngine.getGameState()
        
        // Информация о ходе
        binding.tvTurnInfo.text = getString(R.string.turn, state.currentTurn)
        
        // Ресурсы игрока
        binding.tvPopulation.text = getString(R.string.population, state.playerResources.population)
        
        // Обновляем доступность кнопок юнитов
        val costs = gameEngine.getUnitCosts()
        binding.btnInfantry.isEnabled = state.playerResources.population >= costs[UnitType.INFANTRY]!!
        binding.btnArchers.isEnabled = state.playerResources.population >= costs[UnitType.ARCHERS]!!
        binding.btnCavalry.isEnabled = state.playerResources.population >= costs[UnitType.CAVALRY]!!
        
        // Обновляем стоимость юнитов
        binding.tvInfantryCost.text = costs[UnitType.INFANTRY].toString()
        binding.tvArchersCost.text = costs[UnitType.ARCHERS].toString()
        binding.tvCavalryCost.text = costs[UnitType.CAVALRY].toString()
    }
    
    private fun selectUnit(type: UnitType) {
        soundManager.playSound(SoundManager.SOUND_CLICK)
        binding.gameView.setSelectedUnitType(type)
        
        // Подсвечиваем выбранную кнопку
        binding.btnInfantry.alpha = if (type == UnitType.INFANTRY) 1.0f else 0.6f
        binding.btnArchers.alpha = if (type == UnitType.ARCHERS) 1.0f else 0.6f
        binding.btnCavalry.alpha = if (type == UnitType.CAVALRY) 1.0f else 0.6f
    }
    
    private fun processAITurn() {
        lifecycleScope.launch {
            delay(1000) // Даем игроку время увидеть, что ход ИИ
            
            withContext(Dispatchers.Default) {
                gameEngine.processAITurn()
            }
        }
    }
    
    private fun showPauseDialog() {
        isPaused = true
        
        AlertDialog.Builder(this, R.style.GameDialog)
            .setTitle(R.string.pause)
            .setItems(arrayOf(
                getString(R.string.resume),
                getString(R.string.restart),
                getString(R.string.settings),
                getString(R.string.back_to_menu)
            )) { dialog, which ->
                when (which) {
                    0 -> { // Продолжить
                        isPaused = false
                        dialog.dismiss()
                    }
                    1 -> { // Начать заново
                        restartLevel()
                    }
                    2 -> { // Настройки
                        // TODO: Открыть настройки
                    }
                    3 -> { // В главное меню
                        finish()
                    }
                }
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showVictory() {
        soundManager.playSound(SoundManager.SOUND_VICTORY)
        
        val stars = calculateStars()
        val gloryPoints = calculateGloryPoints(stars)
        
        // Сохраняем результат
        gameManager.completeLevelWith(levelId, stars, gloryPoints)
        prefsManager.addGloryPoints(gloryPoints)
        
        // Показываем диалог
        AlertDialog.Builder(this, R.style.GameDialog)
            .setTitle(R.string.victory)
            .setMessage(
                getString(R.string.level_complete) + "\n\n" +
                getString(R.string.stars_earned, stars) + "\n" +
                getString(R.string.glory_earned, gloryPoints)
            )
            .setPositiveButton(R.string.next_level) { _, _ ->
                if (levelId < Constants.MAX_LEVELS) {
                    // Запускаем следующий уровень
                    intent.putExtra(Constants.EXTRA_LEVEL_ID, levelId + 1)
                    recreate()
                } else {
                    // Это был последний уровень
                    finish()
                }
            }
            .setNegativeButton(R.string.back_to_menu) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showDefeat() {
        soundManager.playSound(SoundManager.SOUND_DEFEAT)
        
        AlertDialog.Builder(this, R.style.GameDialog)
            .setTitle(R.string.defeat)
            .setMessage("Попробуйте еще раз!")
            .setPositiveButton(R.string.retry) { _, _ ->
                restartLevel()
            }
            .setNegativeButton(R.string.back_to_menu) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun calculateStars(): Int {
        val state = gameEngine.getGameState()
        var stars = 1 // Минимум 1 звезда за победу
        
        // +1 звезда за быструю победу (менее 10 ходов)
        if (state.currentTurn < 10) stars++
        
        // +1 звезда за минимальные потери
        val totalUnits = state.playerUnits.size
        val lostUnits = gameEngine.getPlayerLosses()
        if (lostUnits < totalUnits / 4) stars++
        
        return stars.coerceIn(1, 3)
    }
    
    private fun calculateGloryPoints(stars: Int): Int {
        val basePoints = 20 + (levelId * 5)
        return basePoints * stars
    }
    
    private fun restartLevel() {
        recreate()
    }
    
    // GameView.GameViewListener implementation
    override fun onHexClicked(hex: HexCoordinate) {
        val selectedUnit = binding.gameView.getSelectedUnit()
        if (selectedUnit != null) {
            // Пытаемся переместить юнит
            if (gameEngine.canMoveUnit(selectedUnit, hex)) {
                gameEngine.moveUnit(selectedUnit, hex)
                binding.gameView.clearSelection()
            } else if (gameEngine.canAttack(selectedUnit, hex)) {
                gameEngine.attackUnit(selectedUnit, hex)
                binding.gameView.clearSelection()
            }
        } else {
            // Выбираем юнит на этом гексе
            val unit = gameEngine.getUnitAt(hex)
            if (unit != null && unit.owner == GameEngine.Player.PLAYER) {
                binding.gameView.selectUnit(unit)
                showUnitInfo(unit)
            }
        }
        updateUI()
    }
    
    override fun onUnitClicked(unit: Unit) {
        if (unit.owner == GameEngine.Player.PLAYER) {
            binding.gameView.selectUnit(unit)
            showUnitInfo(unit)
        }
    }
    
    override fun onEmptyHexClicked(hex: HexCoordinate) {
        // Пытаемся создать юнит
        val selectedType = binding.gameView.getSelectedUnitType()
        if (selectedType != null && gameEngine.canCreateUnit(selectedType, hex)) {
            gameEngine.createUnit(selectedType, hex)
            binding.gameView.clearSelection()
            updateUI()
        }
    }
    
    private fun showUnitInfo(unit: Unit) {
        val dialogBinding = DialogUnitInfoBinding.inflate(layoutInflater)
        
        dialogBinding.tvUnitName.text = when(unit.type) {
            UnitType.INFANTRY -> getString(R.string.infantry)
            UnitType.ARCHERS -> getString(R.string.archers)
            UnitType.CAVALRY -> getString(R.string.cavalry)
        }
        
        dialogBinding.tvHealth.text = getString(R.string.unit_health, unit.currentHealth, unit.maxHealth)
        dialogBinding.tvAttack.text = getString(R.string.unit_attack, unit.attack)
        dialogBinding.tvDefense.text = getString(R.string.unit_defense, unit.defense)
        dialogBinding.tvMovement.text = getString(R.string.unit_movement, unit.remainingMovement, unit.maxMovement)
        
        dialogBinding.pbHealth.max = unit.maxHealth
        dialogBinding.pbHealth.progress = unit.currentHealth
        
        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.ok, null)
            .show()
    }
    
    override fun onPause() {
        super.onPause()
        soundManager.pauseMusic()
        
        // Сохраняем состояние игры
        if (!isPaused && !isFinishing) {
            gameManager.saveGameState(levelId, gameEngine.getGameState())
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (!isPaused && prefsManager.isMusicEnabled()) {
            soundManager.resumeMusic()
        }
    }
    
    override fun onBackPressed() {
        showPauseDialog()
    }
}