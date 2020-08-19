package com.wechantloup.upnpvideoplayer.dataholder;

import androidx.annotation.Nullable;

import java.io.File;

public class VideoElement {

    private final boolean mDirectory;
    private final String mPath;
    private String mName;
    private final VideoElement mParent;

    public VideoElement(boolean directory, String path, String name, VideoElement parent) {
        mDirectory = directory;
        mPath = path;
        mName = name;
        mParent = parent;
    }

    public VideoElement(File file, VideoElement parent){
        mDirectory = file.isDirectory();
        mPath = file.getAbsolutePath();
        mName = file.getName();
        if (!mDirectory){
            mName = mName.substring(0, mName.lastIndexOf("."));
        }
        mParent = parent;
    }

    public boolean isDirectory() {
        return mDirectory;
    }

    public String getPath() {
        return mPath;
    }

    public String getName() {
        return mName;
    }
    
    public VideoElement getParent() {
        return mParent;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof VideoElement &&
                mPath.equals(((VideoElement) obj).mPath) &&
                mName.equals(((VideoElement) obj).mName);
    }
}
