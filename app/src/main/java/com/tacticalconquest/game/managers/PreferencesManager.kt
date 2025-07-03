package com.tacticalconquest.game.managers

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tacticalconquest.game.models.*

/**
 * Менеджер для работы с SharedPreferences - сохранение настроек и прогресса
 */
class PreferencesManager private constructor(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "TacticalConquestPrefs"
        private const val KEY_GLORY_POINTS = "glory_points"
        private const val KEY_SETTINGS = "game_settings"
        private const val KEY_PLAYER_STATS = "player_stats"
        private const val KEY_LEVEL_PROGRESS = "level_progress"
        private const val KEY_PURCHASED_ITEMS = "purchased_items"
        private const val KEY_PURCHASED_SKINS = "purchased_skins"
        private const val KEY_UNLOCKED_ACHIEVEMENTS = "unlocked_achievements"
        private const val KEY_SAVED_GAME_STATE = "saved_game_state"
        private const val KEY_SELECTED_SKINS = "selected_skins"
        private const val KEY_UI_THEME = "ui_theme"
        
        @Volatile
        private var INSTANCE: PreferencesManager? = null
        
        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // Очки славы
    fun getGloryPoints(): Int {
        return prefs.getInt(KEY_GLORY_POINTS, 0)
    }
    
    fun setGloryPoints(points: Int) {
        prefs.edit().putInt(KEY_GLORY_POINTS, points).apply()
    }
    
    fun addGloryPoints(points: Int) {
        setGloryPoints(getGloryPoints() + points)
    }
    
    fun spendGloryPoints(points: Int): Boolean {
        val current = getGloryPoints()
        return if (current >= points) {
            setGloryPoints(current - points)
            true
        } else {
            false
        }
    }
    
    // Настройки игры
    fun getSettings(): GameSettings {
        val json = prefs.getString(KEY_SETTINGS, null)
        return if (json != null) {
            gson.fromJson(json, GameSettings::class.java)
        } else {
            GameSettings() // Настройки по умолчанию
        }
    }
    
    fun saveSettings(settings: GameSettings) {
        val json = gson.toJson(settings)
        prefs.edit().putString(KEY_SETTINGS, json).apply()
    }
    
    fun isSoundEnabled(): Boolean = getSettings().soundEnabled
    fun isMusicEnabled(): Boolean = getSettings().musicEnabled
    fun isVibrationEnabled(): Boolean = getSettings().vibrationEnabled
    
    // Статистика игрока
    fun getPlayerStats(): PlayerStats {
        val json = prefs.getString(KEY_PLAYER_STATS, null)
        return if (json != null) {
            gson.fromJson(json, PlayerStats::class.java)
        } else {
            PlayerStats()
        }
    }
    
    fun savePlayerStats(stats: PlayerStats) {
        val json = gson.toJson(stats)
        prefs.edit().putString(KEY_PLAYER_STATS, json).apply()
    }
    
    // Прогресс уровней
    fun getLevelProgress(): Map<Int, LevelProgress> {
        val json = prefs.getString(KEY_LEVEL_PROGRESS, null)
        return if (json != null) {
            val type = object : TypeToken<Map<Int, LevelProgress>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyMap()
        }
    }
    
    fun saveLevelProgress(progress: Map<Int, LevelProgress>) {
        val json = gson.toJson(progress)
        prefs.edit().putString(KEY_LEVEL_PROGRESS, json).apply()
    }
    
    // Покупки
    fun getPurchasedItems(): Set<String> {
        return prefs.getStringSet(KEY_PURCHASED_ITEMS, emptySet()) ?: emptySet()
    }
    
    fun addPurchasedItem(itemId: String) {
        val items = getPurchasedItems().toMutableSet()
        items.add(itemId)
        prefs.edit().putStringSet(KEY_PURCHASED_ITEMS, items).apply()
    }
    
    fun isPurchased(itemId: String): Boolean {
        return itemId in getPurchasedItems()
    }
    
    // Скины
    fun getPurchasedSkins(): Set<String> {
        return prefs.getStringSet(KEY_PURCHASED_SKINS, emptySet()) ?: emptySet()
    }
    
    fun addPurchasedSkin(skinId: String) {
        val skins = getPurchasedSkins().toMutableSet()
        skins.add(skinId)
        prefs.edit().putStringSet(KEY_PURCHASED_SKINS, skins).apply()
    }
    
    fun getSelectedSkins(): Map<String, String> {
        val json = prefs.getString(KEY_SELECTED_SKINS, null)
        return if (json != null) {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyMap()
        }
    }
    
    fun setSelectedSkin(unitType: String, skinId: String) {
        val skins = getSelectedSkins().toMutableMap()
        skins[unitType] = skinId
        val json = gson.toJson(skins)
        prefs.edit().putString(KEY_SELECTED_SKINS, json).apply()
    }
    
    // Достижения
    fun getUnlockedAchievements(): Set<String> {
        return prefs.getStringSet(KEY_UNLOCKED_ACHIEVEMENTS, emptySet()) ?: emptySet()
    }
    
    fun unlockAchievement(achievementId: String) {
        val achievements = getUnlockedAchievements().toMutableSet()
        achievements.add(achievementId)
        prefs.edit().putStringSet(KEY_UNLOCKED_ACHIEVEMENTS, achievements).apply()
    }
    
    // Сохранение игры
    fun getSavedGameState(): SavedGameState? {
        val json = prefs.getString(KEY_SAVED_GAME_STATE, null)
        return if (json != null) {
            gson.fromJson(json, SavedGameState::class.java)
        } else {
            null
        }
    }
    
    fun saveSavedGameState(state: SavedGameState) {
        val json = gson.toJson(state)
        prefs.edit().putString(KEY_SAVED_GAME_STATE, json).apply()
    }
    
    fun clearSavedGameState() {
        prefs.edit().remove(KEY_SAVED_GAME_STATE).apply()
    }
    
    // UI тема
    fun getUITheme(): String {
        return prefs.getString(KEY_UI_THEME, "default") ?: "default"
    }
    
    fun setUITheme(theme: String) {
        prefs.edit().putString(KEY_UI_THEME, theme).apply()
    }
    
    // Сброс прогресса
    fun resetProgress() {
        prefs.edit().clear().apply()
    }
    
    // Проверка первого запуска
    fun isFirstLaunch(): Boolean {
        val isFirst = prefs.getBoolean("first_launch", true)
        if (isFirst) {
            prefs.edit().putBoolean("first_launch", false).apply()
            // Даем начальные очки славы
            setGloryPoints(100)
        }
        return isFirst
    }
}