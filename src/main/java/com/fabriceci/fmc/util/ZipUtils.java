package com.fabriceci.fmc.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private static final int BUFFER_SIZE = 4096;

    /* unzip

      public static void main(String[] args) throws IOException {
        String fileZip = "compressed.zip";
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while(zipEntry != null){
            String fileName = zipEntry.getName();
            File newFile = new File("unzipTest/" + fileName);
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }
     */

    static public byte[] zipFolder(File dir) throws IOException {

        ZipOutputStream zout = null;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        try {
            zout = new ZipOutputStream(bout);
            // addFolderToZip("", dir, zout);
            zipFile(dir, dir.getName(), zout);
            zout.close();
            return bout.toByteArray();
        } finally {
            if(zout != null){
                zout.flush();
                zout.close();
            }
            if(bout != null){
                bout.close();
            }
        }
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

    /*
    static private void addFileToZip(String path, String srcFile, ZipOutputStream zout) throws IOException {

        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, folder, zout);
        } else {
            byte[] buf = new byte[BUFFER_SIZE];
            int len;
            FileInputStream in = null;
            try {
                in = new FileInputStream(srcFile);
                zout.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
                while ((len = in.read(buf)) > 0) {
                    zout.write(buf, 0, len);
                }
            } finally {
                if(zout != null) zout.closeEntry();
                if(in != null) in.close();
            }

        }
    }

    static private void addFolderToZip(String path, File folder, ZipOutputStream zout) throws IOException {
        if( folder != null && folder.list() != null) {
            for (String fileName : folder.list()) {
                if (path.equals("")) {
                    addFileToZip(folder.getName(), folder.getPath() + "/" + fileName, zout);
                } else {
                    addFileToZip(path + "/" + folder.getName(), folder.getPath() + "/" + fileName, zout);
                }
            }
        }
    }
    */
}
