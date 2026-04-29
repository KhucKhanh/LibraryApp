package com.example.libraryapp.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.example.libraryapp.R
import com.example.libraryapp.adapter.BookAdapter
import com.example.libraryapp.databinding.FragmentHomeBinding
import com.example.libraryapp.model.Book
import com.example.libraryapp.ai.AIContextManager

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var viewModel: HomeViewModel

    private lateinit var adapter: BookAdapter
    private lateinit var recommendedAdapter: BookAdapter
    private lateinit var recentAdapter: BookAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupViewModel()
    }

    // ✅ Tách riêng — dễ đọc, dễ trình bày trong báo cáo
    private fun navigateToDetail(book: Book) {
        // Cập nhật context cho AI trước khi navigate
        AIContextManager.lastSelectedBook = book
        AIContextManager.currentScreen = "BookDetail"
        AIContextManager.currentBook = book

        val bundle = Bundle().apply {
            putString("bookId", book.id)
            putString("title", book.title)
            putString("author", book.author)
            putString("description", book.description)
            putString("imageUrl", book.imageUrl)
            putString("category", book.category)
        }

        findNavController().navigate(
            R.id.action_homeFragment_to_bookDetailFragment,
            bundle
        )
    }

    private fun setupAdapters() {
        val onBookClick: (Book) -> Unit = { book -> navigateToDetail(book) }

        adapter = BookAdapter(emptyList(), onBookClick)
        recommendedAdapter = BookAdapter(emptyList(), onBookClick)
        recentAdapter = BookAdapter(emptyList(), onBookClick)

        binding.rvBooks.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = this@HomeFragment.adapter
        }

        binding.rvRecommended.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = recommendedAdapter
        }

        binding.rvRecent.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = recentAdapter
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]

        viewModel.loadRecommendations()
        viewModel.loadRecentBooks()

        viewModel.books.observe(viewLifecycleOwner) { books ->
            binding.progressBar.visibility = View.GONE
            binding.rvBooks.visibility = View.VISIBLE
            adapter.updateData(books)
            AIContextManager.allBooks = books
            AIContextManager.currentScreen = "Home"
        }

        viewModel.recommendedBooks.observe(viewLifecycleOwner) { books ->
            books.forEach { Log.d("RECOMMEND", "${it.title} | ${it.category}") }
            recommendedAdapter.updateData(books)
            recommendedAdapter.updateData(books)
        }

        viewModel.recentBooks.observe(viewLifecycleOwner) { books ->
            recentAdapter.updateData(books)
        }
    }
}