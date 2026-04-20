package com.example.libraryapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.libraryapp.R
import com.example.libraryapp.databinding.ItemLibraryBookBinding
import com.example.libraryapp.model.Book

class LibraryBookAdapter(
    private var books: MutableList<Book>,
    private var likedIds: MutableSet<String>,
    private val onClick: (Book) -> Unit,
    private val onRemove: (Book) -> Unit,
    private val onToggleLike: (Book) -> Unit
) : RecyclerView.Adapter<LibraryBookAdapter.VH>() {

    class VH(val binding: ItemLibraryBookBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemLibraryBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {

        val book = books[position]
        val isLiked = likedIds.contains(book.id)

        holder.binding.tvTitle.text = book.title ?: ""
        holder.binding.tvAuthor.text = book.author ?: ""

        Glide.with(holder.itemView.context)
            .load(book.imageUrl ?: "")
            .into(holder.binding.imgBook)

        // open book
        holder.itemView.setOnClickListener {
            onClick(book)
        }

        // remove book
        holder.binding.btnRemove.setOnClickListener {

            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                books.removeAt(pos)
                notifyItemRemoved(pos)
            }

            onRemove(book)
        }

        // set heart UI
        holder.binding.btnFavorite.setImageResource(
            if (isLiked) R.drawable.ic_favorite
            else R.drawable.ic_favorite_border
        )

        // toggle like
        holder.binding.btnFavorite.setOnClickListener {

            val newState = !isLiked

            if (newState) likedIds.add(book.id)
            else likedIds.remove(book.id)

            notifyItemChanged(position)

            onToggleLike(book)
        }
    }

    override fun getItemCount(): Int = books.size

    fun updateData(newBooks: List<Book>, newLiked: Set<String>) {
        books = newBooks.toMutableList()
        likedIds = newLiked.toMutableSet()
        notifyDataSetChanged()
    }
}