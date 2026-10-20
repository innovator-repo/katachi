package app.katachi

import android.app.Application

class KatachiApp : Application() {
    lateinit var store: Store
        private set

    lateinit var lumeState: LumeState
        private set

    override fun onCreate() {
        super.onCreate()
        lumeState = LumeState(this)
        store = Store(this)
    }
}
