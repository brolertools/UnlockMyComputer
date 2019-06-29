package com.kingtous.remotefingerunlock.FileTransferTool;

public class PropModel {


    /**
     * file_name : 123
     * file_size : 500
     * file_size_high : 1
     */

    private String file_name;
    private long file_size;
    private long file_size_high;

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public long getFile_size() {
        return file_size;
    }

    public void setFile_size(long file_size) {
        this.file_size = file_size;
    }

    public long getFile_size_high() {
        return file_size_high;
    }

    public void setFile_size_high(long file_size_high) {
        this.file_size_high = file_size_high;
    }
}
