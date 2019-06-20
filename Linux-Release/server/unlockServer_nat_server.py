import socket
import ssl
import threading
import sys

sys.path.append('..')
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


def startUnlockEx():
    lst = Listener(UNLOCK_CLIENT_PORT, UNLOCK_DEV_PORT, UnlockSocketExchanger)  # create a listen thread
    lst.start()  # then start


class unlock_tr(threading.Thread):
    def __init__(self):
        super().__init__()

    def run(self):
        startUnlockEx()


if __name__ == '__main__':
    startUnlockEx()
