package com.example.libraryapp.ui.profile

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.libraryapp.R
import com.example.libraryapp.databinding.ItemSharedBookBinding
import java.text.SimpleDateFormat
import java.util.*

class SharedBookAdapter(
    private val items: List<SharedBookItem>,
    private val onClick: (SharedBookItem) -> Unit
) : RecyclerView.Adapter<SharedBookAdapter.VH>() {

    inner class VH(val binding: ItemSharedBookBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSharedBookBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val b    = holder.binding
        val ctx  = b.root.context

        b.tvBookTitle.text  = item.bookTitle
        b.tvFromName.text   = "${item.fromName} đã chia sẻ sách này với bạn"
        b.tvTimestamp.text  = formatTime(item.timestamp)

        // Chưa đọc → in đậm + chấm xanh
        if (!item.read) {
            b.tvBookTitle.setTypeface(null, Typeface.BOLD)
            b.dotUnread.visibility = android.view.View.VISIBLE
        } else {
            b.tvBookTitle.setTypeface(null, Typeface.NORMAL)
            b.dotUnread.visibility = android.view.View.GONE
        }

        b.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    private fun formatTime(epochSeconds: Long): String {
        if (epochSeconds == 0L) return ""
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(epochSeconds * 1000))
    }
}