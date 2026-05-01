package com.example.libraryapp.ui.profile

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libraryapp.R
import com.example.libraryapp.databinding.FragmentBookShareInboxBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class SharedBookItem(
    val docId: String = "",
    val bookId: String = "",
    val bookTitle: String = "",
    val fromId: String = "",
    val fromName: String = "",
    val read: Boolean = false,
    val timestamp: Long = 0L
)

class BookShareInboxFragment : Fragment() {

    private var _binding: FragmentBookShareInboxBinding? = null
    private val binding get() = _binding!!

    private val db   = FirebaseFirestore.getInstance()
    private val myId get() = FirebaseAuth.getInstance().currentUser!!.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookShareInboxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.rvSharedBooks.layoutManager = LinearLayoutManager(requireContext())

        loadSharedBooks()
    }

    private fun loadSharedBooks() {
        binding.progressInbox.visibility  = View.VISIBLE
        binding.rvSharedBooks.visibility  = View.GONE
        binding.tvEmptyInbox.visibility   = View.GONE

        db.collection("users/$myId/sharedBooks")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                if (_binding == null) return@addOnSuccessListener

                val items = snap.documents.map { doc ->
                    SharedBookItem(
                        docId     = doc.id,
                        bookId    = doc.getString("bookId")    ?: "",
                        bookTitle = doc.getString("bookTitle") ?: "Không rõ tên sách",
                        fromId    = doc.getString("fromId")    ?: "",
                        read      = doc.getBoolean("read")     ?: false,
                        timestamp = doc.getTimestamp("timestamp")?.seconds ?: 0L
                    )
                }

                binding.progressInbox.visibility = View.GONE

                if (items.isEmpty()) {
                    binding.tvEmptyInbox.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                binding.rvSharedBooks.visibility = View.VISIBLE

                // Resolve tên người gửi rồi bind adapter
                resolveFromNames(items) { resolved ->
                    if (_binding == null) return@resolveFromNames
                    binding.rvSharedBooks.adapter = SharedBookAdapter(resolved) { item ->
                        markAsRead(item.docId)
                        navigateToBook(item)
                    }
                }
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                binding.progressInbox.visibility = View.GONE
                binding.tvEmptyInbox.text = "Không tải được thông báo"
                binding.tvEmptyInbox.visibility = View.VISIBLE
            }
    }

    /** Lấy displayName của từng fromId một lần duy nhất */
    private fun resolveFromNames(
        items: List<SharedBookItem>,
        onDone: (List<SharedBookItem>) -> Unit
    ) {
        val fromIds = items.map { it.fromId }.distinct().filter { it.isNotEmpty() }
        if (fromIds.isEmpty()) { onDone(items); return }

        db.collection("users")
            .whereIn("__name__", fromIds)
            .get()
            .addOnSuccessListener { snap ->
                val nameMap = snap.documents.associate { doc ->
                    doc.id to (doc.getString("displayName") ?: doc.getString("email") ?: "Ai đó")
                }
                onDone(items.map { it.copy(fromName = nameMap[it.fromId] ?: "Ai đó") })
            }
            .addOnFailureListener { onDone(items) }
    }

    private fun markAsRead(docId: String) {
        db.document("users/$myId/sharedBooks/$docId")
            .update("read", true)
    }

    private fun navigateToBook(item: SharedBookItem) {
        // Lấy đầy đủ thông tin sách từ collection books rồi navigate
        db.collection("books").document(item.bookId).get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                val bundle = Bundle().apply {
                    putString("bookId",      doc.id)
                    putString("title",       doc.getString("title")       ?: item.bookTitle)
                    putString("author",      doc.getString("author")      ?: "")
                    putString("description", doc.getString("description") ?: "")
                    putString("imageUrl",    doc.getString("imageUrl")    ?: "")
                    putString("category",    doc.getString("category")    ?: "")
                }
                findNavController().navigate(
                    R.id.action_bookShareInboxFragment_to_bookDetailFragment,
                    bundle
                )
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}