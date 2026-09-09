package com.stockmanager.app.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stockmanager.app.R
import com.stockmanager.app.data.MOVE_IN
import com.stockmanager.app.data.StockTransaction
import com.stockmanager.app.databinding.ItemTransactionBinding
import com.stockmanager.app.util.Formatting

class TransactionAdapter : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    private var items: List<StockTransaction> = emptyList()

    fun submitList(newItems: List<StockTransaction>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tx: StockTransaction) {
            binding.txProductName.text = tx.productName
            binding.txType.text = if (tx.type == MOVE_IN) {
                binding.root.context.getString(R.string.stock_in) + " ⬆"
            } else {
                binding.root.context.getString(R.string.stock_out) + " ⬇"
            }
            binding.txQty.text = "Qty ${tx.quantity} → Bal ${tx.balanceAfter}"
            binding.txDate.text = Formatting.dateTime(tx.createdAt)
            binding.txNote.text = tx.note
            binding.txNote.visibility = if (tx.note.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
        }
    }
}
