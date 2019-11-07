package de.hdm.closeme.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import de.hdm.closeme.model.AlarmBeacon

@Database(entities = [(AlarmBeacon::class)], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun beaconDao(): BeaconDataDao


    companion object {

        /**
         * The only instance
         */
        private var sInstance: AppDatabase? = null

        /**
         * Gets the singleton instance of SampleDatabase.
         *
         * @param context The context.
         * @return The singleton instance of SampleDatabase.
         */
        @Synchronized
        fun getInstance(context: Context): AppDatabase {
            if (sInstance == null) {
                sInstance = Room

                        .databaseBuilder(context.applicationContext, AppDatabase::class.java, "beacon_db")
                        .fallbackToDestructiveMigration()
                        .build()
            }
            return sInstance!!
        }


    }

}