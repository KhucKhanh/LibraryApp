package com.example.libraryapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.libraryapp.R
import com.example.libraryapp.databinding.ItemBookBinding
import com.example.libraryapp.data.LibraryRepository
import com.example.libraryapp.model.Book

class BookAdapter(
    private var books: List<Book>,
    private val onClick: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    private val repo = LibraryRepository()
    private val likedCache = mutableMapOf<String, Boolean>()

    class BookViewHolder(val binding: ItemBookBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]

        holder.binding.tvTitle.text = book.title ?: ""
        holder.binding.tvAuthor.text = book.author ?: ""

        Glide.with(holder.itemView.context)
            .load(book.imageUrl ?: "")
            .placeholder(R.drawable.ic_book_placeholder)
            .error(R.drawable.ic_book_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.binding.imgBook)

        val liked = likedCache[book.id] ?: false
        holder.binding.btnFavorite.setImageResource(
            if (liked) R.drawable.ic_favorite
            else R.drawable.ic_favorite_border
        )

        holder.binding.btnFavorite.setOnClickListener {
            repo.toggleLiked(book.id) { isLiked ->
                likedCache[book.id] = isLiked
                holder.binding.btnFavorite.setImageResource(
                    if (isLiked) R.drawable.ic_favorite
                    else R.drawable.ic_favorite_border
                )
            }
        }

        holder.binding.root.setOnClickListener {
            onClick(book)
        }
    }

    override fun getItemCount(): Int = books.size

    fun updateData(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged()
        preloadLikedState()
    }

    private fun preloadLikedState() {
        books.forEach { book ->
            if (!likedCache.containsKey(book.id)) {
                repo.isBookInLibrary("liked", book.id) { liked ->
                    likedCache[book.id] = liked
                    val index = books.indexOfFirst { it.id == book.id }
                    if (index != -1) notifyItemChanged(index)
                }
            }
        }
    }
}