package com.blacktec.think.scorer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.blacktec.think.scorer.Utils.RecyclerViewDivider;

import java.util.List;
import java.util.UUID;

/**
 * Created by Think on 2018/4/4.
 */

public class TeamResultFragment extends Fragment {
    private static final String SAVED_SUBTITLE_VISIBLE = "subtitle";
    private static final String ARG_TEAM_ID = "team_id";
    private BridgeResultSheetTeam mResultTeam;
    private RecyclerView mTeamResultRecyclerView;
    private BridgeTeamResultAdapter mAdapter;
    private boolean mSubtitleVisible;

    private TextView mHomeVpTextView;
    private TextView mAwayVpTextView;

    public static TeamResultFragment newInstance(UUID teamId) {
        TeamResultFragment fragment = new TeamResultFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TEAM_ID, teamId);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            UUID uuid = (UUID) getArguments().getSerializable(ARG_TEAM_ID);
            mResultTeam=new BridgeResultSheetTeam(getActivity(),uuid);
        }
        if(mResultTeam==null)mResultTeam = BridgeResultSheetTeam.getRecent(getActivity());
        setHasOptionsMenu(true);
    }
    @Override
    public void onResume() {
        super.onResume();
        //updateUI();
    }
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser){
            updateUI();
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_team_result, container, false);
        mAdapter=null;
        mTeamResultRecyclerView = (RecyclerView) view
                .findViewById(R.id.team_result_recycler_view);
        mTeamResultRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mTeamResultRecyclerView.addItemDecoration(new RecyclerViewDivider(getContext(), LinearLayoutManager.VERTICAL));
        if (savedInstanceState != null) {
            mSubtitleVisible = savedInstanceState.getBoolean(SAVED_SUBTITLE_VISIBLE);
        }
        mHomeVpTextView=(TextView)view.findViewById(R.id.textViewHomeVp);
        mAwayVpTextView=(TextView)view.findViewById(R.id.textViewAwayVp);

        updateUI();
        return view;
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_SUBTITLE_VISIBLE, mSubtitleVisible);
    }
    private void updateSubtitle() {

    }
    private void updateVpBar() {
        mHomeVpTextView.setText(String.format(getString(R.string.string_vp_imp),mResultTeam.getHomeVp(),mResultTeam.getHomeIMP()));
        mAwayVpTextView.setText(String.format(getString(R.string.string_vp_imp),mResultTeam.getAwayVp(),mResultTeam.getAwayIMP()));
        LayoutParams param1 = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, mResultTeam.getHomeVp());
        LayoutParams param2 = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, mResultTeam.getAwayVp());
        mHomeVpTextView.setLayoutParams(param1);
        mAwayVpTextView.setLayoutParams(param2);
    }

    private void updateUI() {
        if(mResultTeam==null||!mResultTeam.calculateResult())return;
        List<BridgeIMPInfo> impInfoList = mResultTeam.getIMPInfoList();

        if (mAdapter == null) {
            mAdapter = new BridgeTeamResultAdapter(impInfoList);
            mTeamResultRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.setIMPInfoList(impInfoList);
            mAdapter.notifyDataSetChanged();
        }
        updateVpBar();
        updateSubtitle();
    }
    private class BridgeTeamResultHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private TextView mHandsNumTextView;
        private TextView mOpenResultTextView;
        private TextView mClosedResultTextView;
        private TextView mScoreDifferenceTextView;
        private TextView mHomeIMPTextView;
        private TextView mAwayIMPTextView;

        private BridgeIMPInfo mIMPInfo;

        BridgeTeamResultHolder(View itemView) {
            super(itemView);
            mHandsNumTextView = (TextView) itemView.findViewById(R.id.list_item_Hands_num);
            mOpenResultTextView = (TextView) itemView.findViewById(R.id.list_item_open_result);
            mClosedResultTextView = (TextView) itemView.findViewById(R.id.list_item_closed_result);
            mScoreDifferenceTextView = (TextView) itemView.findViewById(R.id.list_item_score_difference);
            mHomeIMPTextView = (TextView) itemView.findViewById(R.id.list_item_home_score);
            mAwayIMPTextView = (TextView) itemView.findViewById(R.id.list_item_away_score);
        }
        @Override
        public void onClick(View v) {

        }
        public void bindBridgeIMPInfo(BridgeIMPInfo impInfo) {
            mIMPInfo=impInfo;
            mHandsNumTextView.setText(String.valueOf(mIMPInfo.getBoardNum()));
            mOpenResultTextView.setText(impInfo.getOpenRoomContract());
            mClosedResultTextView.setText(impInfo.getClosedRoomContract());
            mScoreDifferenceTextView.setText("");
            mHomeIMPTextView.setText("");
            mAwayIMPTextView.setText("");
            if(mIMPInfo.getOpenRoomContract().equals("")|mIMPInfo.getClosedRoomContract().equals(""))
            {
                mHandsNumTextView.setTextColor(ContextCompat.getColor(getContext(),R.color.color_boardNum_unfinished));
                return;
            }
            mHandsNumTextView.setTextColor(ContextCompat.getColor(getContext(),R.color.color_boardNum_finished1));
            if(mIMPInfo.getScoreDifference()!=0)
            {
                mScoreDifferenceTextView.setText(String.valueOf(mIMPInfo.getScoreDifference()));
                if(mIMPInfo.getScoreDifference()>0)
                {
                    mHomeIMPTextView.setText(String.valueOf(mIMPInfo.getIMP()));
                    mScoreDifferenceTextView.setTextColor(ContextCompat.getColor(getContext(),android.R.color.holo_blue_light));
                }
                else
                {
                    mAwayIMPTextView.setText(String.valueOf(-mIMPInfo.getIMP()));

                    mScoreDifferenceTextView.setTextColor(ContextCompat.getColor(getContext(),android.R.color.holo_red_light));
                }
            }
        }
    }
    private class BridgeTeamResultAdapter extends RecyclerView.Adapter<BridgeTeamResultHolder> {
        private List<BridgeIMPInfo> mIMPInfoList;
        public BridgeTeamResultAdapter(List<BridgeIMPInfo> impInfoList){
            mIMPInfoList = impInfoList;
        }
        @Override
        public BridgeTeamResultHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.item_team_result, parent, false);
            return new BridgeTeamResultHolder(view);
        }
        @Override
        public void onBindViewHolder(BridgeTeamResultHolder holder, int position) {
            BridgeIMPInfo impInfo = mIMPInfoList.get(position);
            holder.bindBridgeIMPInfo(impInfo);
        }
        @Override
        public int getItemCount() {
            return mIMPInfoList.size();
        }
        public void setIMPInfoList(List<BridgeIMPInfo> impInfoList) {
            mIMPInfoList=impInfoList;
        }
    }
}
