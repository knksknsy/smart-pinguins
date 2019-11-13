package de.hdm.smart_penguins.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import de.hdm.smart_penguins.data.manager.ConnectionManager

@Module
class ConnectionModule @Singleton
constructor(private val context: Context) {

    @Singleton
    @Provides
    internal fun provideContext(): Context {
        return context
    }

    @Singleton
    @Provides
    internal fun provideConnectionManager(): ConnectionManager {
        return ConnectionManager.getInstance(provideContext())
    }
}
