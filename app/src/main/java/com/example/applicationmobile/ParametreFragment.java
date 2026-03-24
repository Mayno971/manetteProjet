package com.example.applicationmobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
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

    private boolean etatGaucher = false;
    private boolean etatVibrations = true;
    private int etatTaille = 50;
    private int etatOpacite = 100;

    public void setListener(ParametresListener listener) {
        this.listener = listener;
    }

    public void setEtatsInitiaux(boolean gaucher, boolean vibrations, int taille, int opacite) {
        this.etatGaucher = gaucher;
        this.etatVibrations = vibrations;
        this.etatTaille = taille;
        this.etatOpacite = opacite;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parametres, container, false);

        SeekBar seekSize = view.findViewById(R.id.seek_size);
        SeekBar seekOpacity = view.findViewById(R.id.seek_opacity);

        MaterialSwitch switchGaucher = view.findViewById(R.id.switch_gaucher);
        MaterialSwitch switchVibrations = view.findViewById(R.id.switch_vibrations);

        seekSize.setProgress(etatTaille);
        seekOpacity.setProgress(etatOpacite);
        switchGaucher.setChecked(etatGaucher);
        switchVibrations.setChecked(etatVibrations);

        seekSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (listener != null) {
                    float echelle = 0.6f + (progress / 100f) * 0.8f;
                    listener.onTailleBoutonsChangee(echelle); // On envoie l'info à l'Activity !
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Minimum 20% d'opacité (0.2f) pour ne pas rendre les boutons totalement invisibles !
                if (listener != null) listener.onOpaciteChangee(Math.max(0.2f, progress / 100f));
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