import socket
import ssl
import threading
import select
from Config import BUFSIZE
import time
from pathConvertor import *
from socketSender import *


# a read thread, read data from remote
class SocketExchanger(threading.Thread):
    def __init__(self, client, master):
        threading.Thread.__init__(self)
        self.client = client
        self.master = master

    def run(self):
        pass
        req = self.client.recv(BUFSIZE)
        if req:
            self.master.sendall(req)

            while True:
                try:
                    pre_read, pre_write, err = select.select([self.master,], [self.master,], [], 5)
                except select.error:
                    self.master.shutdown(2)
                    self.master.close()
                    break
                if len(pre_read)>0:
                    recv=self.master.recv(BUFSIZE)
                    if recv==b'':
                        break
                    elif recv:
                        self.client.sendall(recv)

        self.client.close()
        self.master.close()


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
            client_1, cltadd_1 = self.sock.accept()
            s1 = self.SSLContext.wrap_socket(client_1, server_side=True)
            print('s1 conned')

            client_2, cltadd_2 = self.sock.accept()
            s2 = self.SSLContext.wrap_socket(client_2, server_side=True)
            print('s2 conned')

            SocketExchanger(s2, s1).start()


            print("accept a pair")


def startWLAN():
    lst = Listener(PORT)  # create a listen thread
    lst.start()  # then start


if __name__ == '__main__':
    startWLAN()
