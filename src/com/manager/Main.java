package com.manager;

import java.io.File;

import android.app.Dialog;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.StatFs;
import android.os.Environment;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;


public final class Main extends ListActivity {

    private static final String PREFS_NAME = "ManagerPrefsFile";	//user preference file name
    private static final String PREFS_HIDDEN = "hidden";
    private static final String PREFS_COLOR = "color";
    private static final String PREFS_THUMBNAIL = "thumbnail";
    private static final String PREFS_STORAGE = "sdcard space";


    private static final int D_MENU_DELETE = 0x05;			//context menu id
    private static final int D_MENU_RENAME = 0x06;			//context menu id
    private static final int D_MENU_COPY =   0x07;			//context menu id
    private static final int D_MENU_PASTE =  0x08;			//context menu id
    private static final int D_MENU_MOVE = 	 0x30;			//context menu id
    private static final int F_MENU_MOVE = 	 0x20;			//context menu id
    private static final int F_MENU_DELETE = 0x0a;			//context menu id
    private static final int F_MENU_RENAME = 0x0b;			//context menu id
    private static final int F_MENU_COPY =   0x0d;			//context menu id
    private static final int SETTING_REQ = 	 0x10;			//request code for intent

    private FileManager mFileMag;
    private EventHandler mHandler;
    private EventHandler.TableRow mTable;

    private SharedPreferences mSettings;
    private boolean mReturnIntent = false;
    private boolean mHoldingFile = false;
    private boolean mUseBackKey = true;
    private String mCopiedTarget;
    private String mSelectedListItem;				//item from context menu
    private TextView  mPathLabel, mDetailLabel, mStorageLabel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        
        /*read settings*/
        mSettings = getSharedPreferences(PREFS_NAME, 0);
        boolean hide = mSettings.getBoolean(PREFS_HIDDEN, false);
        boolean thumb = mSettings.getBoolean(PREFS_THUMBNAIL, true);
        int space = mSettings.getInt(PREFS_STORAGE, View.VISIBLE);
        int color = mSettings.getInt(PREFS_COLOR, -1);

        mFileMag = new FileManager();
        mFileMag.setShowHiddenFiles(hide);

        if (savedInstanceState != null)
            mHandler = new EventHandler(Main.this, mFileMag, savedInstanceState.getString("location"));
        else
            mHandler = new EventHandler(Main.this, mFileMag);

        mHandler.setTextColor(color);
        mHandler.setShowThumbnails(thumb);
        mTable = mHandler.new TableRow();
        
        /*sets the ListAdapter for our ListActivity and
         *gives our EventHandler class the same adapter
         */
        mHandler.setListAdapter(mTable);
        setListAdapter(mTable);
        
        /* register context menu for our list view */
        registerForContextMenu(getListView());

        mStorageLabel = (TextView)findViewById(R.id.storage_label);
        mDetailLabel = (TextView)findViewById(R.id.detail_label);
        mPathLabel = (TextView)findViewById(R.id.path_label);
        mPathLabel.setText("path: /sdcard");

        updateStorageLabel();
        mStorageLabel.setVisibility(space);

        mHandler.setUpdateLabels(mPathLabel, mDetailLabel);
        
        /* setup buttons */
        int[] img_button_id = {R.id.help_button, R.id.home_button,
                R.id.back_button};

        int[] button_id = {R.id.hidden_copy, R.id.hidden_attach,
                R.id.hidden_delete, R.id.hidden_move};

        ImageButton[] bimg = new ImageButton[img_button_id.length];
        Button[] bt = new Button[button_id.length];

        for(int i = 0; i < img_button_id.length; i++) {
            bimg[i] = (ImageButton)findViewById(img_button_id[i]);
            bimg[i].setOnClickListener(mHandler);

            if(i < 4) {
                bt[i] = (Button)findViewById(button_id[i]);
                bt[i].setOnClickListener(mHandler);
            }
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("location", mFileMag.getCurrentDir());
    }

    /*(non Java-Doc)
     * Returns the file that was selected to the intent that
     * called this activity. usually from the caller is another application.
     */
    private void returnIntentResults(File data) {
        mReturnIntent = false;

        Intent ret = new Intent();
        ret.setData(Uri.fromFile(data));
        setResult(RESULT_OK, ret);

        finish();
    }

