package com.example.leaflinkappv3

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.leaflinkappv3.databinding.LocalDatabaseItemBinding
import com.example.leaflinkappv3.model.sensorScan

class LocalDatabaseAdapter(private var sensorScans: List<sensorScan>) : RecyclerView.Adapter<LocalDatabaseAdapter.SensorScanViewHolder>() {

    class SensorScanViewHolder(val binding: LocalDatabaseItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorScanViewHolder {
        val binding = LocalDatabaseItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SensorScanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SensorScanViewHolder, position: Int) {
        val sensorScan = sensorScans[position]
        holder.binding.apply {
            timeStampTextView.text = sensorScan.timestamp.toString()
            locationTextView.text = "${sensorScan.latitude}, ${sensorScan.longitude}"
            sensorIdTextView.text = sensorScan.sensorId
        }
    }

    fun updateData(newSensorScans: List<sensorScan>) {
        this.sensorScans = newSensorScans
        notifyDataSetChanged()
    }

    override fun getItemCount() = sensorScans.size
}