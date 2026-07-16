package com.labfreezer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import coil.ImageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 公平运行内存适配 — 浅度接入
 *
 * 响应 HyperOS / 荣耀 等国内厂商的内存预警(TRIM)和查杀(KILL)广播。
 * - TRIM: 释放内存缓存（Coil 图片缓存）
 * - KILL: 保存当前导航现场，Binder 回调告知系统后等待被查杀
 *
 * 导航路由由 UI 层（MainScreen）通过 [currentRoute] / [currentRouteArgs] 更新。
 */
@Singleton
class FairMemoryReceiver @Inject constructor(
    @ApplicationContext private val context: Context
) : IBinder.DeathRecipient {

    companion object {
        private const val TAG = "FairMemory"
        private const val ACTION_TRIM = "itgsa.intent.action.TRIM"
        private const val ACTION_KILL = "itgsa.intent.action.KILL"
        private const val PREFS_NAME = "fair_memory_prefs"

        private const val KEY_SAVED_ROUTE = "saved_route"
        private const val KEY_SAVED_ARGS = "saved_args"

        /** 由 UI 层更新的当前路由 pattern（如 "device_detail/{deviceId}"） */
        @Volatile
        var currentRoute: String? = null

        /** 由 UI 层更新的路由参数 JSON（如 {"deviceId":5}） */
        @Volatile
        var currentRouteArgsJson: String? = null
    }

    private var mRemote: IBinder? = null
    private var mHandler: Handler? = null
    private var initialized = false

    /** 在 Application.onCreate 中调用 */
    fun initialize() {
        if (initialized) return
        val ht = HandlerThread(TAG)
        ht.start()
        mHandler = Handler(ht.looper)

        val filter = IntentFilter().apply {
            addAction(ACTION_TRIM)
            addAction(ACTION_KILL)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(innerReceiver, filter, null, mHandler, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(innerReceiver, filter, null, mHandler)
        }
        initialized = true
        Log.i(TAG, "FairMemoryReceiver initialized")
    }

    // ---------- BroadcastReceiver ----------

    private val innerReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            val data = intent.extras ?: return
            val common = data.getBundle("common") ?: return

            val notifyType = common.getInt("notifyType")
            val notifyId = common.getInt("notifyId")
            val reason = common.getString("reason", "")
            val callbackBinder = common.getBinder("callback")

            Log.i(TAG, "action=$action notifyType=$notifyType notifyId=$notifyId reason=$reason")

            if (action == ACTION_TRIM) {
                releaseMemory()
            } else if (action == ACTION_KILL) {
                saveState()
            }

            if (callbackBinder != null) {
                handleReceived(notifyType, notifyId, callbackBinder)
            }
        }
    }

    // ---------- 内存释放 (TRIM) ----------

    private fun releaseMemory() {
        try {
            // 清理 Coil 图片缓存（AsyncImage 的默认 ImageLoader）
            ImageLoader(context).memoryCache?.clear()
            Log.i(TAG, "Coil memory cache cleared")
        } catch (e: Exception) {
            Log.w(TAG, "releaseMemory failed", e)
        }
    }

    // ---------- 状态保存 (KILL) ----------

    private fun saveState() {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val route = currentRoute
            if (route != null) {
                prefs.edit().apply {
                    putString(KEY_SAVED_ROUTE, route)
                    if (currentRouteArgsJson != null) {
                        putString(KEY_SAVED_ARGS, currentRouteArgsJson)
                    } else {
                        remove(KEY_SAVED_ARGS)
                    }
                    apply()
                }
                Log.i(TAG, "State saved: route=$route")
            } else {
                Log.w(TAG, "saveState skipped: no route tracked")
            }
        } catch (e: Exception) {
            Log.w(TAG, "saveState failed", e)
        }
    }

    /**
     * 在 MainScreen 启动时调用，检查是否有被查杀前保存的现场。
     * 如有则恢复导航，返回 true。
     */
    fun restoreState(): SavedNavState? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val route = prefs.getString(KEY_SAVED_ROUTE, null) ?: return null
        val argsJson = prefs.getString(KEY_SAVED_ARGS, null)

        // 读取后清除
        prefs.edit().clear().apply()

        if (argsJson != null) {
            val args = try {
                JSONObject(argsJson)
            } catch (_: Exception) {
                null
            }
            if (args != null && args.length() > 0) {
                return SavedNavState(route, args)
            }
        }
        return SavedNavState(route, null)
    }

    data class SavedNavState(
        val routePattern: String,
        val args: JSONObject?   // null 表示无需参数的路由
    )

    // ---------- Binder 回调 ----------

    private fun handleReceived(notifyType: Int, notifyId: Int, callback: IBinder) {
        if (checkRemote(callback)) {
            val extra = Bundle().apply { putString("reply", "ok") }
            reply(notifyType, notifyId, 0, extra)
        }
    }

    private fun checkRemote(callback: IBinder): Boolean {
        if (mRemote == null) {
            try {
                mRemote = callback
                mRemote?.linkToDeath(this, 0)
            } catch (_: Exception) {
                mRemote = null
                return false
            }
        }
        return true
    }

    override fun binderDied() {
        mRemote?.let {
            try { it.unlinkToDeath(this, 0) } catch (_: Exception) {}
        }
        mRemote = null
    }

    private fun reply(notifyType: Int, notifyId: Int, result: Int, extra: Bundle) {
        val remote = mRemote ?: return
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInt(notifyType)
            data.writeInt(notifyId)
            data.writeInt(result)
            data.writeBundle(extra)
            remote.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, IBinder.FLAG_ONEWAY)
            reply.readException()
            Log.i(TAG, "Binder reply success: result=$result")
        } catch (e: Exception) {
            Log.w(TAG, "Binder reply failed", e)
        } finally {
            reply.recycle()
            data.recycle()
        }
    }
}
