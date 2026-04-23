package com.example.libraryapp.ui.search

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libraryapp.adapter.BookAdapter
import com.example.libraryapp.databinding.FragmentSearchBinding
import kotlinx.coroutines.*
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import com.example.libraryapp.R

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var viewModel: SearchViewModel
    private lateinit var adapter: BookAdapter

    private var job: Job? = null
    private var isSearching = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewModel = ViewModelProvider(requireActivity())[SearchViewModel::class.java]

        adapter = BookAdapter(emptyList()) { book ->

            val bookId = book.id ?: return@BookAdapter

            Log.d("SEARCH", "Navigating with bookId = $bookId")


            val bundle = Bundle().apply {
                putString("bookId", bookId)
                putString("title", book.title ?: "")
                putString("author", book.author ?: "")
                putString("description", book.description ?: "")
                putString("imageUrl", book.imageUrl ?: "")
                putString("category", book.category ?: "")
            }

            findNavController().navigate(
                R.id.action_searchFragment_to_bookDetailFragment,
                bundle
            )
        }

        binding.rvBooks.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.rvBooks.adapter = adapter

        viewModel.books.observe(viewLifecycleOwner) { list ->
            adapter.updateData(list)

            val query = binding.edtSearch.text.toString().trim()
            binding.txtEmpty.visibility =
                if (list.isEmpty() && query.isNotEmpty())
                    View.VISIBLE
                else
                    View.GONE
        }

        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()

                job?.cancel()
                job = CoroutineScope(Dispatchers.Main).launch {
                    delay(300)
                    viewModel.searchBooks(query)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        viewModel.getAllBooks()


    }

    override fun onDestroyView() {
        super.onDestroyView()
        job?.cancel()
    }
}