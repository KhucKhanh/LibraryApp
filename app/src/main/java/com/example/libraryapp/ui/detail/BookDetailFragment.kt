package com.example.libraryapp.ui.detail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.libraryapp.R
import com.example.libraryapp.adapter.ChapterAdapter
import com.example.libraryapp.databinding.FragmentBookDetailBinding
import com.example.libraryapp.ui.reader.ChapterViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions


class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BookDetailViewModel
    private lateinit var bookId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            .load(arguments?.getString("imageUrl"))
            .into(binding.imgBook)

        // 🔥 SAVE RECENT CATEGORIES

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null && category != null) {
            val userRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)

            userRef.get().addOnSuccessListener { doc ->
                val list = doc.get("recentCategories") as? MutableList<String>
                    ?: mutableListOf()

                list.add(category)

                if (list.size > 5) {
                    list.removeAt(0)
                }

                userRef.set(
                    mapOf("recentCategories" to list),
                    SetOptions.merge()
                )
                    .addOnSuccessListener {
                        Log.d("DEBUG", "Saved recentCategories: $list")
                    }
                    .addOnFailureListener {
                        Log.e("DEBUG", "Error saving categories", it)
                    }
            }
        }


        val chapterAdapter = ChapterAdapter(emptyList()) { chapter ->
            val bundle = Bundle().apply {
                putString("bookId", chapter.bookId)
                putInt("order", chapter.order)
            }
            findNavController().navigate(
                R.id.action_bookDetailFragment_to_chapterReaderFragment, bundle
            )
        }

        binding.rvChapters.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chapterAdapter
            setHasFixedSize(true)
        }

        viewModel.chapters.observe(viewLifecycleOwner) { chapterAdapter.updateData(it) }
        viewModel.loadChapters(bookId)

        // "Continue Reading" → lấy lastOrder từ Firestore rồi mới navigate
        binding.btnContinueReading.setOnClickListener {
            val chapterVm = ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
            )[ChapterViewModel::class.java]

            chapterVm.getLastReadOrder(bookId) { lastOrder ->
                val bundle = Bundle().apply {
                    putString("bookId", bookId)
                    putInt("order", lastOrder)
                }
                findNavController().navigate(
                    R.id.action_bookDetailFragment_to_chapterReaderFragment, bundle
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}