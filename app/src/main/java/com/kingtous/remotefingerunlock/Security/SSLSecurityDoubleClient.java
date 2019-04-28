package com.kingtous.remotefingerunlock.Security;

import android.content.Context;
import android.content.res.AssetManager;
import com.kingtous.remotefingerunlock.Common.ToastMessageTool;
import com.kingtous.remotefingerunlock.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
/**
 * Author：Kingtous
 * Date：2019/4/27
 */

/*
准备双向连接
*/

public class SSLSecurityDoubleClient {
    public static Socket CreateSocket(Context context, String Address, int port) {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            KeyStore store = KeyStore.getInstance("BKS");
            store.load(context.getResources().openRawResource(R.raw.remoteunlock), "jintao123".toCharArray());
            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(store);
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            return sslContext.getSocketFactory().createSocket(Address, port);

        } catch (NoSuchAlgorithmException e) {
            ToastMessageTool.ttl(context, e.getMessage());
        } catch (KeyStoreException e) {
            ToastMessageTool.ttl(context, e.getMessage());
        } catch (IOException e) {
            ToastMessageTool.ttl(context, e.getMessage());
        } catch (CertificateException e) {
            ToastMessageTool.ttl(context, e.getMessage());
        } catch (KeyManagementException e) {
            ToastMessageTool.ttl(context, e.getMessage());
        }
        return null;
    }

}
