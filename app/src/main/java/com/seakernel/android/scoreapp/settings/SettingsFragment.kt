package com.seakernel.android.scoreapp.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.CompoundButton
import android.widget.Switch
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.fragment.app.Fragment
import com.google.android.material.color.DynamicColors
import com.seakernel.android.scoreapp.BuildConfig
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.databinding.FragmentSettingsBinding
import com.seakernel.android.scoreapp.utility.AnalyticsConstants
import com.seakernel.android.scoreapp.utility.AppPreferences
import com.seakernel.android.scoreapp.utility.UsageData
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
        initPrivacy()
        initAbout()
    }

    private fun initPrivacy() {
        binding.shareUsageDataSwitch.isChecked = AppPreferences.isShareUsageDataEnabled(requireContext())
        binding.shareUsageDataSwitch.setOnCheckedChangeListener { _, isChecked -> setShareUsageDataEnabled(isChecked) }
        binding.shareUsageDataRow.actAsSwitch(binding.shareUsageDataSwitch)

        binding.shareCrashReportsSwitch.isChecked = AppPreferences.isShareCrashReportsEnabled(requireContext())
        binding.shareCrashReportsSwitch.setOnCheckedChangeListener { _, isChecked -> setShareCrashReportsEnabled(isChecked) }
        binding.shareCrashReportsRow.actAsSwitch(binding.shareCrashReportsSwitch)
    }

    private fun setShareUsageDataEnabled(enabled: Boolean) {
        UsageData.setUsageStatisticsEnabled(requireContext(), enabled)
    }

    private fun setShareCrashReportsEnabled(enabled: Boolean) {
        UsageData.setCrashReportsEnabled(requireContext(), enabled)
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
        binding.dynamicColorRow.actAsSwitch(binding.dynamicColorSwitch)

        binding.trueBlackSwitch.isChecked = AppPreferences.isTrueBlackEnabled(requireContext())
        binding.trueBlackSwitch.setOnCheckedChangeListener { _, isChecked -> setTrueBlackEnabled(isChecked) }
        binding.trueBlackRow.actAsSwitch(binding.trueBlackSwitch)
    }

    private fun initGames() {
        binding.autoLoadSettingsSwitch.isChecked = AppPreferences.isAutoLoadGameSettingsEnabled(requireContext())
        binding.autoLoadSettingsSwitch.setOnCheckedChangeListener { _, isChecked -> setAutoLoadSettingsEnabled(isChecked) }
        binding.autoLoadSettingsRow.actAsSwitch(binding.autoLoadSettingsSwitch)

        binding.calculatorDefaultSwitch.isChecked = AppPreferences.isCalculatorDefaultEnabled(requireContext())
        binding.calculatorDefaultSwitch.setOnCheckedChangeListener { _, isChecked -> setCalculatorDefaultEnabled(isChecked) }
        binding.calculatorDefaultRow.actAsSwitch(binding.calculatorDefaultSwitch)
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

    private fun setCalculatorDefaultEnabled(enabled: Boolean) {
        logEvent(AnalyticsConstants.Event.TOGGLE_CALCULATOR_DEFAULT) {
            putBoolean(AnalyticsConstants.Param.ENABLED, enabled)
        }
        AppPreferences.setCalculatorDefaultEnabled(requireContext(), enabled)
    }

    /**
     * Tapping the row toggles its switch, and screen readers hear the row as that switch: its text, then whether it's
     * on. Otherwise they'd stop on the row without hearing the switch's state, and then on the switch on its own.
     */
    private fun View.actAsSwitch(switch: CompoundButton) {
        setOnClickListener { switch.toggle() }
        switch.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = Switch::class.java.name
                info.isCheckable = true
                info.isChecked = switch.isChecked
            }

            // Sent after a tap toggles the switch, so it's announced as on or off
            override fun onInitializeAccessibilityEvent(host: View, event: AccessibilityEvent) {
                super.onInitializeAccessibilityEvent(host, event)
                event.isChecked = switch.isChecked
            }
        })
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}
