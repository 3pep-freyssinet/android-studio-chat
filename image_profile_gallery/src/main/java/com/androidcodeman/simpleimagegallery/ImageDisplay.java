package com.androidcodeman.simpleimagegallery;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.transition.Fade;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.androidcodeman.simpleimagegallery.fragments.pictureBrowserFragment;
import com.androidcodeman.simpleimagegallery.utils.MarginDecoration;
import com.androidcodeman.simpleimagegallery.utils.PicHolder;
import com.androidcodeman.simpleimagegallery.utils.itemClickListener;
import com.androidcodeman.simpleimagegallery.utils.pictureFacer;
import com.androidcodeman.simpleimagegallery.utils.picture_Adapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This Activity get a path to a folder that contains images from the MainActivity Intent and displays
 * all the images in the folder inside a RecyclerView
 */

public class ImageDisplay extends AppCompatActivity
                          implements itemClickListener, PicHolder.ItemClickListener {

    private final int INTENT_ZOOM_REQUEST_CODE = 100;

    private RecyclerView    imageRecycler;
    private TextView        folderName;
    private TextView        nbPictures;
    private ProgressBar     load;
    private View            head;              //card view

    private ArrayList<pictureFacer> allpictures;
    private String          foldePath;
    public  boolean         multiSelect;
    private boolean         checkBoxSelection[];
    private ActionMode      actionMode;
    private ArrayList<pictureFacer> selectedItems;
    private int             nbChecks;
    private ViewGroup.LayoutParams originalLayoutParams;    //original layout of 'ActionBar
    private TextView        checkCount;                     //menu textview
    private MenuItem        okMenuItem, cancelMenuItem;     // menu icons('OK', 'Cancel')
    public  static ImageDisplay instance;                   // a refrence of this activty.
    private LinearLayout    llStatus;
    private ImageView       statusStar, statusShare, statusCopy, statusDelete;    //status bar icons
    private ImageView       previousPictureCheck = null;

    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu, menu);

            //Set default icons
            okMenuItem = menu.findItem(R.id.action_ok);     //ok icon
            okMenuItem.setIcon(getResources().getDrawable(R.drawable.check_grey_36));

            cancelMenuItem = menu.findItem(R.id.action_cancel); //cancel icon
            cancelMenuItem.setIcon(getResources().getDrawable(R.drawable.cancel_grey_36));

            MenuItem item = menu.findItem(R.id.action_text_);    //textview
            item.setActionView(R.layout.action_mode_textview);
            View view = item.getActionView();
            checkCount = view.findViewById(R.id.action_mode_textview);
            if (null == view) {
                Log.e("NULL POINTER EX", "NULL MENU VIEW");
            } else {
                checkCount.setText(getResources().getString(R.string.picture_selected, 0));
            }

            return true; //false = no bar showed
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

            menu.findItem(R.id.action_text_).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.action_cancel).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.action_ok).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            int itemId = item.getItemId();
            if (itemId == R.id.action_cancel) {
                nbChecks = 0;
                selectedItems.clear();
                previousPictureCheck = null;

                //empty 'checkBoxSelection' witch holds the state of checkbox (false or true).
                for (int i = 0; i < checkBoxSelection.length; i++) {
                    checkBoxSelection[i] = false;
                }

                //update the 'ActionMode' bar textview
                checkCount.setText(getResources().getString(R.string.picture_selected, 0));

                //update the icons, displaying greyed icons.
                okMenuItem.setIcon(R.drawable.check_grey_36);
                cancelMenuItem.setIcon(R.drawable.cancel_grey_36);

                //update the status icons, displaying greyed icons.
                //status icons
                int starIconGreyed = R.drawable.star_36_greyed;
                int shareIconGreyed = R.drawable.share_36_greyed;
                int copyIconGreyed = R.drawable.copy_36_greyed;
                int deleteIconGreyed = R.drawable.delete_36_greyed;

                statusStar.setImageDrawable(getResources().getDrawable(starIconGreyed));
                statusShare.setImageDrawable(getResources().getDrawable(shareIconGreyed));
                statusDelete.setImageDrawable(getResources().getDrawable(copyIconGreyed));
                statusCopy.setImageDrawable(getResources().getDrawable(deleteIconGreyed));

                //Update the recycler since many values has changed.
                imageRecycler.setAdapter(new picture_Adapter(allpictures, ImageDisplay.this,
                        ImageDisplay.this));
                imageRecycler.invalidate();

                //mode.finish(); // Action picked, so close the Contextual Action Bar (CAB).
                return true;
            } else if (itemId == R.id.action_ok) {
                Toast.makeText(ImageDisplay.this,
                        getResources().getQuantityString(R.plurals.quantities, nbChecks, nbChecks), Toast.LENGTH_LONG).show();

                //if the icon is greyed, do nothing.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Drawable d = getDrawable(R.drawable.check_grey_36);
                    Drawable.ConstantState constantState = d.getConstantState();
                    Drawable.ConstantState constantState_ = okMenuItem.getIcon().getConstantState();

                    if (constantState == constantState_) return false;
                }

                //Return back to the caller 'ImageProfileGalleryMainActivity' witch create this intent. We add some extras.
                //Fill the array list 'selectedItems_' with 'imageUri' from 'selectedItems'.
                ArrayList<String> selectedItems_ = new ArrayList<>();
                Iterator<pictureFacer> iterator = selectedItems.iterator();
                while (iterator.hasNext()) {

                    String uri_ = iterator.next().getImageUri(); //uri_ is a string representing an uri

                    /*
                    String id = Uri.parse(uri_).getLastPathSegment();
                    //get the uri of image and then get the bitmap to display
                    Long id_ = Long.parseLong(id);
                    Uri uri  = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id_);
                    */

                    selectedItems_.add(uri_);
                }

                    /*
                    String id = selectedItems.get( 0).getImageUri();
                    Long id_  = Long.parseLong(id);
                    Uri uri   = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id_);
                    //ou
                    String path = selectedItems.get(0).getPicturePath();
                    //Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, path);
                    */

                //Return back to the caller 'ImageProfileGalleryMainActivity' witch create this intent. We add some extras.
                Intent intent = new Intent();
                //intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                //File pathDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                //Uri resultUri = Uri.fromFile(pathDir); // the thing to return
                //intent.setData(resultUri);
                //intent.setData(uri);

                intent.putStringArrayListExtra("uri_selected_pictures", selectedItems_); //'selectedItems' is an arrayList of string representing uris
                setResult(Activity.RESULT_OK, intent);

                finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            //if (actionMode != null)actionMode = null; //.finish();
            //head.setLayoutParams(originalLayoutParams);
            llStatus.setVisibility(View.GONE);
            multiSelect = false;
            actionMode = null;
            nbChecks = 0;
            selectedItems.clear();

            //empty
            for (int i = 0; i < checkBoxSelection.length; i++) {
                checkBoxSelection[i] = false;
            }

            //Update the recucler since many values has changed.
            imageRecycler.setAdapter(new picture_Adapter(allpictures, ImageDisplay.this, ImageDisplay.this));
            imageRecycler.invalidate();

            //show the hiddden 'CardView' bar
            //head.setLayoutParams(originalLayoutParams);
            head.setVisibility(View.VISIBLE);

            //imageAdapter = new ImageAdapter();
            //imagegrid.setAdapter(imageAdapter);
            //show the checkbox in adapter
            //imageAdapter = new ImageAdapter();
            //imagegrid.setAdapter(imageAdapter);
            //selectedItems.clear();

            //Update the adapter and hide the checkbox
            //imageAdapter = new ImageAdapter();
            //imagegrid.setAdapter(imageAdapter);

        }
    };



    //----------------------------------------------------------------------------------------------

    public static ImageDisplay newInstance() {
        ImageDisplay imageDisplay = new ImageDisplay();
        return imageDisplay;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_display);

        //Set a static reference of this activity
        this.instance = this;

        //Get linear layout 'status' or the bottom bar.
        llStatus     = findViewById(R.id.ll_status);
        llStatus.setVisibility(View.GONE);  //at startup

        statusStar   = findViewById(R.id.iv_star);
        statusShare  = findViewById(R.id.iv_share);
        statusCopy   = findViewById(R.id.iv_copy);
        statusDelete = findViewById(R.id.iv_delete);

        //get textView in card view
        folderName = findViewById(R.id.foldername);

        //Get the extras joigned with intent and set text in card view
        folderName.setText(getIntent().getStringExtra("folderName"));

        foldePath = getIntent().getStringExtra("folderPath");

        allpictures   = getIntent().getParcelableArrayListExtra("uri");

        imageRecycler = findViewById(R.id.recycler);
        imageRecycler.addItemDecoration(new MarginDecoration(this));
        imageRecycler.hasFixedSize();
        //load = findViewById(R.id.progress_bar);
        nbPictures    = findViewById(R.id.nb_pictures);
        head          = findViewById(R.id.head);
        actionMode    = null;
        selectedItems = new ArrayList<>();

        if (!allpictures.isEmpty()) {
            //load.setVisibility(View.VISIBLE);
            //allpictures = getAllImagesByFolder(foldePath);
            checkBoxSelection = new boolean[allpictures.size()]; //filled with 'false' default value.

            imageRecycler.setAdapter(new picture_Adapter(allpictures, this,
                    this));
            //load.setVisibility(View.GONE);
            nbPictures.setText(getResources().getString(R.string.all_pictures, allpictures.size()));

        } else { }
    }


    /**
     * @param holder   The ViewHolder for the clicked picture
     * @param position The position in the grid of the picture that was clicked
     * @param pics     An ArrayList of all the items in the Adapter
     */
    @Override
    public void onPicClicked(PicHolder holder, int position, ArrayList<pictureFacer> pics) {
        //After click on picture in folder
        pictureBrowserFragment browser = pictureBrowserFragment.newInstance(pics, position, ImageDisplay.this);

        // Note that we need the API version check here because the actual transition classes (e.g. Fade)
        // are not in the support library and are only available in API 21+. The methods we are calling on the Fragment
        // ARE available in the support library (though they don't do anything on API < 21)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //browser.setEnterTransition(new Slide());
            //browser.setExitTransition(new Slide()); uncomment this to use slide transition and comment the two lines below
            browser.setEnterTransition(new Fade());
            browser.setExitTransition(new Fade());
        }

        getSupportFragmentManager()
                .beginTransaction()
                .addSharedElement(holder.picture, position + "picture")
                .add(R.id.displayContainer, browser)
                .addToBackStack(null)
                .commit();

        //Return back to the caller 'ImageProfileGalleryMainActivity' witch create this intent.
        // We add some extras.
        String id = pics.get(position).getImageUri();
        Long id_  = Long.parseLong(id);
        Uri uri   = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id_);

        //or
        String path = pics.get(position).getPicturePath();
        //Uri uri   = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, path);

        Intent intent = new Intent();
        //File pathDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        //Uri resultUri = Uri.fromFile(pathDir); // the thing to return
        //intent.setData(resultUri);
        intent.setData(uri);
        setResult(Activity.RESULT_OK, intent);

        finish();

    }

    public void onStart() {
        super.onStart();
    }

    public void onResume() {
        super.onResume();
    }

    public void finish() {
        super.finish();
    }

    @Override
    public void onBackPressed(){
        finish();
    }

    public void onPause() {
        super.onPause();
    }

    public void onStop() {
        super.onStop();
    }

    @Override
    public void onPicClicked(String pictureFolderPath, String folderName) {
    }

    /**
     * This Method gets all the images in the folder paths passed as a String to the method and returns
     * and ArrayList of pictureFacer a custom object that holds data of a given image
     *
     * @param folderPath a String corresponding to a folder path on the device external storage
     */
    public ArrayList<pictureFacer> getAllImagesByFolder(String folderPath) {
        ArrayList<pictureFacer> images = new ArrayList<>();
        Uri allImagesuri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN, //DATE_ADDED not correct
                MediaStore.Images.Media.SIZE};

        Cursor cursor = getContentResolver().query(allImagesuri,
                projection,
                MediaStore.Images.Media.DATA + " like ? ",
                new String[]{"%" + folderPath + "%"},
                MediaStore.Images.ImageColumns._ID + " DESC");   //DATE_TAKEN

        try {
            cursor.moveToFirst();
            do {
                pictureFacer pic = new pictureFacer();

                pic.setPictureDate(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)));

                pic.setPicturName(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)));

                pic.setPicturePath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)));

                pic.setPictureSize(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)));

                pic.setImageUri(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)));

                images.add(pic);

            } while (cursor.moveToNext());
            cursor.close();

            /*
            //Reverse order
            ArrayList<pictureFacer> reSelection = new ArrayList<>();
            for(int i = images.size() - 1; i > -1; i--){
                reSelection.add(images.get(i));
            }
            images = reSelection;
            */
        } catch (Exception e) {
            e.printStackTrace();
        }
        return images;
    }


    //Simple Click in pic Holder item
    @Override
    public void onItemClick(View view, int position) {

        //In case of 'multiselect' mode
        if (multiSelect){
            //empty 'selectedItems' array list
            selectedItems.clear();

            boolean b = checkBoxSelection[position];
            View v =    (View)view.getParent();
            ImageView pictureCheck = v.findViewById(R.id.iv_check);
            //int visibility = (pictureCheck.getVisibility() == View.VISIBLE) ? View.INVISIBLE : View.VISIBLE;

            //test if we check the same check or not.
            if(pictureCheck == previousPictureCheck) {
                if(pictureCheck.getVisibility() == View.VISIBLE){
                    pictureCheck.setVisibility(View.INVISIBLE);
                    nbChecks--;
                }else{
                    pictureCheck.setVisibility(View.VISIBLE);
                    nbChecks++;
                }
            }
            if(pictureCheck != previousPictureCheck){
                if(previousPictureCheck != null){
                    if(previousPictureCheck.getVisibility() == View.VISIBLE){
                        previousPictureCheck.setVisibility(View.INVISIBLE);
                        nbChecks--;
                    }
                }
                pictureCheck.setVisibility(View.VISIBLE);
                nbChecks++;
            }
            previousPictureCheck = pictureCheck;



            //pictureCheck.setVisibility(View.VISIBLE);

            /*
            //CheckBox cb = (CheckBox) view;
            //int id = cb.getId();
           //CheckBox checkBox =
                View v =    (View)view.getParent();
                CheckBox checkBox = v.findViewById(R.id.itemCheckBox);
                checkBox.setVisibility(View.VISIBLE);

            if (checkBoxSelection[position]) {
                checkBox.setVisibility(View.INVISIBLE);
                checkBox.setChecked(false);
                checkBoxSelection[position] = false;
                nbChecks--;
                //update the 'selectedItems' arraylist to remove this image.
                selectItem(allpictures.get(position));
            } else {
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(true);
                checkBoxSelection[position] = true;
                nbChecks++;
                //update the 'selectedItems' arraylist to add this image.
                selectItem(allpictures.get(position));
            }
            */
            //update the menu in 'ActionMode' (Top bar). Show the number of checks and icons
            checkCount.setText(getResources().getQuantityString(R.plurals.quantities, nbChecks, nbChecks));

            int okIcon     = (nbChecks > 0) ? R.drawable.check_36  : R.drawable.check_grey_36;
            int cancelIcon = (nbChecks > 0) ? R.drawable.cancel_36 : R.drawable.cancel_grey_36;

            //status icons
            int starIcon    = (nbChecks > 0) ? R.drawable.star_36   : R.drawable.star_36_greyed;
            int shareIcon   = (nbChecks > 0) ? R.drawable.share_36  : R.drawable.share_36_greyed;
            int deleteIcon  = (nbChecks > 0) ? R.drawable.delete_36 : R.drawable.delete_36_greyed;
            int copyIcon    = (nbChecks > 0) ? R.drawable.copy_36   : R.drawable.copy_36_greyed;

            okMenuItem.setIcon(okIcon);
            cancelMenuItem.setIcon(cancelIcon);

            //Status icon
            statusStar.setImageDrawable(getResources().getDrawable(starIcon));
            statusShare.setImageDrawable(getResources().getDrawable(shareIcon));
            statusCopy.setImageDrawable(getResources().getDrawable(deleteIcon));
            statusDelete.setImageDrawable(getResources().getDrawable(copyIcon));

            //Get the selected picture and store it in 'selectedItem' array list.
            selectItem(allpictures.get(position));

            return;
        }

        //here, we are not in multiselect mode, send the uri of the clicked picture to zoom.
        //Get the selected picture and store it in 'selectedItem' array list.
        selectItem(allpictures.get(position));

        //display the selected image. Open zoom intent.
        Intent intentZoomImage = new Intent(this, DisplayZoomImage.class);
        intentZoomImage.putExtra("image", allpictures.get(position));
        intentZoomImage.putExtra("position", position);
        intentZoomImage.putExtra("total", allpictures.size());

        startActivityForResult(intentZoomImage, INTENT_ZOOM_REQUEST_CODE);


        //Return back to the caller witch create this intent. We add some extras.
        //Fill 'selectedItems_' with 'imageUri' from 'selectedItems'.
        ArrayList<String> selectedItems_ = new ArrayList<>();
        /*
        //we have only one picture clicked.
        String id = selectedItems.get(0).getImageUri();
        Long id_  = Long.parseLong(id);
        Uri uri   = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id_);
        selectedItems_.add(uri.toString());

        Intent intent = new Intent();
        //File pathDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        //Uri resultUri = Uri.fromFile(pathDir); // the thing to return
        //intent.setData(resultUri);
        //intent.setData(uri);
        intent.putStringArrayListExtra("uri_selected_pictures", selectedItems_);
        setResult(Activity.RESULT_OK, intent);

        String s = "Simple click on image at position " + position + " will be deleted";
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
        */

        //Toast.makeText(this, "picture deleted", Toast.LENGTH_LONG).show();
        //finish();
    }

    private void cancelDeletingPicture() {
        Toast.makeText(this, "no picture deleted", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLongItemClick(View view, int position) {
        //Long Click in pic Holder item.
        String s = "Long click on image at position " + position;
        //Toast.makeText(this, s, Toast.LENGTH_LONG).show();
        //multiSelect = !multiSelect;

        //update once since 'actionMode' is one time.
        //update the recycler since the 'multiSelect' value has changed.

        if (actionMode != null) {
            return;
        }

        //show the status bar, default icons greyed
        llStatus.setVisibility(View.VISIBLE);

        //On recharge le 'imageRecycler' avec 'multiSelect = true'. Dans l'adapter 'picture_adapter'
        //la visibilité des 'checkBox' dépend de la valeur de 'multiSelect'.
        //We can remove the test '(actionMode == null)'.
        if (actionMode == null) {
            multiSelect = true;
            imageRecycler.setAdapter(new picture_Adapter(allpictures, this, this));
            imageRecycler.invalidate();

            //Show the 'ActionMode' bar
            actionMode = startActionMode(actionModeCallback);

            //Get 'CardView' bar or top bar and hide it.
            head = findViewById(R.id.head);
            //originalLayoutParams = head.getLayoutParams();

            //create dynamically .
            //LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
            //        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            //hide the 'card view' bar and show it when we do a simple click.
            // Then hide it when we go out (pressing the left arraow).
            head.setVisibility(View.GONE); //at startup

            //RelativeLayout.LayoutParams lparams = new RelativeLayout.LayoutParams(0, 0);
            //head.setLayoutParams(lparams);
        }
        /*
        final View actionModeView = findViewById(R.id.action_mode_bar);
        if (actionModeView != null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                actionModeView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            }
        */
    }

    //@Override
    public void onChecBoxItemClick(View view, int position) {
        /*
        //CheckBox clicked

        CheckBox cb = (CheckBox) view;
        int id = cb.getId();
        if (checkBoxSelection[position]) {
            cb.setChecked(false);
            checkBoxSelection[position] = false;
            nbChecks--;
            //update the 'selectedItems' arraylist to remove this image.
            selectItem(allpictures.get(position));
        } else {
            cb.setChecked(true);
            checkBoxSelection[position] = true;
            nbChecks++;
            //update the 'selectedItems' arraylist to add this image.
            selectItem(allpictures.get(position));
        }
        //update the menu in 'ActionMode'. Show the number of checks and icons
        //checkCount.setText(getResources().getString(R.string.picture_selected, nbChecks));

        checkCount.setText(getResources().getQuantityString(R.plurals.quantities, nbChecks, nbChecks));

        int okIcon     = (nbChecks > 0) ? R.drawable.check_36 : R.drawable.check_grey_36;
        int cancelIcon = (nbChecks > 0) ? R.drawable.cancel_36 : R.drawable.cancel_grey_36;

        //status icons
        int starIcon    = (nbChecks > 0) ? R.drawable.star_36   : R.drawable.star_36_greyed;
        int shareIcon   = (nbChecks > 0) ? R.drawable.share_36  : R.drawable.share_36_greyed;
        int deleteIcon  = (nbChecks > 0) ? R.drawable.delete_36 : R.drawable.delete_36_greyed;
        int copyIcon    = (nbChecks > 0) ? R.drawable.copy_36   : R.drawable.copy_36_greyed;

        okMenuItem.setIcon(okIcon);
        cancelMenuItem.setIcon(cancelIcon);

        //Status icon
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            statusStar.setImageDrawable(getDrawable(starIcon));
            statusShare.setImageDrawable(getDrawable(shareIcon));
            statusCopy.setImageDrawable(getDrawable(copyIcon));
            statusDelete.setImageDrawable(getDrawable(deleteIcon));
        }
        */
    }

    // helper function that adds/removes an item to the list depending on the app's state
    private void selectItem(pictureFacer image) {
        // If the "selectedItems" list contains the item, remove it and set it's state to normal
        if (selectedItems.contains(image)) {
            selectedItems.remove(image);
            //holder.itemView.setAlpha(1.0f);
        } else {
            // Else, add it to the list and add a darker shade over the image, letting the user know that it was selected
            selectedItems.add(image);
            //holder.itemView.setAlpha(0.3f);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Here, we come back from 'DisplayZoomImage' after delete one image OR when clicking 'Back'.
        if ((resultCode == Activity.RESULT_OK) &&
                (requestCode == INTENT_ZOOM_REQUEST_CODE)
                && (data != null)) {
            String mode  = data.getExtras().getString("mode");
            int position = data.getExtras().getInt("position");   //position picture deleted

            switch (mode){
                case "back" :
                    //nothing to do
                    break;
                case "delete" :
                    //remove the deleted picture from array list
                    allpictures.remove(position);

                    //update display pictures number
                    nbPictures.setText(getResources().getString(R.string.all_pictures, allpictures.size()));

                    //get all images in mediastore after deleting one image.
                    //allpictures = getAllImagesByFolder(foldePath);

                    //Update the recyclerView
                    imageRecycler.setAdapter(new picture_Adapter(allpictures, ImageDisplay.this,
                            ImageDisplay.this));
                    imageRecycler.invalidate();
                    break;
            }
        }
    }
}