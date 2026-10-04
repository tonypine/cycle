package com.tonypine.cycle

import android.app.Application
import com.tonypine.cycle.core.data.CycleData

/** Holds the one [CycleData] of the process: DataStore allows a single instance per file. */
class CycleApplication : Application() {
    val data: CycleData by lazy { CycleData(this) }
}
