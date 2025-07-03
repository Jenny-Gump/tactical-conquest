package com.tacticalconquest.game

import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.tacticalconquest.game.databinding.ActivitySettingsBinding
import com.tacticalconquest.game.managers.PreferencesManager
import com.tacticalconquest.game.managers.SoundManager
import com.tacticalconquest.game.models.GameSettings
import com.tacticalconquest.game.models.GraphicsQuality

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var soundManager: SoundManager
    private lateinit var settings: GameSettings
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefsManager = PreferencesManager.getInstance(this)
        soundManager = SoundManager.getInstance(this)
        settings = prefsManager.getSettings()
        
        setupUI()
        loadSettings()
    }
    
    private fun setupUI() {
        // Кнопка назад
        binding.btnBack.setOnClickListener {
            soundManager.playSound(SoundManager.SOUND_CLICK)
            saveSettings()
            onBackPressed()
        }
        
        // Переключатели
        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            settings.soundEnabled = isChecked
            soundManager.playSound(SoundManager.SOUND_CLICK)
        }
        
        binding.switchMusic.setOnCheckedChangeListener { _, isChecked ->
            settings.musicEnabled = isChecked
            if (isChecked) {
                soundManager.playMusic(SoundManager.MUSIC_MENU)
            } else {
                soundManager.stopMusic()
            }
        }
        
        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            settings.vibrationEnabled = isChecked
            if (isChecked) {
                soundManager.playSound(SoundManager.SOUND_CLICK)
            }
        }
        
        // Качество графики
        binding.rgGraphicsQuality.setOnCheckedChangeListener { _, checkedId ->
            soundManager.playSound(SoundManager.SOUND_CLICK)
            settings.graphicsQuality = when (checkedId) {
                R.id.rbLow -> GraphicsQuality.LOW
                R.id.rbMedium -> GraphicsQuality.MEDIUM
                R.id.rbHigh -> GraphicsQuality.HIGH
                else -> GraphicsQuality.MEDIUM
            }
        }
        
        // Громкость звука
        binding.seekBarSoundVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvSoundVolumeValue.text = "$progress%"
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                soundManager.playSound(SoundManager.SOUND_CLICK)
            }
        })
        
        // Громкость музыки
        binding.seekBarMusicVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvMusicVolumeValue.text = "$progress%"
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // TODO: Применить громкость к музыке
            }
        })
        
        // Выбор скинов
        binding.btnSelectInfantrySkin.setOnClickListener {
            soundManager.playSound(SoundManager.SOUND_CLICK)
            showSkinSelectionDialog("infantry")
        }
        
        binding.btnSelectArchersSkin.setOnClickListener {
            soundManager.playSound(SoundManager.SOUND_CLICK)
            showSkinSelectionDialog("archers")
        }
        
        binding.btnSelectCavalrySkin.setOnClickListener {
            soundManager.playSound(SoundManager.SOUND_CLICK)
            showSkinSelectionDialog("cavalry")
        }
        
        // Сброс прогресса
        binding.btnResetProgress.setOnClickListener {
            soundManager.playSound(SoundManager.SOUND_CLICK)
            showResetProgressDialog()
        }
        
        // Информация о игре
        binding.btnAbout.setOnClickListener {
            soundManager.playSound(SoundManager.SOUND_CLICK)
            showAboutDialog()
        }
        
        // Политика конфиденциальности
        binding.btnPrivacyPolicy.setOnClickListener {
            soundManager.playSound(SoundManager.SOUND_CLICK)
            showPrivacyPolicyDialog()
        }
    }
    
    private fun loadSettings() {
        // Загружаем текущие настройки
        binding.switchSound.isChecked = settings.soundEnabled
        binding.switchMusic.isChecked = settings.musicEnabled
        binding.switchVibration.isChecked = settings.vibrationEnabled
        
        // Качество графики
        when (settings.graphicsQuality) {
            GraphicsQuality.LOW -> binding.rbLow.isChecked = true
            GraphicsQuality.MEDIUM -> binding.rbMedium.isChecked = true
            GraphicsQuality.HIGH -> binding.rbHigh.isChecked = true
        }
        
        // Громкость (пока используем фиксированные значения)
        binding.seekBarSoundVolume.progress = 70
        binding.seekBarMusicVolume.progress = 50
        binding.tvSoundVolumeValue.text = "70%"
        binding.tvMusicVolumeValue.text = "50%"
        
        // Выбранные скины
        updateSelectedSkins()
    }
    
    private fun updateSelectedSkins() {
        val selectedSkins = prefsManager.getSelectedSkins()
        
        binding.tvSelectedInfantrySkin.text = getSkinName(selectedSkins["infantry"] ?: "default")
        binding.tvSelectedArchersSkin.text = getSkinName(selectedSkins["archers"] ?: "default")
        binding.tvSelectedCavalrySkin.text = getSkinName(selectedSkins["cavalry"] ?: "default")
    }
    
    private fun getSkinName(skinId: String): String {
        return when (skinId) {
            "default" -> "По умолчанию"
            "roman_skin" -> "Римские легионеры"
            "greek_skin" -> "Греческие гоплиты"
            "barbarian_skin" -> "Варвары"
            "egyptian_skin" -> "Египтяне"
            "persian_skin" -> "Персы"
            else -> "По умолчанию"
        }
    }
    
    private fun showSkinSelectionDialog(unitType: String) {
        val purchasedSkins = prefsManager.getPurchasedSkins()
        val availableSkins = mutableListOf("default") // Всегда доступен скин по умолчанию
        availableSkins.addAll(purchasedSkins)
        
        val skinNames = availableSkins.map { getSkinName(it) }.toTypedArray()
        val currentSkin = prefsManager.getSelectedSkins()[unitType] ?: "default"
        val selectedIndex = availableSkins.indexOf(currentSkin).coerceAtLeast(0)
        
        AlertDialog.Builder(this)
            .setTitle("Выберите скин для ${getUnitTypeName(unitType)}")
            .setSingleChoiceItems(skinNames, selectedIndex) { dialog, which ->
                val selectedSkin = availableSkins[which]
                prefsManager.setSelectedSkin(unitType, selectedSkin)
                updateSelectedSkins()
                soundManager.playSound(SoundManager.SOUND_CLICK)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun getUnitTypeName(unitType: String): String {
        return when (unitType) {
            "infantry" -> "пехоты"
            "archers" -> "лучников"
            "cavalry" -> "кавалерии"
            else -> ""
        }
    }
    
    private fun showResetProgressDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.reset_progress)
            .setMessage(R.string.reset_confirm)
            .setPositiveButton(R.string.yes) { _, _ ->
                prefsManager.resetProgress()
                soundManager.playSound(SoundManager.SOUND_CLICK)
                
                // Возвращаемся в главное меню
                finishAffinity()
                startActivity(intent)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }
    
    private fun showAboutDialog() {
        val message = """
            Tactical Conquest v${BuildConfig.VERSION_NAME}
            
            Пошаговая стратегия в античном мире.
            
            Разработчик: Indie Game Studio
            
            Спасибо за игру!
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("О игре")
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }
    
    private fun showPrivacyPolicyDialog() {
        val privacyPolicy = """
            ПОЛИТИКА КОНФИДЕНЦИАЛЬНОСТИ
            
            1. Сбор данных
            Наше приложение НЕ собирает персональные данные пользователей.
            
            2. Локальные данные
            Игра сохраняет прогресс локально на устройстве:
            - Пройденные уровни
            - Заработанные очки славы
            - Купленные предметы
            - Настройки игры
            
            3. Аналитика
            Мы НЕ используем сторонние аналитические сервисы.
            
            4. Реклама
            Игра НЕ содержит рекламы третьих лиц.
            
            5. Покупки
            Все покупки обрабатываются через Google Play Store согласно их политике.
            
            6. Контакты
            По вопросам: support@tacticalconquest.com
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Политика конфиденциальности")
            .setMessage(privacyPolicy)
            .setPositiveButton(R.string.ok, null)
            .show()
    }
    
    private fun saveSettings() {
        prefsManager.saveSettings(settings)
    }
    
    override fun onBackPressed() {
        saveSettings()
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}