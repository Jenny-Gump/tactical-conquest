package com.tacticalconquest.game.models

import com.tacticalconquest.game.engine.UnitType

data class Tile(
    val x: Int,
    val y: Int,
    val terrain: TerrainType,
    val owner: String = "neutral"
)

data class Position(
    val x: Int,
    val y: Int
)

data class StartUnit(
    val type: UnitType,
    val count: Int
)

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

data class VictoryConditions(
    val captureAllTerritories: Boolean = true,
    val destroyAllEnemies: Boolean = false,
    val captureSpecificHexes: List<Position> = emptyList(),
    val surviveForTurns: Int? = null
)

data class LevelRewards(
    val baseGloryPoints: Int,
    val speedBonus: Int = 25,
    val flawlessBonus: Int = 30,
    val unlockLevel: Int? = null
)

data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val icon: String = "🏆",
    var isUnlocked: Boolean = false,
    val gloryPointsReward: Int = 0
)

data class LevelProgress(
    val levelId: Int,
    var isUnlocked: Boolean = false,
    var isCompleted: Boolean = false,
    var stars: Int = 0,
    var bestTime: Int = Int.MAX_VALUE,
    var totalGloryEarned: Int = 0
)

data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val price: String,
    val gloryPrice: Int? = null,
    val type: ShopItemType,
    val icon: String = "🛒",
    var isPurchased: Boolean = false
)

enum class ShopItemType {
    PREMIUM_PACK,
    UNIT_SKIN,
    UI_THEME,
    BOOSTER
}

data class GameSettings(
    var soundEnabled: Boolean = true,
    var musicEnabled: Boolean = true,
    var vibrationEnabled: Boolean = true,
    var graphicsQuality: GraphicsQuality = GraphicsQuality.MEDIUM,
    var selectedLanguage: String = "ru"
)

enum class GraphicsQuality {
    LOW,
    MEDIUM,
    HIGH
}

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

data class SavedGameState(
    val levelId: Int,
    val currentTurn: Int,
    val playerUnitsJson: String,
    val aiUnitsJson: String,
    val mapStateJson: String,
    val playerResources: String,
    val aiResources: String,
    val timestamp: Long = System.currentTimeMillis()
)
