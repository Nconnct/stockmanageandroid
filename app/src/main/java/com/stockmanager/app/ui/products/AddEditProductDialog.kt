package com.stockmanager.app.ui.products

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stockmanager.app.R
import com.stockmanager.app.data.Product
import com.stockmanager.app.databinding.DialogAddEditProductBinding

val QUALITY_GRADES = listOf("A - Premium", "B - Standard", "C - Economy", "D - Clearance")

object AddEditProductDialog {

    data class Result(
        val name: String,
        val category: String,
        val quality: String,
        val quantity: Int,
        val price: Double,
        val reorderLevel: Int,
    )

    fun show(context: Context, product: Product?, defaultReorderLevel: Int, onSave: (Result) -> Unit) {
        val binding = DialogAddEditProductBinding.inflate(LayoutInflater.from(context))

        val qualityAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, QUALITY_GRADES)
        binding.inputQuality.setAdapter(qualityAdapter)

        if (product != null) {
            binding.inputName.setText(product.name)
            binding.inputCategory.setText(product.category)
            binding.inputQuality.setText(product.quality, false)
            binding.inputQuantity.setText(product.quantity.toString())
            binding.inputQuantity.isEnabled = false
            binding.hintQuantityLocked.visibility = View.VISIBLE
            binding.inputPrice.setText(trimZero(product.price))
            binding.inputReorder.setText(product.reorderLevel.toString())
        } else {
            binding.inputQuantity.setText("0")
            binding.inputReorder.setText(defaultReorderLevel.toString())
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (product != null) R.string.edit_product else R.string.add_product)
            .setView(binding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = binding.inputName.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) {
                    binding.inputName.error = "Required"
                    return@setOnClickListener
                }
                val quantity = binding.inputQuantity.text?.toString()?.toIntOrNull() ?: 0
                val price = binding.inputPrice.text?.toString()?.toDoubleOrNull() ?: 0.0
                val reorder = binding.inputReorder.text?.toString()?.toIntOrNull() ?: defaultReorderLevel
                val category = binding.inputCategory.text?.toString()?.trim().orEmpty()
                val quality = binding.inputQuality.text?.toString()?.trim().orEmpty()

                onSave(Result(name, category, quality, quantity, price, reorder))
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun trimZero(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
