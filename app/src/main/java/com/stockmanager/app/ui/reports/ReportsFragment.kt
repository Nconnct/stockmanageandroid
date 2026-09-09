package com.stockmanager.app.ui.reports

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.stockmanager.app.R
import com.stockmanager.app.StockManagerApp
import com.stockmanager.app.data.Product
import com.stockmanager.app.databinding.FragmentReportsBinding
import com.stockmanager.app.export.CsvExporter
import com.stockmanager.app.export.PdfExporter
import com.stockmanager.app.repository.StockRepository
import com.stockmanager.app.util.Formatting
import kotlinx.coroutines.launch

private enum class ReportKind { FULL, LOW_STOCK, OUT_OF_STOCK }

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: StockRepository
    private var pendingKind: ReportKind = ReportKind.FULL

    private val exportPdfLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
            uri?.let { exportPdf(pendingKind, it) }
        }
    private val exportCsvLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            uri?.let { exportCsv(pendingKind, it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = (requireActivity().application as StockManagerApp).repository

        binding.cardFullInventory.reportTitle.text = "📋  ${getString(R.string.report_full_inventory)}"
        binding.cardFullInventory.reportDesc.text = getString(R.string.report_full_inventory_desc)
        binding.cardFullInventory.btnReportPdf.setOnClickListener { requestExport(ReportKind.FULL, "pdf") }
        binding.cardFullInventory.btnReportCsv.setOnClickListener { requestExport(ReportKind.FULL, "csv") }

        binding.cardLowStock.reportTitle.text = "⚠️  ${getString(R.string.report_low_stock)}"
        binding.cardLowStock.reportDesc.text = getString(R.string.report_low_stock_desc)
        binding.cardLowStock.btnReportPdf.setOnClickListener { requestExport(ReportKind.LOW_STOCK, "pdf") }
        binding.cardLowStock.btnReportCsv.setOnClickListener { requestExport(ReportKind.LOW_STOCK, "csv") }

        binding.cardOutOfStock.reportTitle.text = "🚫  ${getString(R.string.report_out_of_stock)}"
        binding.cardOutOfStock.reportDesc.text = getString(R.string.report_out_of_stock_desc)
        binding.cardOutOfStock.btnReportPdf.setOnClickListener { requestExport(ReportKind.OUT_OF_STOCK, "pdf") }
        binding.cardOutOfStock.btnReportCsv.setOnClickListener { requestExport(ReportKind.OUT_OF_STOCK, "csv") }
    }

    private fun requestExport(kind: ReportKind, format: String) {
        pendingKind = kind
        val baseName = when (kind) {
            ReportKind.FULL -> "inventory_report"
            ReportKind.LOW_STOCK -> "low_stock_report"
            ReportKind.OUT_OF_STOCK -> "out_of_stock_report"
        }
        if (format == "pdf") exportPdfLauncher.launch("$baseName.pdf") else exportCsvLauncher.launch("$baseName.csv")
    }

    private suspend fun filteredProducts(kind: ReportKind): List<Product> {
        val all = repository.getProducts()
        return when (kind) {
            ReportKind.FULL -> all
            ReportKind.LOW_STOCK -> all.filter { it.isLowStock }
            ReportKind.OUT_OF_STOCK -> all.filter { it.isOutOfStock }
        }
    }

    private fun titleFor(kind: ReportKind): String = when (kind) {
        ReportKind.FULL -> "Inventory Report"
        ReportKind.LOW_STOCK -> "Low Stock Report"
        ReportKind.OUT_OF_STOCK -> "Out of Stock Report"
    }

    private fun exportPdf(kind: ReportKind, uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val products = filteredProducts(kind)
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
                requireContext(), uri, titleFor(kind), headers, rows,
                columnWeights = listOf(1f, 4f, 2.5f, 1f, 1.5f, 1.5f), footerRow = footer,
            )
            Toast.makeText(requireContext(), "Exported", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportCsv(kind: ReportKind, uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val products = filteredProducts(kind)
            val headers = listOf("Name", "Category", "Quality", "Quantity", "Unit Price", "Total")
            val rows = products.map { listOf(it.name, it.category, it.quality, it.quantity, it.price, it.total) }
            val footer = listOf("", "", "", "", "Grand Total", products.sumOf { it.total })
            CsvExporter.exportTable(requireContext(), uri, headers, rows, footer)
            Toast.makeText(requireContext(), "Exported", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
