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

        initGames()
        initAppearance()
        initAbout()
    }

    override fun onResume() {
        super.onResume()
        logScreenView(AnalyticsConstants.ScreenName.SettingsFragment)
    }

    private fun initAppearance() {
        // Dynamic color is only supported on Android 12+, older devices always use the app colors
        binding.dynamicColorRow.setVisible(DynamicColors.isDynamicColorAvailable())

        binding.dynamicColorSwitch.isChecked = AppPreferences.isDynamicColorEnabled(requireContext())
        binding.dynamicColorSwitch.setOnCheckedChangeListener { _, isChecked -> setDynamicColorEnabled(isChecked) }
        binding.dynamicColorRow.setOnClickListener { binding.dynamicColorSwitch.toggle() }

        binding.trueBlackSwitch.isChecked = AppPreferences.isTrueBlackEnabled(requireContext())
        binding.trueBlackSwitch.setOnCheckedChangeListener { _, isChecked -> setTrueBlackEnabled(isChecked) }
        binding.trueBlackRow.setOnClickListener { binding.trueBlackSwitch.toggle() }
    }

    private fun initGames() {
        binding.autoLoadSettingsSwitch.isChecked = AppPreferences.isAutoLoadGameSettingsEnabled(requireContext())
        binding.autoLoadSettingsSwitch.setOnCheckedChangeListener { _, isChecked -> setAutoLoadSettingsEnabled(isChecked) }
        binding.autoLoadSettingsRow.setOnClickListener { binding.autoLoadSettingsSwitch.toggle() }
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

    private fun setTrueBlackEnabled(enabled: Boolean) {
        logEvent(AnalyticsConstants.Event.TOGGLE_TRUE_BLACK) {
            putBoolean(AnalyticsConstants.Param.ENABLED, enabled)
        }
        AppPreferences.setTrueBlackEnabled(requireContext(), enabled)
        // The true black overlay is applied when the activity is created, so recreate to pick up the change
        requireActivity().recreate()
    }

    private fun setAutoLoadSettingsEnabled(enabled: Boolean) {
        logEvent(AnalyticsConstants.Event.TOGGLE_AUTO_LOAD_GAME_SETTINGS) {
            putBoolean(AnalyticsConstants.Param.ENABLED, enabled)
        }
        AppPreferences.setAutoLoadGameSettingsEnabled(requireContext(), enabled)
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}
