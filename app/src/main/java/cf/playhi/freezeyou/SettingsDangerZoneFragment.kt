package cf.playhi.freezeyou

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.Keep
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cf.playhi.freezeyou.utils.AlertDialogUtils.buildAlertDialog
import cf.playhi.freezeyou.utils.DevicePolicyManagerUtils
import cf.playhi.freezeyou.utils.DevicePolicyManagerUtils.isDeviceOwner
import cf.playhi.freezeyou.utils.DevicePolicyManagerUtils.isProfileOwner
import cf.playhi.freezeyou.utils.ShizukuUtils
import cf.playhi.freezeyou.utils.ToastUtils.showToast
import cf.playhi.freezeyou.utils.VersionUtils.checkUpdate
import rikka.shizuku.Shizuku
import java.lang.IllegalStateException
import java.util.concurrent.TimeUnit

@Keep
class SettingsDangerZoneFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.spr_danger_zone, rootKey)

        if (isDeviceOwner(activity)) {
            findPreference<Preference?>("clearAllUserData")?.let {
                preferenceScreen?.removePreference(it)
            }
            findPreference<Preference?>("makeDeviceOwner")?.let {
                preferenceScreen?.removePreference(it)
            }
        }

        findPreference<Preference?>("clearAllUserData")?.setOnPreferenceClickListener {
            buildAlertDialog(
                requireActivity(),
                R.drawable.ic_warning,
                R.string.clearAllUserData,
                R.string.notice
            )
                .setPositiveButton(R.string.yes) { _, _ ->
                    val activityManager =
                        requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
                    if (activityManager != null && Build.VERSION.SDK_INT >= 19) {
                        try {
                            showToast(
                                requireActivity(),
                                if (activityManager.clearApplicationUserData())
                                    R.string.success
                                else
                                    R.string.failed
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            showToast(requireActivity(), R.string.failed)
                        }
                    } else {
                        showToast(requireActivity(), R.string.sysVerLow)
                    }
                }
                .setNegativeButton(R.string.no, null)
                .show()
            true
        }

        findPreference<Preference?>("makeDeviceOwner")?.setOnPreferenceClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!ShizukuUtils.isShizukuInstalled(requireContext())) {
                    showToast(requireActivity(), "Shizuku not installed")
                    return@setOnPreferenceClickListener false
                }
            } else {
                showToast(requireActivity(), R.string.sysVerLow)
                return@setOnPreferenceClickListener false
            }

            try {
                ShizukuUtils.requestPermission()
            } catch (e: IllegalStateException){
                showToast(requireActivity(), "Shizuku not running")
                return@setOnPreferenceClickListener false
            } catch (e: SecurityException) {
                showToast(requireActivity(), e.message)
                return@setOnPreferenceClickListener false
            }

            buildAlertDialog(
                    requireActivity(),
                    null,
                    "Make FreezeYou the Device Owner?",
                    null
            )
                    .setPositiveButton(R.string.yes) { _, _ ->
                            try {
                                val process = Shizuku.newProcess(arrayOf("/system/bin/dpm", "set-device-owner", "%s/.%s".format(requireActivity().packageName, DeviceAdminReceiver::class.simpleName)), null, null)
                                process.waitForTimeout(15, TimeUnit.SECONDS)
                                showToast(
                                        requireActivity(),
                                        if (process.exitValue() == 0)
                                            R.string.success
                                        else
                                            R.string.failed
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                                showToast(requireActivity(), R.string.failed)
                            }
                    }
                    .setNegativeButton(R.string.no, null)
                    .show()
            true
        }

        findPreference<Preference?>("selfRevokeProfileOwner")?.setOnPreferenceClickListener {
            buildAlertDialog(
                requireActivity(),
                R.drawable.ic_warning,
                R.string.selfRevokeProfileOwner,
                R.string.plsConfirm
            )
                .setPositiveButton(R.string.yes) { _, _ ->
                    if (isProfileOwner(requireContext())) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                DevicePolicyManagerUtils
                                    .getDevicePolicyManager(
                                        requireContext()
                                    )
                                    .clearProfileOwner(
                                        ComponentName(
                                            requireContext(),
                                            DeviceAdminReceiver::class.java
                                        )
                                    )
                                showToast(requireActivity(), R.string.success)
                            } else {
                                // TODO: Unsupported
                            }
                        } catch (e: SecurityException) {
                            // TODO: Is not an active profile owner, or the method is being called from a managed profile.
                        }
                    } else {
                        // TODO: Is not an active profile owner
                    }
                }
                .setNegativeButton(R.string.no, null)
                .setNeutralButton(R.string.update) { _, _ ->
                    checkUpdate(requireActivity())
                }
                .show()
            true
        }

        findPreference<Preference?>("uninstall")?.setOnPreferenceClickListener {
            activity?.startActivity(
                Intent(
                    Intent.ACTION_DELETE,
                    Uri.parse("package:cf.playhi.freezeyou")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )

            true
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.setTitle(R.string.dangerZone)
    }

}