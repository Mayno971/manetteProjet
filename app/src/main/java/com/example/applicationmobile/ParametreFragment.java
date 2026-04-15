package com.example.applicationmobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.materialswitch.MaterialSwitch;

public class ParametreFragment extends BottomSheetDialogFragment {
    public interface ParametresListener {
        void onTailleBoutonsChangee(float echelle);
        void onOpaciteChangee(float alpha);
        void onModeGaucherChange(boolean isGaucher);
        void onVibrationsChange(boolean isVibrationActive);
    }

    private ParametresListener listener;

    private static final String ARG_GAUCHER = "arg_gaucher";
    private static final String ARG_VIBRE = "arg_vibre";
    private static final String ARG_TAILLE = "arg_taille";
    private static final String ARG_OPACITE = "arg_opacite";

    public static ParametreFragment newInstance(boolean gaucher, boolean vibrations, int taille, int opacite) {
        ParametreFragment fragment = new ParametreFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_GAUCHER, gaucher);
        args.putBoolean(ARG_VIBRE, vibrations);
        args.putInt(ARG_TAILLE, taille);
        args.putInt(ARG_OPACITE, opacite);
        fragment.setArguments(args);
        return fragment;
    }

    public void setListener(ParametresListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parametres, container, false);

        boolean etatGaucher = false;
        boolean etatVibrations = true;
        int etatTaille = 50;
        int etatOpacite = 100;

        if (getArguments() != null) {
            etatGaucher = getArguments().getBoolean(ARG_GAUCHER, false);
            etatVibrations = getArguments().getBoolean(ARG_VIBRE, true);
            etatTaille = getArguments().getInt(ARG_TAILLE, 50);
            etatOpacite = getArguments().getInt(ARG_OPACITE, 100);
        }

        SeekBar seekSize = view.findViewById(R.id.seek_size);
        SeekBar seekOpacity = view.findViewById(R.id.seek_opacity);
        MaterialSwitch switchGaucher = view.findViewById(R.id.switch_gaucher);
        MaterialSwitch switchVibrations = view.findViewById(R.id.switch_vibrations);
        ImageView btnClose = view.findViewById(R.id.btn_disconnect); // Le bouton Croix

        seekSize.setProgress(etatTaille);
        seekOpacity.setProgress(etatOpacite);
        switchGaucher.setChecked(etatGaucher);
        switchVibrations.setChecked(etatVibrations);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        seekSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (listener != null) {
                    float echelle = 0.6f + (progress / 100f) * 0.8f;
                    listener.onTailleBoutonsChangee(echelle);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Minimum 20% d'opacité (0.2f)
                if (listener != null) {
                    listener.onOpaciteChangee(Math.max(0.2f, progress / 100f));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        switchGaucher.setOnCheckedChangeListener((btn, isChecked) -> {
            if (listener != null) listener.onModeGaucherChange(isChecked);
        });

        switchVibrations.setOnCheckedChangeListener((btn, isChecked) -> {
            if (listener != null) listener.onVibrationsChange(isChecked);
        });

        return view;
    }
}