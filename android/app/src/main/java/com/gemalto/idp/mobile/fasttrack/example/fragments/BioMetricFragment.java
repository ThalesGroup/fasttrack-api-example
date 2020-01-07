/**
 * ------------------------------------------------------------------------------
 *
 *     Copyright (c) 2019  GEMALTO DEVELOPMENT - R&D
 *
 * ------------------------------------------------------------------------------
 * GEMALTO MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. GEMALTO SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * THIS SOFTWARE IS NOT DESIGNED OR INTENDED FOR USE OR RESALE AS ON-LINE
 * CONTROL EQUIPMENT IN HAZARDOUS ENVIRONMENTS REQUIRING FAIL-SAFE
 * PERFORMANCE, SUCH AS IN THE OPERATION OF NUCLEAR FACILITIES, AIRCRAFT
 * NAVIGATION OR COMMUNICATION SYSTEMS, AIR TRAFFIC CONTROL, DIRECT LIFE
 * SUPPORT MACHINES, OR WEAPONS SYSTEMS, IN WHICH THE FAILURE OF THE
 * SOFTWARE COULD LEAD DIRECTLY TO DEATH, PERSONAL INJURY, OR SEVERE
 * PHYSICAL OR ENVIRONMENTAL DAMAGE ("HIGH RISK ACTIVITIES"). GEMALTO
 * SPECIFICALLY DISCLAIMS ANY EXPRESS OR IMPLIED WARRANTY OF FITNESS FOR
 * HIGH RISK ACTIVITIES.
 *
 * ------------------------------------------------------------------------------
 */
package com.gemalto.idp.mobile.fasttrack.example.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.gemalto.idp.mobile.fasttrack.example.R;

/**
 * A dialog which uses fingerprint APIs to authenticate the user, and falls back
 * to password authentication if fingerprint is not available.
 */
public class BioMetricFragment extends DialogFragment {

    private TextView promptTxtView;
    private BioFpFragmentCallback listener = null;
    private String title = null;

    public static BioMetricFragment newInstance(BioFpFragmentCallback listener, String title) {
        BioMetricFragment fragment = new BioMetricFragment();
        fragment.listener = listener;
        fragment.title = title;
        return fragment;
    }

    @Override
    @SuppressWarnings("InlinedApi")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(STYLE_NO_TITLE);
        setCancelable(false);

        View v = inflater.inflate(R.layout.fingerprint_dialog_container, container, false);
        TextView titleTxtView = v.findViewById(R.id.fingerprint_title);
        titleTxtView.setText(title);

        promptTxtView = v.findViewById(R.id.fingerprint_prompt);

        Button cancelButton = v.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(view -> {
            dismiss();
            if (listener != null) {
                listener.onCancel();
            }
        });

        return v;
    }

    public void setPromptText(String text) {
        promptTxtView.setText(text);
    }

    public interface BioFpFragmentCallback {
        void onCancel();
    }
}
