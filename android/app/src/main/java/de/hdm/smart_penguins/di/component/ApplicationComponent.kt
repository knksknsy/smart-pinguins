package de.hdm.smart_penguins.di.component

import dagger.Component
import de.hdm.smart_penguins.di.module.ConnectionModule
import de.hdm.smart_penguins.ui.BaseActivity
import javax.inject.Singleton

@Singleton
@Component(modules = [ConnectionModule::class])
interface ApplicationComponent {

    fun inject(activity: BaseActivity)
}

