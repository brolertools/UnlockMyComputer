package com.kingtous.remotefingerunlock.FileTransferTool;

import java.net.Socket;

public class SocketHolder {

    private static Socket socket;

    public static synchronized Socket getSocket(){
        return socket;
    }

    public static synchronized void setSocket(Socket socket){
        SocketHolder.socket=socket;
    }

}
