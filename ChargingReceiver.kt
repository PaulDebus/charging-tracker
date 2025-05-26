class ChargingReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
        val database = Room.databaseBuilder(
            context.applicationContext,
            ChargingDatabase::class.java,
            "charging_database"
        ).build()

        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val batteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1

        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    val event = ChargingEvent(
                        startTime = System.currentTimeMillis(),
                        startBatteryLevel = batteryLevel
                    )
                    database.chargingEventDao().insertEvent(event)
                }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    val lastEvent = database.chargingEventDao().getLastEvent()
                    if (lastEvent != null && lastEvent.endTime == null) {
                        lastEvent.endTime = System.currentTimeMillis()
                        lastEvent.endBatteryLevel = batteryLevel
                        database.chargingEventDao().updateEvent(lastEvent)
                    }
                }
            }
        }
    }
}
