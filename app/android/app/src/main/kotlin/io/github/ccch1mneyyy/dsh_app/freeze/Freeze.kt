package io.github.ccch1mneyyy.dsh_app.freeze

import android.content.Context
import android.content.SharedPreferences
import io.github.ccch1mneyyy.dsh_app.pet.PetOverlayService
import io.github.ccch1mneyyy.dsh_app.phone.a11y.PhoneA11yService
import io.github.ccch1mneyyy.dsh_app.runtime.RuntimeController

/**
 * 一键冻结: everything DSH keeps in the background stops at once — the local engine, the pet's
 * service and windows, a running phone-control task and its overlay — so nothing holds the phone
 * awake afterwards. With no foreground component left the system may also end the process.
 *
 * Opening the app thaws it: [clear] runs in `MainActivity.onCreate`, and 「打开 App 时自动启动」
 * (LaunchStart) starts the engine again if the person wants it. The flag outlives a process death
 * so the pet and phone control are not silently brought back while frozen.
 */
object Freeze {
    private const val FILE = "dsh_freeze"
    private const val K_AT = "frozenAt"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** True from 一键冻结 until the app is opened again. */
    fun isFrozen(ctx: Context): Boolean = prefs(ctx).getLong(K_AT, 0L) > 0L

    /**
     * Stops every background part of DSH. Returns whether the engine itself was running, so the
     * caller can say what was stopped.
     */
    fun freeze(ctx: Context): Boolean {
        val app = ctx.applicationContext
        val controller = RuntimeController.get(app)
        val engine = controller.phase == RuntimeController.Phase.RUNNING ||
            controller.phase == RuntimeController.Phase.STARTING
        prefs(app).edit().putLong(K_AT, System.currentTimeMillis()).putBoolean("engine", engine).apply()
        // Phone control first: its overlay and the task it shows go with the engine.
        runCatching { PhoneA11yService.instance?.stopAll("一键冻结") }
        runCatching { PetOverlayService.ensure(app, false) }
        runCatching { controller.stop() }
        return engine
    }

    /** Opening the app ends a freeze (the engine, the pet and phone control start again the usual way). */
    fun clear(ctx: Context) {
        val p = prefs(ctx)
        if (p.getLong(K_AT, 0L) != 0L) p.edit().remove(K_AT).apply()
    }
}
