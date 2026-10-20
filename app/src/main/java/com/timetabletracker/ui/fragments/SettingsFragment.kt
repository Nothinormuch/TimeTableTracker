package com.timetabletracker.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.timetabletracker.BuildConfig
import com.timetabletracker.R
import com.timetabletracker.databinding.FragmentSettingsBinding
import com.timetabletracker.scheduler.AlarmScheduler
import com.timetabletracker.ui.MainViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Populate current settings
        binding.switchNotifications.isChecked = viewModel.isNotificationsEnabled()
        updateCheckTimeDisplay(viewModel.getCheckHour(), viewModel.getCheckMinute())
        binding.tvAppVersion.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)

        // Login status observer
        viewModel.loginStatus.observe(viewLifecycleOwner) { status ->
            binding.tvLoginStatus.text = status
            binding.tvLoginStatus.visibility = if (status.isNullOrBlank()) View.GONE else View.VISIBLE
        }

        viewModel.isLoggedIn.observe(viewLifecycleOwner) { loggedIn ->
            val statusText = if (loggedIn) "✅ Logged in" else "Not logged in"
            if (viewModel.loginStatus.value.isNullOrBlank()) {
                binding.tvLoginStatus.text = statusText
            }
        }

        // Login button
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            if (email.isBlank() || password.isBlank()) {
                Snackbar.make(binding.root, "Please enter both email and password.", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.saveCredentials(email, password)
        }

        // Check time picker
        binding.btnPickTime.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(viewModel.getCheckHour())
                .setMinute(viewModel.getCheckMinute())
                .setTitleText("Select check time")
                .build()

            picker.addOnPositiveButtonClickListener {
                viewModel.saveCheckTime(picker.hour, picker.minute)
                updateCheckTimeDisplay(picker.hour, picker.minute)
                Snackbar.make(binding.root, "Check time saved!", Snackbar.LENGTH_SHORT).show()
            }
            picker.show(childFragmentManager, "time_picker")
        }

        // Notification toggle
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.saveNotificationsEnabled(isChecked)
        }

        // Save settings button
        binding.btnSave.setOnClickListener {
            viewModel.saveNotificationsEnabled(binding.switchNotifications.isChecked)
            if (!AlarmScheduler.canScheduleExactAlarms(requireContext())) {
                Snackbar.make(
                    binding.root,
                    "Exact alarm permission not granted. Alarm scheduling may be imprecise.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
            Snackbar.make(binding.root, "Settings saved!", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun updateCheckTimeDisplay(hour: Int, minute: Int) {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        binding.tvSelectedTime.text = getString(
            R.string.check_time_format,
            displayHour,
            minute.toString().padStart(2, '0'),
            amPm
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
