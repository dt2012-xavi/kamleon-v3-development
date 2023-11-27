package com.dynatech2012.kamleonuserapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MeasureDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasure(measureData: MeasureData): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasures(measureDataList: List<MeasureData>)

    @Query("DELETE FROM MeasureData WHERE analysisDate < :analysisDate")
    suspend fun deleteMeasuresFromDate(analysisDate: Long)

    @Query("SELECT * FROM MeasureData WHERE analysisDate <= :date")
    suspend fun getMeasuresFromDate(date: Long): List<MeasureData>?

    @Query("SELECT * FROM MeasureData")
    suspend fun getAllMeasures(): List<MeasureData>?

    @Query("UPDATE MeasureData SET isNew = 0 WHERE isNew = 1")
    suspend fun setMeasureAsNotNew(): Int

    @Query("DELETE FROM MeasureData")
    suspend fun deleteAllMeasures()

    /*
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGroupItems(ArrayList<GroupItem> items)

    @Delete
    void deleteGroupItem(GroupItem groupItem);

    @Update
    void updateGroupItem(GroupItem groupItem);

    @Query("SELECT * FROM GroupItem WHERE groupId = :groupId")
    GroupItem getGroupItemById(String groupId);

    @Query("SELECT * FROM GroupItem")
    List<GroupItem> getGroupItems();

    @Query("DELETE FROM GroupItem")
    void deleteAllGroupItems();
    */
}