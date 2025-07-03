package com.tacticalconquest.game.managers

import android.content.Context
import com.google.gson.Gson
import com.tacticalconquest.game.engine.GameEngine
import com.tacticalconquest.game.engine.UnitType
import com.tacticalconquest.game.models.*
import java.io.InputStreamReader

/**
 * Центральный менеджер игры - управляет уровнями, прогрессом и достижениями
 */
class GameManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: GameManager? = null
        
        fun getInstance(context: Context): GameManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GameManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val gson = Gson()
    private val prefsManager = PreferencesManager.getInstance(context)
    
    // Кэш данных
    private var levels: List<Level>? = null
    private var achievements: List<Achievement>? = null
    private var levelProgress: MutableMap<Int, LevelProgress> = mutableMapOf()
    
    init {
        loadLevels()
        loadAchievements()
        loadProgress()
    }
    
    /**
     * Получить все уровни
     */
    fun getAllLevels(): List<Level> {
        return levels ?: emptyList()
    }
    
    /**
     * Получить конкретный уровень
     */
    fun getLevel(levelId: Int): Level {
        return levels?.find { it.id == levelId } 
            ?: throw IllegalArgumentException("Level $levelId not found")
    }
    
    /**
     * Получить прогресс уровня
     */
    fun getLevelProgress(levelId: Int): LevelProgress {
        return levelProgress.getOrPut(levelId) {
            LevelProgress(levelId, isUnlocked = levelId == 1)
        }
    }
    
    /**
     * Завершить уровень с результатами
     */
    fun completeLevelWith(levelId: Int, stars: Int, gloryEarned: Int) {
        val progress = getLevelProgress(levelId)
        progress.isCompleted = true
        progress.stars = maxOf(progress.stars, stars)
        progress.totalGloryEarned += gloryEarned
        
        // Разблокируем следующий уровень
        if (levelId < 10) {
            val nextProgress = getLevelProgress(levelId + 1)
            nextProgress.isUnlocked = true
        }
        
        // Обновляем статистику
        val stats = prefsManager.getPlayerStats()
        stats.totalVictories++
        stats.totalBattles++
        prefsManager.savePlayerStats(stats)
        
        // Проверяем достижения
        checkAchievements()
        
        // Сохраняем прогресс
        saveProgress()
    }
    
    /**
     * Получить все достижения
     */
    fun getAchievements(): List<Achievement> {
        return achievements ?: emptyList()
    }
    
    /**
     * Сохранить состояние игры
     */
    fun saveGameState(levelId: Int, state: GameEngine.GameStateData) {
        val savedState = SavedGameState(
            levelId = levelId,
            currentTurn = state.currentTurn,
            playerUnitsJson = gson.toJson(state.playerUnits),
            aiUnitsJson = gson.toJson(state.aiUnits),
            mapStateJson = gson.toJson(state.map),
            playerResources = gson.toJson(state.playerResources),
            aiResources = gson.toJson(state.aiResources)
        )
        prefsManager.saveSavedGameState(savedState)
    }
    
    /**
     * Загрузить сохраненное состояние игры
     */
    fun loadGameState(): SavedGameState? {
        return prefsManager.getSavedGameState()
    }
    
    /**
     * Очистить сохраненное состояние
     */
    fun clearSavedGameState() {
        prefsManager.clearSavedGameState()
    }
    
    private fun loadLevels() {
        // В реальном приложении загружаем из assets/levels.json
        // Для MVP создаем уровни программно
        levels = createDefaultLevels()
    }
    
    private fun createDefaultLevels(): List<Level> {
        val levelList = mutableListOf<Level>()
        
        // Уровень 1 - Обучение
        levelList.add(Level(
            id = 1,
            name = "Первая битва",
            description = "Изучите основы тактики",
            mapWidth = 8,
            mapHeight = 6,
            tiles = generateSimpleMap(8, 6, TerrainType.PLAINS),
            playerStartPosition = Position(1, 2),
            playerStartUnits = listOf(StartUnit(UnitType.INFANTRY, 2)),
            enemyStartPosition = Position(6, 3),
            enemyStartUnits = listOf(StartUnit(UnitType.INFANTRY, 1)),
            victoryConditions = VictoryConditions(captureAllTerritories = true),
            rewards = LevelRewards(baseGloryPoints = 30, unlockLevel = 2)
        ))
        
        // Уровень 2 - Оборона
        levelList.add(Level(
            id = 2,
            name = "Оборона рубежей",
            description = "Защитите свои территории",
            mapWidth = 10,
            mapHeight = 8,
            tiles = generateMixedMap(10, 8),
            playerStartPosition = Position(2, 4),
            playerStartUnits = listOf(
                StartUnit(UnitType.INFANTRY, 2),
                StartUnit(UnitType.ARCHERS, 1)
            ),
            enemyStartPosition = Position(7, 4),
            enemyStartUnits = listOf(
                StartUnit(UnitType.INFANTRY, 2),
                StartUnit(UnitType.CAVALRY, 1)
            ),
            victoryConditions = VictoryConditions(destroyAllEnemies = true),
            rewards = LevelRewards(baseGloryPoints = 40, unlockLevel = 3)
        ))
        
        // Уровень 3 - Река
        levelList.add(Level(
            id = 3,
            name = "Переправа через реку",
            description = "Форсируйте водную преграду",
            mapWidth = 12,
            mapHeight = 8,
            tiles = generateRiverMap(12, 8),
            playerStartPosition = Position(2, 4),
            playerStartUnits = listOf(
                StartUnit(UnitType.INFANTRY, 2),
                StartUnit(UnitType.ARCHERS, 1),
                StartUnit(UnitType.CAVALRY, 1)
            ),
            enemyStartPosition = Position(9, 4),
            enemyStartUnits = listOf(
                StartUnit(UnitType.INFANTRY, 3),
                StartUnit(UnitType.ARCHERS, 2)
            ),
            victoryConditions = VictoryConditions(captureAllTerritories = true),
            rewards = LevelRewards(baseGloryPoints = 50, unlockLevel = 4)
        ))
        
        // Уровни 4-10 с возрастающей сложностью
        for (i in 4..10) {
            val mapSize = 10 + (i - 4) * 2  // От 10x10 до 22x22
            levelList.add(Level(
                id = i,
                name = when(i) {
                    4 -> "Осада крепости"
                    5 -> "Битва в лесу"
                    6 -> "Горный перевал"
                    7 -> "Великая равнина"
                    8 -> "Последний рубеж"
                    9 -> "Решающее сражение"
                    10 -> "Финальная битва"
                    else -> "Уровень $i"
                },
                description = "Сложность возрастает!",
                mapWidth = mapSize,
                mapHeight = mapSize - 2,
                tiles = generateComplexMap(mapSize, mapSize - 2, i),
                playerStartPosition = Position(2, mapSize / 2),
                playerStartUnits = listOf(
                    StartUnit(UnitType.INFANTRY, 2 + i / 3),
                    StartUnit(UnitType.ARCHERS, 1 + i / 4),
                    StartUnit(UnitType.CAVALRY, 1 + i / 5)
                ),
                enemyStartPosition = Position(mapSize - 3, mapSize / 2),
                enemyStartUnits = listOf(
                    StartUnit(UnitType.INFANTRY, 2 + i / 2),
                    StartUnit(UnitType.ARCHERS, 1 + i / 3),
                    StartUnit(UnitType.CAVALRY, i / 3)
                ),
                victoryConditions = VictoryConditions(
                    captureAllTerritories = i <= 7,
                    destroyAllEnemies = i > 7
                ),
                rewards = LevelRewards(
                    baseGloryPoints = 30 + i * 10,
                    unlockLevel = if (i < 10) i + 1 else null
                )
            ))
        }
        
        return levelList
    }
    
    private fun generateSimpleMap(width: Int, height: Int, terrain: TerrainType): List<Tile> {
        val tiles = mutableListOf<Tile>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                tiles.add(Tile(x, y, terrain))
            }
        }
        // Добавляем стартовые города
        tiles[coordinateToIndex(1, 2, width)] = Tile(1, 2, TerrainType.CITY, "player")
        tiles[coordinateToIndex(width - 2, height - 3, width)] = Tile(width - 2, height - 3, TerrainType.CITY, "ai")
        return tiles
    }
    
    private fun generateMixedMap(width: Int, height: Int): List<Tile> {
        val tiles = mutableListOf<Tile>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                val terrain = when {
                    (x + y) % 5 == 0 -> TerrainType.HILLS
                    (x * y) % 7 == 0 -> TerrainType.FOREST
                    else -> TerrainType.PLAINS
                }
                tiles.add(Tile(x, y, terrain))
            }
        }
        // Города
        tiles[coordinateToIndex(2, height / 2, width)] = Tile(2, height / 2, TerrainType.CITY, "player")
        tiles[coordinateToIndex(width - 3, height / 2, width)] = Tile(width - 3, height / 2, TerrainType.CITY, "ai")
        return tiles
    }
    
    private fun generateRiverMap(width: Int, height: Int): List<Tile> {
        val tiles = mutableListOf<Tile>()
        val riverX = width / 2
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val terrain = when {
                    x == riverX || x == riverX - 1 -> TerrainType.RIVER
                    kotlin.math.abs(x - riverX) <= 2 && (x + y) % 3 == 0 -> TerrainType.FOREST
                    else -> TerrainType.PLAINS
                }
                tiles.add(Tile(x, y, terrain))
            }
        }
        // Города и мосты
        tiles[coordinateToIndex(2, height / 2, width)] = Tile(2, height / 2, TerrainType.CITY, "player")
        tiles[coordinateToIndex(width - 3, height / 2, width)] = Tile(width - 3, height / 2, TerrainType.CITY, "ai")
        tiles[coordinateToIndex(riverX, height / 2, width)] = Tile(riverX, height / 2, TerrainType.PLAINS) // Мост
        return tiles
    }
    
    private fun generateComplexMap(width: Int, height: Int, levelId: Int): List<Tile> {
        val tiles = mutableListOf<Tile>()
        val random = kotlin.random.Random(levelId)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val terrain = when (random.nextInt(10)) {
                    0, 1 -> TerrainType.HILLS
                    2, 3 -> TerrainType.FOREST
                    4 -> TerrainType.RIVER
                    else -> TerrainType.PLAINS
                }
                tiles.add(Tile(x, y, terrain))
            }
        }
        
        // Добавляем города
        val cityCount = 2 + levelId / 3
        for (i in 0 until cityCount) {
            val x = random.nextInt(width)
            val y = random.nextInt(height)
            val owner = when {
                x < width / 3 -> "player"
                x > 2 * width / 3 -> "ai"
                else -> "neutral"
            }
            tiles[coordinateToIndex(x, y, width)] = Tile(x, y, TerrainType.CITY, owner)
        }
        
        return tiles
    }
    
    private fun coordinateToIndex(x: Int, y: Int, width: Int): Int = y * width + x
    
    private fun loadAchievements() {
        achievements = listOf(
            Achievement(
                id = "first_win",
                name = "Первая победа",
                description = "Выиграйте свою первую битву",
                gloryPointsReward = 50
            ),
            Achievement(
                id = "flawless_victory",
                name = "Безупречная победа",
                description = "Победите без потерь",
                icon = "⭐",
                gloryPointsReward = 100
            ),
            Achievement(
                id = "speed_demon",
                name = "Молниеносная война",
                description = "Победите за 5 ходов или меньше",
                icon = "⚡",
                gloryPointsReward = 75
            ),
            Achievement(
                id = "collector",
                name = "Коллекционер",
                description = "Соберите все скины юнитов",
                icon = "🎨",
                gloryPointsReward = 200
            ),
            Achievement(
                id = "veteran",
                name = "Ветеран",
                description = "Завершите все 10 уровней",
                icon = "🎖️",
                gloryPointsReward = 500
            ),
            Achievement(
                id = "perfectionist",
                name = "Перфекционист",
                description = "Получите 3 звезды на всех уровнях",
                icon = "💎",
                gloryPointsReward = 1000
            )
        )
    }
    
    private fun checkAchievements() {
        val stats = prefsManager.getPlayerStats()
        val unlockedAchievements = prefsManager.getUnlockedAchievements()
        
        achievements?.forEach { achievement ->
            if (achievement.id in unlockedAchievements) return@forEach
            
            val shouldUnlock = when (achievement.id) {
                "first_win" -> stats.totalVictories >= 1
                "flawless_victory" -> stats.perfectVictories >= 1
                "speed_demon" -> stats.fastVictories >= 1
                "collector" -> prefsManager.getPurchasedSkins().size >= 5
                "veteran" -> levelProgress.values.count { it.isCompleted } >= 10
                "perfectionist" -> levelProgress.values.count { it.stars == 3 } >= 10
                else -> false
            }
            
            if (shouldUnlock) {
                achievement.isUnlocked = true
                prefsManager.unlockAchievement(achievement.id)
                prefsManager.addGloryPoints(achievement.gloryPointsReward)
            }
        }
    }
    
    private fun loadProgress() {
        val savedProgress = prefsManager.getLevelProgress()
        savedProgress.forEach { (levelId, progress) ->
            levelProgress[levelId] = progress
        }
        
        // Убеждаемся, что первый уровень разблокирован
        if (levelProgress.isEmpty()) {
            levelProgress[1] = LevelProgress(1, isUnlocked = true)
        }
    }
    
    private fun saveProgress() {
        prefsManager.saveLevelProgress(levelProgress)
    }
}