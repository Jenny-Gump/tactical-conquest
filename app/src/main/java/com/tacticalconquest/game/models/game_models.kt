package com.tacticalconquest.game.models

import com.tacticalconquest.game.engine.UnitType

/**
 * Типы местности
 */
enum class TerrainType {
    PLAINS,  // Равнины
    HILLS,   // Холмы
    FOREST,  // Лес
    RIVER,   // Река
    CITY     // Город
}

/**
 * Описание тайла на карте
 */
data class Tile(
    val x: Int,
    val y: Int,
    val terrain: TerrainType,
    val owner: String = "neutral"  // "player", "ai", "neutral"
)

/**
 * Позиция на карте
 */
data class Position(
    val x: Int,
    val y: Int
)

/**
 * Начальные юниты
 */
data class StartUnit(
    val type: UnitType,
    val count: Int
)

/**
 * Описание уровня
 */
data class Level(
    val id: Int,
    val name: String,
    val description: String,
    val mapWidth: Int,
    val mapHeight: Int,
    val tiles: List<Tile>,
    val playerStartPosition: Position,
    val playerStartUnits: List<StartUnit>,
    val enemyStartPosition: Position,
    val enemyStartUnits: List<StartUnit>,
    val victoryConditions: VictoryConditions,
    val rewards: LevelRewards
)

/**
 * Условия победы
 */
data class VictoryConditions(
    val captureAllTerritories: Boolean = true,
    val destroyAllEnemies: Boolean = false,
    val captureSpecificHexes: List<Position> = emptyList(),
    val surviveForTurns: Int? = null
)

/**
 * Награды за уровень
 */
data class LevelRewards(
    val baseGloryPoints: Int,
    val speedBonus: Int = 25,      // Бонус за быструю победу
    val flawlessBonus: Int = 30,   // Бонус за победу без потерь
    val unlockLevel: Int? = null   // Какой уровень открывается
)

/**
 * Достижение
 */
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val icon: String = "🏆",
    var isUnlocked: Boolean = false,
    val gloryPointsReward: Int = 0
)

/**
 * Прогресс прохождения уровня
 */
data class LevelProgress(
    val levelId: Int,
    var isUnlocked: Boolean = false,
    var isCompleted: Boolean = false,
    var stars: Int = 0,            // 0-3 звезды
    var bestTime: Int = Int.MAX_VALUE,  // Лучшее время в ходах
    var totalGloryEarned: Int = 0
)

/**
 * Товар в магазине
 */
data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val price: String,          // Цена в формате "$0.99"
    val gloryPrice: Int? = null,  // Альтернативная цена в очках славы
    val type: ShopItemType,
    val icon: String = "🛒",
    var isPurchased: Boolean = false
)

/**
 * Типы товаров в магазине
 */
enum class ShopItemType {
    PREMIUM_PACK,
    UNIT_SKIN,
    UI_THEME,
    BOOSTER
}

/**
 * Скин юнита
 */
data class UnitSkin(
    val id: String,
    val name: String,
    val unitType: UnitType,
    val icon: String,
    val color: String  // Hex color
)

/**
 * Настройки игры
 */
data class GameSettings(
    var soundEnabled: Boolean = true,
    var musicEnabled: Boolean = true,
    var vibrationEnabled: Boolean = true,
    var graphicsQuality: GraphicsQuality = GraphicsQuality.MEDIUM,
    var selectedLanguage: String = "ru"
)

/**
 * Качество графики
 */
enum class GraphicsQuality {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Статистика игрока
 */
data class PlayerStats(
    var totalGloryPoints: Int = 0,
    var totalBattles: Int = 0,
    var totalVictories: Int = 0,
    var totalDefeats: Int = 0,
    var unitsCreated: Int = 0,
    var unitsLost: Int = 0,
    var territoriesCaptured: Int = 0,
    var perfectVictories: Int = 0,
    var fastVictories: Int = 0
)

/**
 * Сохраненное состояние игры
 */
data class SavedGameState(
    val levelId: Int,
    val currentTurn: Int,
    val playerUnitsJson: String,    // Сериализованные юниты
    val aiUnitsJson: String,
    val mapStateJson: String,        // Состояние карты
    val playerResources: String,
    val aiResources: String,
    val timestamp: Long = System.currentTimeMillis()
)