package com.example.libraryapp.ui.detail

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.libraryapp.R
import com.example.libraryapp.adapter.ChapterAdapter
import com.example.libraryapp.data.LibraryRepository
import com.example.libraryapp.databinding.FragmentBookDetailBinding
import com.example.libraryapp.ui.reader.ChapterViewModel
import com.example.libraryapp.utils.RecommendationUtils

class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BookDetailViewModel
    private lateinit var bookId: String
    private val libraryRepo = LibraryRepository()

    private var isLiked = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        bookId = arguments?.getString("bookId") ?: return

        val title = arguments?.getString("title") ?: "No title"
        val author = arguments?.getString("author") ?: "No author"
        val description = arguments?.getString("description") ?: "No description"
        val imageUrl = arguments?.getString("imageUrl")
        val category = arguments?.getString("category")

        viewModel = ViewModelProvider(this)[BookDetailViewModel::class.java]

        binding.tvTitle.text = title
        binding.tvAuthor.text = author
        binding.tvDescription.text = description

        Glide.with(requireContext())
            .load(imageUrl)
            .into(binding.imgBook)

        RecommendationUtils.addCategoryScore(category, 1)

        libraryRepo.isBookInLibrary("liked", bookId) { liked ->

            isLiked = liked

            binding.btnFavorite.setImageResource(
                if (liked) R.drawable.ic_favorite
                else R.drawable.ic_favorite_border
            )
        }

        binding.btnFavorite.setOnClickListener {

            libraryRepo.toggleLiked(bookId) { liked ->

                isLiked = liked

                binding.btnFavorite.setImageResource(
                    if (liked) R.drawable.ic_favorite
                    else R.drawable.ic_favorite_border
                )
            }
        }


        val chapterAdapter = ChapterAdapter(emptyList()) { chapter ->
            val bundle = Bundle().apply {
                putString("bookId", chapter.bookId)
                putInt("order", chapter.order)
                putString("category", category)
            }

            findNavController().navigate(
                R.id.action_bookDetailFragment_to_chapterReaderFragment,
                bundle
            )
        }

        binding.rvChapters.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chapterAdapter
        }

        viewModel.chapters.observe(viewLifecycleOwner) {
            chapterAdapter.updateData(it)
        }

        viewModel.loadChapters(bookId)

        binding.btnContinueReading.setOnClickListener {

            val chapterVm = ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
            )[ChapterViewModel::class.java]

            chapterVm.getLastReadOrder(bookId) { lastOrder ->

                val bundle = Bundle().apply {
                    putString("bookId", bookId)
                    putInt("order", lastOrder)
                    putString("category", category)
                }

                findNavController().navigate(
                    R.id.action_bookDetailFragment_to_chapterReaderFragment,
                    bundle
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}