package com.bc.server.manager;

import java.io.*;
import java.util.*;

public class Request {
    private BufferedInputStream input;
    private Map<String, List<String>> headMap = new HashMap<String, List<String>>();
    private Map<String, String> argsMap = new HashMap<String, String>();
    private String protocol;
    private String requestType;
    private String requestPath;
    private String contentStr;

    public String getProtocol() {
        return this.protocol;
    }

    public String getRequestType() {
        return this.requestType;
    }

    public String getRequestPath() {
        return this.requestPath;
    }

    public String getContentStr() {
        return this.contentStr;
    }

    public Request(InputStream input) {
        this.input = new BufferedInputStream(input);
        analyze();
    }

    private void analyze() {
        try {
            int headEnd = 0;
            boolean sign = false;
            StringBuffer headBuffer = new StringBuffer();
            StringBuffer bodyBuffer = new StringBuffer();
            while (true) {
                int c = input.read();
                if (c == -1) {
                    break;
                }
                headBuffer.append((char) c);
                if (c == '\r') {
                    sign = true;
                } else if (c == '\n' && sign) {
                    headEnd++;
                    sign = false;
                } else {
                    sign = false;
                    headEnd = 0;
                }
                if (headEnd >= 2) {
                    break;
                }
            }
//            System.out.print(headBuffer.toString());
            StringReader stringReader = new StringReader(headBuffer.toString());
            BufferedReader reader = new BufferedReader(stringReader);
            String str;
            boolean firstLine = true;
            while ((str = reader.readLine()) != null) {
                String[] contents = str.split(" ");
                if (firstLine) {
                    if (str.toUpperCase().contains("HTTP")) {
                        this.requestType = contents[0];
                        formatePath(contents[1]);
                        this.protocol = contents[2];
                    }
                } else {
                    String key = contents[0].split(":")[0];
                    List<String> contentList = new ArrayList<String>();
                    for (int i = 1; i < contents.length; i++) {
                        contentList.add(contents[i]);
                    }
                    headMap.put(key, contentList);
                }
                firstLine = false;
            }
            reader.close();
            stringReader.close();
            if (getHead("content-length") != null) {
                int contentLength = Integer.parseInt(getHead("content-length").get(0));
                while (true) {
                    int c = input.read();
                    if (c == -1) {
                        break;
                    }
                    bodyBuffer.append((char) c);
                    contentLength--;
                    if (contentLength <= 0) {
                        break;
                    }
                }
            } else {

            }
//            System.out.print(bodyBuffer.toString());
//            if (getHead("content-type") != null) {
                if (getHead("content-type") != null&&getHead("content-type").get(0).indexOf("multipart/form-data") >= 0) {
                    String boundary = getHead("content-type").get(1).split("=")[1];
                    String[] bodys = bodyBuffer.toString().split("--" + boundary);
                    for (String body : bodys) {
                    }
                } else {
                    String[] contents = bodyBuffer.toString().split("&");
                    StringBuffer contentBuffer = new StringBuffer();
                    for (String content : contents) {
                        String[] entry = content.split("=");
                        if(entry.length > 1){
                            argsMap.put(entry[0], entry[1]);
                        }else {
                            contentBuffer.append(entry[0]);
                        }
                    }
                    this.contentStr = bodyBuffer.toString();
                }
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void formatePath(String requestPath) {
        String[] paths = requestPath.split("\\?");
        this.requestPath = paths[0];
        if (paths.length > 1) {
            String[] args = paths[1].split("&");
            for (String arg : args) {
                String[] entry = arg.split("=");
                argsMap.put(entry[0], entry[1]);
            }
        }
    }

    public List<String> getHead(String headArg) {
        return headMap.get(headArg);
    }

    public String getArg(String argkey) {
        return argsMap.get(argkey);
    }

    public Set<Map.Entry<String, List<String>>> getHeadEntrySet() {
        return headMap.entrySet();
    }

    public Set<Map.Entry<String, String>> getArgEntrySet() {
        return argsMap.entrySet();
    }
}
