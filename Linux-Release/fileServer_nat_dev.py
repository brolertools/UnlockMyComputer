import socket
import ssl
import threading

from pathConvertor import *
from socketSender import *


# a read thread, read data from remote
class Reader(threading.Thread):
    def __init__(self, client):
        threading.Thread.__init__(self)
        self.client = client

    def run(self):
        while True:
            data = self.client.recv(BUFSIZE)
            if data:
                string = bytes.decode(data, encoding)

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
                                    self.client.sendall(data)
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

    def readline(self):
        rec = self.inputs.readline()
        if rec:
            string = bytes.decode(rec, encoding)
            if len(string) > 2:
                string = string[0:-2]
            else:
                string = ' '
        else:
            string = False
        return string


# a listen thread, listen remote connect
# when a remote machine request to connect, it will create a read thread to handle
class Connector(threading.Thread):
    def __init__(self, port):
        threading.Thread.__init__(self)
        # SSL
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.ssl_sock = ssl.wrap_socket(self.socket, ca_certs="cacert.pem", cert_reqs=ssl.CERT_REQUIRED)
        Reader(self.ssl_sock).start()

    def run(self):
        print("Connector started")
        while True:
            if not self.ssl_sock._connected:
                self.ssl_sock.connect(('192.168.1.105', 2090))
                Reader(self.ssl_sock).start()
            if self.ssl_sock._closed:
                self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                self.ssl_sock = ssl.wrap_socket(self.socket, ca_certs="cacert.pem", cert_reqs=ssl.CERT_REQUIRED)


def startWLAN():
    lst = Connector(PORT)  # create a listen thread
    lst.start()  # then start


if __name__ == '__main__':
    startWLAN()
