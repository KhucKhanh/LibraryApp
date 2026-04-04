package com.example.libraryapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libraryapp.databinding.FragmentHomeBinding
import androidx.navigation.fragment.findNavController
import com.example.libraryapp.R
import com.example.libraryapp.adapter.BookAdapter

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: BookAdapter

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

        adapter = BookAdapter(emptyList()) { selectedBook ->

            val bundle = Bundle().apply {
                putString("bookId", selectedBook.id)
                putString("title", selectedBook.title)
                putString("author", selectedBook.author)
                putString("description", selectedBook.description)
                putString("imageUrl", selectedBook.imageUrl)
            }

            findNavController().navigate(
                R.id.action_homeFragment_to_bookDetailFragment,
                bundle
            )
        }
        binding.rvBooks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBooks.adapter = adapter

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        viewModel.books.observe(viewLifecycleOwner) {
            adapter.updateData(it)
        }
    }
}