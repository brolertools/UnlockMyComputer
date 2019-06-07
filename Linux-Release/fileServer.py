import threading
import socket
import ssl
from common import *

encoding = 'utf-8'
BUFSIZE = 1024

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

                if act.get('Query', -1) != -1:
                    json_tobe_send = dict()
                    # 文件
                    path = act['Query']

                    json_tobe_send[path] = path

                    if os.path.exists(path):
                        file_list = os.listdir(path)
                        for f in file_list:
                            if os.path.isfile(os.path.join(path, f)):
                                json_tobe_send[f] = FILE
                            else:
                                json_tobe_send[f] = FOLDER

                    send_str = json.JSONEncoder().encode(json_tobe_send)
                    # try:
                    if self.client.sendall(send_str.encode('utf-8')) is None:
                        print('发送成功')
                        break


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
    lst = Listener(2084)  # create a listen thread
    lst.start()  # then start


if __name__ == '__main__':
    startWLAN()
