package app.katachi

import android.util.Log
import app.katachi.core.GridMath
import app.katachi.core.GridOp
import app.katachi.core.KotlinMath

/**
 * The compiled Lume core (lume/puzzles.lm), loaded from liblumecore.so.
 * Each native call copies the arrays in, runs inside a Lume arena and copies
 * the result out, so nothing Lume allocates outlives the call.
 */
object LumeCore : GridMath {
    private const val TAG = "LumeCore"

    /** True when liblumecore.so loaded and agreed with Kotlin on every operation. */
    val available: Boolean = runCatching {
        System.loadLibrary("lumecore")
        selfCheck()
    }.onFailure { Log.i(TAG, "Lume core not loaded, using Kotlin: ${it.message}") }.getOrDefault(false)

    /** The core in use: compiled Lume when available, Kotlin otherwise. */
    val active: GridMath get() = if (available) this else KotlinMath

    private external fun nativeApply(op: Int, a: DoubleArray, b: DoubleArray): DoubleArray
    private external fun nativeMismatch(a: DoubleArray, b: DoubleArray): Double
    private external fun nativeTangramError(target: DoubleArray, cover: DoubleArray): Double

    override fun apply(op: GridOp, a: DoubleArray, b: DoubleArray) = nativeApply(op.code, a, b)

    override fun mismatch(a: DoubleArray, b: DoubleArray) = nativeMismatch(a, b)

    override fun tangramError(target: DoubleArray, cover: DoubleArray) = nativeTangramError(target, cover)

    private fun selfCheck(): Boolean {
        // An L and a bar that cross it, so every operation has something to move and overlap.
        val a = DoubleArray(GridMath.CELLS).also { it[0] = 1.0; it[6] = 1.0; it[12] = 1.0; it[13] = 1.0 }
        val b = DoubleArray(GridMath.CELLS).also { for (c in 0 until 4) it[12 + c] = 1.0 }
        return GridOp.entries.all { op ->
            KotlinMath.mismatch(nativeApply(op.code, a, b), KotlinMath.apply(op, a, b)) == 0.0
        } && nativeMismatch(a, b) == KotlinMath.mismatch(a, b)
    }
}
