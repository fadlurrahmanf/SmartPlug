package com.smartplug.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.media.ToneGenerator
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optional tap sound (spec: "Gunakan SoundPool untuk suara tap opsional; pengguna dapat
 * mematikannya."). The `tap` raw resource is looked up by name rather than `R.raw.tap` so the
 * module still compiles/runs with the tap sound disabled if no `res/raw/tap.*` asset is bundled;
 * drop a short click/tap sample at `app/src/main/res/raw/tap.ogg` to enable it.
 */
@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var tapSoundId: Int? = null
    private val fallbackTone = ToneGenerator(AudioManager.STREAM_SYSTEM, 35)

    init {
        runCatching {
            val resId = context.resources.getIdentifier("tap", "raw", context.packageName)
            if (resId != 0) {
                tapSoundId = soundPool.load(context, resId, 1)
            }
        }
    }

    fun playTap(enabled: Boolean) {
        if (!enabled) return
        val id = tapSoundId
        if (id != null) soundPool.play(id, 0.6f, 0.6f, 0, 0, 1f)
        else fallbackTone.startTone(ToneGenerator.TONE_PROP_BEEP2, 20)
    }

    fun release() {
        soundPool.release()
        fallbackTone.release()
    }
}
