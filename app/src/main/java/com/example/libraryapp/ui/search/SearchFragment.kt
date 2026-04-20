package com.example.libraryapp.ui.search

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libraryapp.adapter.BookAdapter
import com.example.libraryapp.databinding.FragmentSearchBinding
import kotlinx.coroutines.*

import android.text.Editable
import android.text.TextWatcher

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var viewModel: SearchViewModel
    private lateinit var adapter: BookAdapter

    private var job: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewModel = ViewModelProvider(requireActivity())[SearchViewModel::class.java]

        adapter = BookAdapter(emptyList()) {}

        // ✅ QUAN TRỌNG
        binding.rvBooks.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.rvBooks.adapter = adapter

        // observe data
        viewModel.books.observe(viewLifecycleOwner) {
            adapter.updateData(it)

            binding.txtEmpty.visibility =
                if (it.isEmpty() && binding.edtSearch.text.toString().isNotEmpty())
                    View.VISIBLE
                else
                    View.GONE
        }

        // 🔥 search realtime + debounce (FIXED)
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

        // load lần đầu
        viewModel.getAllBooks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        job?.cancel() // ✅ tránh leak
    }
}