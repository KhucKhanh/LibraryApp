package com.example.libraryapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.libraryapp.databinding.ItemLibraryBinding
import com.example.libraryapp.model.Library

class LibraryAdapter(
    private val items: List<Library>,
    private val onClick: (Library) -> Unit,
    private val onRename: (Library) -> Unit,
    private val onDelete: (Library) -> Unit
) : RecyclerView.Adapter<LibraryAdapter.VH>() {

    class VH(val binding: ItemLibraryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemLibraryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val lib = items[position]

        holder.binding.tvLibraryName.text = lib.name
        holder.binding.tvCount.text = "${lib.books.size} books"

        holder.itemView.setOnClickListener { onClick(lib) }

        val isSystem = lib.id == "liked"
        holder.binding.btnMore.visibility =
            if (isSystem) View.GONE else View.VISIBLE

        holder.binding.btnMore.setOnClickListener { anchor ->
            val popup = PopupMenu(anchor.context, anchor)
            popup.menu.add(0, 1, 0, "Đổi tên")
            popup.menu.add(0, 2, 0, "Xoá")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> onRename(lib)
                    2 -> onDelete(lib)
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount() = items.size
}