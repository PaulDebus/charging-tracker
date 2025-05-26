class ChargingEventAdapter : RecyclerView.Adapter<ChargingEventAdapter.ViewHolder>() {
    private var events = listOf<ChargingEvent>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun submitList(newEvents: List<ChargingEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }

    fun findPositionForDate(dateMillis: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        return events.indexOfFirst { event ->
            event.startTime in startOfDay until endOfDay
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_charging_event, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount() = events.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateText: TextView = itemView.findViewById(R.id.dateText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val batteryText: TextView = itemView.findViewById(R.id.batteryText)
        private val durationText: TextView = itemView.findViewById(R.id.durationText)

        fun bind(event: ChargingEvent) {
            dateText.text = dateFormat.format(Date(event.startTime))
            
            val startTime = timeFormat.format(Date(event.startTime))
            val endTime = event.endTime?.let { timeFormat.format(Date(it)) } ?: "Charging..."
            timeText.text = "$startTime - $endTime"
            
            val batteryChange = if (event.endBatteryLevel != null) {
                "${event.startBatteryLevel}% → ${event.endBatteryLevel}%"
            } else {
                "${event.startBatteryLevel}% → ..."
            }
            batteryText.text = batteryChange
            
            event.endTime?.let { end ->
                val duration = (end - event.startTime) / 1000 / 60 // minutes
                val hours = duration / 60
                val minutes = duration % 60
                durationText.text = if (hours > 0) {
                    "${hours}h ${minutes}m"
                } else {
                    "${minutes}m"
                }
                durationText.visibility = View.VISIBLE
            } ?: run {
                durationText.visibility = View.GONE
            }
        }
    }
}
