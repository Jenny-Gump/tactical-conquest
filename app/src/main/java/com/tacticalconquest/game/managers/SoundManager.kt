package com.tacticalconquest.game.managers

import android.content.Context
import android.media.*
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import kotlinx.coroutines.*
import kotlin.math.sin

/**
 * Менеджер звуков и музыки
 */
class SoundManager private constructor(private val context: Context) {
    
    companion object {
        // Звуковые эффекты
        const val SOUND_CLICK = 1
        const val SOUND_MOVE = 2
        const val SOUND_ATTACK = 3
        const val SOUND_CAPTURE = 4
        const val SOUND_VICTORY = 5
        const val SOUND_DEFEAT = 6
        const val SOUND_COIN = 7
        const val SOUND_ERROR = 8
        
        // Музыкальные треки
        const val MUSIC_MENU = 1
        const val MUSIC_GAME = 2
        const val MUSIC_VICTORY = 3
        
        @Volatile
        private var INSTANCE: SoundManager? = null
        
        fun getInstance(context: Context): SoundManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SoundManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefsManager = PreferencesManager.getInstance(context)
    private val vibrator = context.getSystemService<Vibrator>()
    
    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<Int, Int>()
    private var musicPlayer: MediaPlayer? = null
    private var currentMusic: Int? = null
    
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        initSoundPool()
        generateSounds()
    }
    
    private fun initSoundPool() {
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()
    }
    
    /**
     * Генерация звуковых эффектов программно
     */
    private fun generateSounds() {
        scope.launch {
            // Генерируем базовые звуки
            generateClickSound()
            generateMoveSound()
            generateAttackSound()
            generateCaptureSound()
            generateVictorySound()
            generateDefeatSound()
            generateCoinSound()
            generateErrorSound()
        }
    }
    
    private suspend fun generateClickSound() = withContext(Dispatchers.IO) {
        val sound = generateTone(1000.0, 0.05, 0.8) // 1000Hz, 50ms
        saveAndLoadSound(SOUND_CLICK, sound)
    }
    
    private suspend fun generateMoveSound() = withContext(Dispatchers.IO) {
        val sound = generateSweep(500.0, 800.0, 0.2) // Sweep 500-800Hz, 200ms
        saveAndLoadSound(SOUND_MOVE, sound)
    }
    
    private suspend fun generateAttackSound() = withContext(Dispatchers.IO) {
        val sound = generateNoise(0.3, 0.9) // White noise, 300ms
        saveAndLoadSound(SOUND_ATTACK, sound)
    }
    
    private suspend fun generateCaptureSound() = withContext(Dispatchers.IO) {
        val sound = generateChord(doubleArrayOf(440.0, 554.0, 659.0), 0.5) // A major chord
        saveAndLoadSound(SOUND_CAPTURE, sound)
    }
    
    private suspend fun generateVictorySound() = withContext(Dispatchers.IO) {
        val sound = generateFanfare()
        saveAndLoadSound(SOUND_VICTORY, sound)
    }
    
    private suspend fun generateDefeatSound() = withContext(Dispatchers.IO) {
        val sound = generateSadTrombone()
        saveAndLoadSound(SOUND_DEFEAT, sound)
    }
    
    private suspend fun generateCoinSound() = withContext(Dispatchers.IO) {
        val sound = generateCoinDrop()
        saveAndLoadSound(SOUND_COIN, sound)
    }
    
    private suspend fun generateErrorSound() = withContext(Dispatchers.IO) {
        val sound = generateTone(200.0, 0.2, 0.7) // Low tone
        saveAndLoadSound(SOUND_ERROR, sound)
    }
    
    /**
     * Генерация простого тона
     */
    private fun generateTone(frequency: Double, duration: Double, amplitude: Double): ByteArray {
        val sampleRate = 44100
        val numSamples = (duration * sampleRate).toInt()
        val samples = DoubleArray(numSamples)
        val generatedSound = ByteArray(2 * numSamples)
        
        // Генерация синусоиды
        for (i in 0 until numSamples) {
            samples[i] = sin(2 * Math.PI * i * frequency / sampleRate) * amplitude
        }
        
        // Конвертация в 16-bit PCM
        var idx = 0
        for (sample in samples) {
            val value = (sample * 32767).toInt().toShort()
            generatedSound[idx++] = (value.toInt() and 0x00ff).toByte()
            generatedSound[idx++] = ((value.toInt() and 0xff00) shr 8).toByte()
        }
        
        return generatedSound
    }
    
