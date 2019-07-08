import daemon
import threading
import sys
from pathConvertor import *


# a read thread, read data from remote
class UnlockSocketExchanger(threading.Thread):
    def __init__(self, client, master, string=None):
        threading.Thread.__init__(self)
        self.client = client
        self.master = master
        self.string = string

    def run(self):
        if self.string is None:
            req = self.client.recv(BUFSIZE)
        else:
            req = self.string
        if req:
            try:
                self.master.sendall(req)
                # 等待回应
                rep = self.master.recv(BUFSIZE)
                if rep is None or rep == '':
                    daemon.sendError(self.client)
                else:
                    self.client.sendall(rep)
                print('发送成功')
            except BrokenPipeError or TimeoutError or ConnectionResetError:
                print('对方不在线')
                daemon.sendError(self.client)
        self.client.close()
        self.master.close()


def startUnlockEx():
    # 无限监听，并accept
    lst = daemon.MasterHolderd(UNLOCK_DEV_PORT)  # create a listen thread
    lst.start()  # then start

    # 接收客户端连接请求并与服务端对接
    rcv = daemon.ClientHolderd(UNLOCK_DEV_PORT,UNLOCK_CLIENT_PORT, UnlockSocketExchanger)
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
