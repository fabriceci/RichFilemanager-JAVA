package com.fabriceci.fmc;

import com.fabriceci.fmc.error.FileManagerException;
import com.fabriceci.fmc.model.FileData;
import com.fabriceci.fmc.model.InitiateData;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface IFileManager {

    void handleRequest(HttpServletRequest request, HttpServletResponse response);

    // GET

    InitiateData actionInitiate() throws FileManagerException;

    FileData actionGetFile(String path) throws FileManagerException;

    List<FileData> actionGetFolder(String path, String type) throws FileManagerException;

    FileData actionMove(String sourcePath, String targetPath) throws FileManagerException;

    FileData actionDelete(String path) throws FileManagerException;

    FileData actionAddFolder(String path, String name) throws FileManagerException;

    FileData actionGetImage(HttpServletResponse response, String path, Boolean thumbnail) throws FileManagerException;

    // TO DO :

    FileData actionRename(String sourcePath, String targetPath) throws FileManagerException;

    FileData actionDownload(HttpServletResponse response, String path) throws FileManagerException;

    FileData actionEditFile(String path) throws FileManagerException;

    FileData actionReadFile(String path) throws FileManagerException;

    FileData actionSummarize() throws FileManagerException;

    // POST

    FileData actionUpload(String path) throws FileManagerException;

    FileData actionSaveFile(String pathParam, String contentParam) throws FileManagerException;

    FileData actionExtract(String sourcePath, String targetPath) throws FileManagerException;
}
