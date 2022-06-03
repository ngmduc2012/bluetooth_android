package com.example.bluetooth_android.adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetooth_android.R

internal class ListDeviceAdapter(private var itemsList: List<BluetoothDevice>) :
    RecyclerView.Adapter<ListDeviceAdapter.MyViewHolder>()
    {
        var onItemClick: ((BluetoothDevice) -> Unit)? = null
        internal inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var tv_name_item: TextView = view.findViewById(R.id.tv_name_item)
            var tv_mac_item: TextView = view.findViewById(R.id.tv_mac_item)
            init {
                itemView.setOnClickListener {
                    onItemClick?.invoke(itemsList[adapterPosition])
                }
            }
        }

        @NonNull
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_list, parent, false)
            return MyViewHolder(itemView)
        }

        @SuppressLint("MissingPermission")
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val item = itemsList[position]
            holder.tv_name_item.text = item.name
            holder.tv_mac_item.text = item.address
        }


        override fun getItemCount(): Int {
            return itemsList.size
        }


}