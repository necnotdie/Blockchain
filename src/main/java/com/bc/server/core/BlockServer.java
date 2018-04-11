package com.bc.server.core;

import com.bc.server.content.Summer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Administrator on 2018/3/6.
 */
public class BlockServer extends Thread {
    private int port = 80;
    private ExecutorService executorService;

    public BlockServer(int port) {
        if (port < 1 || port > 65535) {
            port = 80;
        }
        this.port = port;
        executorService = Executors.newCachedThreadPool();
        Summer.newinstance();
    }


    @Override
    public void run() {
        try {
            ServerSocket server = new ServerSocket(this.port);
            while (true) {
                Socket connection = server.accept();
                BlockThread blockThread = new BlockThread(connection);
                executorService.execute(blockThread);
            }
        } catch (IOException e) {
            System.err.println("无法启动服务器，端口" + this.port + "被占用！");
        }
    }

    public static void main(String[] args) {
        ExecutorService singleService = Executors.newSingleThreadExecutor();
        BlockServer blockServer = new BlockServer(5000);
        singleService.execute(blockServer);
    }
}
