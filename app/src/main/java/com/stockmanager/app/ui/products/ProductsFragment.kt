package com.stockmanager.app.ui.products

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stockmanager.app.R
import com.stockmanager.app.StockManagerApp
import com.stockmanager.app.data.MOVE_IN
import com.stockmanager.app.data.MOVE_OUT
import com.stockmanager.app.data.Product
import com.stockmanager.app.databinding.FragmentProductsBinding
import com.stockmanager.app.export.CsvExporter
import com.stockmanager.app.export.CsvImporter
import com.stockmanager.app.export.PdfExporter
import com.stockmanager.app.repository.StockRepository
import com.stockmanager.app.util.Formatting
import kotlinx.coroutines.launch
import kotlin.math.abs

class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: StockRepository
    private lateinit var adapter: ProductAdapter
    private var searchQuery: String = ""

    private val exportCsvLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let { exportCsv(it) } }
    private val exportPdfLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri -> uri?.let { exportPdf(it) } }
    private val importCsvLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { importCsv(it) } }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = (requireActivity().application as StockManagerApp).repository

        adapter = ProductAdapter(onEdit = { showEditDialog(it) }, onDelete = { confirmDelete(it) })
        binding.productsList.layoutManager = LinearLayoutManager(requireContext())
        binding.productsList.adapter = adapter

        binding.searchInput.addTextChangedListener { text ->
            searchQuery = text?.toString().orEmpty()
            refresh()
        }

        binding.fabAddProduct.setOnClickListener {
            AddEditProductDialog.show(requireContext(), null, repository.prefs.defaultReorderLevel) { result ->
                viewLifecycleOwner.lifecycleScope.launch {
                    if (repository.findByName(result.name) != null) {
                        Toast.makeText(requireContext(), "A product with this name already exists.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    repository.addProduct(
                        result.name, result.category, result.quality,
                        result.quantity, result.price, result.reorderLevel,
                    )
                    refresh()
                }
            }
        }

        binding.swipeRefresh.setOnRefreshListener { refresh() }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                inflater.inflate(R.menu.products_menu, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean = when (item.itemId) {
                R.id.action_import_csv -> {
                    importCsvLauncher.launch(arrayOf("text/*"))
                    true
                }
                R.id.action_export_csv -> {
                    exportCsvLauncher.launch("products.csv")
                    true
                }
                R.id.action_export_pdf -> {
                    exportPdfLauncher.launch("products.pdf")
                    true
                }
                else -> false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        viewLifecycleOwner.lifecycleScope.launch {
            val products = repository.getProducts(searchQuery)
            adapter.submitList(products)
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun showEditDialog(product: Product) {
        AddEditProductDialog.show(requireContext(), product, repository.prefs.defaultReorderLevel) { result ->
            viewLifecycleOwner.lifecycleScope.launch {
                repository.updateProductDetails(
                    product, result.name, result.category, result.quality, result.price, result.reorderLevel
                )
                refresh()
            }
        }
    }

    private fun confirmDelete(product: Product) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_product_title)
            .setMessage(getString(R.string.delete_product_message, product.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.deleteProduct(product)
                    refresh()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun exportCsv(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val products = repository.getProducts(searchQuery)
            val headers = listOf("Name", "Category", "Quality", "Quantity", "Unit Price", "Total")
            val rows = products.map { listOf(it.name, it.category, it.quality, it.quantity, it.price, it.total) }
            val footer = listOf("", "", "", "", "Grand Total", products.sumOf { it.total })
            CsvExporter.exportTable(requireContext(), uri, headers, rows, footer)
            Toast.makeText(requireContext(), "Exported", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportPdf(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val products = repository.getProducts(searchQuery)
            val headers = listOf("#", "Product", "Quality", "Qty", "Unit Price", "Total")
            var grandTotal = 0.0
            val rows = products.mapIndexed { i, p ->
                grandTotal += p.total
                listOf(
                    (i + 1).toString(), p.name, p.quality.ifBlank { "-" }, p.quantity.toString(),
                    Formatting.currency(p.price), Formatting.currency(p.total),
                )
            }
            val footer = listOf("", "", "", "", "Grand Total", Formatting.currency(grandTotal))
            PdfExporter.exportTable(
                requireContext(), uri, "Inventory Report", headers, rows,
                columnWeights = listOf(1f, 4f, 2.5f, 1f, 1.5f, 1.5f), footerRow = footer,
            )
            Toast.makeText(requireContext(), "Exported", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importCsv(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val (rows, errors) = CsvImporter.importProducts(requireContext(), uri)
                var added = 0
                var updated = 0
                rows.forEach { row ->
                    val existing = repository.findByName(row.name)
                    if (existing != null) {
                        repository.updateProductDetails(
                            existing, row.name, row.category, row.quality, row.price, existing.reorderLevel
                        )
                        if (row.quantity != existing.quantity) {
                            val diff = row.quantity - existing.quantity
                            val moveType = if (diff > 0) MOVE_IN else MOVE_OUT
                            repository.recordStockMove(existing.id, moveType, abs(diff), row.price, "CSV import")
                        }
                        updated++
                    } else {
                        repository.addProduct(
                            row.name, row.category, row.quality, row.quantity, row.price,
                            repository.prefs.defaultReorderLevel,
                        )
                        added++
                    }
                }
                refresh()
                var msg = "Imported: $added new, $updated updated."
                if (errors.isNotEmpty()) msg += "\n${errors.size} row(s) skipped."
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Import complete")
                    .setMessage(msg)
                    .setPositiveButton("OK", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message ?: "Import failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
