package com.fabriceci.fmc.impl;

import com.fabriceci.fmc.AbstractFileManager;
import com.fabriceci.fmc.error.ClientErrorMessage;
import com.fabriceci.fmc.error.FMInitializationException;
import com.fabriceci.fmc.error.FileManagerException;
import com.fabriceci.fmc.model.FileAttributes;
import com.fabriceci.fmc.model.FileData;
import com.fabriceci.fmc.model.FileType;
import com.fabriceci.fmc.util.FileManagerUtils;
import com.fabriceci.fmc.util.FileUtils;
import com.fabriceci.fmc.util.ImageUtils;
import com.fabriceci.fmc.util.StringUtils;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.List;

import static com.fabriceci.fmc.util.FileUtils.getExtension;

public class LocalFileManager extends AbstractFileManager {

    private File docRoot;

    public LocalFileManager() throws FMInitializationException {
        this(null);
    }

    public LocalFileManager(Map<String, String> options) throws FMInitializationException {
        super(options);

        String serverRoot = propertiesConfig.getProperty("serverRoot", "");
        String fileRoot = propertiesConfig.getProperty("fileRoot", "");

        if (!serverRoot.isEmpty() && serverRoot.endsWith("/")) {
            serverRoot = serverRoot.substring(0, serverRoot.length() - 1);
            propertiesConfig.setProperty("serverRoot", serverRoot);
        }
        if (!fileRoot.isEmpty() && fileRoot.endsWith("/")) {
            fileRoot = fileRoot.substring(0, fileRoot.length() - 1);
            propertiesConfig.setProperty("fileRoot", fileRoot);
        }

        docRoot = new File(serverRoot.isEmpty() ? fileRoot : serverRoot + '/' + fileRoot);

        if (docRoot.exists() && docRoot.isFile()) {
            throw new FMInitializationException("File manager root must be a directory !");
        } else if (!docRoot.exists()) {
            try {
                Files.createDirectory(docRoot.toPath());
            } catch (IOException e) {
                throw new FMInitializationException("Unable the create the doc root directory: " + docRoot.getAbsolutePath(), e);
            }
        }
    }

    @Override
    public List<FileData> actionGetFolder(String path, String type) throws FileManagerException {

        File dir = getFile(path);

        checkPath(dir);
        checkReadPermission(dir);

        if (!dir.isDirectory()) {
            throw new FileManagerException(ClientErrorMessage.DIRECTORY_NOT_EXIST, Collections.singletonList(path));
        }

        checkRestrictions(dir);

        String[] files;
        try {
            files = dir.list();
        } catch (SecurityException e) {
            throw new FileManagerException(ClientErrorMessage.UNABLE_TO_OPEN_DIRECTORY, Collections.singletonList(path));
        }

        String filePath;
        File file = null;

        List<FileData> fileDataList = new ArrayList<>();
        if (files != null) {
            for (String f : files) {

                file = new File(docRoot.getPath() + path + f);
                filePath = path + f;
                String filename = file.getName();

                if (file.isDirectory()) {
                    if (isAllowedPattern(filename, file.isDirectory())) {
                        fileDataList.add(getFileInfo(filePath + "/"));
                    }
                } else if (isAllowedPattern(filename, file.isDirectory())) {
                    if (type == null || type.equals("images") && isAllowedImageExt(getExtension(filename))) {
                        fileDataList.add(getFileInfo(filePath));
                    }
                }
            }
        }

        return fileDataList;
    }

    @Override
    public FileData actionGetFile(String path) throws FileManagerException {

        File file = new File(docRoot.getPath() + path);

        if (file.isDirectory()) {
            throw new FileManagerException(ClientErrorMessage.FORBIDDEN_ACTION_DIR);
        }

        checkPath(file);
        checkReadPermission(file);
        checkRestrictions(file);

        // check if file is readable
        if (!file.canRead()) {
            throw new FileManagerException(ClientErrorMessage.NOT_ALLOWED_SYSTEM);
        }

        return getFileInfo(path);

    }

