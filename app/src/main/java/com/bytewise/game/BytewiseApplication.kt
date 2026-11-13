package com.bytewise.game

import android.app.Application

class BytewiseApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
