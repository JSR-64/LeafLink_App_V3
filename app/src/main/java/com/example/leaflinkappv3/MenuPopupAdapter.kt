package com.example.leaflinkappv3

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.recyclerview.widget.RecyclerView

data class PopupMenuItem(val imageResId: Int, val text: String, val id: Int)

class PopupMenuAdapter(private val items: List<PopupMenuItem>) : RecyclerView.Adapter<PopupMenuAdapter.ViewHolder>() {


    @OptIn(ExperimentalGetImage::class)
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.popupMenuIcon)
        val text: TextView = itemView.findViewById(R.id.popupMenuText)

        init {
            itemView.setOnClickListener {
                val item = items[adapterPosition]
                when (item.id) {
                    R.id.scanPage -> {
                        val intent = Intent(itemView.context, MainActivity::class.java)
                        itemView.context.startActivity(intent)
                    }
                    R.id.localDatabasePage -> {
                        // Open the Local Database Page
                        val intent = Intent(itemView.context, LocalDatabaseActivity::class.java)
                        itemView.context.startActivity(intent)
                    }
                    R.id.page3 -> {
                        // Start Activity or Fragment for Page 3
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.menu_popup_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.imageResId)
        holder.text.text = item.text
    }

    override fun getItemCount() = items.size
}