    public FileData getFileInfo(String path) throws FileManagerException {

        FileData fileData = new FileData();
        fileData.setId(path);
        FileAttributes fileAttributes = new FileAttributes();
        // get file
        File file = getFile(path);

        if (file.isDirectory() && !path.endsWith("/")) {
            throw new FileManagerException("Error reading the file: " + file.getAbsolutePath());
        }

        BasicFileAttributes attr;
        try {
            attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        } catch (IOException e) {
            throw new FileManagerException("Error reading the file: " + file.getAbsolutePath(), e);
        }

        fileAttributes.setReadable(file.canRead() ? 1 : 0);
        fileAttributes.setWritable(file.canWrite() ? 1 : 0);

        String filename = file.getName();
        if (file.isDirectory()) {
            fileData.setType(FileType.folder);
        } else {
            fileData.setType(FileType.file);
            Dimension dim = new Dimension(0, 0);
            if (fileAttributes.isReadable()) {
                fileAttributes.setSize(file.length());
                if (isAllowedImageExt(getExtension(filename))) {
                    if (file.length() > 0) {
                        dim = ImageUtils.getImageSize(docRoot.getPath() + path);
                    }
                }
            }
            fileAttributes.setWidth(dim.getWidth());
            fileAttributes.setHeight(dim.getHeight());
        }

        fileAttributes.setName(filename);
        fileAttributes.setPath(getDynamicPath(path));
        fileAttributes.setTimestamp(attr.lastModifiedTime().toMillis());
        fileAttributes.setModified(df.format(new Date(attr.lastModifiedTime().toMillis())));
        fileAttributes.setCreated(df.format(new Date(attr.creationTime().toMillis())));

        fileData.setAttributes(fileAttributes);

        return fileData;
    }

    @Override
    public FileData actionAddFolder(String path, String name) throws FileManagerException {

        String filename = normalizeName(name);
        File parentFile = new File(docRoot.getPath() + path);
        File targetFolderFile = new File(docRoot.getPath() + path + filename);

        checkPath(parentFile);
        checkWritePermission(parentFile);
        checkRestrictions(targetFolderFile);

        if (filename.length() == 0) {
            throw new FileManagerException(ClientErrorMessage.FORBIDDEN_NAME, Collections.singletonList(name));
        }

        if (targetFolderFile.isDirectory()) {
            throw new FileManagerException(ClientErrorMessage.DIRECTORY_ALREADY_EXISTS, Collections.singletonList(path + filename));
        }
        try {
            Files.createDirectories(targetFolderFile.toPath());
        } catch (IOException e) {
            throw new FileManagerException(ClientErrorMessage.UNABLE_TO_CREATE_DIRECTORY, Collections.singletonList(path + filename));
        }

        return getFileInfo(path + filename + "/");
    }

    @Override
    public FileData actionMove(String sourcePath, String targetPath) throws FileManagerException {

        File sourceFile = getFile(sourcePath);
        String filename = sourceFile.getName();
        File targetDir = getFile(targetPath);
        File targetFile = getFile(targetPath + "/" + filename);

        String finalPath = targetPath + filename + (sourceFile.isDirectory() ? "/" : "");

        if (!targetDir.isDirectory()) {
            throw new FileManagerException(ClientErrorMessage.DIRECTORY_NOT_EXIST, Collections.singletonList(targetPath));
        }

        // check if not requesting main FM userfiles folder
        if (sourceFile.equals(docRoot)) {
            throw new FileManagerException(ClientErrorMessage.NOT_ALLOWED);
        }

        // check permissions
        checkPath(sourceFile);
        checkPath(targetDir);
        checkReadPermission(sourceFile);
        checkWritePermission(sourceFile);
        checkWritePermission(targetDir);
        checkRestrictions(sourceFile);
        checkRestrictions(targetFile);

        // check if file already exists
        if (targetFile.exists()) {
            if (targetFile.isDirectory()) {
                throw new FileManagerException(ClientErrorMessage.DIRECTORY_ALREADY_EXISTS, Collections.singletonList(targetPath));
            } else {
                throw new FileManagerException(ClientErrorMessage.FILE_ALREADY_EXISTS, Collections.singletonList(targetPath));
            }
        }

        try {

            Files.move(sourceFile.toPath(), targetFile.toPath());
            File thumbnailFile = new File(getThumbnailPath(sourcePath));
            if (thumbnailFile.exists()) {
                if (thumbnailFile.isFile()) {
                    File newThumbnailFile = new File(getThumbnailPath(targetPath + filename));
                    Files.createDirectories(newThumbnailFile.getParentFile().toPath());
                    Files.move(thumbnailFile.toPath(), newThumbnailFile.toPath());

                } else {
                    FileUtils.removeDirectory(thumbnailFile.toPath());
                }
            }

        } catch (IOException e) {
            if (sourceFile.isDirectory()) {
                throw new FileManagerException("ERROR_MOVING_DIRECTORY", Collections.singletonList(targetPath));
            } else {
                throw new FileManagerException("ERROR_MOVING_FILE", Collections.singletonList(targetPath));
            }

        }

        return getFileInfo(finalPath);
    }


