package com.fabriceci;

import javax.servlet.ServletOutputStream;
// import javax.servlet.WriteListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class StubServletOutputStream extends ServletOutputStream {

    private FileOutputStream outputStream;

    public StubServletOutputStream(File file){
        try {
            outputStream = new FileOutputStream(file);
        } catch(IOException ignore){}
    }

    public void write(int i) throws IOException {
        outputStream.write(i);
    }
}
