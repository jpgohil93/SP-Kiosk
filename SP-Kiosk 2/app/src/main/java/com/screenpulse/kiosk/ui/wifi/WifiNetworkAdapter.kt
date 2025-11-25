package com.screenpulse.kiosk.ui.wifi

import android.net.wifi.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.screenpulse.kiosk.R

class WifiNetworkAdapter(
    private val onNetworkSelected: (ScanResult) -> Unit
) : RecyclerView.Adapter<WifiNetworkAdapter.ViewHolder>() {

    private var networks: List<ScanResult> = emptyList()

    fun submitList(newNetworks: List<ScanResult>) {
        networks = newNetworks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wifi_network, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(networks[position])
    }

    override fun getItemCount(): Int = networks.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ssidText: TextView = itemView.findViewById(R.id.ssidText)
//        private val securityText: TextView = itemView.findViewById(R.id.securityText)

        fun bind(scanResult: ScanResult) {
            ssidText.text = scanResult.SSID
//            securityText.text = scanResult.capabilities
            itemView.setOnClickListener { onNetworkSelected(scanResult) }
        }
    }
}