    @Override
    public FileData actionRename(String sourcePath, String targetName) throws FileManagerException {
        return null;

        /*
        File sourceFile = getFile(sourcePath);
        String filename = sourceFile.getName();

        int pathPos = sourcePath.lastIndexOf("/");
        String targetDirPath = sourcePath.substring(0, pathPos + 1);

        File targetDir = new File(targetDirPath);

        String targetPath = targetDirPath + targetName;

        File targetFile = new File(targetPath);

        // forbid to change path during rename
        if(targetName.indexOf("/") != -1){
            throw new FileManagerException(ClientErrorMessage.FORBIDDEN_CHAR_SLASH);
        }

        // check if not requesting main FM userfiles folder
        if (sourceFile.equals(docRoot)) {
            throw new FileManagerException(ClientErrorMessage.NOT_ALLOWED);
        }

        // check permissions
        checkPath(sourceFile);
        checkReadPermission(sourceFile);
        checkWritePermission(sourceFile);
        checkWritePermission(targetDir);
        checkRestrictions(sourceFile);
        checkRestrictions(targetFile);





        if (fileTo.exists()) {
            if (fileTo.isDirectory()) {
                return getErrorResponse("DIRECTORY_ALREADY_EXISTS", new String[] {targetName});
            } else { // fileTo.isFile
                return getErrorResponse("FILE_ALREADY_EXISTS", new String[] {targetName});
            }
        } else if (!sourceFile.renameTo(fileTo)) {

            if (sourceFile.isDirectory()) {
                return getErrorResponse("ERROR_RENAMING_DIRECTORY", new String[] { filename, targetName});
            } else {
                return getErrorResponse("ERROR_RENAMING_FILE", new String[] {filename, targetName});
            }
        }

        File oldThumbnailFile = new File(getThumbnailPath(sourcePath));
        if (oldThumbnailFile.exists()) {
            oldThumbnailFile.renameTo(new File(getThumbnailPath(targetPath)));
        }

        if (fileTo.isDirectory()) {
            if (!targetPath.endsWith("/"))
                targetPath = targetPath + "/";
        }

        return new JSONObject().put("data", new JSONObject(getFileInfo(targetPath)));
        */
    }

    @Override
    public FileData actionDelete(String path) throws FileManagerException {

        File thumbnail = new File(getThumbnailPath(path));
        File file = new File(docRoot.getPath() + path);

        checkPath(file);
        checkWritePermission(file);
        checkRestrictions(file);

        // check if not requesting main FM userfiles folder
        if (file.equals(docRoot)) {
            throw new FileManagerException(ClientErrorMessage.NOT_ALLOWED);
        }

        // Recover the result before the operation
        FileData result = getFileInfo(path);

        if (file.isDirectory()) {
            try {
                FileUtils.removeDirectory(file.toPath());
                if (thumbnail.exists()) {
                    FileUtils.removeDirectory(thumbnail.toPath());
                }
            } catch (IOException e) {
                logger.error("Cannot remove directory : " + path);
                throw new FileManagerException(ClientErrorMessage.ERROR_SERVER);
            }
        } else {
            if (!file.delete()) {
                throw new FileManagerException(ClientErrorMessage.ERROR_SERVER);
            }
            if (thumbnail.exists()) thumbnail.delete();
        }
        return result;
    }

