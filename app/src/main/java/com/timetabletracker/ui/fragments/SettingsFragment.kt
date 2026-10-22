package com.timetabletracker.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.materialswitch.MaterialSwitch
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

        // Login status observer — badge shows signed-in state, detail line shows errors
        viewModel.isLoggedIn.observe(viewLifecycleOwner) { loggedIn ->
            if (viewModel.loginStatus.value.isNullOrBlank()) {
                updateLoginBadge(loggedIn, null)
            }
        }

        viewModel.loginStatus.observe(viewLifecycleOwner) { status ->
            if (status.isNullOrBlank()) {
                binding.tvLoginStatus.visibility = View.GONE
            } else {
                binding.tvLoginStatus.visibility = View.VISIBLE
                binding.tvLoginStatus.text = status
                updateLoginBadge(viewModel.isLoggedIn.value == true, status)
            }
        }

        // Login button
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            if (email.isBlank() || password.isBlank()) {
                Snackbar.make(binding.root, R.string.msg_enter_credentials, Snackbar.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            viewModel.saveCredentials(email, password)
        }

        // Tap the time row to open the picker
        binding.layoutPickTime.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(viewModel.getCheckHour())
                .setMinute(viewModel.getCheckMinute())
                .setTitleText(R.string.check_schedule)
                .build()

            picker.addOnPositiveButtonClickListener {
                viewModel.saveCheckTime(picker.hour, picker.minute)
                updateCheckTimeDisplay(picker.hour, picker.minute)
                val amPm = if (picker.hour < 12) "AM" else "PM"
                val displayHour = when {
                    picker.hour == 0 -> 12
                    picker.hour > 12 -> picker.hour - 12
                    else -> picker.hour
                }
                Snackbar.make(
                    binding.root,
                    getString(
                        R.string.msg_time_saved,
                        "$displayHour:${picker.minute.toString().padStart(2, '0')} $amPm"
                    ),
                    Snackbar.LENGTH_SHORT
                ).show()
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
                Snackbar.make(binding.root, R.string.msg_alarm_permission, Snackbar.LENGTH_LONG)
                    .show()
            }
            Snackbar.make(binding.root, R.string.msg_settings_saved, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun updateLoginBadge(loggedIn: Boolean, detail: String?) {
        val colorRes = if (loggedIn) R.color.status_ok else R.color.status_idle
        binding.tvLoginState.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), colorRes)
        binding.tvLoginState.text = getString(
            if (loggedIn) R.string.login_state_in else R.string.login_state_out
        )
        // Detail line color: neutral for progress, error-colored for failures
        if (detail != null) {
            binding.tvLoginStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.on_surface_variant)
            )
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
