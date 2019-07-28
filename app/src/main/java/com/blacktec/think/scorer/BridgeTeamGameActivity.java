package com.blacktec.think.scorer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import me.weyye.library.colortrackview.ColorTrackTabLayout;

/**
 * Created by Think on 2018/4/1.
 */

public class BridgeTeamGameActivity extends AppCompatActivity {
    private String[] titles;
    private ColorTrackTabLayout mTab;
    private ViewPager mViewPager;
    private BridgeResultSheetTeam mResultSheetTeam;

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu != null) {
            if (menu.getClass().getSimpleName().equalsIgnoreCase("MenuBuilder")) {
                try {
                    Method method = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    method.setAccessible(true);
                    method.invoke(menu, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(mResultSheetTeam==null)mResultSheetTeam=BridgeResultSheetTeam.getRecent(this);
        loadViewLayout();
        initView();
        initTab();
    }
    private void loadViewLayout()
    {
        setContentView(R.layout.activity_team_game);
        //setTitle("队式赛");
    }
    private void initView() {
        mTab = (ColorTrackTabLayout) findViewById(R.id.tab);
//        TabLayout tab = (TabLayout) findViewById(R.id.tab);
        mViewPager = (ViewPager) findViewById(R.id.viewPager);

    }

    protected void initTab() {
        titles = new String[]{"开  室", "闭  室","成 绩 表"};

        //mTab.setTabPaddingLeftAndRight(70, 70);
        //mTab.setSelectedTabIndicatorHeight(0);

        final List<Fragment> fragments = new ArrayList<>();
        fragments.add(ResultListFragment.newInstance(mResultSheetTeam.getTeamInfo().getOpenRoomId(),mResultSheetTeam.getId()));
        fragments.add(ResultListFragment.newInstance(mResultSheetTeam.getTeamInfo().getClosedRoomId(),mResultSheetTeam.getId()));
        fragments.add(TeamResultFragment.newInstance(mResultSheetTeam.getId()));
        //fragments.add(ResultListFragment.newInstance(mResultSheetTeam.getTeamInfo().getClosedRoomId(),mResultSheetTeam.getId()));

        mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return fragments.get(position);
            }

            @Override
            public int getCount() {
                return titles.length;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return titles[position];
            }

        });
        mTab.setupWithViewPager(mViewPager);
        //默认选中第5个
        //mTab.setLastSelectedTabPosition(1);
        //移动到第5个
        //mTab.setCurrentItem(1);
        //mViewPager.setOffscreenPageLimit(titles.length);
    }
}