    @Override
    public FileData actionGetImage(HttpServletResponse response, String path, Boolean thumbnail) throws FileManagerException {
        InputStream is = null;
        File file = getFile(path);

        checkPath(file);
        checkReadPermission(file);
        checkRestrictions(file);

        if (file.isDirectory()) {
            throw new FileManagerException(ClientErrorMessage.FORBIDDEN_ACTION_DIR);
        }

        checkRestrictions(file);

        try {
            String filename = file.getName();
            String fileExt = filename.substring(filename.lastIndexOf(".") + 1);
            String mimeType = (!StringUtils.isEmpty(getExtension(fileExt))) ? FileManagerUtils.getMimeTypeByExt(fileExt) : "application/octet-stream";
            long fileSize = file.length();
            if (thumbnail) {

                if (Boolean.parseBoolean(propertiesConfig.getProperty("images.thumbnail.enabled"))) {

                    File thumbnailFile = getThumbnail(path, true);
                    if (thumbnailFile == null) throw new FileManagerException(ClientErrorMessage.ERROR_SERVER);
                    is = new FileInputStream(thumbnailFile);
                    fileSize = thumbnailFile.length();
                } else {
                    // no cache
                    BufferedImage image = ImageIO.read(file);
                    BufferedImage resizedImage = generateThumbnail(image);
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    ImageIO.write(resizedImage, fileExt, os);
                    is = new ByteArrayInputStream(os.toByteArray());
                    fileSize = os.toByteArray().length;
                }

            } else {
                is = new FileInputStream(file);
            }

            response.setContentType(mimeType);
            response.setHeader("Content-Length", Long.toString(fileSize));
            response.setHeader("Content-Transfer-Encoding", "binary");
            response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");

            FileUtils.copy(new BufferedInputStream(is), response.getOutputStream());
        } catch (IOException e) {
            logger.error("Error serving image: " + file.getName() , e);
            throw new FileManagerException(ClientErrorMessage.ERROR_SERVER);
        }
        return null;
    }


    /*
@Override
public FileData actionReplace(String path) throws FileManagerException {
    File file = getFile(path);
    File targetDirectory = new File(file.getParent());

    String targetDirectoryString = path.substring(0, path.lastIndexOf("/") + 1);

    if (!hasPermission("replace") || !hasPermission("upload")) {
        return getErrorResponse("NOT_ALLOWED");
    }

    if (file.isDirectory()) {
        return getErrorResponse("NOT_ALLOWED");
    }

    if (!isAllowedName(file.getName(), false)) {
        return getErrorResponse("INVALID_DIRECTORY_OR_FILE");
    }

    if (!targetDirectory.canWrite()) {
        return getErrorResponse("NOT_ALLOWED_SYSTEM");
    }

    JSONArray array = null;

    array = uploadFiles(request, targetDirectoryString);
    file.delete();
    File thumbnail = getThumbnail(path, false);
    if (thumbnail != null && thumbnail.exists()) {
        thumbnail.delete();
    }

    return new JSONObject().put("data", array);
}

*/


    private String getDynamicPath(String path) {
        String fileRoot = propertiesConfig.getProperty("fileRoot");
        if (fileRoot.isEmpty()) return path;

        return fileRoot + path;
    }

    private File getFile(String path) {
        return new File(docRoot.getPath() + path);
    }

    protected String getThumbnailPath(String path) throws FileManagerException {
        return getThumbnailDir().getPath() + path;
    }

    protected File getThumbnailDir() throws FileManagerException {

        final String serverRoot = propertiesConfig.getProperty("serverRoot", "");
        final String thumbnailDirPath = propertiesConfig.getProperty("images.thumbnail.dir");
        File thumbnailDirFile = new File(StringUtils.isEmpty(serverRoot) ? thumbnailDirPath : serverRoot + '/' + thumbnailDirPath);

        if (!thumbnailDirFile.exists()) {
            try {
                Files.createDirectory(thumbnailDirFile.toPath());
            } catch(IOException e){
                logger.error("Could not create the thumbnail directory: " + thumbnailDirFile.getAbsolutePath(), e);
                throw new FileManagerException(ClientErrorMessage.ERROR_SERVER);
            }
        }

        return thumbnailDirFile;
    }

    protected File getThumbnail(String path, boolean create) throws FileManagerException, IOException {

        File thumbnailFile = new File(getThumbnailPath(path));

        if (thumbnailFile.exists()) {
            return thumbnailFile;
        } else if (!create) {
            return null;
        }

        File originalFile = new File(docRoot.getPath() + path);
        String ext = FileUtils.getExtension(originalFile.getName());

        if (!originalFile.exists()) {
            throw new FileManagerException(ClientErrorMessage.FILE_DOES_NOT_EXIST, Collections.singletonList(path));
        }

        try {
            Files.createDirectories(thumbnailFile.getParentFile().toPath());

            BufferedImage source = ImageIO.read(originalFile);
            BufferedImage resizedImage = generateThumbnail(source);
            ImageIO.write(resizedImage, ext, thumbnailFile);
        } catch (IOException e) {
            logger.error("Error during thumbnail generation - ext: " + ext + " name: " + originalFile.getName(), e);
            return null;
        }

        return thumbnailFile;
    }




