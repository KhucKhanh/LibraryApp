package com.example.libraryapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.libraryapp.databinding.ItemLibraryBinding
import com.example.libraryapp.model.Library

class LibraryAdapter(
    private val items: List<Library>,
    private val onClick: (Library) -> Unit
) : RecyclerView.Adapter<LibraryAdapter.VH>() {

    class VH(val binding: ItemLibraryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {

        val binding = ItemLibraryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {

        val lib = items[position]

        holder.binding.tvLibraryName.text = lib.name
        holder.binding.tvCount.text = "${lib.books.size} books"

        // click sau này mở list sách
        holder.itemView.setOnClickListener {
            onClick(lib)
        }
    }

    override fun getItemCount() = items.size
}