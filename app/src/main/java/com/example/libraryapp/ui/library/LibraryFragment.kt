package com.example.libraryapp.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libraryapp.R
import com.example.libraryapp.adapter.LibraryAdapter
import com.example.libraryapp.data.LibraryRepository
import com.example.libraryapp.databinding.FragmentLibraryBinding
import com.example.libraryapp.model.Library

class LibraryFragment : Fragment() {

    private lateinit var binding: FragmentLibraryBinding
    private val repo = LibraryRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLibraryBinding.inflate(inflater, container, false)
        binding.rvLibraries.layoutManager = LinearLayoutManager(requireContext())
        loadLibraries()
        binding.btnAddLibrary.setOnClickListener { showCreateDialog() }
        return binding.root
    }

    private fun loadLibraries() {
        repo.getLibraries { list ->
            binding.rvLibraries.adapter = LibraryAdapter(
                items = list,
                onClick = { library ->
                    val bundle = Bundle().apply {
                        putString("libraryId", library.id)
                        putString("libraryName", library.name)
                    }
                    findNavController().navigate(
                        R.id.action_libraryFragment_to_libraryDetailFragment,
                        bundle
                    )
                },
                onRename = { library -> showRenameDialog(library) },
                onDelete = { library -> showDeleteDialog(library) }
            )
        }
    }

    private fun showCreateDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Tên thư viện"
            setPadding(48, 32, 48, 16)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Tạo thư viện mới")
            .setView(input)
            .setPositiveButton("Tạo") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    repo.createLibrary(name) { if (it) loadLibraries() }
                }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun showRenameDialog(library: Library) {
        val input = EditText(requireContext()).apply {
            setText(library.name)
            setPadding(48, 32, 48, 16)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Đổi tên thư viện")
            .setView(input)
            .setPositiveButton("Lưu") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    repo.renameLibrary(library.id, newName) { if (it) loadLibraries() }
                }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun showDeleteDialog(library: Library) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xoá thư viện")
            .setMessage("Xoá \"${library.name}\"?\nSách trong thư viện sẽ không bị xoá.")
            .setPositiveButton("Xoá") { _, _ ->
                repo.deleteLibrary(library.id) { if (it) loadLibraries() }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }
}