    /*

    @Override
    public JSONObject actionDownload(HttpServletRequest request, HttpServletResponse response) throws FileManagerException {
        String path = getPath(request, "path");

        File file = getFile(path);
        String filename = file.getName();

        if (!hasPermission("download")) {
            return getErrorResponse("NOT_ALLOWED");
        }

        if (!file.exists()) {
            return getErrorResponse("FILE_DOES_NOT_EXIST", new String[]{file.getName()});
        }

        if (!file.canRead()) {
            return getErrorResponse("NOT_ALLOWED_SYSTEM");
        }

        if (!isAllowedName(filename, file.isDirectory())) {
            return getErrorResponse("INVALID_DIRECTORY_OR_FILE");
        }

        if (file.isDirectory()) {

            // check  if permission is granted
            if (!Boolean.parseBoolean(propertiesConfig.getProperty("allowFolderDownload"))) {
                return getErrorResponse("NOT_ALLOWED");
            }

            // check if not requestion the main FM userfiles folder
            if (file.equals(docRoot)) {
                return getErrorResponse("NOT_ALLOWED");
            }
        }

        // Ajax
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return new JSONObject().put("data", new JSONObject(getFileInfo(path)));
        } else {

            try {
                response.setHeader("Content-Description", "File Transfer");
                if (file.isFile()) {
                    String fileExt = filename.substring(filename.lastIndexOf(".") + 1);
                    String mimeType = (!StringUtils.isEmpty(FileManagerUtils.mimetypes.get(fileExt))) ? FileManagerUtils.mimetypes.get(fileExt) : "application/octet-stream";
                    response.setContentLength((int) file.length());
                    response.setContentType(mimeType);
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                    response.setContentLength((int) file.length());

                    FileUtils.copy(new BufferedInputStream(new FileInputStream(file)), response.getOutputStream());
                } else {
                    String[] files = file.list();

                    if (files == null || files.length == 0) {
                        return getErrorResponse("DIRECTORY_EMPTY", new String[]{file.getName()});
                    }

                    String zipFileName = FileUtils.getBaseName(path.substring(0, path.length() - 1)) + ".zip";
                    String mimType = FileManagerUtils.mimetypes.get("zip");
                    response.setContentType(mimType);
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + "\"");
                    byte[] zipFileByteArray;
                    try {
                        zipFileByteArray = ZipUtils.zipFolder(file);
                    } catch (IOException e) {
                        throw new FMIOException("Exception during ZipFiles", e);
                    }
                    response.setContentLength(zipFileByteArray.length);

                    FileUtils.copy(new ByteArrayInputStream(zipFileByteArray), response.getOutputStream());
                }

            } catch (IOException e) {
                throw new FMIOException("Download error: " + file.getName(), e);
            }

            return null;
        }
    }

    @Override
    public JSONObject actionUpload(HttpServletRequest request) throws FileManagerException {
        String path = getPath(request, "path");
        File targetDirectory = getFile(path);
        String targetDirectoryString = path.substring(0, path.lastIndexOf("/") + 1);
        if (!hasPermission("upload")) {
            return getErrorResponse("NOT_ALLOWED");
        }

        if(!targetDirectory.exists()){
            return getErrorResponse("DIRECTORY_NOT_EXIST", new String[]{path});
        }
        if (!targetDirectory.canWrite()) {
            return getErrorResponse("NOT_ALLOWED_SYSTEM");
        }

        JSONArray array = uploadFiles(request, targetDirectoryString);

        return new JSONObject().put("data", array);

    }

    @Override
    public JSONObject actionCopy(HttpServletRequest request) throws FileManagerException {
        String sourcePath = getPath(request, "source");
        String targetPath = getPath(request, "target");

        // security check
        if (!targetPath.startsWith("/")) targetPath = "/" + targetPath;
        if (!targetPath.endsWith("/")) targetPath += "/";

        File sourceFile = getFile(sourcePath);
        String filename = sourceFile.getName();
        File targetDir = getFile(targetPath);
        File targetFile = getFile(targetPath + filename);

        String finalPath = targetPath + filename + (sourceFile.isDirectory() ? "/" : "");

        if (!hasPermission("copy")) {
            return getErrorResponse("NOT_ALLOWED");
        }
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            return getErrorResponse("DIRECTORY_NOT_EXIST", new String[]{targetPath});
        }
        // check system permission
        if (!sourceFile.canRead() && !targetDir.canWrite()) {
            return getErrorResponse("NOT_ALLOWED_SYSTEM");
        }
        // check if not requesting main FM userfiles folder
        if (sourceFile.equals(docRoot)) {
            return getErrorResponse("NOT_ALLOWED");
        }
        // check if name are not excluded
        if (!isAllowedName(targetFile.getName(), false)) {
            return getErrorResponse("INVALID_DIRECTORY_OR_FILE");
        }
        // check if file already exists
        if (targetFile.exists()) {
            if (targetFile.isDirectory()) {
                return getErrorResponse("DIRECTORY_ALREADY_EXISTS", new String[]{targetFile.getName()});
            } else {
                return getErrorResponse("FILE_ALREADY_EXISTS", new String[]{targetFile.getName()});
            }
        }

        try {
            if (sourceFile.isDirectory()) {
                FileUtils.copyDirectory(sourceFile.toPath(), targetFile.toPath());
            } else {
                Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (IOException e) {
            if (sourceFile.isDirectory()) {
                return getErrorResponse("ERROR_COPYING_DIRECTORY", new String[]{filename, targetPath});
            } else {
                return getErrorResponse("ERROR_COPYING_FILE", new String[]{filename, targetPath});
            }

        }

        return new JSONObject().put("data", new JSONObject(getFileInfo(finalPath)));
    }

    @Override
    public JSONObject actionReadFile(HttpServletRequest request, HttpServletResponse response) throws FileManagerException {

        String path = getPath(request, "path");

        File file = new File(docRoot.getPath() + path);

        if (!file.exists()) {
            return getErrorResponse("INVALID_DIRECTORY_OR_FILE");
        }

        if (file.isDirectory()) {
            return getErrorResponse("FORBIDDEN_ACTION_DIR");
        }

        if (!isAllowedName(file.getName(), false)) {
            return getErrorResponse("INVALID_DIRECTORY_OR_FILE");
        }

        // check if file is readable
        if (!file.canRead()) {
            return getErrorResponse("NOT_ALLOWED_SYSTEM");
        }

        String filename = file.getName();
        String fileExt = filename.substring(filename.lastIndexOf(".") + 1);
        String mimeType = FileManagerUtils.getMimeTypeByExt(fileExt);
        long fileSize = file.length();

        //TO DO : IMPLEMENT HTTP RANGE FOR STREAM FILE (AUDIO/VIDEO)

        response.setContentType(mimeType);
        response.setHeader("Content-Length", Long.toString(fileSize));
        response.setHeader("Content-Transfer-Encoding", "binary");
        response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");

        try {
            FileUtils.copy(new BufferedInputStream(new FileInputStream(file)), response.getOutputStream());
        } catch (IOException e) {
            throw new FMIOException("Read file error: " + path, e);
        }
        return null;
    }

    @Override
    public JSONObject actionEditFile(HttpServletRequest request) throws FileManagerException {
        String path = getPath(request, "path");
        File file = getFile(path);

        if (!file.exists()) {
            return getErrorResponse("INVALID_DIRECTORY_OR_FILE");
        }

        if (file.isDirectory()) {
            return getErrorResponse("NOT_ALLOWED");
        }

        if (!hasPermission("edit") || !isEditable(file.getName())) {
            return getErrorResponse("FORBIDDEN_ACTION_DIR");
        }

        if (!file.canWrite()) {
            return getErrorResponse("NOT_ALLOWED_SYSTEM");
        }

        if (!isAllowedName(file.getName(), false)) {
            return getErrorResponse("INVALID_DIRECTORY_OR_FILE");
        }

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        try {
            br = new BufferedReader(new FileReader(file));
            for (String line; (line = br.readLine()) != null; ) {
                sb.append(line);
                sb.append('\n');
            }
        } catch (IOException e) {
            throw new FMIOException(e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {}
            }
        }

        String fileContent = sb.toString();
        Map fileInfo = getFileInfo(path);
        Map attributes = (Map) fileInfo.get("attributes");
        attributes.put("content", fileContent);
        fileInfo.put("attributes", attributes);

        return new JSONObject().put("data", new JSONObject(fileInfo));
    }

    @Override
    public JSONObject actionSaveFile(HttpServletRequest request) throws FileManagerException {
        String path = getPath(request, "path");
        String content = request.getParameter("content");
        File file = getFile(path);

        if (!file.exists()) {
            return getErrorResponse("INVALID_DIRECTORY_OR_FILE");
        }

        if (file.isDirectory()) {
            return getErrorResponse("NOT_ALLOWED");
        }

        if (!hasPermission("edit") || !isEditable(file.getName())) {
            return getErrorResponse("FORBIDDEN_ACTION_DIR");
        }

        if (!file.canWrite()) {
            return getErrorResponse("NOT_ALLOWED_SYSTEM");
        }

        if (!isAllowedName(file.getName(), false)) {
            return getErrorResponse("INVALID_DIRECTORY_OR_FILE");
        }

        try {
            FileOutputStream oldFile = new FileOutputStream(file, false);
            oldFile.write(content.getBytes());
            oldFile.close();
        } catch (IOException e) {
            throw new FMIOException("Error writing modified file", e);
        }

        return new JSONObject().put("data", new JSONObject(getFileInfo(path)));
    }

    @Override
    public JSONObject actionSummarize() throws FileManagerException {
        JSONObject attributes = null;
        try {
            attributes = getDirSummary(getFile("/").toPath());
        } catch (IOException e) {
            throw new FMIOException("Error during the building of the summary", e);
        }
        JSONObject result = new JSONObject();
        result.put("id", "/");
        result.put("type", "summary");
        result.put("attributes", attributes);
        return new JSONObject().put("data", result);
    }

    private JSONArray uploadFiles(HttpServletRequest request, String targetDirectory) throws FileManagerException {
        JSONArray array = new JSONArray();
        try {
            for (Part uploadedFile : request.getParts()) {

                if (uploadedFile.getContentType() == null) {
                    continue;
                }

                if (uploadedFile.getSize() == 0) {
                    throw new FMUploadException("FILE_EMPTY");
                }

                String submittedFileName = uploadedFile.getSubmittedFileName();
                String filename = StringUtils.normalize(FileUtils.getBaseName(submittedFileName)) + '.' + FileUtils.getExtension(submittedFileName);

                if (!isAllowedName(filename, false)) {
                    throw new FMUnallowedException(filename);
                }
                Long uploadFileSizeLimit = 0L;
                String uploadFileSizeLimitString = propertiesConfig.getProperty("upload_fileSizeLimit");
                try {
                    uploadFileSizeLimit = Long.parseLong(uploadFileSizeLimitString);
                } catch (NumberFormatException e) {
                    throw new FMConfigException("Upload_fileSizeLimit wrong format");
                }

                if (uploadedFile.getSize() > uploadFileSizeLimit) {
                    throw new FMUploadException("upload_file_too_big");
                }

                String uploadedPath = getFile(targetDirectory).getAbsolutePath() + "/" + filename;

                Files.copy(new BufferedInputStream(uploadedFile.getInputStream()), Paths.get(uploadedPath), StandardCopyOption.REPLACE_EXISTING);
                array.put(new JSONObject(getFileInfo(targetDirectory + filename)));
            }
        } catch (IOException|ServletException e){
            throw new FMIOException("ERROR_UPLOADING_FILE", e);
        }
        return array;
    }

    private static JSONObject getDirSummary(Path path) throws IOException
    {

        final Map<String, Long> result =  new HashMap<>();
        result.put("files", 0L);
        result.put("folders", 0L);
        result.put("size", 0L);

        Files.walkFileTree(path, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException
            {
                result.put("files", (result.get("files")) + 1);
                result.put("size", (result.get("size")) + Files.size(file));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException
            {
                result.put("files", (result.get("files")) + 1);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
            {
                if (exc == null)
                {
                    result.put("folders", (result.get("folders")) + 1);
                    return FileVisitResult.CONTINUE;
                }
                else
                {
                    // directory iteration failed; propagate exception
                    throw exc;
                }
            }
        });

        return new JSONObject(result);
    }*/
}