package com.example.libraryapp.ui.reader

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.libraryapp.databinding.FragmentChapterReaderBinding
import com.example.libraryapp.model.Book
import com.example.libraryapp.utils.RecommendationUtils
import com.example.libraryapp.ai.AIContextManager
import com.example.libraryapp.ai.SimpleRAGEngine
import com.example.libraryapp.utils.BookTTSManager

class ChapterReaderFragment : Fragment() {

    private var _binding: FragmentChapterReaderBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChapterViewModel
    private lateinit var bookId: String
    private lateinit var book: Book

    private var chapterOrder: Int = 1
    private var category: String? = null

    private var hasStartScore = false
    private var hasFinishScore = false

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
        super.onViewCreated(view, savedInstanceState)

        bookId = arguments?.getString("bookId") ?: return
        chapterOrder = arguments?.getInt("order") ?: 1
        category = arguments?.getString("category")

        book = Book(
            id = bookId,
            title = arguments?.getString("title") ?: "",
            author = arguments?.getString("author") ?: "",
            description = arguments?.getString("description") ?: "",
            category = category ?: "",
            imageUrl = arguments?.getString("imageUrl") ?: ""
        )

        hasStartScore = false
        hasFinishScore = false

        ttsManager = BookTTSManager(requireContext())

        AIContextManager.currentScreen = "ChapterReader"
        AIContextManager.currentBook = book

        setupViewModel()
        setupButtons()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        )[ChapterViewModel::class.java]

        viewModel.loadChapters(bookId, chapterOrder)

        viewModel.currentChapter.observe(viewLifecycleOwner) { chapter ->
            if (chapter == null || _binding == null) return@observe

            SimpleRAGEngine.clear()

            AIContextManager.currentChapter = chapter.title
            AIContextManager.currentChapterContent = chapter.content

            binding.tvChapterTitle.text = chapter.title
            binding.tvChapterContent.text = chapter.content

            viewModel.saveRecentBook(bookId, chapter.order)

            // +3: Bắt đầu đọc chapter → quan tâm thật sự (chỉ tính 1 lần/phiên)
            if (!hasStartScore) {
                RecommendationUtils.addCategoryScore(category, 3)
                hasStartScore = true
            }

            viewModel.getScrollForChapter(bookId, chapter.order) { scrollY ->
                val b = _binding ?: return@getScrollForChapter

                b.scrollView.post {
                    val safeBinding = _binding ?: return@post
                    safeBinding.scrollView.scrollTo(0, scrollY)
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnNext.setOnClickListener {
            stopReading()
            ttsManager.resetPosition()

            if (viewModel.isLastChapter() && !hasFinishScore) {
                // +5: Hoàn thành sách → tín hiệu mạnh nhất
                RecommendationUtils.addCategoryScore(category, 5)
                hasFinishScore = true
            } else {
                // +2: Hoàn thành 1 chapter thường → đọc liên tục
                RecommendationUtils.addCategoryScore(category, 2)
            }

            viewModel.nextChapter()
        }

        binding.btnPrev.setOnClickListener {
            stopReading()
            ttsManager.resetPosition()
            viewModel.prevChapter()
        }

        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val currentOrder = viewModel.currentChapter.value?.order ?: return@setOnScrollChangeListener
            viewModel.saveReadingPosition(bookId, currentOrder, scrollY)
        }

        binding.btnListen.setOnClickListener {
            Log.d("TTS", "Nút bấm - isReading=$isReading")
            if (isReading) stopReading() else startReading()
        }
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
        if (_binding != null) binding.btnListen.text = "▶ Nghe"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ttsManager.shutdown()
        _binding = null
    }
}