import daemon
import threading
import sys

sys.path.append('..')
from pathConvertor import *


# a read thread, read data from remote
class UnlockSocketExchanger(threading.Thread):
    def __init__(self, client, master, info=None):
        threading.Thread.__init__(self)
        self.client = client
        self.master = master
        self.info = info

    def run(self):
        req = self.client.recv(BUFSIZE)
        if req:
            self.master.sendall(req)
        self.client.close()
        self.master.close()


def startUnlockEx():
    # 无限监听，并accept
    lst = daemon.MasterHolderd(UNLOCK_DEV_PORT)  # create a listen thread
    lst.start()  # then start

    # 接收客户端连接请求并与服务端对接
    rcv = daemon.ClientHolderd(UNLOCK_CLIENT_PORT, UnlockSocketExchanger)
    rcv.start()

    # 实时Client连接
    d = daemon.reportd(UNLOCK_DEV_PORT)
    d.setDaemon(True)
    d.start()


class unlock_tr(threading.Thread):
    def __init__(self):
        super().__init__()

    def run(self):
        startUnlockEx()


if __name__ == '__main__':
    startUnlockEx()
