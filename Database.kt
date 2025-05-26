@Entity(tableName = "charging_events")
data class ChargingEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    var endTime: Long? = null,
    val startBatteryLevel: Int,
    var endBatteryLevel: Int? = null
)

@Dao
interface ChargingEventDao {
    @Query("SELECT * FROM charging_events ORDER BY startTime DESC")
    fun getAllEvents(): Flow<List<ChargingEvent>>

    @Query("SELECT * FROM charging_events WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getLastEvent(): ChargingEvent?

    @Insert
    suspend fun insertEvent(event: ChargingEvent)

    @Update
    suspend fun updateEvent(event: ChargingEvent)
}

@Database(entities = [ChargingEvent::class], version = 1)
abstract class ChargingDatabase : RoomDatabase() {
    abstract fun chargingEventDao(): ChargingEventDao
}
