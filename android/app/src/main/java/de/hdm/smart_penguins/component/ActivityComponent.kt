package de.hdm.smart_penguins.component

import androidx.lifecycle.MutableLiveData
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import de.hdm.smart_penguins.SmartApplication
import de.hdm.smart_penguins.data.manager.ConnectionManager
import de.hdm.smart_penguins.data.manager.DataManager
import de.hdm.smart_penguins.data.model.NodeList
import de.hdm.smart_penguins.data.model.PersistentNode
import de.hdm.smart_penguins.ui.BaseActivity
import de.hdm.smart_penguins.ui.BaseFragment
import de.hdm.smart_penguins.ui.map.MapFragment
import javax.inject.Scope

@Scope
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class ActivityScope

@ActivityScope
@Subcomponent(
    modules = arrayOf(
        ConnectionModule::class,
        LiveDataModule::class
    )
)
interface ActivityComponent {
    fun inject(activity: BaseActivity)
    fun inject(fragment: BaseFragment)
    fun inject(fragment: MapFragment)
    fun inject(manager: ConnectionManager)
}

typealias BleNodesLiveData = MutableLiveData<NodeList>
typealias PersistentLiveData = MutableLiveData<List<PersistentNode>>

@Module
class LiveDataModule {
    @Provides
    @ActivityScope
    fun providesCurrentNodeList() = BleNodesLiveData()

    @Provides
    @ActivityScope
    fun providePersistentList() = PersistentLiveData()
}

@Module
class ConnectionModule {
    @ActivityScope
    @Provides
    fun providesConnectionManager(application: SmartApplication): ConnectionManager {
        return ConnectionManager(application)
    }
}

@Module
class DataModule {
    @ActivityScope
    @Provides
    fun providesDataManager(application: SmartApplication): DataManager {
        return DataManager(application)
    }
}



