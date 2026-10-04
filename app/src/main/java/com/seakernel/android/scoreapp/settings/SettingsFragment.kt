package com.seakernel.android.scoreapp.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewGroupCompat
import androidx.fragment.app.Fragment
import com.google.android.material.color.DynamicColors
import com.seakernel.android.scoreapp.BuildConfig
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.databinding.FragmentSettingsBinding
import com.seakernel.android.scoreapp.utility.AnalyticsConstants
import com.seakernel.android.scoreapp.utility.AppPreferences
import com.seakernel.android.scoreapp.utility.applyWindowInsetsCutout
import com.seakernel.android.scoreapp.utility.applyWindowInsetsNavigationPadding
import com.seakernel.android.scoreapp.utility.logEvent
import com.seakernel.android.scoreapp.utility.logScreenView
import com.seakernel.android.scoreapp.utility.openChangelog
import com.seakernel.android.scoreapp.utility.rateApp
import com.seakernel.android.scoreapp.utility.setVisible

/**
 * App-wide settings and information about the app
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        ViewGroupCompat.installCompatInsetsDispatch(binding.root)
        binding.appBar.applyWindowInsetsCutout()
        binding.settingsContainer.applyWindowInsetsNavigationPadding()

        initAppearance()
        initAbout()
    }

    override fun onResume() {
        super.onResume()
        logScreenView(AnalyticsConstants.ScreenName.SettingsFragment)
    }

    private fun initAppearance() {
        // Dynamic color is only supported on Android 12+, older devices always use the app colors
        binding.appearanceSection.setVisible(DynamicColors.isDynamicColorAvailable())

        binding.dynamicColorSwitch.isChecked = AppPreferences.isDynamicColorEnabled(requireContext())
        binding.dynamicColorSwitch.setOnCheckedChangeListener { _, isChecked -> setDynamicColorEnabled(isChecked) }
        binding.dynamicColorRow.setOnClickListener { binding.dynamicColorSwitch.toggle() }
    }

    private fun initAbout() {
        binding.changelogRow.setOnClickListener { openChangelog() }
        binding.rateRow.setOnClickListener { rateApp() }
        binding.versionText.text =
            getString(R.string.settingsVersionFormat, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }

    private fun setDynamicColorEnabled(enabled: Boolean) {
        logEvent(AnalyticsConstants.Event.TOGGLE_DYNAMIC_COLOR) {
            putBoolean(AnalyticsConstants.Param.ENABLED, enabled)
        }
        AppPreferences.setDynamicColorEnabled(requireContext(), enabled)
        // Dynamic colors are applied when an activity is created, so recreate to pick up the change
        requireActivity().recreate()
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}
