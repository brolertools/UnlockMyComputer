import socket
import ssl
import threading

from pathConvertor import *


# a read thread, read data from remote
class UnlockSocketExchanger(threading.Thread):
    def __init__(self, client, master):
        threading.Thread.__init__(self)
        self.client = client
        self.master = master

    def run(self):
        req = self.client.recv(BUFSIZE)
        if req:
            self.master.sendall(req)
        self.client.close()
        self.master.close()


# a listen thread, listen remote connect
# when a remote machine request to connect, it will create a read thread to handle
class UNLOCK_Listener(threading.Thread):
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
        print("listener started on", self.port)
        while True:
            client_1, cltadd_1 = self.sock.accept()
            s1 = self.SSLContext.wrap_socket(client_1, server_side=True)
            print(self.port,'s1 conned')

            client_2, cltadd_2 = self.sock.accept()
            s2 = self.SSLContext.wrap_socket(client_2, server_side=True)
            print(self.port,'s2 conned')

            UnlockSocketExchanger(s2, s1).start()

            print("accept a pair")


def startUnlockEx():
    lst = UNLOCK_Listener(UNLOCK_PORT)  # create a listen thread
    lst.start()  # then start


if __name__ == '__main__':
    startUnlockEx()
