package it.Ettore.egalfilemanager.activity;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.fragment.FragmentAbout;
import it.Ettore.egalfilemanager.fragment.FragmentCrediti;
import it.Ettore.egalfilemanager.fragment.FragmentTraduzioni;


/**
 * Activity per la visualizzazione delle informazioni sull'app
 */
public class ActivityAbout extends BaseActivity {
    private FragmentManager fragmentManager;
    private Fragment fragmentCorrente;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        settaTitolo(R.string.about);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final TabLayout tabLayout = findViewById(R.id.tabLayout);
        fragmentManager = getSupportFragmentManager();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                int position = tab.getPosition();
                switch (position){
                    case 0: //about
                        fragmentCorrente = new FragmentAbout();
                        break;
                    case 1: //crediti
                        fragmentCorrente = new FragmentCrediti();
                        break;
                    case 2: //traduttori
                        fragmentCorrente = new FragmentTraduzioni();
                        break;
                    default:
                        throw new IllegalArgumentException("Posizione tab non gestita: " + position);
                }
                fragmentTransaction.replace(R.id.container, fragmentCorrente);
                fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                fragmentTransaction.commit();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        //Visualizzo il tab predefinito
        if(savedInstanceState == null){
            fragmentManager.beginTransaction().replace(R.id.container, new FragmentAbout()).commit();
        }
    }
}
