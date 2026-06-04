package com.example.vestiaire

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ClothingAdapter(private val clothingList: List<ClothingItem>) :
    RecyclerView.Adapter<ClothingAdapter.ClothingViewHolder>() {

    class ClothingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.ivClothingImage)
        val tvName: TextView = itemView.findViewById(R.id.tvClothingName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClothingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clothing, parent, false)
        return ClothingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClothingViewHolder, position: Int) {
        val clothing = clothingList[position]
        holder.tvName.text = clothing.name

        // Glide to load image from URL
        Glide.with(holder.itemView.context)
            .load(clothing.imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery) // Placeholder while loading
            .into(holder.ivImage)
    }

    override fun getItemCount(): Int {
        return clothingList.size
    }
}