package com.google.zxing.client.android;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public final class PreferencesFragment extends PreferenceFragment  {
  
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    addPreferencesFromResource(R.xml.preferences);
  }
}
