import threading
import socket
import ssl
from common import *

encoding = 'utf-8'
BUFSIZE = 1024
PORT=2090

FILE = 0
FOLDER = 1


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
                try:
                    if act.get('action', -1) == 'Query':
                        # 文件
                        path = act['path']
                        if os.path.isdir(path):
                            json_tobe_send['current_folder'] = path
                            json_tobe_send['detail'] = []

                            if os.path.exists(path):
                                file_list = os.listdir(path)
                                for f in file_list:
                                    try:
                                        detail = dict()
                                        detail.clear()
                                        detail['file_name'] = f
                                        if os.path.isfile(os.path.join(path, f)):
                                            detail['attributes'] = FILE
                                        else:
                                            detail['attributes'] = FOLDER
                                        # detail['size'] = os.path.getsize(os.path.join(path, f))
                                        json_tobe_send['detail'].append(detail.copy())
                                    except FileNotFoundError:
                                        print(f, '未找到')
                            json_tobe_send['status'] = '0'
                            send_str = json.JSONEncoder().encode(json_tobe_send)
                            if self.client.sendall(send_str.encode('utf-8')) is None:
                                print('发送成功')
                                print(send_str)
                    elif act.get('action', -1) == 'Get':
                        path = act['path']
                        if os.path.exists(path):
                            print('传输', path)
                            with open(path, 'rb') as f:
                                for data in f:
                                    self.client.sendall(data)
                    elif act.get('action', -1) == 'Prop':
                        path = act['path']
                        json_tobe_send['file_name'] = os.path.split(path)[1]  # 1为文件名
                        json_tobe_send['file_size'] = os.path.getsize(path)
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
class Listener(threading.Thread):
    def __init__(self, port):
        threading.Thread.__init__(self)
        # SSL
        self.SSLContext = ssl.SSLContext(ssl.PROTOCOL_SSLv23)
        self.SSLContext.load_cert_chain(certfile='cacert.pem', keyfile='privkey.pem')
        self.port = port
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.bind(("0.0.0.0", port))
        self.sock.listen(0)

    def run(self):
        print("listener started")
        while True:
            client, cltadd = self.sock.accept()
            ssl_conn = self.SSLContext.wrap_socket(client, server_side=True)
            Reader(ssl_conn).start()
            cltadd = cltadd
            print("accept a connect")


def startWLAN():
    lst = Listener(PORT)  # create a listen thread
    lst.start()  # then start


if __name__ == '__main__':
    startWLAN()
