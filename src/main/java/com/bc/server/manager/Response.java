package com.bc.server.manager;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Response {
    BufferedOutputStream out;

    private byte[] getHeader(String status, String MIMEType, int contentLength) {
        StringBuffer headerBuffer = new StringBuffer();
        headerBuffer.append("HTTP/1.0 200 OK\r\n");
        headerBuffer.append("Server: OneFile 1.0\r\n");
        headerBuffer.append("Connection: close");
        headerBuffer.append("Content-length: ").append(contentLength).append("\r\n");
        headerBuffer.append("Content-type: ").append(MIMEType).append("\r\n\r\n");
        return headerBuffer.toString().getBytes();
    }

    public Response(OutputStream outputStream) {
        out = new BufferedOutputStream(outputStream);
    }

    public void write(String status, String mimeType, String content) throws IOException {
        if (content.endsWith(".html")) {

        } else {
            out.write(getHeader(status, mimeType, content.length()));
            out.flush();
            out.write(content.getBytes());
            out.flush();
        }
    }
}
