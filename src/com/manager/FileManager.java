package com.manager;

import java.util.ArrayList;
import java.util.Stack;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.util.Log;

/**
 * This class is completely modular, which is to say that it has
 * no reference to the any GUI activity. This class could be taken
 * and placed into in other java (not just Android) project and work.
 * <br>
 * <br>
 * This class handles all file and folder operations on the system.
 * This class dictates how files and folders are copied/pasted, (un)zipped
 * renamed and searched. The EventHandler class will generally call these
 * methods and have them performed in a background thread. Threading is not
 * done in this class.  
 *
 *
 *
 */
public class FileManager {
    private static final int BUFFER = 		2048;
    private boolean mShowHiddenFiles = false;
    private Stack<String> mPathStack;
    private ArrayList<String> mDirContent;

    /**
     * Constructs an object of the class
     * <br>
     * this class uses a stack to handle the navigation of directories.
     */
    public FileManager() {
        mDirContent = new ArrayList<String>();
        mPathStack = new Stack<String>();

        mPathStack.push("/");
        mPathStack.push(mPathStack.peek() + "sdcard");
    }

    /**
     * This will return a string of the current directory path
     * @return the current directory
     */
    public String getCurrentDir() {
        return mPathStack.peek();
    }

    /**
     * This will return a string of the current home path.
     * @return	the home directory
     */
    public ArrayList<String> setHomeDir(String name) {
        //This will eventually be placed as a settings item
        mPathStack.clear();
        mPathStack.push("/");
        mPathStack.push(name);

        return populate_list();
    }

    /**
     * This will determine if hidden files and folders will be visible to the
     * user.
     * @param choice	true if user is veiwing hidden files, false otherwise
     */
    public void setShowHiddenFiles(boolean choice) {
        mShowHiddenFiles = choice;
    }

    /**
     *
     * @param type
     */

    /**
     * This will return a string that represents the path of the previous path
     * @return	returns the previous path
     */
    public ArrayList<String> getPreviousDir() {
        int size = mPathStack.size();

        if (size >= 2)
            mPathStack.pop();

        else if(size == 0)
            mPathStack.push("/");

        return populate_list();
    }

    /**
     *
     * @param path
     * @param isFullPath
     * @return
     */
    public ArrayList<String> getNextDir(String path, boolean isFullPath) {
        int size = mPathStack.size();

        if(!path.equals(mPathStack.peek()) && !isFullPath) {
            if(size == 1)
                mPathStack.push("/" + path);
            else
                mPathStack.push(mPathStack.peek() + "/" + path);
        }

        else if(!path.equals(mPathStack.peek()) && isFullPath) {
            mPathStack.push(path);
        }

        return populate_list();
    }

    /**
     *
     * @param old		the file to be copied
     * @param newDir	the directory to move the file to
     * @return
     */
    public int copyToDirectory(String old, String newDir) {
        File old_file = new File(old);
        File temp_dir = new File(newDir);
        byte[] data = new byte[BUFFER];
        int read = 0;

        if(old_file.isFile() && temp_dir.isDirectory() && temp_dir.canWrite()){
            String file_name = old.substring(old.lastIndexOf("/"), old.length());
            File cp_file = new File(newDir + file_name);

            try {
                BufferedOutputStream o_stream = new BufferedOutputStream(
                        new FileOutputStream(cp_file));
                BufferedInputStream i_stream = new BufferedInputStream(
                        new FileInputStream(old_file));

                while((read = i_stream.read(data, 0, BUFFER)) != -1)
                    o_stream.write(data, 0, read);

                o_stream.flush();
                i_stream.close();
                o_stream.close();

            } catch (FileNotFoundException e) {
                Log.e("FileNotFoundException", e.getMessage());
                return -1;

            } catch (IOException e) {
                Log.e("IOException", e.getMessage());
                return -1;
            }

        }else if(old_file.isDirectory() && temp_dir.isDirectory() && temp_dir.canWrite()) {
            String files[] = old_file.list();
            String dir = newDir + old.substring(old.lastIndexOf("/"), old.length());
            int len = files.length;

            if(!new File(dir).mkdir())
                return -1;

            for(int i = 0; i < len; i++)
                copyToDirectory(old + "/" + files[i], dir);

        } else if(!temp_dir.canWrite())
            return -1;

        return 0;
    }


    /**
     *
     * @param path
     */

    /**
     *
     * @param filePath
     * @param newName
     * @return
     */
    public int renameTarget(String filePath, String newName) {
        File src = new File(filePath);
        String ext = "";
        File dest;

        if(src.isFile())
			/*get file extension*/
            ext = filePath.substring(filePath.lastIndexOf("."), filePath.length());

        if(newName.length() < 1)
            return -1;

        String temp = filePath.substring(0, filePath.lastIndexOf("/"));

        dest = new File(temp + "/" + newName + ext);
        if(src.renameTo(dest))
            return 0;
        else
            return -1;
    }

    /**
     *
     * @param path
     * @param name
     * @return
     */

    /**
     * The full path name of the file to delete.
     *
     * @param path name
     * @return
     */
    public int deleteTarget(String path) {
        File target = new File(path);

        if(target.exists() && target.isFile() && target.canWrite()) {
            target.delete();
            return 0;
        }

        else if(target.exists() && target.isDirectory() && target.canRead()) {
            String[] file_list = target.list();

            if(file_list != null && file_list.length == 0) {
                target.delete();
                return 0;

            } else if(file_list != null && file_list.length > 0) {

                for(int i = 0; i < file_list.length; i++) {
                    File temp_f = new File(target.getAbsolutePath() + "/" + file_list[i]);

                    if(temp_f.isDirectory())
                        deleteTarget(temp_f.getAbsolutePath());
                    else if(temp_f.isFile())
                        temp_f.delete();
                }
            }
            if(target.exists())
                if(target.delete())
                    return 0;
        }
        return -1;
    }

    /**
     *
     * @param name
     * @return
     */
    public boolean isDirectory(String name) {
        return new File(mPathStack.peek() + "/" + name).isDirectory();
    }





    /* (non-Javadoc)
     * this function will take the string from the top of the directory stack
     * and list all files/folders that are in it and return that list so
     * it can be displayed. Since this function is called every time we need
     * to update the the list of files to be shown to the user, this is where
     * we do our sorting (by type, alphabetical, etc).
     *
     * @return
     */
    private ArrayList<String> populate_list() {

        if(!mDirContent.isEmpty())
            mDirContent.clear();

        File file = new File(mPathStack.peek());

        if(file.exists() && file.canRead()) {
            String[] list = file.list();
            int len = list.length;
			
			/* add files/folder to arraylist depending on hidden status */
            for (int i = 0; i < len; i++) {
                if(!mShowHiddenFiles) {
                    if(list[i].toString().charAt(0) != '.')
                        mDirContent.add(list[i]);

                } else {
                    mDirContent.add(list[i]);
                }
            }


        } else {
            mDirContent.add("Emtpy");
        }

        return mDirContent;
    }





}