package io.github.ccch1mneyyy.dsh_app.phone.a11y

import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// 屏幕四边炫光（EdgeBeam）已按补丁删除；这里只留 OverlayLook 取色用的调色板。
internal object EdgeBeamPalette {
    /**
     * The edge's own tones: [fromSeed]'s hues, more saturated, and darker on a [dark] == false
     * screen, so the colour reads over whatever app is underneath.
     */
    fun edge(seed: Int, dark: Boolean): IntArray = fromSeed(seed).map {
        val (h, s, _) = hsl(it)
        fromHsl(h, (s * 1.45f).coerceIn(0.6f, 0.88f), if (dark) 0.62f else 0.5f)
    }.toIntArray()

    /** ARGB of the nine rim colours for the accent [seed] (ARGB). */
    fun fromSeed(seed: Int): IntArray {
        val (hue, s, _) = hsl(seed)
        val sat = (s * 0.85f).coerceIn(0.4f, 0.6f)
        fun tone(shift: Float, s: Float, l: Float) = fromHsl(((hue + shift) % 360f + 360f) % 360f, s.coerceIn(0f, 1f), l)
        return intArrayOf(
            tone(0f, sat, 0.62f),
            tone(-24f, sat * 0.9f, 0.66f),
            tone(18f, sat, 0.6f),
            tone(-10f, sat * 1.1f, 0.58f),
            tone(32f, sat * 0.85f, 0.66f),
            tone(-36f, sat, 0.62f),
            tone(8f, sat * 0.9f, 0.7f),
            tone(-16f, sat, 0.6f),
            tone(26f, sat, 0.64f),
        )
    }

    /** Hue (0–360), saturation and lightness of an ARGB colour (Flutter's HSLColor.fromColor). */
    fun hsl(argb: Int): Triple<Float, Float, Float> {
        val r = (argb shr 16 and 0xFF) / 255f
        val g = (argb shr 8 and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val hue = when {
            max == 0f || delta == 0f -> 0f
            max == r -> 60f * ((((g - b) / delta) % 6f + 6f) % 6f)
            max == g -> 60f * ((b - r) / delta + 2f)
            else -> 60f * ((r - g) / delta + 4f)
        }
        val l = (max + min) / 2f
        val s = if (l == 1f) 0f else (delta / (1f - abs(2f * l - 1f))).coerceIn(0f, 1f)
        return Triple(hue, s, l)
    }

    /** Opaque ARGB from hue, saturation, lightness (Flutter's HSLColor.toColor). */
    fun fromHsl(hue: Float, s: Float, l: Float): Int {
        val chroma = (1f - abs(2f * l - 1f)) * s
        val secondary = chroma * (1f - abs((hue / 60f) % 2f - 1f))
        val match = l - chroma / 2f
        val (r, g, b) = when {
            hue < 60f -> Triple(chroma, secondary, 0f)
            hue < 120f -> Triple(secondary, chroma, 0f)
            hue < 180f -> Triple(0f, chroma, secondary)
            hue < 240f -> Triple(0f, secondary, chroma)
            hue < 300f -> Triple(secondary, 0f, chroma)
            else -> Triple(chroma, 0f, secondary)
        }
        fun ch(v: Float) = ((v + match) * 255f).roundToInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }
}
