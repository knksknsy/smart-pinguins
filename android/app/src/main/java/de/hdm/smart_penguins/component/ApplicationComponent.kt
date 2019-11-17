package de.hdm.smart_penguins.component

import android.app.Application
import android.content.Context
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(ApplicationModule::class))
interface ApplicationComponent {
    fun plus(connectionModule: ConnectionModule,
        liveDataModule: LiveDataModule
    ): ActivityComponent
}


@Module
class ApplicationModule(private val application: Application) {
    @Singleton
    @Provides
    fun provideContext(): Context {
        return application
    }
}

