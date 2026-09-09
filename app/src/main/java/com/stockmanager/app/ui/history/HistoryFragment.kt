package com.stockmanager.app.ui.history

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
import com.google.android.material.datepicker.MaterialDatePicker
import com.stockmanager.app.R
import com.stockmanager.app.StockManagerApp
import com.stockmanager.app.data.MOVE_IN
import com.stockmanager.app.data.MOVE_OUT
import com.stockmanager.app.databinding.FragmentHistoryBinding
import com.stockmanager.app.export.CsvExporter
import com.stockmanager.app.export.PdfExporter
import com.stockmanager.app.repository.StockRepository
import com.stockmanager.app.ui.common.TransactionAdapter
import com.stockmanager.app.util.Formatting
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: StockRepository
    private lateinit var adapter: TransactionAdapter

    private var fromMillis: Long = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
    private var toMillis: Long = System.currentTimeMillis()
    private var searchQuery: String = ""

    private val exportCsvLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let { exportCsv(it) } }
    private val exportPdfLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri -> uri?.let { exportPdf(it) } }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = (requireActivity().application as StockManagerApp).repository

        adapter = TransactionAdapter()
        binding.historyList.layoutManager = LinearLayoutManager(requireContext())
        binding.historyList.adapter = adapter

        updateDateButtonLabels()

        binding.btnDateFrom.setOnClickListener { pickDate(isFrom = true) }
        binding.btnDateTo.setOnClickListener { pickDate(isFrom = false) }

        binding.typeFilterGroup.setOnCheckedStateChangeListener { _, _ -> refresh() }
        binding.searchInput.addTextChangedListener { text ->
            searchQuery = text?.toString().orEmpty()
            refresh()
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                inflater.inflate(R.menu.history_menu, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean = when (item.itemId) {
                R.id.action_export_csv -> {
                    exportCsvLauncher.launch("stock_history.csv")
                    true
                }
                R.id.action_export_pdf -> {
                    exportPdfLauncher.launch("stock_history.pdf")
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

    private fun currentTypeFilter(): String = when (binding.typeFilterGroup.checkedChipId) {
        R.id.chip_in -> MOVE_IN
        R.id.chip_out -> MOVE_OUT
        else -> "ALL"
    }

    private fun pickDate(isFrom: Boolean) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(if (isFrom) fromMillis else toMillis)
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            if (isFrom) {
                fromMillis = selection
            } else {
                toMillis = selection + 86_399_999L // include the whole selected day
            }
            updateDateButtonLabels()
            refresh()
        }
        picker.show(childFragmentManager, "date_picker")
    }

    private fun updateDateButtonLabels() {
        binding.btnDateFrom.text = "${getString(R.string.filter_from)}: ${Formatting.date(fromMillis)}"
        binding.btnDateTo.text = "${getString(R.string.filter_to)}: ${Formatting.date(toMillis)}"
    }

    private fun refresh() {
        viewLifecycleOwner.lifecycleScope.launch {
            val txs = repository.getFilteredTransactions(currentTypeFilter(), fromMillis, toMillis, searchQuery)
            adapter.submitList(txs)
        }
    }

    private fun exportCsv(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val txs = repository.getFilteredTransactions(currentTypeFilter(), fromMillis, toMillis, searchQuery)
            val headers = listOf("Product", "Type", "Quantity", "Unit Price", "Note", "Date")
            val rows = txs.map {
                listOf(it.productName, it.type, it.quantity, it.unitPrice, it.note, Formatting.dateTime(it.createdAt))
            }
            CsvExporter.exportTable(requireContext(), uri, headers, rows)
            Toast.makeText(requireContext(), "Exported", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportPdf(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val txs = repository.getFilteredTransactions(currentTypeFilter(), fromMillis, toMillis, searchQuery)
            val headers = listOf("Product", "Type", "Qty", "Unit Price", "Note", "Date")
            val rows = txs.map {
                listOf(
                    it.productName, it.type, it.quantity.toString(),
                    Formatting.currency(it.unitPrice), it.note, Formatting.dateTime(it.createdAt),
                )
            }
            PdfExporter.exportTable(
                requireContext(), uri, "Stock History Report", headers, rows,
                columnWeights = listOf(2.2f, 0.9f, 0.8f, 1.3f, 2f, 1.8f),
            )
            Toast.makeText(requireContext(), "Exported", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