    /**
     * Генерация частотного свипа
     */
    private fun generateSweep(startFreq: Double, endFreq: Double, duration: Double): ByteArray {
        val sampleRate = 44100
        val numSamples = (duration * sampleRate).toInt()
        val samples = DoubleArray(numSamples)
        val generatedSound = ByteArray(2 * numSamples)
        
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val frequency = startFreq + (endFreq - startFreq) * progress
            samples[i] = sin(2 * Math.PI * i * frequency / sampleRate) * 0.7
        }
        
        // Fade in/out
        applyEnvelope(samples, 0.05, 0.05)
        
        // Конвертация в PCM
        var idx = 0
        for (sample in samples) {
            val value = (sample * 32767).toInt().toShort()
            generatedSound[idx++] = (value.toInt() and 0x00ff).toByte()
            generatedSound[idx++] = ((value.toInt() and 0xff00) shr 8).toByte()
        }
        
        return generatedSound
    }
    
    /**
     * Генерация белого шума
     */
    private fun generateNoise(duration: Double, amplitude: Double): ByteArray {
        val sampleRate = 44100
        val numSamples = (duration * sampleRate).toInt()
        val samples = DoubleArray(numSamples)
        val generatedSound = ByteArray(2 * numSamples)
        
        for (i in 0 until numSamples) {
            samples[i] = (Math.random() - 0.5) * 2 * amplitude
        }
        
        applyEnvelope(samples, 0.01, 0.1)
        
        var idx = 0
        for (sample in samples) {
            val value = (sample * 32767).toInt().toShort()
            generatedSound[idx++] = (value.toInt() and 0x00ff).toByte()
            generatedSound[idx++] = ((value.toInt() and 0xff00) shr 8).toByte()
        }
        
        return generatedSound
    }
    
    /**
     * Генерация аккорда
     */
    private fun generateChord(frequencies: DoubleArray, duration: Double): ByteArray {
        val sampleRate = 44100
        val numSamples = (duration * sampleRate).toInt()
        val samples = DoubleArray(numSamples)
        val generatedSound = ByteArray(2 * numSamples)
        
        for (i in 0 until numSamples) {
            var sample = 0.0
            for (freq in frequencies) {
                sample += sin(2 * Math.PI * i * freq / sampleRate)
            }
            samples[i] = sample / frequencies.size * 0.7
        }
        
        applyEnvelope(samples, 0.05, 0.2)
        
        var idx = 0
        for (sample in samples) {
            val value = (sample * 32767).toInt().toShort()
            generatedSound[idx++] = (value.toInt() and 0x00ff).toByte()
            generatedSound[idx++] = ((value.toInt() and 0xff00) shr 8).toByte()
        }
        
        return generatedSound
    }
    
    /**
     * Генерация победного фанфара
     */
    private fun generateFanfare(): ByteArray {
        val notes = listOf(
            Pair(523.25, 0.2),  // C5
            Pair(659.25, 0.2),  // E5
            Pair(783.99, 0.2),  // G5
            Pair(1046.50, 0.4) // C6
        )
        
        val sampleRate = 44100
        val totalSamples = (notes.sumOf { it.second } * sampleRate).toInt()
        val samples = DoubleArray(totalSamples)
        
        var sampleIndex = 0
        for ((freq, duration) in notes) {
            val noteSamples = (duration * sampleRate).toInt()
            for (i in 0 until noteSamples) {
                samples[sampleIndex++] = sin(2 * Math.PI * i * freq / sampleRate) * 0.6
            }
        }
        
        applyEnvelope(samples, 0.01, 0.1)
        
        val generatedSound = ByteArray(2 * totalSamples)
        var idx = 0
        for (sample in samples) {
            val value = (sample * 32767).toInt().toShort()
            generatedSound[idx++] = (value.toInt() and 0x00ff).toByte()
            generatedSound[idx++] = ((value.toInt() and 0xff00) shr 8).toByte()
        }
        
        return generatedSound
    }
    
    /**
     * Генерация грустного тромбона
     */
    private fun generateSadTrombone(): ByteArray {
        val sampleRate = 44100
        val duration = 1.0
        val numSamples = (duration * sampleRate).toInt()
        val samples = DoubleArray(numSamples)
        
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val frequency = 300 - 100 * progress // Падающая частота
            samples[i] = sin(2 * Math.PI * i * frequency / sampleRate) * 0.5
        }
        
        applyEnvelope(samples, 0.05, 0.3)
        
        val generatedSound = ByteArray(2 * numSamples)
        var idx = 0
        for (sample in samples) {
            val value = (sample * 32767).toInt().toShort()
            generatedSound[idx++] = (value.toInt() and 0x00ff).toByte()
            generatedSound[idx++] = ((value.toInt() and 0xff00) shr 8).toByte()
        }
        
        return generatedSound
    }
    
    /**
     * Генерация звука монетки
     */
    private fun generateCoinDrop(): ByteArray {
        val frequencies = doubleArrayOf(1318.5, 1568.0) // E6, G6
        val sampleRate = 44100
        val duration = 0.3
        val numSamples = (duration * sampleRate).toInt()
        val samples = DoubleArray(numSamples)
        
        for (i in 0 until numSamples) {
            var sample = 0.0
            for (freq in frequencies) {
                sample += sin(2 * Math.PI * i * freq / sampleRate)
            }
            samples[i] = sample / frequencies.size * 0.5
        }
        
        applyEnvelope(samples, 0.01, 0.15)
        
        val generatedSound = ByteArray(2 * numSamples)
        var idx = 0
        for (sample in samples) {
            val value = (sample * 32767).toInt().toShort()
            generatedSound[idx++] = (value.toInt() and 0x00ff).toByte()
            generatedSound[idx++] = ((value.toInt() and 0xff00) shr 8).toByte()
        }
        
        return generatedSound
    }
    
    /**
     * Применение огибающей (fade in/out)
     */
    private fun applyEnvelope(samples: DoubleArray, attackTime: Double, releaseTime: Double) {
        val sampleRate = 44100
        val attackSamples = (attackTime * sampleRate).toInt()
        val releaseSamples = (releaseTime * sampleRate).toInt()
        
        // Fade in
        for (i in 0 until attackSamples.coerceAtMost(samples.size)) {
            samples[i] *= i.toDouble() / attackSamples
        }
        
        // Fade out
        val releaseStart = samples.size - releaseSamples
        for (i in releaseStart until samples.size) {
            if (i >= 0) {
                samples[i] *= (samples.size - i).toDouble() / releaseSamples
            }
        }
    }
    
    /**
     * Сохранение и загрузка звука в SoundPool
     */
    private fun saveAndLoadSound(soundId: Int, audioData: ByteArray) {
        try {
            // Создаем временный файл
            val tempFile = context.cacheDir.resolve("sound_$soundId.wav")
            
            // Записываем WAV заголовок и данные
            tempFile.outputStream().use { stream ->
                writeWavHeader(stream, audioData.size)
                stream.write(audioData)
            }
            
            // Загружаем в SoundPool
            soundPool?.load(tempFile.path, 1)?.let { id ->
                soundIds[soundId] = id
            }
            
            // Удаляем временный файл
            tempFile.delete()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Запись WAV заголовка
     */
    private fun writeWavHeader(stream: java.io.OutputStream, dataSize: Int) {
        val sampleRate = 44100
        val channels = 1
        val bitsPerSample = 16
        
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        
        stream.write("RIFF".toByteArray())
        stream.write(intToByteArray(36 + dataSize))
        stream.write("WAVE".toByteArray())
        stream.write("fmt ".toByteArray())
        stream.write(intToByteArray(16)) // Sub chunk size
        stream.write(shortToByteArray(1)) // Audio format (PCM)
        stream.write(shortToByteArray(channels))
        stream.write(intToByteArray(sampleRate))
        stream.write(intToByteArray(byteRate))
        stream.write(shortToByteArray(blockAlign))
        stream.write(shortToByteArray(bitsPerSample))
        stream.write("data".toByteArray())
        stream.write(intToByteArray(dataSize))
    }
    
    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xff).toByte(),
            ((value shr 8) and 0xff).toByte(),
            ((value shr 16) and 0xff).toByte(),
            ((value shr 24) and 0xff).toByte()
        )
    }
    
    private fun shortToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xff).toByte(),
            ((value shr 8) and 0xff).toByte()
        )
    }
    
    /**
     * Воспроизведение звукового эффекта
     */
    fun playSound(soundId: Int) {
        if (!prefsManager.isSoundEnabled()) return
        
        soundIds[soundId]?.let { id ->
            soundPool?.play(id, 0.7f, 0.7f, 1, 0, 1.0f)
        }
        
        // Вибрация для некоторых звуков
        if (prefsManager.isVibrationEnabled()) {
            when (soundId) {
                SOUND_ATTACK, SOUND_VICTORY, SOUND_DEFEAT -> vibrate(100)
                SOUND_ERROR -> vibrate(200)
            }
        }
    }
    
    /**
     * Воспроизведение музыки
     */
    fun playMusic(musicId: Int) {
        if (!prefsManager.isMusicEnabled()) return
        
        if (currentMusic == musicId && musicPlayer?.isPlaying == true) return
        
        stopMusic()
        
        // Для MVP используем сгенерированную музыку
        scope.launch {
            val musicData = when (musicId) {
                MUSIC_MENU -> generateMenuMusic()
                MUSIC_GAME -> generateGameMusic()
                MUSIC_VICTORY -> generateVictoryMusic()
                else -> return@launch
            }
            
            withContext(Dispatchers.Main) {
                try {
                    val tempFile = context.cacheDir.resolve("music_$musicId.wav")
                    tempFile.outputStream().use { stream ->
                        writeWavHeader(stream, musicData.size)
                        stream.write(musicData)
                    }
                    
                    musicPlayer = MediaPlayer().apply {
                        setDataSource(tempFile.path)
                        isLooping = musicId != MUSIC_VICTORY
                        setVolume(0.5f, 0.5f)
                        prepare()
                        start()
                    }
                    
                    currentMusic = musicId
                    tempFile.delete()
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    /**
     * Генерация простой фоновой музыки для меню
     */
    private fun generateMenuMusic(): ByteArray {
        // Простая мелодия с аккордами
        val sampleRate = 44100
        val duration = 8.0 // 8 секунд, будет зациклена
        val numSamples = (duration * sampleRate).toInt()
        val samples = DoubleArray(numSamples)
        
        // Базовые аккорды
        val chordProgression = listOf(
            doubleArrayOf(261.63, 329.63, 392.00), // C major
            doubleArrayOf(293.66, 369.99, 440.00), // D minor
            doubleArrayOf(329.63, 415.30, 493.88), // E minor
            doubleArrayOf(261.63, 329.63, 392.00)  // C major
        )
        
        val chordDuration = 2.0 // 2 секунды на аккорд
        val samplesPerChord = (chordDuration * sampleRate).toInt()
        
        var sampleIndex = 0
        for (chord in chordProgression) {
            for (i in 0 until samplesPerChord) {
                if (sampleIndex >= numSamples) break
                
                var sample = 0.0
                for (freq in chord) {
                    sample += sin(2 * Math.PI * i * freq / sampleRate) * 0.2
                }
                samples[sampleIndex++] = sample
            }
        }
        
        applyEnvelope(samples, 0.5, 0.5)
        
        val generatedSound = ByteArray(2 * numSamples)
        var idx = 0
        for (sample in samples) {
            val value = (sample * 32767).toInt().toShort()
            generatedSound[idx++] = (value.toInt() and 0x00ff).toByte()
            generatedSound[idx++] = ((value.toInt() and 0xff00) shr 8).toByte()
        }
        
        return generatedSound
    }
    
    /**
     * Генерация игровой музыки
     */
    private fun generateGameMusic(): ByteArray {
        // Более динамичная музыка для игры
        return generateMenuMusic() // Пока используем ту же музыку
    }
    
    /**
     * Генерация победной музыки
     */
    private fun generateVictoryMusic(): ByteArray {
        return generateFanfare() // Используем фанфары
    }
    
    fun pauseMusic() {
        musicPlayer?.pause()
    }
    
    fun resumeMusic() {
        if (prefsManager.isMusicEnabled()) {
            musicPlayer?.start()
        }
    }
    
    fun stopMusic() {
        musicPlayer?.apply {
            stop()
            release()
        }
        musicPlayer = null
        currentMusic = null
    }
    
    private fun vibrate(duration: Long) {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(duration)
            }
        }
    }
    
    fun release() {
        scope.cancel()
        soundPool?.release()
        soundPool = null
        stopMusic()
    }
}