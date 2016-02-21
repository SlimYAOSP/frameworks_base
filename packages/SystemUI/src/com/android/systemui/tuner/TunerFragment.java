/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.systemui.tuner;

/*import static com.android.systemui.BatteryMeterView.SHOW_PERCENT_SETTING;
*/
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.tuner.TunerService.Tunable;

public class TunerFragment extends PreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "TunerFragment";

    private static final String KEY_QS_TUNER = "qs_tuner";
/*    private static final String KEY_BATTERY_PCT = "battery_pct";*/
    private static final String STATUS_BAR_SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";
    private static final String STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";
    private static final String STATUS_BAR_BATTERY_STYLE_HIDDEN = "4";
    private static final String STATUS_BAR_BATTERY_STYLE_TEXT = "6";

    private static final String QUICK_PULLDOWN = "quick_pulldown";
    private static final String PREF_SMART_PULLDOWN = "smart_pulldown";
    private static final String QS_SHOW_BRIGHTNESS_SLIDER = "qs_show_brightness_slider";
    private static final String STATUS_BAR_BRIGHTNESS_CONTROL = "status_bar_brightness_control";

    private static final String SHOW_CLEAR_ALL_RECENTS = "show_clear_all_recents";
    private static final String RECENTS_CLEAR_ALL_LOCATION = "recents_clear_all_location";
    private static final String RECENTS_CLEAR_ALL_DISMISS_ALL = "recents_clear_all_dismiss_all";
    private static final String RECENTS_SHOW_SEARCH_BAR = "recents_show_search_bar";
    private static final String RECENT_SHOW_RUNNING_TASKS = "recent_show_running_tasks";

    public static final String SETTING_SEEN_TUNER_WARNING = "seen_tuner_warning";

/*    private final SettingObserver mSettingObserver = new SettingObserver();

    private SwitchPreference mBatteryPct;
*/
    private ListPreference mStatusBarBattery;
    private ListPreference mStatusBarBatteryShowPercent;

    private ListPreference mQuickPulldown;
    private ListPreference mSmartPulldown;

    private SwitchPreference mShowBrightnessSlider;
    private SwitchPreference mStatusbarBrightnessControl;

    private int mbatteryStyle;
    private int mbatteryShowPercent;

    private SwitchPreference mRecentsClearAll;
    private ListPreference mRecentsClearAllLocation;
    private SwitchPreference mRecentsClearAllDimissAll;
    private SwitchPreference mRecentsShowSearchBar;
    private SwitchPreference mRecentsShowRunningTasks;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.tuner_prefs);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        setHasOptionsMenu(true);

        findPreference(KEY_QS_TUNER).setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(android.R.id.content, new QsTuner(), "QsTuner");
                ft.addToBackStack(null);
                ft.commit();
                return true;
            }
        });
