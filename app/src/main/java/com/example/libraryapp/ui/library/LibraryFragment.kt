package com.example.libraryapp.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libraryapp.adapter.LibraryAdapter
import com.example.libraryapp.data.LibraryRepository
import com.example.libraryapp.databinding.FragmentLibraryBinding

class LibraryFragment : Fragment() {

    private lateinit var binding: FragmentLibraryBinding
    private val repo = LibraryRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentLibraryBinding.inflate(inflater, container, false)

        setupRecycler()

        return binding.root
    }

    private fun setupRecycler() {

        binding.rvLibraries.layoutManager =
            LinearLayoutManager(requireContext())

        loadLibraries()
    }

    private fun loadLibraries() {

        repo.getLibraries { list ->

            binding.rvLibraries.adapter =
                LibraryAdapter(list)
        }
    }
}