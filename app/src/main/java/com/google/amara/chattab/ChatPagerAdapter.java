package com.google.amara.chattab;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ChatPagerAdapter extends FragmentStateAdapter {

    public ChatPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) return new ChatBoxUsers();
        return new ChatBoxMessage();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
