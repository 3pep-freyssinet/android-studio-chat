package com.google.amara.chattab.ui.main;

import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;


import com.google.amara.chattab.ChatBoxMessage;
import com.google.amara.chattab.ChatBoxUsers;
import com.google.amara.chattab.R;
import com.google.amara.chattab.TabChatActivity;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
//the 'FragmentPagerAdapter' will keep all the views that it loads into memory forever.
//public class SectionsPagerAdapter extends FragmentPagerAdapter {
public class SectionsPagerAdapter extends FragmentStatePagerAdapter {   //see above why ?
    @StringRes
    private static final int[] TAB_TITLES =  new int[]{R.string.tab_text_1, R.string.tab_text_2};
    private final Context mContext;
    FragmentManager fragmentManager;

    public SectionsPagerAdapter(Context context, FragmentManager fragmentManager) {
        super(fragmentManager);
        mContext = context;
        this.fragmentManager = fragmentManager;
    }

    //Ne fait rien
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        // And here i am removing it from the sparse array
        fragmentManager.getFragments().remove(position);
        super.destroyItem(container, position, object);
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given tab.
        // When it is defined, it returns a fragment else a PlaceholderFragment (defined as a static inner class in 'PlaceHolderFragment').
        switch (position) {
            case 0:
                //return new TabbedFragment();
                Fragment f0 = new ChatBoxUsers();

                //set tag for this fragment ---> exception : the fragment is already added
                //fragmentManager.beginTransaction().add(f0, "chat_box_user").commit();

                //la ligne suivante fait que le fragment ne s'affiche pas.
                //fragmentManager.beginTransaction().replace(R.id.view_pager, f0, "chat_box_user")
                //    .commit();

                Bundle bundle0 = new Bundle();
                TabChatActivity tabChatActivity = (TabChatActivity)mContext;
                bundle0.putString("Nickname", tabChatActivity.Nickname);

                //bundle0.putSerializable("chat_user_list", (Serializable) TabChatActivity.chatUserList);
                //bundle0.putString("nickname", TabChatActivity.Nickname);
                f0.setArguments(bundle0);

                //getSupportFragmentManager().beginTransaction().add(f0,"Some Tag").commit();
                return f0;

            case 1:
                //return new TabbedFragment();
                Fragment f1 = new ChatBoxMessage();

                //set tag for this fragment ----->exception : the fragment is already added
                //fragmentManager.beginTransaction().add(f1, "chat_box_message").commit();

                //la ligne suivante fait que le fragment ne s'affiche pas
                //fragmentManager.beginTransaction().replace(R.id.view_pager, f1, "chat_box_message")
                //        .commit();

                Bundle bundle1 = new Bundle();
                //bundle1.putSerializable("chat_message_list", (Serializable) TabChatActivity.messageListUser);
                bundle1.putInt("tab", position);
                //bundle1.putString("Nickname", TabChatActivity.Nickname);
                // le id du Nickname n'est pas encore connu
                f1.setArguments(bundle1);
                return f1;

            default:
                return null;
        }
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getResources().getString(TAB_TITLES[position]);
    }

    @Override
    public int getCount() {
        // Show total pages.
        return TAB_TITLES.length;
    }

    //this is called when notifyDataSetChanged() is called
    @Override
    public int getItemPosition(Object object){
        //int index = fragmentManager.getFragments().indexOf(object);
        //if (index == -1)
        //    return POSITION_NONE;
        //else
        //    return index;
        return PagerAdapter.POSITION_NONE;
    }

    /*
    public View getView(final int position, View convertView, ViewGroup parent) {

        if (convertView == null) {

            LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            convertView = layoutInflater.inflate(R.layout.layout_test, null); //layout du detail de la listView
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.out.println("Tabbed tab = "+position);
                }
            });
        }
        return convertView;
    }
    */

}