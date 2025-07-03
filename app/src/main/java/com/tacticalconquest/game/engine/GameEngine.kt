package com.tacticalconquest.game.engine

import com.tacticalconquest.game.models.Level
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Основной игровой движок, управляющий логикой игры
 */
class GameEngine(
    private val level: Level,
    private val difficulty: Difficulty = Difficulty.MEDIUM
) {
    
    enum class Player {
        PLAYER,
        AI,
        NEUTRAL
    }
    
    enum class GameState {
        PLAYER_TURN,
        AI_TURN,
        VICTORY,
        DEFEAT
    }
    
    enum class Difficulty {
        EASY,    // ИИ делает случайные ходы
        MEDIUM,  // ИИ использует базовую тактику
        HARD     // ИИ агрессивен и эффективен
    }
    
    interface GameListener {
        fun onGameStateChanged(state: GameState)
        fun onUnitMoved(unit: Unit, from: HexCoordinate, to: HexCoordinate)
        fun onUnitAttacked(attacker: Unit, defender: Unit, damage: Int)
        fun onTerritoryChanged(hex: HexCoordinate, newOwner: Player)
    }
    
    data class GameStateData(
        val currentTurn: Int,
        val playerUnits: List<Unit>,
        val aiUnits: List<Unit>,
        val playerResources: Resources,
        val aiResources: Resources,
        val map: HexMap
    )
    
    data class Resources(
        var population: Int = 5,
        var territories: Int = 1
    )
    
    private var gameListener: GameListener? = null
    private var currentState = GameState.PLAYER_TURN
    private var currentTurn = 1
    
    private val hexMap: HexMap = HexMap(level.mapWidth, level.mapHeight)
    private val units = mutableListOf<Unit>()
    private val playerResources = Resources()
    private val aiResources = Resources()
    
    private var playerLosses = 0
    private var aiLosses = 0
    
    private val unitCosts = mapOf(
        UnitType.INFANTRY to 1,
        UnitType.ARCHERS to 2,
        UnitType.CAVALRY to 3
    )
    
    init {
        initializeMap()
        initializeUnits()
    }
    
    fun setGameListener(listener: GameListener) {
        gameListener = listener
    }
    
    fun startGame() {
        currentState = GameState.PLAYER_TURN
        gameListener?.onGameStateChanged(currentState)
        updateResourcesForTurn(Player.PLAYER)
    }
    
    fun getGameState(): GameStateData {
        return GameStateData(
            currentTurn = currentTurn,
            playerUnits = units.filter { it.owner == Player.PLAYER },
            aiUnits = units.filter { it.owner == Player.AI },
            playerResources = playerResources,
            aiResources = aiResources,
            map = hexMap
        )
    }
    
    fun endPlayerTurn() {
        if (currentState != GameState.PLAYER_TURN) return
        
        // Сбрасываем движение юнитов игрока
        units.filter { it.owner == Player.PLAYER }.forEach { it.resetMovement() }
        
        // Проверяем условия победы/поражения
        if (checkVictoryConditions()) {
            currentState = GameState.VICTORY
            gameListener?.onGameStateChanged(currentState)
            return
        }
        
        if (checkDefeatConditions()) {
            currentState = GameState.DEFEAT
            gameListener?.onGameStateChanged(currentState)
            return
        }
        
        // Переход к ходу ИИ
        currentState = GameState.AI_TURN
        gameListener?.onGameStateChanged(currentState)
    }
    
    fun processAITurn() {
        if (currentState != GameState.AI_TURN) return
        
        updateResourcesForTurn(Player.AI)
        
        // ИИ логика в зависимости от сложности
        when (difficulty) {
            Difficulty.EASY -> processAITurnEasy()
            Difficulty.MEDIUM -> processAITurnMedium()
            Difficulty.HARD -> processAITurnHard()
        }
        
        // Сбрасываем движение юнитов ИИ
        units.filter { it.owner == Player.AI }.forEach { it.resetMovement() }
        
        // Проверяем условия победы/поражения
        if (checkVictoryConditions()) {
            currentState = GameState.VICTORY
            gameListener?.onGameStateChanged(currentState)
            return
        }
        
        if (checkDefeatConditions()) {
            currentState = GameState.DEFEAT
            gameListener?.onGameStateChanged(currentState)
            return
        }
        
        // Возврат к ходу игрока
        currentTurn++
        currentState = GameState.PLAYER_TURN
        updateResourcesForTurn(Player.PLAYER)
        gameListener?.onGameStateChanged(currentState)
    }
    
    fun canCreateUnit(type: UnitType, hex: HexCoordinate): Boolean {
        // Проверяем, что гекс принадлежит игроку
        if (hexMap.getHexOwner(hex) != Player.PLAYER) return false
        
        // Проверяем, что на гексе нет юнита
        if (getUnitAt(hex) != null) return false
        
        // Проверяем ресурсы
        return playerResources.population >= unitCosts[type]!!
    }
    
    fun createUnit(type: UnitType, hex: HexCoordinate): Boolean {
        if (!canCreateUnit(type, hex)) return false
        
        val unit = Unit(
            id = units.size + 1,
            type = type,
            owner = Player.PLAYER,
            position = hex,
            skinId = null // TODO: Применить выбранный скин
        )
        
        units.add(unit)
        playerResources.population -= unitCosts[type]!!
        
        return true
    }
    
    fun canMoveUnit(unit: Unit, targetHex: HexCoordinate): Boolean {
        if (unit.owner != Player.PLAYER || currentState != GameState.PLAYER_TURN) return false
        if (unit.remainingMovement <= 0) return false
        if (getUnitAt(targetHex) != null) return false
        
        val distance = hexMap.getDistance(unit.position, targetHex)
        val movementCost = hexMap.getMovementCost(unit.position, targetHex)
        
        return distance <= unit.remainingMovement && movementCost <= unit.remainingMovement
    }
    
    fun moveUnit(unit: Unit, targetHex: HexCoordinate): Boolean {
        if (!canMoveUnit(unit, targetHex)) return false
        
        val from = unit.position
        val movementCost = hexMap.getMovementCost(from, targetHex)
        
        unit.position = targetHex
        unit.remainingMovement -= movementCost
        
        // Захват территории
        if (hexMap.getHexOwner(targetHex) != unit.owner) {
            hexMap.setHexOwner(targetHex, unit.owner)
            gameListener?.onTerritoryChanged(targetHex, unit.owner)
        }
        
        gameListener?.onUnitMoved(unit, from, targetHex)
        return true
    }
    
    fun canAttack(attacker: Unit, targetHex: HexCoordinate): Boolean {
        if (attacker.owner != Player.PLAYER || currentState != GameState.PLAYER_TURN) return false
        if (attacker.hasAttacked) return false
        
        val defender = getUnitAt(targetHex) ?: return false
        if (defender.owner == attacker.owner) return false
        
        val distance = hexMap.getDistance(attacker.position, targetHex)
        return distance <= attacker.attackRange
    }
    
    fun attackUnit(attacker: Unit, targetHex: HexCoordinate): Boolean {
        if (!canAttack(attacker, targetHex)) return false
        
        val defender = getUnitAt(targetHex)!!
        
        // Расчет урона
        val terrainBonus = hexMap.getTerrainDefenseBonus(targetHex)
        val damage = calculateDamage(attacker, defender, terrainBonus)
        
        defender.takeDamage(damage)
        attacker.hasAttacked = true
        
        gameListener?.onUnitAttacked(attacker, defender, damage)
        
        // Проверяем, уничтожен ли защитник
        if (defender.currentHealth <= 0) {
            units.remove(defender)
            if (defender.owner == Player.PLAYER) playerLosses++
            else aiLosses++
            
            // Захват территории, если атакующий - пехота или кавалерия
            if (attacker.type != UnitType.ARCHERS && hexMap.getDistance(attacker.position, targetHex) == 1) {
                moveUnit(attacker, targetHex)
            }
        }
        
        return true
    }
    
    fun getUnitAt(hex: HexCoordinate): Unit? {
        return units.find { it.position == hex }
    }
    
    fun getUnitCosts(): Map<UnitType, Int> = unitCosts
    
    fun getPlayerLosses(): Int = playerLosses
    
    private fun initializeMap() {
        // Инициализируем карту из данных уровня
        level.tiles.forEach { tile ->
            val hex = HexCoordinate(tile.x, tile.y)
            hexMap.setTerrain(hex, tile.terrain)
            hexMap.setHexOwner(hex, when(tile.owner) {
                "player" -> Player.PLAYER
                "ai" -> Player.AI
                else -> Player.NEUTRAL
            })
        }
    }
    
    private fun initializeUnits() {
        // Создаем начальные юниты игрока
        level.playerStartUnits.forEach { startUnit ->
            repeat(startUnit.count) {
                val unit = Unit(
                    id = units.size + 1,
                    type = startUnit.type,
                    owner = Player.PLAYER,
                    position = HexCoordinate(level.playerStartPosition.x, level.playerStartPosition.y)
                )
                units.add(unit)
            }
        }
        
        // Создаем начальные юниты ИИ
        level.enemyStartUnits.forEach { startUnit ->
            repeat(startUnit.count) {
                val unit = Unit(
                    id = units.size + 1,
                    type = startUnit.type,
                    owner = Player.AI,
                    position = HexCoordinate(level.enemyStartPosition.x, level.enemyStartPosition.y)
                )
                units.add(unit)
            }
        }
        
        // Подсчитываем начальные территории
        updateTerritoryCount()
    }
    
    private fun updateResourcesForTurn(player: Player) {
        val resources = if (player == Player.PLAYER) playerResources else aiResources
        
        // Получаем население от контролируемых городов
        val cities = hexMap.getCitiesOwnedBy(player)
        resources.population += cities.size * 2
        
        // Ограничиваем максимальное население
        resources.population = min(resources.population, 20)
    }
    
    private fun updateTerritoryCount() {
        playerResources.territories = hexMap.getTerritoriesOwnedBy(Player.PLAYER).size
        aiResources.territories = hexMap.getTerritoriesOwnedBy(Player.AI).size
    }
    
    private fun calculateDamage(attacker: Unit, defender: Unit, terrainBonus: Int): Int {
        val baseDamage = attacker.attack
        val defense = defender.defense + terrainBonus
        
        // Формула урона: базовый урон - (защита / 2) + случайность
        var damage = baseDamage - (defense / 2) + Random.nextInt(-5, 6)
        
        // Бонусы типов юнитов
        when {
            // Кавалерия эффективна против лучников
            attacker.type == UnitType.CAVALRY && defender.type == UnitType.ARCHERS -> damage += 10
            // Лучники эффективны против пехоты
            attacker.type == UnitType.ARCHERS && defender.type == UnitType.INFANTRY -> damage += 5
            // Пехота эффективна против кавалерии
            attacker.type == UnitType.INFANTRY && defender.type == UnitType.CAVALRY -> damage += 5
        }
        
        return max(1, damage) // Минимум 1 урон
    }
    
    private fun checkVictoryConditions(): Boolean {
        // Победа, если все вражеские юниты уничтожены
        if (units.none { it.owner == Player.AI }) return true
        
        // Победа, если захвачены все вражеские территории
        if (hexMap.getTerritoriesOwnedBy(Player.AI).isEmpty()) return true
        
        return false
    }
    
    private fun checkDefeatConditions(): Boolean {
        // Поражение, если все юниты игрока уничтожены
        if (units.none { it.owner == Player.PLAYER }) return true
        
        // Поражение, если потеряны все территории
        if (hexMap.getTerritoriesOwnedBy(Player.PLAYER).isEmpty()) return true
        
        return false
    }
    
    // ИИ логика - простая сложность
    private fun processAITurnEasy() {
        val aiUnits = units.filter { it.owner == Player.AI }
        
        aiUnits.forEach { unit ->
            // Случайное движение
            val possibleMoves = hexMap.getNeighbors(unit.position)
                .filter { canMoveAIUnit(unit, it) }
            
            if (possibleMoves.isNotEmpty()) {
                val target = possibleMoves.random()
                moveAIUnit(unit, target)
            }
            
            // Атака, если есть враг рядом
            val enemiesNearby = findEnemiesInRange(unit)
            if (enemiesNearby.isNotEmpty()) {
                attackWithAIUnit(unit, enemiesNearby.random())
            }
        }
        
        // Создаем юнитов случайно
        if (aiResources.population >= 1 && Random.nextBoolean()) {
            createAIUnit(UnitType.values().random())
        }
    }
    
    // ИИ логика - средняя сложность
    private fun processAITurnMedium() {
        val aiUnits = units.filter { it.owner == Player.AI }.sortedByDescending { it.attack }
        
        // Сначала атакуем
        aiUnits.forEach { unit ->
            val enemies = findEnemiesInRange(unit)
            if (enemies.isNotEmpty()) {
                // Атакуем самого слабого врага
                val weakestEnemy = enemies.minByOrNull { it.currentHealth }!!
                attackWithAIUnit(unit, weakestEnemy)
            }
        }
        
        // Затем двигаемся к врагу
        aiUnits.forEach { unit ->
            if (unit.remainingMovement > 0) {
                val nearestEnemy = findNearestEnemy(unit)
                if (nearestEnemy != null) {
                    moveTowardsTarget(unit, nearestEnemy.position)
                }
            }
        }
        
        // Создаем юнитов с приоритетом
        while (aiResources.population >= 1) {
            val unitType = when {
                units.count { it.owner == Player.AI && it.type == UnitType.CAVALRY } < 2 && 
                    aiResources.population >= 3 -> UnitType.CAVALRY
                units.count { it.owner == Player.AI && it.type == UnitType.ARCHERS } < 3 && 
                    aiResources.population >= 2 -> UnitType.ARCHERS
                else -> UnitType.INFANTRY
            }
            
            if (aiResources.population >= unitCosts[unitType]!!) {
                createAIUnit(unitType)
            } else {
                break
            }
        }
    }
    
    // ИИ логика - сложная
    private fun processAITurnHard() {
        // TODO: Реализовать продвинутую ИИ стратегию
        // Пока используем среднюю сложность
        processAITurnMedium()
    }
    
    private fun canMoveAIUnit(unit: Unit, target: HexCoordinate): Boolean {
        if (getUnitAt(target) != null) return false
        val movementCost = hexMap.getMovementCost(unit.position, target)
        return movementCost <= unit.remainingMovement
    }
    
    private fun moveAIUnit(unit: Unit, target: HexCoordinate) {
        val from = unit.position
        val movementCost = hexMap.getMovementCost(from, target)
        
        unit.position = target
        unit.remainingMovement -= movementCost
        
        if (hexMap.getHexOwner(target) != unit.owner) {
            hexMap.setHexOwner(target, unit.owner)
            gameListener?.onTerritoryChanged(target, unit.owner)
        }
        
        gameListener?.onUnitMoved(unit, from, target)
    }
    
    private fun attackWithAIUnit(attacker: Unit, defender: Unit) {
        if (attacker.hasAttacked) return
        
        val terrainBonus = hexMap.getTerrainDefenseBonus(defender.position)
        val damage = calculateDamage(attacker, defender, terrainBonus)
        
        defender.takeDamage(damage)
        attacker.hasAttacked = true
        
        gameListener?.onUnitAttacked(attacker, defender, damage)
        
        if (defender.currentHealth <= 0) {
            units.remove(defender)
            if (defender.owner == Player.PLAYER) playerLosses++
            
            // Захват территории
            if (attacker.type != UnitType.ARCHERS && hexMap.getDistance(attacker.position, defender.position) == 1) {
                moveAIUnit(attacker, defender.position)
            }
        }
    }
    
    private fun findEnemiesInRange(unit: Unit): List<Unit> {
        return units.filter { 
            it.owner == Player.PLAYER && 
            hexMap.getDistance(unit.position, it.position) <= unit.attackRange 
        }
    }
    
    private fun findNearestEnemy(unit: Unit): Unit? {
        return units
            .filter { it.owner == Player.PLAYER }
            .minByOrNull { hexMap.getDistance(unit.position, it.position) }
    }
    
    private fun moveTowardsTarget(unit: Unit, target: HexCoordinate) {
        val path = hexMap.findPath(unit.position, target)
        if (path.size > 1) {
            var currentPos = unit.position
            for (i in 1 until path.size) {
                if (canMoveAIUnit(unit, path[i])) {
                    moveAIUnit(unit, path[i])
                    currentPos = path[i]
                } else {
                    break
                }
            }
        }
    }
    
    private fun createAIUnit(type: UnitType): Boolean {
        // Находим города ИИ без юнитов
        val availableCities = hexMap.getCitiesOwnedBy(Player.AI)
            .filter { getUnitAt(it) == null }
        
        if (availableCities.isEmpty()) return false
        
        val position = availableCities.random()
        val unit = Unit(
            id = units.size + 1,
            type = type,
            owner = Player.AI,
            position = position
        )
        
        units.add(unit)
        aiResources.population -= unitCosts[type]!!
        
        return true
    }
}