/*        mBatteryPct = (SwitchPreference) findPreference(KEY_BATTERY_PCT);*/
        if (Settings.Secure.getInt(getContext().getContentResolver(), SETTING_SEEN_TUNER_WARNING,
                0) == 0) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.tuner_warning_title)
                    .setMessage(R.string.tuner_warning)
                    .setPositiveButton(R.string.got_it, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.Secure.putInt(getContext().getContentResolver(),
                                    SETTING_SEEN_TUNER_WARNING, 1);
                        }
                    }).show();
        }

        mStatusBarBattery = (ListPreference) findPreference(STATUS_BAR_BATTERY_STYLE);
        mStatusBarBatteryShowPercent =
                (ListPreference) findPreference(STATUS_BAR_SHOW_BATTERY_PERCENT);

        mbatteryStyle = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY_STYLE, 0);
        mStatusBarBattery.setValue(String.valueOf(mbatteryStyle));
        mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());
        mStatusBarBattery.setOnPreferenceChangeListener(this);

        mbatteryShowPercent = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0);
        mStatusBarBatteryShowPercent.setValue(String.valueOf(mbatteryShowPercent));
        mStatusBarBatteryShowPercent.setSummary(mStatusBarBatteryShowPercent.getEntry());
        mStatusBarBatteryShowPercent.setOnPreferenceChangeListener(this);
        enableStatusBarBatteryDependents(String.valueOf(mbatteryStyle));

        mQuickPulldown = (ListPreference) findPreference(QUICK_PULLDOWN);
        int quickPulldownValue = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_QUICK_QS_PULLDOWN, 0, UserHandle.USER_CURRENT);
        mQuickPulldown.setValue(String.valueOf(quickPulldownValue));
        mQuickPulldown.setOnPreferenceChangeListener(this);
        updatePulldownSummary(quickPulldownValue);

        mSmartPulldown = (ListPreference) findPreference(PREF_SMART_PULLDOWN);
        int smartPulldownValue = Settings.System.getIntForUser(resolver,
                Settings.System.QS_SMART_PULLDOWN, 0, UserHandle.USER_CURRENT);
        mSmartPulldown.setValue(String.valueOf(smartPulldownValue));
        mSmartPulldown.setOnPreferenceChangeListener(this);
        updateSmartPulldownSummary(smartPulldownValue);

        mShowBrightnessSlider = (SwitchPreference) findPreference(QS_SHOW_BRIGHTNESS_SLIDER);
        int showBrightnessSlider = Settings.Secure.getIntForUser(resolver,
            Settings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, 1, UserHandle.USER_CURRENT);
        mShowBrightnessSlider.setChecked(showBrightnessSlider == 1);
        mShowBrightnessSlider.setOnPreferenceChangeListener(this);

        mStatusbarBrightnessControl = (SwitchPreference) findPreference(STATUS_BAR_BRIGHTNESS_CONTROL);
        int statusbarBrightnessControl = Settings.System.getIntForUser(resolver,
            Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, 0, UserHandle.USER_CURRENT);
        mStatusbarBrightnessControl.setChecked(statusbarBrightnessControl == 1);
        mStatusbarBrightnessControl.setOnPreferenceChangeListener(this);

        mRecentsClearAll = (SwitchPreference) findPreference(SHOW_CLEAR_ALL_RECENTS);
        int recentsClearAllValue = Settings.System.getIntForUser(resolver,
                Settings.System.SHOW_CLEAR_ALL_RECENTS, 0, UserHandle.USER_CURRENT);
        mRecentsClearAll.setChecked(recentsClearAllValue == 1);
        mRecentsClearAll.setOnPreferenceChangeListener(this);

        mRecentsClearAllLocation = (ListPreference) findPreference(RECENTS_CLEAR_ALL_LOCATION);
        int location = Settings.System.getIntForUser(resolver,
                Settings.System.RECENTS_CLEAR_ALL_LOCATION, 3, UserHandle.USER_CURRENT);
        mRecentsClearAllLocation.setValue(String.valueOf(location));
        mRecentsClearAllLocation.setSummary(mRecentsClearAllLocation.getEntry());
        mRecentsClearAllLocation.setOnPreferenceChangeListener(this);

        mRecentsClearAllDimissAll = (SwitchPreference) findPreference(RECENTS_CLEAR_ALL_DISMISS_ALL);
        int recentsClearAllDimissAll = Settings.System.getIntForUser(resolver,
                Settings.System.RECENTS_CLEAR_ALL_DISMISS_ALL, 1, UserHandle.USER_CURRENT);
        mRecentsClearAllDimissAll.setChecked(recentsClearAllDimissAll == 1);
        mRecentsClearAllDimissAll.setOnPreferenceChangeListener(this);

        mRecentsShowSearchBar = (SwitchPreference) findPreference(RECENTS_SHOW_SEARCH_BAR);
        int recentsShowSearchBar = Settings.System.getIntForUser(resolver,
                Settings.System.RECENTS_SHOW_SEARCH_BAR, 1, UserHandle.USER_CURRENT);
        mRecentsShowSearchBar.setChecked(recentsShowSearchBar == 1);
        mRecentsShowSearchBar.setOnPreferenceChangeListener(this);

        mRecentsShowRunningTasks = (SwitchPreference) findPreference(RECENT_SHOW_RUNNING_TASKS);
        int recentsShowRunningTasks = Settings.System.getIntForUser(resolver,
                Settings.System.RECENT_SHOW_RUNNING_TASKS, 0, UserHandle.USER_CURRENT);
        mRecentsShowRunningTasks.setChecked(recentsShowRunningTasks == 1);
        mRecentsShowRunningTasks.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
