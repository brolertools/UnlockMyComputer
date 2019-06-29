# 只适用于Linux

import socket
import ssl
import threading
import sys

sys.path.append('..')
from pathConvertor import *
from socketSender import *


# a read thread, read data from remote
class Reader(threading.Thread):
    def __init__(self, client):
        threading.Thread.__init__(self)
        self.client = client

    def run(self):
        while True:
            send_str = {}
            send_str['mac'] = getMacAddress()
            send_str = json.JSONEncoder().encode(send_str)
            try:
                self.client.sendall(send_str.encode('utf-8'))
            except BrokenPipeError:
                print('服务器断线')
            print('发送', send_str)
            try:
                data = self.client.recv(BUFSIZE)
            except ConnectionResetError:
                break
            if data:
                string = bytes.decode(data, encoding)
                if string == '':
                    print('收到包')
                    break

                act = json.loads(string)
                json_tobe_send = dict()
                json_tobe_send.clear()
                json_tobe_send['status'] = '0'
                try:
                    if act.get('action', -1) == 'Query':
                        # 文件
                        path = act['path']

                        # For Windows ,如果为根目录到话会直接返回要发送到json
                        path_t = pathAdjtor(path)

                        if type(path_t) != str:
                            if send(self.client, path_t):
                                print('发送成功')
                                print(path_t)
                            else:
                                print('发送失败')
                            break

                        if os.path.isdir(path_t):
                            json_tobe_send['current_folder'] = path
                            json_tobe_send['detail'] = []

                            if os.path.exists(path_t):
                                file_list = os.listdir(path_t)
                                for f in file_list:
                                    try:
                                        detail = dict()
                                        detail.clear()
                                        detail['file_name'] = f
                                        if os.path.isfile(os.path.join(path_t, f)):
                                            detail['attributes'] = FILE
                                        else:
                                            detail['attributes'] = FOLDER
                                        # detail['size'] = os.path.getsize(os.path.join(path, f))
                                        json_tobe_send['detail'].append(detail.copy())
                                    except FileNotFoundError:
                                        print(f, '未找到')
                            if send(self.client, json_tobe_send):
                                print('发送成功')
                                print(json_tobe_send)

                    elif act.get('action', -1) == 'Get':
                        path = act['path']
                        path_t = pathAdjtor(path)
                        if os.path.exists(path_t):
                            print('传输', path_t)
                            with open(path_t, 'rb') as f:
                                for data in f:
                                    try:
                                        self.client.sendall(data)
                                    except BrokenPipeError:
                                        print('服务器断线')
                                        break
                    elif act.get('action', -1) == 'Prop':
                        path = act['path']
                        path_t = pathAdjtor(path)
                        json_tobe_send['file_name'] = os.path.split(path_t)[1]  # 1为文件名
                        json_tobe_send['file_size'] = os.path.getsize(path_t)
                        json_tobe_send['status'] = '0'
                        send_str = json.JSONEncoder().encode(json_tobe_send)
                        if self.client.sendall(send_str.encode('utf-8')) is None:
                            print('发送成功')
                            print(send_str)
                    break

                except PermissionError:
                    print('权限错误')
                    json_tobe_send['status'] = '-1'
                    send_str = json.JSONEncoder().encode(json_tobe_send)
                    self.client.sendall(send_str.encode('utf-8'))
                    break
                    # 3875

        print('关闭会话')
        self.client.close()


class Connector(threading.Thread):
    def __init__(self, port):
        threading.Thread.__init__(self)
        self.port = port
        # SSL
        self.sock = None
        self.ssl_sock = None
        self.init()

    def init(self, after_idle_sec=1, interval_sec=3, max_fails=5):
        # 当闲置1s时，3s一次发送keepalive ping，失败5次则断开连接
        # 只适用于Linux
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
        self.sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPIDLE, after_idle_sec)
        self.sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPINTVL, interval_sec)
        self.sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPCNT, max_fails)
        self.ssl_sock = ssl.wrap_socket(self.sock, ca_certs="cacert.pem", cert_reqs=ssl.CERT_REQUIRED)

    def run(self):
        print("Connector started")
        while True:
            if not self.ssl_sock._connected:
                try:
                    self.ssl_sock.connect((NAT_SERVER, self.port))
                    Reader(self.ssl_sock).start()
                    print('连接成功')
                except ConnectionError:
                    print('连接失败，3s后重试')
                    self.init()

            if self.ssl_sock._closed:
                print('重新连接')
                self.init()


def startWLAN():
    lst = Connector(FILE_MASTER_PORT)  # create a listen thread
    lst.start()  # then start


if __name__ == '__main__':
    startWLAN()
