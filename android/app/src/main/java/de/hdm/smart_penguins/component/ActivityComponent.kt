package de.hdm.smart_penguins.component

import android.content.Context
import androidx.lifecycle.MutableLiveData
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import de.hdm.smart_penguins.data.manager.ConnectionManager
import de.hdm.smart_penguins.ui.BaseActivity
import org.w3c.dom.NodeList
import javax.inject.Scope

@Scope
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class ActivityScope

@ActivityScope
@Subcomponent(modules = arrayOf(ConnectionModule::class,
    LiveDataModule::class))
interface ActivityComponent {
    fun inject(activity: BaseActivity)
}

typealias BleNodesLiveData = MutableLiveData<NodeList>

@Module
class LiveDataModule {
    @Provides
    @ActivityScope
    fun providesCurrentNodeList() = BleNodesLiveData()
}

@Module
class ConnectionModule {
    @ActivityScope
    @Provides
    fun providesConnectionManager(context: Context): ConnectionManager {
        return ConnectionManager(context)
    }
}