/*        updateBatteryPct();
        getContext().getContentResolver().registerContentObserver(
                System.getUriFor(SHOW_PERCENT_SETTING), false, mSettingObserver);
*/
        registerPrefs(getPreferenceScreen());
        MetricsLogger.visibility(getContext(), MetricsLogger.TUNER, true);
        enableStatusBarBatteryDependents(String.valueOf(mbatteryStyle));
    }

    @Override
    public void onPause() {
        super.onPause();
/*        getContext().getContentResolver().unregisterContentObserver(mSettingObserver);
*/
        unregisterPrefs(getPreferenceScreen());
        MetricsLogger.visibility(getContext(), MetricsLogger.TUNER, false);
    }

    private void registerPrefs(PreferenceGroup group) {
        TunerService tunerService = TunerService.get(getContext());
        final int N = group.getPreferenceCount();
        for (int i = 0; i < N; i++) {
            Preference pref = group.getPreference(i);
            if (pref instanceof StatusBarSwitch) {
                tunerService.addTunable((Tunable) pref, StatusBarIconController.ICON_BLACKLIST);
            } else if (pref instanceof PreferenceGroup) {
                registerPrefs((PreferenceGroup) pref);
            }
        }
    }

    private void unregisterPrefs(PreferenceGroup group) {
        TunerService tunerService = TunerService.get(getContext());
        final int N = group.getPreferenceCount();
        for (int i = 0; i < N; i++) {
            Preference pref = group.getPreference(i);
            if (pref instanceof Tunable) {
                tunerService.removeTunable((Tunable) pref);
            } else if (pref instanceof PreferenceGroup) {
                registerPrefs((PreferenceGroup) pref);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mStatusBarBattery) {
            mbatteryStyle = Integer.valueOf((String) newValue);
            int index = mStatusBarBattery.findIndexOfValue((String) newValue);
            Settings.System.putInt(
                    resolver, Settings.System.STATUS_BAR_BATTERY_STYLE, mbatteryStyle);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[index]);
            enableStatusBarBatteryDependents((String) newValue);
            return true;
        } else if (preference == mStatusBarBatteryShowPercent) {
            mbatteryShowPercent = Integer.valueOf((String) newValue);
            int index = mStatusBarBatteryShowPercent.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver,
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, mbatteryShowPercent);
            mStatusBarBatteryShowPercent.setSummary(
                    mStatusBarBatteryShowPercent.getEntries()[index]);
            return true;
        } else if (preference == mQuickPulldown) {
            int quickPulldownValue = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(resolver, Settings.System.STATUS_BAR_QUICK_QS_PULLDOWN,
                    quickPulldownValue, UserHandle.USER_CURRENT);
            updatePulldownSummary(quickPulldownValue);
            return true;
        } else if (preference == mSmartPulldown) {
            int smartPulldownValue = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(resolver, Settings.System.QS_SMART_PULLDOWN,
                    smartPulldownValue, UserHandle.USER_CURRENT);
            updateSmartPulldownSummary(smartPulldownValue);
            return true;
        } else if (preference == mShowBrightnessSlider) {
            Settings.Secure.putIntForUser(resolver, Settings.Secure.QS_SHOW_BRIGHTNESS_SLIDER,
                    mShowBrightnessSlider.isChecked() ? 0 : 1, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mStatusbarBrightnessControl) {
            Settings.System.putIntForUser(resolver, Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL,
                    mStatusbarBrightnessControl.isChecked() ? 0 : 1, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mRecentsClearAll) {
            Settings.System.putIntForUser(resolver, Settings.System.SHOW_CLEAR_ALL_RECENTS,
                    mRecentsClearAll.isChecked() ? 0 : 1, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mRecentsClearAllLocation) {
            int location = Integer.valueOf((String) newValue);
            int index = mRecentsClearAllLocation.findIndexOfValue((String) newValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.RECENTS_CLEAR_ALL_LOCATION, location, UserHandle.USER_CURRENT);
            mRecentsClearAllLocation.setSummary(mRecentsClearAllLocation.getEntries()[index]);
            return true;
        } else if (preference == mRecentsClearAllDimissAll) {
            Settings.System.putIntForUser(resolver, Settings.System.RECENTS_CLEAR_ALL_DISMISS_ALL,
                    mRecentsClearAllDimissAll.isChecked() ? 0 : 1, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mRecentsShowSearchBar) {
            Settings.System.putIntForUser(resolver, Settings.System.RECENTS_SHOW_SEARCH_BAR,
                    mRecentsShowSearchBar.isChecked() ? 0 : 1, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mRecentsShowRunningTasks) {
            Settings.System.putIntForUser(resolver, Settings.System.RECENT_SHOW_RUNNING_TASKS,
                    mRecentsShowRunningTasks.isChecked() ? 0 : 1, UserHandle.USER_CURRENT);
            return true;
        }
        return false;
     }

/*    private void updateBatteryPct() {
        mBatteryPct.setOnPreferenceChangeListener(null);
        mBatteryPct.setChecked(System.getInt(getContext().getContentResolver(),
                SHOW_PERCENT_SETTING, 0) != 0);
        mBatteryPct.setOnPreferenceChangeListener(mBatteryPctChange);
    }

    private final class SettingObserver extends ContentObserver {
        public SettingObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange, Uri uri, int userId) {
            super.onChange(selfChange, uri, userId);
            updateBatteryPct();
        }
    }

    private final OnPreferenceChangeListener mBatteryPctChange = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final boolean v = (Boolean) newValue;
            MetricsLogger.action(getContext(), MetricsLogger.TUNER_BATTERY_PERCENTAGE, v);
            System.putInt(getContext().getContentResolver(), SHOW_PERCENT_SETTING, v ? 1 : 0);
            return true;
        }
    };
*/
    private void enableStatusBarBatteryDependents(String value) {
        boolean enabled = !(value.equals(STATUS_BAR_BATTERY_STYLE_TEXT)
                || value.equals(STATUS_BAR_BATTERY_STYLE_HIDDEN));
        mStatusBarBatteryShowPercent.setEnabled(enabled);
    }

    private void updatePulldownSummary(int value) {
        Resources res = getResources();
        if (value == 0) {
            // quick pulldown deactivated
            mQuickPulldown.setSummary(res.getString(R.string.quick_pulldown_off));
        } else if (value == 3) {
            // quick pulldown always
            mQuickPulldown.setSummary(res.getString(R.string.quick_pulldown_summary_always));
        } else {
            String direction = res.getString(value == 2
                    ? R.string.quick_pulldown_summary_left
                    : R.string.quick_pulldown_summary_right);
            mQuickPulldown.setSummary(res.getString(R.string.quick_pulldown_summary, direction));
        }
    }

    private void updateSmartPulldownSummary(int value) {
        Resources res = getResources();

        if (value == 0) {
            // Smart pulldown deactivated
            mSmartPulldown.setSummary(res.getString(R.string.smart_pulldown_off));
        } else {
            String type = null;
            switch (value) {
                case 1:
                    type = res.getString(R.string.smart_pulldown_dismissable);
                    break;
                case 2:
                    type = res.getString(R.string.smart_pulldown_persistent);
                    break;
                default:
                    type = res.getString(R.string.smart_pulldown_all);
                    break;
            }
            // Remove title capitalized formatting
            type = type.toLowerCase();
            mSmartPulldown.setSummary(res.getString(R.string.smart_pulldown_summary, type));
        }
    }
}
