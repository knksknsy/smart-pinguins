package de.hdm.smart_penguins

import android.app.Application
import de.hdm.smart_penguins.component.*

class SmartApplication : Application() {
    lateinit var applicationComponent: ApplicationComponent

    override fun onCreate() {
        super.onCreate()
        applicationComponent = DaggerApplicationComponent.builder()
            .applicationModule(ApplicationModule(this))
            .build()
    }

    // ActivityComponent (@ActivityScope / Local singleton).
    var activityComponent: ActivityComponent? = null

    fun createActivityComponent(): ActivityComponent {
        if (activityComponent == null) {
            activityComponent = applicationComponent.plus(
                ConnectionModule(),LiveDataModule() )
        }
        return activityComponent!!
    }

    fun destroyActivityComponent() {
        activityComponent = null
    }

}