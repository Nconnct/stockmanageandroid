package com.stockmanager.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.stockmanager.app.R
import com.stockmanager.app.StockManagerApp
import com.stockmanager.app.databinding.FragmentDashboardBinding
import com.stockmanager.app.repository.StockRepository
import com.stockmanager.app.ui.common.TransactionAdapter
import com.stockmanager.app.util.Formatting
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: StockRepository
    private lateinit var adapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = (requireActivity().application as StockManagerApp).repository

        adapter = TransactionAdapter()
        binding.recentActivityList.layoutManager = LinearLayoutManager(requireContext())
        binding.recentActivityList.adapter = adapter

        binding.cardProducts.statIcon.text = "📦"
        binding.cardProducts.statLabel.text = getString(R.string.stat_total_products)
        binding.cardStock.statIcon.text = "📊"
        binding.cardStock.statLabel.text = getString(R.string.stat_total_stock)
        binding.cardValue.statIcon.text = "💰"
        binding.cardValue.statLabel.text = getString(R.string.stat_total_value)
        binding.cardLow.statIcon.text = "⚠️"
        binding.cardLow.statLabel.text = getString(R.string.stat_low_stock)
        binding.cardOut.statIcon.text = "🚫"
        binding.cardOut.statLabel.text = getString(R.string.stat_out_of_stock)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        viewLifecycleOwner.lifecycleScope.launch {
            val stats = repository.getDashboardStats()
            binding.cardProducts.statValue.text = stats.totalProducts.toString()
            binding.cardStock.statValue.text = stats.totalStockQty.toString()
            binding.cardValue.statValue.text = Formatting.currency(stats.totalStockValue)
            binding.cardLow.statValue.text = stats.lowStock.toString()
            binding.cardOut.statValue.text = stats.outOfStock.toString()

            adapter.submitList(repository.getRecentTransactions(10))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