    private void updateStorageLabel() {
        long total, aval;
        int kb = 1024;

        StatFs fs = new StatFs(Environment.
                getExternalStorageDirectory().getPath());

        total = fs.getBlockCount() * (fs.getBlockSize() / kb);
        aval = fs.getAvailableBlocks() * (fs.getBlockSize() / kb);

        mStorageLabel.setText(String.format("sdcard: Total %.2f GB " +
                        "\t\tAvailable %.2f GB",
                (double)total / (kb * kb), (double)aval / (kb * kb)));
    }

    /**
     *  To add more functionality and let the user interact with more
     *  file types, this is the function to add the ability.
     *
     *  (note): this method can be done more efficiently
     */
    @Override
    public void onListItemClick(ListView parent, View view, int position, long id) {
        final String item = mHandler.getData(position);
        File file = new File(mFileMag.getCurrentDir() + "/" + item);
        String item_ext = null;

        try {
            item_ext = item.substring(item.lastIndexOf("."), item.length());

        } catch(IndexOutOfBoundsException e) {
            item_ext = "";
        }
    	


            if (file.isDirectory()) {
                if(file.canRead()) {
                    mHandler.stopThumbnailThread();
                    mHandler.updateDirectory(mFileMag.getNextDir(item, false));
                    mPathLabel.setText(mFileMag.getCurrentDir());
		    		

                    if(!mUseBackKey)
                        mUseBackKey = true;

                } else {
                    Toast.makeText(this, "Can't read folder due to permissions",
                            Toast.LENGTH_SHORT).show();
                }
            }
	    	
	    	/*music file selected--add more audio formats*/
            else if (item_ext.equalsIgnoreCase(".mp3") ||
                    item_ext.equalsIgnoreCase(".m4a")||
                    item_ext.equalsIgnoreCase(".mp4")) {

                if(mReturnIntent) {
                    returnIntentResults(file);
                } else {
                    Intent i = new Intent();
                    i.setAction(android.content.Intent.ACTION_VIEW);
                    i.setDataAndType(Uri.fromFile(file), "audio/*");
                    startActivity(i);
                }
            }
	    	
	    	/*photo file selected*/
            else if(item_ext.equalsIgnoreCase(".jpeg") ||
                    item_ext.equalsIgnoreCase(".jpg")  ||
                    item_ext.equalsIgnoreCase(".png")  ||
                    item_ext.equalsIgnoreCase(".gif")  ||
                    item_ext.equalsIgnoreCase(".tiff")) {

                if (file.exists()) {
                    if(mReturnIntent) {
                        returnIntentResults(file);

                    } else {
                        Intent picIntent = new Intent();
                        picIntent.setAction(android.content.Intent.ACTION_VIEW);
                        picIntent.setDataAndType(Uri.fromFile(file), "image/*");
                        startActivity(picIntent);
                    }
                }
            }
	    	
	    	/*video file selected--add more video formats*/
            else if(item_ext.equalsIgnoreCase(".m4v") ||
                    item_ext.equalsIgnoreCase(".3gp") ||
                    item_ext.equalsIgnoreCase(".wmv") ||
                    item_ext.equalsIgnoreCase(".mp4") ||
                    item_ext.equalsIgnoreCase(".ogg") ||
                    item_ext.equalsIgnoreCase(".wav")) {

                if (file.exists()) {
                    if(mReturnIntent) {
                        returnIntentResults(file);

                    } else {
                        Intent movieIntent = new Intent();
                        movieIntent.setAction(android.content.Intent.ACTION_VIEW);
                        movieIntent.setDataAndType(Uri.fromFile(file), "video/*");
                        startActivity(movieIntent);
                    }
                }
            }
	    	


	    	
	    	/*pdf file selected*/
            else if(item_ext.equalsIgnoreCase(".pdf")) {

                if(file.exists()) {
                    if(mReturnIntent) {
                        returnIntentResults(file);

                    } else {
                        Intent pdfIntent = new Intent();
                        pdfIntent.setAction(android.content.Intent.ACTION_VIEW);
                        pdfIntent.setDataAndType(Uri.fromFile(file),
                                "application/pdf");

                        try {
                            startActivity(pdfIntent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(this, "Sorry, couldn't find a pdf viewer",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
	    	
	    	/*Android application file*/
            else if(item_ext.equalsIgnoreCase(".apk")){

                if(file.exists()) {
                    if(mReturnIntent) {
                        returnIntentResults(file);

                    } else {
                        Intent apkIntent = new Intent();
                        apkIntent.setAction(android.content.Intent.ACTION_VIEW);
                        apkIntent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                        startActivity(apkIntent);
                    }
                }
            }
	    	
	    	/* HTML file */
            else if(item_ext.equalsIgnoreCase(".html")) {

                if(file.exists()) {
                    if(mReturnIntent) {
                        returnIntentResults(file);

                    } else {
                        Intent htmlIntent = new Intent();
                        htmlIntent.setAction(android.content.Intent.ACTION_VIEW);
                        htmlIntent.setDataAndType(Uri.fromFile(file), "text/html");

                        try {
                            startActivity(htmlIntent);
                        } catch(ActivityNotFoundException e) {
                            Toast.makeText(this, "Sorry, couldn't find a HTML viewer",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
	    	
	    	/* text file*/
            else if(item_ext.equalsIgnoreCase(".txt")) {

                if(file.exists()) {
                    if(mReturnIntent) {
                        returnIntentResults(file);

                    } else {
                        Intent txtIntent = new Intent();
                        txtIntent.setAction(android.content.Intent.ACTION_VIEW);
                        txtIntent.setDataAndType(Uri.fromFile(file), "text/plain");

                        try {
                            startActivity(txtIntent);
                        } catch(ActivityNotFoundException e) {
                            txtIntent.setType("text/*");
                            startActivity(txtIntent);
                        }
                    }
                }
            }
	    	
	    	/* generic intent */
            else {
                if(file.exists()) {
                    if(mReturnIntent) {
                        returnIntentResults(file);

                    } else {
                        Intent generic = new Intent();
                        generic.setAction(android.content.Intent.ACTION_VIEW);
                        generic.setDataAndType(Uri.fromFile(file), "text/plain");

                        try {
                            startActivity(generic);
                        } catch(ActivityNotFoundException e) {
                            Toast.makeText(this, "Sorry, couldn't find anything " +
                                            "to open " + file.getName(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        SharedPreferences.Editor editor = mSettings.edit();
        boolean check;
        boolean thumbnail;
        int color, space;
    	
    	/* resultCode must equal RESULT_CANCELED because the only way
    	 * out of that activity is pressing the back button on the phone
    	 * this publishes a canceled result code not an ok result code
    	 */
        if(requestCode == SETTING_REQ && resultCode == RESULT_CANCELED) {
            check = data.getBooleanExtra("HIDDEN", false);
            thumbnail = data.getBooleanExtra("THUMBNAIL", true);
            color = data.getIntExtra("COLOR", -1);
            space = data.getIntExtra("SPACE", View.VISIBLE);

            editor.putBoolean(PREFS_HIDDEN, check);
            editor.putBoolean(PREFS_THUMBNAIL, thumbnail);
            editor.putInt(PREFS_COLOR, color);
            editor.putInt(PREFS_STORAGE, space);
            editor.commit();

            mFileMag.setShowHiddenFiles(check);
            mHandler.setTextColor(color);
            mHandler.setShowThumbnails(thumbnail);
            mStorageLabel.setVisibility(space);
            mHandler.updateDirectory(mFileMag.getNextDir(mFileMag.getCurrentDir(), true));
        }
    }

    /* =================================*/


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo info) {
        super.onCreateContextMenu(menu, v, info);

        AdapterContextMenuInfo _info = (AdapterContextMenuInfo)info;
        mSelectedListItem = mHandler.getData(_info.position);

    	/* is it a directory  */
        if(mFileMag.isDirectory(mSelectedListItem) ) {
            menu.setHeaderTitle("Folder operations");
            menu.add(0, D_MENU_DELETE, 0, "Delete Folder");
            menu.add(0, D_MENU_RENAME, 0, "Rename Folder");
            menu.add(0, D_MENU_COPY, 0, "Copy Folder");
            menu.add(0, D_MENU_MOVE, 0, "Move Folder");
            menu.add(0, D_MENU_PASTE, 0, "Paste into folder").setEnabled(mHoldingFile);

        /* is it a file  */
        } else if(!mFileMag.isDirectory(mSelectedListItem) ) {
            menu.setHeaderTitle("File Operations");
            menu.add(0, F_MENU_DELETE, 0, "Delete File");
            menu.add(0, F_MENU_RENAME, 0, "Rename File");
            menu.add(0, F_MENU_COPY, 0, "Copy File");
            menu.add(0, F_MENU_MOVE, 0, "Move File");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case D_MENU_DELETE:
            case F_MENU_DELETE:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Warning ");
                builder.setIcon(R.drawable.warning);
                builder.setMessage("Deleting " + mSelectedListItem +
                        " cannot be undone. Are you sure you want to delete?");
                builder.setCancelable(false);

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mHandler.deleteFile(mFileMag.getCurrentDir() + "/" + mSelectedListItem);
                    }
                });
                AlertDialog alert_d = builder.create();
                alert_d.show();
                return true;

            case D_MENU_RENAME:
                showDialog(D_MENU_RENAME);
                return true;

            case F_MENU_RENAME:
                showDialog(F_MENU_RENAME);
                return true;


            case F_MENU_MOVE:
            case D_MENU_MOVE:
            case F_MENU_COPY:
            case D_MENU_COPY:
                if(item.getItemId() == F_MENU_MOVE || item.getItemId() == D_MENU_MOVE)
                    mHandler.setDeleteAfterCopy(true);

                mHoldingFile = true;

                mCopiedTarget = mFileMag.getCurrentDir() +"/"+ mSelectedListItem;
                mDetailLabel.setText("Holding " + mSelectedListItem);
                return true;


            case D_MENU_PASTE:
                if(mHoldingFile && mCopiedTarget.length() > 1) {

                    mHandler.copyFile(mCopiedTarget, mFileMag.getCurrentDir() +"/"+ mSelectedListItem);
                    mDetailLabel.setText("");
                }

                mHoldingFile = false;
                return true;


        }
        return false;
    }
    
    /* ================Menus, options menu and context menu end here=================*/

    @Override
    protected Dialog onCreateDialog(int id) {
        final Dialog dialog = new Dialog(Main.this);

        switch(id) {

            case D_MENU_RENAME:
            case F_MENU_RENAME:
                dialog.setContentView(R.layout.input_layout);
                dialog.setTitle("Rename " + mSelectedListItem);
                dialog.setCancelable(false);

                ImageView rename_icon = (ImageView)dialog.findViewById(R.id.input_icon);
                rename_icon.setImageResource(R.drawable.rename);

                TextView rename_label = (TextView)dialog.findViewById(R.id.input_label);
                rename_label.setText(mFileMag.getCurrentDir());
                final EditText rename_input = (EditText)dialog.findViewById(R.id.input_inputText);

                Button rename_cancel = (Button)dialog.findViewById(R.id.input_cancel_b);
                Button rename_create = (Button)dialog.findViewById(R.id.input_create_b);
                rename_create.setText("Rename");

                rename_create.setOnClickListener(new OnClickListener() {
                    public void onClick (View v) {
                        if(rename_input.getText().length() < 1)
                            dialog.dismiss();

                        if(mFileMag.renameTarget(mFileMag.getCurrentDir() +"/"+ mSelectedListItem, rename_input.getText().toString()) == 0) {
                            Toast.makeText(Main.this, mSelectedListItem + " was renamed to " +rename_input.getText().toString(),
                                    Toast.LENGTH_LONG).show();
                        }else
                            Toast.makeText(Main.this, mSelectedListItem + " was not renamed", Toast.LENGTH_LONG).show();

                        dialog.dismiss();
                        String temp = mFileMag.getCurrentDir();
                        mHandler.updateDirectory(mFileMag.getNextDir(temp, true));
                    }
                });
                rename_cancel.setOnClickListener(new OnClickListener() {
                    public void onClick (View v) {	dialog.dismiss(); }
                });
                break;

        }
        return dialog;
    }

    /*
     * (non-Javadoc)
     * This will check if the user is at root directory. If so, if they press back
     * again, it will close the application. 
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyDown(int keycode, KeyEvent event) {
        String current = mFileMag.getCurrentDir();

        if(keycode == KeyEvent.KEYCODE_BACK && mUseBackKey && !current.equals("/")) {

                //stop updating thumbnail icons if its running
                mHandler.stopThumbnailThread();
                mHandler.updateDirectory(mFileMag.getPreviousDir());
                mPathLabel.setText(mFileMag.getCurrentDir());

            return true;

        } else if(keycode == KeyEvent.KEYCODE_BACK && mUseBackKey && current.equals("/")) {
            Toast.makeText(Main.this, "Press back again to quit.", Toast.LENGTH_SHORT).show();


            mUseBackKey = false;
            mPathLabel.setText(mFileMag.getCurrentDir());

            return false;

        } else if(keycode == KeyEvent.KEYCODE_BACK && !mUseBackKey && current.equals("/")) {
            finish();

            return false;
        }
        return false;
    }
}