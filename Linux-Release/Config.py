import threading
import socket
import ssl
import time
import sys


# 编码
encoding = 'utf-8'

# UDP广播端口
UDP_PORT = 8970

# 数据包BUF大小
BUFSIZE = 8192

# 文件传输
FILE_LOCAL_PORT = 2090
FILE_MASTER_PORT = 2071
FILE_CLIENT_PORT = 2072

# 解锁
UNLOCK_PORT = 2084
UNLOCK_CLIENT_PORT = 2075
UNLOCK_DEV_PORT = 2077

# 远程唤醒
WAKE_ON_LAN_CLIENT_PORT = 2073
WAKE_ON_LAN_DEV_PORT = 2074

# 文件类型定义
FILE = 0
FOLDER = 1

# 心跳接口
HEART_BEAT_CLIENT_PORT = 2076
HEART_BEAT_MASTER_PORT = 2078

# 代理服务器定义
NAT_SERVER = '123.206.34.50'


# 等待连接线程设置
class WaitConn(threading.Thread):

    def __init__(self, SSL_Context, m_socket, port):
        super().__init__()
        self.socket = m_socket
        self.result_code = -1
        self.conn = None
        self.addr = None
        self.client = None
        self.port = port
        self.SSL_Context = SSL_Context

    def run(self):
        try:
            self.conn, self.addr = self.socket.accept()
            self.client = self.SSL_Context.wrap_socket(self.conn, server_side=True)
            print('设备端口：', self.port, '已连接')
            self.result_code = 0
        except:
            self.socket.close()
            return


class Listener(threading.Thread):
    def __init__(self, client_port, master_port, SocketExchanger):
        threading.Thread.__init__(self)
        self.SSLContext = None
        self.sock = None
        self.client_port = client_port
        self.master_port = master_port
        self.SocketExchanger = SocketExchanger

    def createSocket(self, port):
        self.SSLContext = ssl.SSLContext(ssl.PROTOCOL_TLSv1_2)
        self.SSLContext.load_cert_chain(certfile='cacert.pem', keyfile='privkey.pem')
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.bind(("0.0.0.0", port))
        self.sock.listen(0)
        return self.SSLContext, self.sock, port

    def waitConnect(self):
        print('等待连接')

        c, s, p = self.createSocket(self.client_port)
        w1 = WaitConn(c, s, p)
        c, s, p = self.createSocket(self.master_port)
        w2 = WaitConn(c, s, p)

        w1.start()
        w2.start()

        w1.join()
        w2.join()

        if w1.result_code == 0 and w2.result_code == 0:
            # 同时连接且都在线
            self.SocketExchanger(w1.client, w2.client).start()
            return True
        else:
            return False

    def run(self):
        while True:
            print('等待连接')
            self.waitConnect()
            print("进行数据交换")


class Connector(threading.Thread):
    def __init__(self, port, reader):
        threading.Thread.__init__(self)
        self.port = port
        # SSL
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.ssl_sock = ssl.wrap_socket(self.socket, ca_certs="cacert.pem", cert_reqs=ssl.CERT_REQUIRED)
        self.reader = reader

    def run(self):
        print("Connector started")
        while True:
            if not self.ssl_sock._connected:
                try:
                    print('尝试连接')
                    self.ssl_sock.connect((NAT_SERVER, self.port))
                    print('连接成功')
                    tr = self.reader(self.ssl_sock)
                    tr.start()
                    tr.join()
                    print('完成对话')
                    break
                except ConnectionRefusedError or TimeoutError:
                    # 重新初始化
                    self.__init__(self.port, self.reader)
                    time.sleep(3)
                    print('3s后尝试连接NAT公网代理服务器')
                    continue

            if self.ssl_sock._closed:
                print('重新连接')
                self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                self.ssl_sock = ssl.wrap_socket(self.socket, ca_certs="cacert.pem", cert_reqs=ssl.CERT_REQUIRED)
