package com.stockmanager.app.ui.settings

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stockmanager.app.BuildConfig
import com.stockmanager.app.StockManagerApp
import com.stockmanager.app.data.AppDatabase
import com.stockmanager.app.databinding.FragmentSettingsBinding
import com.stockmanager.app.repository.StockRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: StockRepository

    private val backupLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            uri?.let { doBackup(it) }
        }
    private val restoreLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { doRestore(it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = (requireActivity().application as StockManagerApp).repository

        binding.inputDefaultReorder.setText(repository.prefs.defaultReorderLevel.toString())
        binding.inputDefaultReorder.addTextChangedListener { text ->
            text?.toString()?.toIntOrNull()?.let { repository.prefs.defaultReorderLevel = it }
        }

        val dbFile = AppDatabase.databaseFile(requireContext())
        binding.backupStatus.text = "Data file: ${dbFile.name}  •  ${formatSize(dbFile.length())}"

        binding.btnBackupNow.setOnClickListener {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            backupLauncher.launch("stock_manager_backup_$stamp.db")
        }
        binding.btnRestoreBackup.setOnClickListener {
            restoreLauncher.launch(arrayOf("*/*"))
        }

        binding.aboutText.text = "Stock Manager v${BuildConfig.VERSION_NAME}"
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }

    private fun doBackup(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.backupTo(uri)
            Toast.makeText(requireContext(), "Backup saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun doRestore(uri: Uri) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Restore backup")
            .setMessage("This replaces all current data with the selected backup file. This cannot be undone.")
            .setPositiveButton("Restore") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.restoreFrom(uri)
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Restored")
                        .setMessage("Backup restored. Please close and reopen the app.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
