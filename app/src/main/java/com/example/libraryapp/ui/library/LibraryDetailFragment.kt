package com.example.libraryapp.ui.library

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libraryapp.R
import com.example.libraryapp.adapter.LibraryBookAdapter
import com.example.libraryapp.data.LibraryRepository
import com.example.libraryapp.databinding.FragmentLibraryDetailBinding
import com.example.libraryapp.model.Book

class LibraryDetailFragment : Fragment() {

    private var _binding: FragmentLibraryDetailBinding? = null
    private val binding get() = _binding!!

    private val repo = LibraryRepository()

    private lateinit var libraryId: String

    private lateinit var adapter: LibraryBookAdapter

    private var currentBooks: MutableList<Book> = mutableListOf()
    private var likedIds: MutableSet<String> = mutableSetOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        libraryId = arguments?.getString("libraryId") ?: return

        binding.rvBooks.layoutManager =
            LinearLayoutManager(requireContext())

        loadData()
    }

    private fun loadData() {

        repo.getLibraryBooks(libraryId) { books ->

            currentBooks = books.toMutableList()

            repo.getLibraries { libs ->

                val liked = libs.find { it.id == "liked" }
                likedIds = liked?.books?.toMutableSet() ?: mutableSetOf()

                adapter = LibraryBookAdapter(
                    currentBooks,
                    likedIds,
                    onClick = { book ->

                        val bundle = Bundle().apply {
                            putString("bookId", book.id)
                            putString("title", book.title)
                            putString("author", book.author)
                            putString("imageUrl", book.imageUrl)
                        }

                        findNavController().navigate(
                            R.id.action_libraryDetailFragment_to_bookDetailFragment,
                            bundle
                        )
                    },
                    onRemove = { book ->
                        repo.removeBook(libraryId, book.id)
                    },
                    onToggleLike = { book ->
                        repo.toggleLiked(book.id) { newState ->
                            if (newState) likedIds.add(book.id)
                            else likedIds.remove(book.id)

                            adapter.updateData(currentBooks, likedIds)
                        }
                    }
                )

                binding.rvBooks.adapter = adapter
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}