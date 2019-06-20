import socket
import ssl
import threading

from pathConvertor import *
import sys
sys.path.append('..')

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


if __name__ == '__main__':
    startUnlockEx()
