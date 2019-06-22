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


class MasterHolderd(threading.Thread):
    def __init__(self, port):
        threading.Thread.__init__(self)
        self.SSLContext = None
        self.sock = None
        self.port = port
        # 初始化字典
        socket_pool.master_dict[port] = {}

    def createSocket(self, port):
        self.SSLContext = ssl.SSLContext(ssl.PROTOCOL_TLSv1_2)
        self.SSLContext.load_cert_chain(certfile='cacert.pem', keyfile='privkey.pem')
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.bind(("0.0.0.0", port))
        self.sock.listen(0)
        return self.SSLContext, self.sock, port

    def run(self):
        while True:
            self.waitConnect()

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
    def __init__(self, port, exchanger):
        threading.Thread.__init__(self)
        self.SSLContext = None
        self.sock = None
        self.master_port = port
        self.exchanger = exchanger
        socket_pool.master_dict[port] = {}

    def createSocket(self, port):
        self.SSLContext = ssl.SSLContext(ssl.PROTOCOL_TLSv1_2)
        self.SSLContext.load_cert_chain(certfile='cacert.pem', keyfile='privkey.pem')
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.bind(("0.0.0.0", port))
        self.sock.listen(0)

        return self.SSLContext, self.sock, port

    def run(self):
        while True:
            self.waitConnect()

    def waitConnect(self):
        print('D:等待连接')
        c, s, p = self.createSocket(self.master_port)
        while True:
            socket_client, addr = s.accept()
            ssl_conn = self.SSLContext.wrap_socket(socket_client, server_side=True)
            print('D:接受来自', addr, '的连接')

            mac, info = self.getDestFromClient(ssl_conn)

            query_result = socket_pool.master_dict[self.master_port].get(mac, -1)

            if query_result != -1 and query_result._connected:
                self.exchanger(ssl_conn, query_result, info)
            else:
                self.sendError(ssl_conn)

    def sendError(self, socket_client):
        try:
            err = {}
            err['status'] = '-2'
            send_data = json.JSONEncoder().encode(err)
            socket_client.sendall(send_data.encode(encoding))
        except:
            return
        finally:
            socket_client.close()

    def getDestFromClient(self, s):
        data = s.recv(BUFSIZE)
        if data:
            try:
                string = bytes.decode(data, encoding)
                info = json.loads(string)
                if info.get('oriMac', -1) != -1:
                    return info.get('oriMac'), info
                else:
                    return '', ''
            except json.decoder.JSONDecodeError:
                return '', ''
