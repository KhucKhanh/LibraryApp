package com.example.libraryapp.ui.reader

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.libraryapp.databinding.FragmentChapterReaderBinding
import com.example.libraryapp.utils.RecommendationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChapterReaderFragment : Fragment() {

    private var _binding: FragmentChapterReaderBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChapterViewModel

    private lateinit var bookId: String
    private var chapterOrder: Int = 1
    private var category: String? = null

    private var hasStartScore = false
    private var hasFinishScore = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChapterReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        bookId = arguments?.getString("bookId") ?: return
        chapterOrder = arguments?.getInt("order") ?: 1
        category = arguments?.getString("category")

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        )[ChapterViewModel::class.java]

        viewModel.loadChapters(bookId, chapterOrder)

        viewModel.currentChapter.observe(viewLifecycleOwner) { chapter ->

            if (chapter != null) {

                binding.tvChapterTitle.text = chapter.title
                binding.tvChapterContent.text = chapter.content

                saveRecentBook(bookId, chapter.order)

                // 🔥 +2 (START READING)
                if (!hasStartScore) {
                    RecommendationUtils.addCategoryScore(category, 2)
                    hasStartScore = true
                }

                // scroll restore
                viewModel.getScrollForChapter(bookId, chapter.order) { scrollY ->
                    binding.scrollView.post {
                        binding.scrollView.scrollTo(0, scrollY)
                    }
                }
            }
        }

        binding.btnNext.setOnClickListener {
            viewModel.nextChapter()

            // 🔥 +3 (FINISH BOOK)
            if (viewModel.isLastChapter() && !hasFinishScore) {
                RecommendationUtils.addCategoryScore(category, 3)
                hasFinishScore = true
            }
        }

        binding.btnPrev.setOnClickListener {
            viewModel.prevChapter()
        }

        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val currentOrder = viewModel.currentChapter.value?.order
                ?: return@setOnScrollChangeListener
            viewModel.saveReadingPosition(bookId, currentOrder, scrollY)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun saveRecentBook(bookId: String, chapterOrder: Int?) {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val data = hashMapOf(
            "bookId" to bookId,
            "lastChapterId" to chapterOrder,
            "timestamp" to System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("recentBooks")
            .document(bookId)
            .set(data)
    }

}