package com.kingtous.remotefingerunlock.FileTransferTool;

import java.util.List;

public class FileModel {

    private String current_folder;
    private List<DetailBean> detail;

    public String getCurrent_folder() {
        return current_folder;
    }

    public void setCurrent_folder(String current_folder) {
        this.current_folder = current_folder;
    }

    public List<DetailBean> getDetail() {
        return detail;
    }

    public void setDetail(List<DetailBean> detail) {
        this.detail = detail;
    }

    public static class DetailBean {
        /**
         * file_name : sw
         * attributes : 1
         * size : 64
         */

        private String file_name;
        private int attributes;
        private int size;

        public String getFile_name() {
            return file_name;
        }

        public void setFile_name(String file_name) {
            this.file_name = file_name;
        }

        public int getAttributes() {
            return attributes;
        }

        public void setAttributes(int attributes) {
            this.attributes = attributes;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }
    }
}
