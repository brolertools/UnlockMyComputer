import threading
import socket
import ssl
import time

# 编码
encoding = 'utf-8'

# 数据包BUF大小
BUFSIZE = 8192

# 文件传输
FILE_LOCAL_PORT = 2090
FILE_CLIENT_PORT = 2071
FILE_MASTER_PORT = 2072

# 解锁
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

    def __init__(self, m_socket, string):
        super().__init__()
        self.socket = m_socket
        self.result_code = -1
        self.conn = None
        self.addr = None
        self.string = string

    def run(self):
        try:
            self.conn, self.addr = self.socket.accept()
            print('设备', self.string, '已连接')
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
        self.SSLContext = ssl.SSLContext(ssl.PROTOCOL_SSLv23)
        self.SSLContext.load_cert_chain(certfile='cacert.pem', keyfile='privkey.pem')
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.bind(("0.0.0.0", port))
        self.sock.listen(0)
        return self.sock

    def waitConnect(self):
        print('等待连接')
        w1 = WaitConn(self.createSocket(self.client_port), '1')
        w2 = WaitConn(self.createSocket(self.master_port), '2')

        w1.start()
        w2.start()

        w1.join()
        w2.join()

        if w1.result_code == 0 and w2.result_code == 0:
            # 同时连接且都在线
            self.SocketExchanger(w1, w2).start()
            return True
        else:
            return False

    def run(self):
        while True:
            print('等待连接')
            self.waitConnect()
            print("accept a pair")
            time.sleep(3)
