package com.corposense

import groovy.transform.CompileStatic
import ratpack.server.BaseDir

import java.nio.file.Path

@CompileStatic
class Constants {

    public final static String uploadDir = 'uploads'
    public final static String publicDir = 'public'
    public final static String downloadsDir = 'downloads'

    public static final Path baseUploadPath = BaseDir.find("${publicDir}/${uploadDir}")
    public static final Path baseDownloadPath = BaseDir.find("${publicDir}/${downloadsDir}")

    static Path getUploadPath(){
        baseUploadPath.resolve(uploadDir)
    }

    static Path getDownloadPath(){
        baseDownloadPath.resolve(downloadsDir)
    }

}
