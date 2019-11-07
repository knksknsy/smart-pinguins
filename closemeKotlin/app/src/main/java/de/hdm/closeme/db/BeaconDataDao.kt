package de.hdm.closeme.db

import android.arch.persistence.room.*
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import de.hdm.closeme.model.AlarmBeacon
import de.hdm.closeme.model.AlarmSpot

@Dao
interface BeaconDataDao {

    @Query("SELECT * FROM alarm_beacon")
    fun getAll(): List<AlarmBeacon>

    @Insert(onConflict = REPLACE)
    fun insertOne(alarmBeacon: AlarmBeacon)

    @Query("DELETE FROM alarm_beacon")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(objects: List<AlarmBeacon>)

    @Delete
    fun deleteOne(alarmBeacon: AlarmBeacon)

}