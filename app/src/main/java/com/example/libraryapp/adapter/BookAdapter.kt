package com.example.libraryapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.libraryapp.databinding.ItemBookBinding
import com.example.libraryapp.data.LibraryRepository
import com.example.libraryapp.model.Book

class BookAdapter(
    private var books: List<Book>,
    private val onClick: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    private val repo = LibraryRepository()


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
            .into(holder.binding.imgBook)

        repo.isBookInLibrary("liked", book.id) { liked ->

            holder.binding.btnFavorite.setImageResource(
                if (liked) com.example.libraryapp.R.drawable.ic_favorite
                else com.example.libraryapp.R.drawable.ic_favorite_border
            )
        }

        holder.binding.btnFavorite.setOnClickListener {

            repo.toggleLiked(book.id) { liked ->

                holder.binding.btnFavorite.setImageResource(
                    if (liked) com.example.libraryapp.R.drawable.ic_favorite
                    else com.example.libraryapp.R.drawable.ic_favorite_border
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
    }
}