package com.example.libraryapp.ui.detail

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.libraryapp.R
import com.example.libraryapp.adapter.ChapterAdapter
import com.example.libraryapp.data.LibraryRepository
import com.example.libraryapp.data.ShareRepository
import com.example.libraryapp.databinding.FragmentBookDetailBinding
import com.example.libraryapp.model.Book
import com.example.libraryapp.ui.friend.FriendRepository
import com.example.libraryapp.ui.reader.ChapterViewModel
import com.example.libraryapp.utils.RecommendationUtils
import com.example.libraryapp.ai.AIContextManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BookDetailViewModel
    private lateinit var bookId: String
    private lateinit var book: Book

    private val libraryRepo = LibraryRepository()
    private val friendRepo  = FriendRepository()
    private val shareRepo   = ShareRepository()

    private var isLiked = false

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

        Log.d("BOOK_DETAIL", "bookId = $bookId")

        book = Book(
            id          = bookId,
            title       = arguments?.getString("title")       ?: "No title",
            author      = arguments?.getString("author")      ?: "No author",
            description = arguments?.getString("description") ?: "No description",
            imageUrl    = arguments?.getString("imageUrl")    ?: "",
            category    = arguments?.getString("category")    ?: ""
        )

        setupAIContext()
        setupUI()
        setupFavorite()
        setupChapterList()
        setupButtons()

        viewModel = ViewModelProvider(this)[BookDetailViewModel::class.java]
        viewModel.chapters.observe(viewLifecycleOwner) { chapters ->
            Log.d("FRAGMENT", "observe triggered, size = ${chapters.size}")
            (binding.rvChapters.adapter as? ChapterAdapter)?.updateData(chapters)
        }
        viewModel.loadChapters(bookId)

        if (book.category.isNotEmpty()) {
            RecommendationUtils.addCategoryScore(book.category, 1)
        }
    }

    private fun setupAIContext() {
        AIContextManager.currentScreen         = "BookDetail"
        AIContextManager.currentBook          = book
        AIContextManager.currentChapter       = null
        AIContextManager.currentChapterContent = null
    }

    private fun setupUI() {
        binding.tvTitle.text       = book.title
        binding.tvAuthor.text      = book.author
        binding.tvDescription.text = book.description

        Glide.with(requireContext())
            .load(book.imageUrl)
            .into(binding.imgBook)
    }

    private fun setupFavorite() {
        libraryRepo.isBookInLibrary("liked", bookId) { liked ->
            if (_binding == null) return@isBookInLibrary
            isLiked = liked
            updateFavoriteIcon()
        }

        binding.btnFavorite.setOnClickListener {
            libraryRepo.toggleLiked(bookId) { liked ->
                if (_binding == null) return@toggleLiked
                isLiked = liked
                updateFavoriteIcon()
                if (liked) RecommendationUtils.addCategoryScore(book.category, 4)
            }
        }
    }

    private fun updateFavoriteIcon() {
        binding.btnFavorite.setImageResource(
            if (isLiked) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        )
    }

    private fun setupChapterList() {
        val chapterAdapter = ChapterAdapter(emptyList()) { chapter ->
            AIContextManager.currentChapter = chapter.title
            AIContextManager.currentScreen  = "ChapterReader"
            navigateToReader(order = chapter.order, chapterTitle = chapter.title)
        }

        binding.rvChapters.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter       = chapterAdapter
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnReadFromStart.setOnClickListener {
            navigateToReader(order = 0)
        }

        binding.btnContinueReading.setOnClickListener {
            val chapterVm = ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
            )[ChapterViewModel::class.java]

            chapterVm.getLastReadOrder(bookId) { lastOrder ->
                if (_binding == null) return@getLastReadOrder
                navigateToReader(order = lastOrder)
            }
        }

        binding.btnAddToLibrary.setOnClickListener {
            showAddToLibraryDialog()
        }

        binding.btnShare.setOnClickListener {
            showShareBookDialog()
        }
    }

    // ─── Share ────────────────────────────────────────────────────────────────

    private fun showShareBookDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val friends = try {
                friendRepo.getFriendsFlow().first()
            } catch (e: Exception) {
                if (_binding == null) return@launch
                Toast.makeText(requireContext(), "Không tải được danh sách bạn bè", Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (_binding == null) return@launch

            if (friends.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Chia sẻ sách")
                    .setMessage("Bạn chưa có bạn bè nào. Thêm bạn bè để chia sẻ sách!")
                    .setPositiveButton("OK", null)
                    .show()
                return@launch
            }

            val names        = friends.map { it.displayName.ifEmpty { it.email } }.toTypedArray()
            val checkedItems = BooleanArray(friends.size) { false }

            AlertDialog.Builder(requireContext())
                .setTitle("Chia sẻ sách tới")
                .setMultiChoiceItems(names, checkedItems) { _, index, isChecked ->
                    checkedItems[index] = isChecked
                }
                .setPositiveButton("Chia sẻ") { _, _ ->
                    val selectedIds = friends
                        .filterIndexed { index, _ -> checkedItems[index] }
                        .map { it.uid }

                    if (selectedIds.isEmpty()) {
                        Toast.makeText(requireContext(), "Chọn ít nhất 1 bạn bè", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    shareRepo.shareBookToFriends(bookId, book.title, selectedIds) { success ->
                        if (_binding == null) return@shareBookToFriends
                        Toast.makeText(
                            requireContext(),
                            if (success) "Đã chia sẻ!" else "Chia sẻ thất bại, thử lại!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    // ─── Add to library ───────────────────────────────────────────────────────

    private fun showAddToLibraryDialog() {
        libraryRepo.getLibraries { libs ->
            if (_binding == null) return@getLibraries

            val userLibs = libs.filter { it.id != "liked" }

            if (userLibs.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Chưa có thư viện")
                    .setMessage("Bạn chưa tạo thư viện nào. Tạo ngay?")
                    .setPositiveButton("Tạo") { _, _ -> showCreateLibraryDialog() }
                    .setNegativeButton("Hủy", null)
                    .show()
                return@getLibraries
            }

            val names        = userLibs.map { it.name }.toTypedArray()
            val checkedItems = BooleanArray(userLibs.size) { userLibs[it].books.contains(bookId) }

            AlertDialog.Builder(requireContext())
                .setTitle("Thêm vào thư viện")
                .setMultiChoiceItems(names, checkedItems) { _, index, isChecked ->
                    checkedItems[index] = isChecked
                }
                .setPositiveButton("Lưu") { _, _ ->
                    userLibs.forEachIndexed { index, lib ->
                        val alreadyIn  = lib.books.contains(bookId)
                        val shouldBeIn = checkedItems[index]
                        when {
                            shouldBeIn && !alreadyIn -> {
                                libraryRepo.addBookToLibrary(lib.id, bookId)
                                RecommendationUtils.addCategoryScore(book.category, 4)
                            }
                            !shouldBeIn && alreadyIn -> libraryRepo.removeBook(lib.id, bookId)
                        }
                    }
                    Toast.makeText(requireContext(), "Đã lưu!", Toast.LENGTH_SHORT).show()
                }
                .setNeutralButton("+ Tạo thư viện mới") { _, _ -> showCreateLibraryDialog() }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    private fun showCreateLibraryDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Tên thư viện"
            setPadding(48, 24, 48, 24)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Tạo thư viện mới")
            .setView(input)
            .setPositiveButton("Tạo") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton

                libraryRepo.createLibrary(name) { success ->
                    if (_binding == null) return@createLibrary
                    if (success) showAddToLibraryDialog()
                    else Toast.makeText(requireContext(), "Tạo thất bại!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private fun navigateToReader(order: Int, chapterTitle: String? = null) {
        val bundle = Bundle().apply {
            putString("bookId",      book.id)
            putInt("order",          order)
            putString("category",    book.category)
            putString("title",       book.title)
            putString("author",      book.author)
            putString("description", book.description)
            putString("imageUrl",    book.imageUrl)
            chapterTitle?.let { putString("chapterTitle", it) }
        }

        findNavController().navigate(
            R.id.action_bookDetailFragment_to_chapterReaderFragment,
            bundle
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}