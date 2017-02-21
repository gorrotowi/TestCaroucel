package com.gorrotowi.testcarouselfragments;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ViewPagerAdapter viewPagerAdapter;
    NonSwipeableViewPager viewPagerMainTab;
    TabLayout tablayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPagerMainTab = (NonSwipeableViewPager) findViewById(R.id.viewPagerMainTab);
        tablayout = (TabLayout) findViewById(R.id.tablayout);
        List<Fragment> fragmentList = new ArrayList<>();

        fragmentList.add(new FragmentOne());
        fragmentList.add(new FragmentTwo());
        fragmentList.add(new FragmentThree());
        fragmentList.add(new FragmentFour());

        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), fragmentList);
        viewPagerMainTab.setAdapter(viewPagerAdapter);
        tablayout.setupWithViewPager(viewPagerMainTab);

        for (int i = 0; i < tablayout.getTabCount(); i++) {
            if (tablayout.getTabAt(i) != null)
                tablayout.getTabAt(i).setText("Tab " + i);
        }

    }
}
