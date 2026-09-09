package com.stockmanager.app.ui.stock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.stockmanager.app.R
import com.stockmanager.app.StockManagerApp
import com.stockmanager.app.data.MOVE_IN
import com.stockmanager.app.data.MOVE_OUT
import com.stockmanager.app.data.Product
import com.stockmanager.app.databinding.FragmentStockBinding
import com.stockmanager.app.repository.StockMoveException
import com.stockmanager.app.repository.StockRepository
import com.stockmanager.app.ui.common.TransactionAdapter
import kotlinx.coroutines.launch

class StockFragment : Fragment() {

    private var _binding: FragmentStockBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: StockRepository
    private lateinit var adapter: TransactionAdapter
    private var products: List<Product> = emptyList()
    private var selectedProductId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = (requireActivity().application as StockManagerApp).repository

        adapter = TransactionAdapter()
        binding.movesList.layoutManager = LinearLayoutManager(requireContext())
        binding.movesList.adapter = adapter

        binding.moveTypeToggle.check(R.id.btn_stock_in)
        binding.moveTypeToggle.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) refreshRecentMoves()
        }

        binding.inputProduct.setOnItemClickListener { _, _, position, _ ->
            selectedProductId = products.getOrNull(position)?.id
        }

        binding.btnConfirm.setOnClickListener { submitMove() }
    }

    override fun onResume() {
        super.onResume()
        loadProducts()
        refreshRecentMoves()
    }

    private fun loadProducts() {
        viewLifecycleOwner.lifecycleScope.launch {
            products = repository.getProducts()
            val labels = products.map { "${it.name}  (in stock: ${it.quantity})" }
            val adapterItems = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, labels)
            binding.inputProduct.setAdapter(adapterItems)
        }
    }

    private fun currentMoveType(): String =
        if (binding.moveTypeToggle.checkedButtonId == R.id.btn_stock_in) MOVE_IN else MOVE_OUT

    private fun refreshRecentMoves() {
        viewLifecycleOwner.lifecycleScope.launch {
            adapter.submitList(repository.getTransactionsByType(currentMoveType()))
        }
    }

    private fun submitMove() {
        val productId = selectedProductId
        if (productId == null) {
            Toast.makeText(requireContext(), "Select a product first.", Toast.LENGTH_SHORT).show()
            return
        }
        val quantity = binding.inputQuantity.text?.toString()?.toIntOrNull()
        if (quantity == null || quantity <= 0) {
            binding.inputQuantity.error = "Enter a valid quantity"
            return
        }
        val price = binding.inputPrice.text?.toString()?.toDoubleOrNull() ?: 0.0
        val note = binding.inputNote.text?.toString()?.trim().orEmpty()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.recordStockMove(productId, currentMoveType(), quantity, price, note)
                binding.inputProduct.setText("", false)
                binding.inputQuantity.setText("")
                binding.inputPrice.setText("")
                binding.inputNote.setText("")
                selectedProductId = null
                loadProducts()
                refreshRecentMoves()
                Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
            } catch (e: StockMoveException) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
