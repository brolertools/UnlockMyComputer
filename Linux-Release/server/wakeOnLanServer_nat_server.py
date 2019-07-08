import sys
import threading
import daemon

from pathConvertor import *


# a read thread, read data from remote
class WakeSocketExchanger(threading.Thread):
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
                rep = self.master.recv(BUFSIZE)
                if rep is None or rep == '':
                    daemon.sendError(self.client)
                else:
                    self.client.sendall(rep)
                print('发送成功')
            except BrokenPipeError or TimeoutError or ConnectionResetError:
                print('对方不在线或者异常')
                daemon.sendError(self.client)
        self.client.close()
        self.master.close()


def startWake():
    # 无限监听，并accept
    lst = daemon.MasterHolderd(WAKE_ON_LAN_DEV_PORT)  # create a listen thread
    lst.start()  # then start

    # 接收客户端连接请求并与服务端对接
    rcv = daemon.ClientHolderd(WAKE_ON_LAN_DEV_PORT, WAKE_ON_LAN_CLIENT_PORT, WakeSocketExchanger)
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
