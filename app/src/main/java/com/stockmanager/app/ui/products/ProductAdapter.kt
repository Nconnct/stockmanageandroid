package com.stockmanager.app.ui.products

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stockmanager.app.data.Product
import com.stockmanager.app.databinding.ItemProductBinding
import com.stockmanager.app.util.Formatting

class ProductAdapter(
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit,
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    private var items: List<Product> = emptyList()

    fun submitList(newItems: List<Product>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = items[position]
        holder.bind(product)
        holder.binding.btnEdit.setOnClickListener { onEdit(product) }
        holder.binding.btnDelete.setOnClickListener { onDelete(product) }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.productName.text = product.name
            val subtitleParts = listOf(product.category, product.quality).filter { it.isNotBlank() }
            binding.productSubtitle.text = if (subtitleParts.isEmpty()) "—" else subtitleParts.joinToString(" • ")
            binding.productQtyPrice.text = "Qty ${product.quantity} × ${Formatting.currency(product.price)}"
            binding.productTotal.text = Formatting.currency(product.total)

            val (statusText, statusColor) = when {
                product.isOutOfStock -> "Out of stock" to Color.parseColor("#DC2626")
                product.isLowStock -> "Low stock" to Color.parseColor("#B45309")
                else -> "In stock" to Color.parseColor("#059669")
            }
            binding.productStatus.text = statusText
            binding.productStatus.setTextColor(statusColor)
        }
    }
}
