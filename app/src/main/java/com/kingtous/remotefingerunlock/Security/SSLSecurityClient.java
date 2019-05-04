package com.kingtous.remotefingerunlock.Security;

import android.content.Context;
import android.content.res.AssetManager;

import com.kingtous.remotefingerunlock.Common.ToastMessageTool;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/*
准备双向连接
*/

public class SSLSecurityClient {

    public static Socket CreateSocket(Context context, String Address, int port) throws IOException {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("SSL");
            MyX509TrustManager trustManager = new MyX509TrustManager();
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            return sslContext.getSocketFactory().createSocket(Address, port);
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

}
