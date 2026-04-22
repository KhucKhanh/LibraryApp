package com.example.libraryapp.ui.home

import android.os.Bundle
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

        val onBookClick: (Book) -> Unit = { selectedBook ->

            AIContextManager.lastSelectedBook = selectedBook

            val bundle = Bundle().apply {
                putString("bookId", selectedBook.id)
                putString("title", selectedBook.title)
                putString("author", selectedBook.author)
                putString("description", selectedBook.description)
                putString("imageUrl", selectedBook.imageUrl)
                putString("category", selectedBook.category)
            }

            findNavController().navigate(
                R.id.action_homeFragment_to_bookDetailFragment,
                bundle
            )

        }

        // ===== Adapters =====
        adapter = BookAdapter(emptyList(), onBookClick)
        recommendedAdapter = BookAdapter(emptyList(), onBookClick)
        recentAdapter = BookAdapter(emptyList(), onBookClick)

        // ===== RecyclerViews =====
        binding.rvBooks.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvBooks.adapter = adapter

        binding.rvRecommended.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvRecommended.adapter = recommendedAdapter

        binding.rvRecent.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvRecent.adapter = recentAdapter

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        viewModel.loadRecommendations()
        viewModel.loadRecentBooks()

        viewModel.books.observe(viewLifecycleOwner) {
            adapter.updateData(it)

            AIContextManager.currentScreen = "Home"
            AIContextManager.allBooks = it
        }

        viewModel.recommendedBooks.observe(viewLifecycleOwner) {
            recommendedAdapter.updateData(it)
        }

        viewModel.recentBooks.observe(viewLifecycleOwner) {
            recentAdapter.updateData(it)
        }
    }
}