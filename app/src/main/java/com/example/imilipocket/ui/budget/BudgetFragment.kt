package com.example.imilipocket.ui.budget

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.imilipocket.R
import com.example.imilipocket.data.PreferenceManager
import com.example.imilipocket.databinding.FragmentBudgetBinding
import com.example.imilipocket.util.NotificationHelper
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.NumberFormat
import java.util.Locale

class BudgetFragment : Fragment() {
    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var viewModel: BudgetViewModel
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            _binding = FragmentBudgetBinding.inflate(inflater, container, false)
            preferenceManager = PreferenceManager(requireContext())
            viewModel = ViewModelProvider(
                this,
                BudgetViewModel.Factory(preferenceManager)
            )[BudgetViewModel::class.java]
            notificationHelper = NotificationHelper(requireContext())

            setupUI()
            setupClickListeners()
            observeViewModel()
            setupPieChart()

            return binding.root
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error initializing budget screen", Toast.LENGTH_SHORT).show()
            return binding.root
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            updateBudgetProgress()
            updatePieChart()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupUI() {
        try {
            val currentBudget = preferenceManager.getMonthlyBudget()
            binding.budgetInputEditText.setText(currentBudget.toString())
            updateBudgetProgress()
        } catch (e: Exception) {
            e.printStackTrace()
            binding.budgetInputEditText.setText("0")
        }
    }

    private fun setupClickListeners() {
        binding.saveBudgetButton.setOnClickListener {
            saveBudget()
        }
    }

    private fun observeViewModel() {
        viewModel.budget.observe(viewLifecycleOwner) { budget ->
            try {
                binding.budgetInputEditText.setText(budget.toString())
                updateBudgetProgress()
                updatePieChart()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveBudget() {
        try {
            val budget = binding.budgetInputEditText.text.toString().toDouble()
            if (budget < 0) {
                Toast.makeText(requireContext(), "Budget cannot be negative", Toast.LENGTH_SHORT).show()
                return
            }
            viewModel.updateBudget(budget)
            showBudgetNotification()
            Toast.makeText(requireContext(), "Budget updated", Toast.LENGTH_SHORT).show()
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Invalid budget amount", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error saving budget", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBudgetNotification() {
        try {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val monthlyBudget = preferenceManager.getMonthlyBudget()
                notificationHelper.showBudgetNotification(monthlyBudget)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateBudgetProgress() {
        try {
            val monthlyBudget = preferenceManager.getMonthlyBudget()
            val monthlyExpenses = viewModel.getMonthlyExpenses()
            val progress = if (monthlyBudget > 0) {
                (monthlyExpenses / monthlyBudget * 100).toInt()
            } else {
                0
            }
            binding.budgetProgressIndicator.progress = progress
            binding.budgetPercentageText.text = "$progress%"
            
            val remaining = monthlyBudget - monthlyExpenses
            binding.remainingBudgetText.text = "Remaining: ${formatCurrency(remaining)}"
        } catch (e: Exception) {
            e.printStackTrace()
            binding.budgetProgressIndicator.progress = 0
            binding.budgetPercentageText.text = "0%"
            binding.remainingBudgetText.text = "Remaining: ${formatCurrency(0.0)}"
        }
    }

    private fun setupPieChart() {
        try {
            binding.budgetPieChart.apply {
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(true)
                setHoleColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
                setTransparentCircleColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
                setNoDataText("No budget data available")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updatePieChart() {
        try {
            val monthlyBudget = preferenceManager.getMonthlyBudget()
            val monthlyExpenses = viewModel.getMonthlyExpenses()
            val remaining = monthlyBudget - monthlyExpenses

            val entries = listOf(
                PieEntry(monthlyExpenses.toFloat(), "Expenses"),
                PieEntry(remaining.toFloat(), "Remaining")
            )

            val dataSet = PieDataSet(entries, "Budget").apply {
                colors = listOf(
                    ContextCompat.getColor(requireContext(), R.color.red_500),
                    ContextCompat.getColor(requireContext(), R.color.green_500)
                )
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return formatCurrency(value.toDouble())
                    }
                }
            }

            binding.budgetPieChart.data = PieData(dataSet)
            binding.budgetPieChart.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
            binding.budgetPieChart.setNoDataText("Error loading budget data")
            binding.budgetPieChart.invalidate()
        }
    }

    private fun formatCurrency(amount: Double): String {
        return try {
            val currency = preferenceManager.getSelectedCurrency()
            val locale = when (currency) {
                "USD" -> Locale.US
                "EUR" -> Locale.GERMANY
                "GBP" -> Locale.UK
                "JPY" -> Locale.JAPAN
                "INR" -> Locale("en", "IN")
                "AUD" -> Locale("en", "AU")
                "CAD" -> Locale("en", "CA")
                "LKR" -> Locale("si", "LK")
                "CNY" -> Locale("zh", "CN")
                "SGD" -> Locale("en", "SG")
                "MYR" -> Locale("ms", "MY")
                "THB" -> Locale("th", "TH")
                "IDR" -> Locale("id", "ID")
                "PHP" -> Locale("en", "PH")
                "VND" -> Locale("vi", "VN")
                "KRW" -> Locale("ko", "KR")
                "AED" -> Locale("ar", "AE")
                "SAR" -> Locale("ar", "SA")
                "QAR" -> Locale("ar", "QA")
                else -> Locale.US
            }
            val format = NumberFormat.getCurrencyInstance(locale)
            format.format(amount)
        } catch (e: Exception) {
            e.printStackTrace()
            "$0.00"
        }
    }
} 