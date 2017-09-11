package com.jakdor.gpsspeedometer;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

/**
 * Class defining AppPreferencesActivity(settings screen) behaviour
 */
public class AppPreferencesActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fragment fr = new AppPreferencesFragment();
        FragmentManager fm = getFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.add(android.R.id.content, fr);
        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {
        MainActivity.settingsClickHandler();
        finish();
        super.onBackPressed();
    }

    public static class AppPreferencesFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
        }

        @Override
        public void onResume() { //end fragment onBack pressed
            super.onResume();
            //handleBackButtonPress();
        }

        private void handleBackButtonPress(){
            try {
                getView().setFocusableInTouchMode(true);
            }
            catch (java.lang.NullPointerException e){
                Log.e("Exception", "PreferenceFragment setFocusable problem: " + e.getMessage());
            }
            getView().requestFocus();
            getView().setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {

                    if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK){

                        getActivity().getFragmentManager().popBackStack();
                        getActivity().finish();
                        return true;
                    }
                    return false;
                }
            });
        }

    }
}
