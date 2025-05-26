package com.example.batterytracker

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var database: ChargingDatabase
    private lateinit var adapter: ChargingEventAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = Room.databaseBuilder(
            applicationContext,
            ChargingDatabase::class.java,
            "charging_database"
        ).build()

        setupUI()
        observeChargingEvents()
        checkCurrentChargingState()
    }

    private fun setupUI() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.recyclerView)
        adapter = ChargingEventAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val fab = findViewById<FloatingActionButton>(R.id.fabDatePicker)
        fab.setOnClickListener {
            showDatePicker()
        }
    }

    private fun observeChargingEvents() {
        lifecycleScope.launch {
            database.chargingEventDao().getAllEvents().collect { events ->
                adapter.submitList(events)
            }
        }
    }

    private fun checkCurrentChargingState() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            registerReceiver(null, filter)
        }
        
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        lifecycleScope.launch {
            val lastEvent = withContext(Dispatchers.IO) {
                database.chargingEventDao().getLastEvent()
            }

            if (isCharging && (lastEvent == null || lastEvent.endTime != null)) {
                // Charger is connected but no active session
                val batteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val event = ChargingEvent(
                    startTime = System.currentTimeMillis(),
                    startBatteryLevel = batteryLevel
                )
                withContext(Dispatchers.IO) {
                    database.chargingEventDao().insertEvent(event)
                }
            }
        }
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            lifecycleScope.launch {
                val position = adapter.findPositionForDate(selection)
                if (position != -1) {
                    recyclerView.scrollToPosition(position)
                }
            }
        }

        datePicker.show(supportFragmentManager, "datePicker")
    }
}
