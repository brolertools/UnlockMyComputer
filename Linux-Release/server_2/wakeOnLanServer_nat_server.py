import sys
import threading
import daemon

sys.path.append('..')
from pathConvertor import *


# a read thread, read data from remote
class WakeSocketExchanger(threading.Thread):
    def __init__(self, client, master, info=None):
        threading.Thread.__init__(self)
        self.client = client
        self.master = master
        self.info = info

    def run(self):
        req = self.client.recv(BUFSIZE)
        if req:
            print('发送成功')
            self.master.sendall(req)
        self.client.close()
        self.master.close()


def startWake():
    # 无限监听，并accept
    lst = daemon.MasterHolderd(WAKE_ON_LAN_DEV_PORT)  # create a listen thread
    lst.start()  # then start

    # 接收客户端连接请求并与服务端对接
    rcv = daemon.ClientHolderd(WAKE_ON_LAN_CLIENT_PORT, WakeSocketExchanger)
    rcv.start()

    # 实时Client连接
    d = daemon.reportd(WAKE_ON_LAN_DEV_PORT)
    d.setDaemon(True)
    d.start()


class wake_tr(threading.Thread):
    def __init__(self):
        super().__init__()

    def run(self):
        startWake()


if __name__ == '__main__':
    startWake()
