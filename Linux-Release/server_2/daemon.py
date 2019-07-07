import threading, socket, ssl, json, sys, select, time, socket_pool

sys.path.append('..')
from pathConvertor import *


class reportd(threading.Thread):
    def __init__(self, port):
        super().__init__()
        self.port = port

    def run(self):
        while True:
            print(self.port, socket_pool.master_dict[self.port])
            time.sleep(3)


def sendError(socket_client):
    try:
        err = {}
        err['status'] = '-2'
        send_data = json.JSONEncoder().encode(err)
        socket_client.sendall(send_data.encode(encoding))
    except:
        return
    finally:
        socket_client.close()


class MasterHolderd(threading.Thread):
    def __init__(self, port):
        threading.Thread.__init__(self)
        self.SSLContext = None
        self.sock = None
        self.port = port
        # 初始化字典
        socket_pool.master_dict[port] = {}

    def createSocket(self, port, after_idle_sec=1, interval_sec=3, max_fails=5):
        self.SSLContext = ssl.SSLContext(ssl.PROTOCOL_TLSv1_2)
        self.SSLContext.load_cert_chain(certfile='cacert.pem', keyfile='privkey.pem')
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
        self.sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPIDLE, after_idle_sec)
        self.sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPINTVL, interval_sec)
        self.sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPCNT, max_fails)
        self.sock.bind(("0.0.0.0", port))
        self.sock.listen(0)
        return self.SSLContext, self.sock, port

    def run(self):
        while True:
        	try:
            	self.waitConnect()
            except Exception:
        		print(self.port,"重置")
        		time.sleep(1)

    def waitConnect(self):
        print('M:等待连接')
        c, s, p = self.createSocket(self.port)
        while True:
            socket_a, addr = s.accept()
            print('M:接受来自', addr, '的连接')
            ssl_conn = self.SSLContext.wrap_socket(socket_a, server_side=True)
            socket_pool.master_dict[self.port][self.getInfoFromSocket(ssl_conn)] = ssl_conn
            pass

    def getInfoFromSocket(self, s):
        data = s.recv(BUFSIZE)
        if data:
            try:
                string_ = bytes.decode(data, encoding)
                info = json.loads(string_)
                if info.get('mac', -1) != -1:
                    return info.get('mac')
                else:
                    return ''
            except json.decoder.JSONDecodeError:
                return ''
            except UnicodeDecodeError:
                return ''


class ClientHolderd(threading.Thread):
    def __init__(self, master_port, client_port, exchanger):
        threading.Thread.__init__(self)
        self.SSLContext = None
        self.sock = None
        self.master_port = master_port
        self.client_port = client_port
        self.exchanger = exchanger
        socket_pool.master_dict[client_port] = {}

    def createSocket(self, port, after_idle_sec=1, interval_sec=3, max_fails=5):
        self.SSLContext = ssl.SSLContext(ssl.PROTOCOL_TLSv1_2)
        self.SSLContext.load_cert_chain(certfile='cacert.pem', keyfile='privkey.pem')
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
        self.sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPIDLE, after_idle_sec)
        self.sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPINTVL, interval_sec)
        self.sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPCNT, max_fails)
        self.sock.bind(("0.0.0.0", port))
        self.sock.listen(0)

        return self.SSLContext, self.sock, port

    def run(self):
        while True:
        	try:
        		self.waitConnect()
        	except Exception:
        		print(self.client_port,"重置")
        		time.sleep(1)

    def waitConnect(self):
        print('D:等待连接')
        c, s, p = self.createSocket(self.client_port)
        while True:
            socket_client, addr = s.accept()
            try:
                ssl_conn = self.SSLContext.wrap_socket(socket_client, server_side=True)
                print('D:接受来自', addr, '的连接')
            except ssl.SSLEOFError:
                # 可能中途断开了
                print(addr, '中途断开')
                continue
            mac, data = self.getDestFromClient(ssl_conn)

            query_result = socket_pool.master_dict[self.master_port].get(mac, -1)

            if query_result != -1 and query_result._connected and not query_result._closed:
                # 判断 master 是否在线
                print('双方在线，进入数据传输阶段')
                self.exchanger(ssl_conn, query_result, data).start()
            else:
                sendError(ssl_conn)

    def getDestFromClient(self, s):
        data = s.recv(BUFSIZE)
        if data:
            try:
                string = bytes.decode(data, encoding)
                info = json.loads(string)
                if info.get('oriMac', -1) != -1:
                    return info.get('oriMac'), data
                else:
                    return '', ''
            except json.decoder.JSONDecodeError:
                return '', ''
