package com.example.libraryapp.ui.reader

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.libraryapp.databinding.FragmentChapterReaderBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChapterReaderFragment : Fragment() {

    private var _binding: FragmentChapterReaderBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChapterViewModel
    private lateinit var bookId: String
    private var chapterOrder: Int = 1

    var db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChapterReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bookId = arguments?.getString("bookId") ?: return
        chapterOrder = arguments?.getInt("order") ?: 1

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        )[ChapterViewModel::class.java]

        viewModel.loadChapters(bookId, chapterOrder)

        viewModel.currentChapter.observe(viewLifecycleOwner) { chapter ->
            if (chapter != null) {
                binding.tvChapterTitle.text = chapter.title
                binding.tvChapterContent.text = chapter.content

                saveRecent(bookId)

                // Lấy scroll từ Firestore theo đúng chapter
                viewModel.getScrollForChapter(bookId, chapter.order) { scrollY ->
                    binding.scrollView.post {
                        binding.scrollView.scrollTo(0, scrollY)
                    }
                }
            }
        }

        binding.btnNext.setOnClickListener { viewModel.nextChapter() }
        binding.btnPrev.setOnClickListener { viewModel.prevChapter() }

        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val currentOrder = viewModel.currentChapter.value?.order
                ?: return@setOnScrollChangeListener
            viewModel.saveReadingPosition(bookId, currentOrder, scrollY)
        }
    }

    private fun saveRecent(bookId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val data = hashMapOf(
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(userId)
            .collection("recent")
            .document(bookId)
            .set(data)

        Log.d("RECENT", "Saved book: $bookId")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}