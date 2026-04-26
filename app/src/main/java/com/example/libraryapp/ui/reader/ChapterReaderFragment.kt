package com.example.libraryapp.ui.reader

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.libraryapp.databinding.FragmentChapterReaderBinding
import com.example.libraryapp.utils.RecommendationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.libraryapp.ai.AIContextManager
import com.example.libraryapp.model.Book
import com.example.libraryapp.utils.BookTTSManager


class ChapterReaderFragment : Fragment() {

    private var _binding: FragmentChapterReaderBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChapterViewModel

    private lateinit var bookId: String
    private var chapterOrder: Int = 1
    private var category: String? = null

    private var hasStartScore = false
    private var hasFinishScore = false

    private var title: String? = null
    private var author: String? = null
    private var description: String? = null

    private lateinit var ttsManager: BookTTSManager
    private var isReading = false


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

        title = arguments?.getString("title")
        author = arguments?.getString("author")
        description = arguments?.getString("description")

        ttsManager = BookTTSManager(requireContext())


        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        )[ChapterViewModel::class.java]

        viewModel.loadChapters(bookId, chapterOrder)

        viewModel.currentChapter.observe(viewLifecycleOwner) { chapter ->

            if (chapter != null) {

                val book = Book(
                    id = bookId,
                    title = title ?: "",
                    author = author ?: "",
                    description = description ?: "",
                    category = category ?: "",
                    imageUrl = ""
                )

                AIContextManager.currentScreen = "ChapterReader"
                AIContextManager.currentBook = book
                AIContextManager.currentChapter = chapter.title
                AIContextManager.currentChapterContent = chapter.content

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
            stopReading()
            ttsManager.resetPosition()
            viewModel.nextChapter()

            // 🔥 +3 (FINISH BOOK)
            if (viewModel.isLastChapter() && !hasFinishScore) {
                RecommendationUtils.addCategoryScore(category, 3)
                hasFinishScore = true
            }
        }

        binding.btnPrev.setOnClickListener {
            stopReading()
            ttsManager.resetPosition()
            viewModel.prevChapter()
        }

        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val currentOrder = viewModel.currentChapter.value?.order
                ?: return@setOnScrollChangeListener
            viewModel.saveReadingPosition(bookId, currentOrder, scrollY)
        }

        binding.btnListen.setOnClickListener {
            Log.d("TTS", "Nút bấm - isReading=$isReading")

            if (isReading) {
                stopReading()
            } else {
                startReading()
            }
        }
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

    private fun startReading() {
        if (_binding == null) return
        val content = viewModel.currentChapter.value?.content ?: return

        isReading = true
        binding.btnListen.text = "⏸ Dừng"

        ttsManager.read(content) {
            requireActivity().runOnUiThread {
                if (!isReading || _binding == null) return@runOnUiThread

                if (viewModel.hasNextChapter()) {
                    viewModel.nextChapter()
                    binding.root.postDelayed({ startReading() }, 300)
                } else {
                    stopReading()
                }
            }
        }
    }

    private fun stopReading() {
        isReading = false
        ttsManager.stop()
        binding.btnListen.text = "▶ Nghe"
    }
    override fun onDestroyView() {
        super.onDestroyView()
        ttsManager.shutdown()
        _binding = null
    }
}