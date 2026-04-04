package com.example.libraryapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.libraryapp.databinding.ItemChapterBinding
import com.example.libraryapp.model.Chapter

class ChapterAdapter(
    private var chapters: List<Chapter>,
    private val onClick: (Chapter) -> Unit
) : RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder>() {

    class ChapterViewHolder(val binding: ItemChapterBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val binding = ItemChapterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        val chapter = chapters[position]

        holder.binding.chapterTitle.text = chapter.title

        holder.binding.root.setOnClickListener {
            onClick(chapter)
        }
    }

    override fun getItemCount(): Int = chapters.size

    fun updateData(newChapters: List<Chapter>) {
        chapters = newChapters
        notifyDataSetChanged()
    }
}