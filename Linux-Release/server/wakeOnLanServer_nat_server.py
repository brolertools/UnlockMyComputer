import sys

sys.path.append('..')
from pathConvertor import *


# a read thread, read data from remote
class WakeSocketExchanger(threading.Thread):
    def __init__(self, client, master):
        threading.Thread.__init__(self)
        self.client = client
        self.master = master

    def run(self):
        req = self.client.recv(BUFSIZE)
        if req:
            print('发送成功')
            self.master.sendall(req)
        self.client.close()
        self.master.close()


def startWake():
    lst = Listener(WAKE_ON_LAN_CLIENT_PORT, WAKE_ON_LAN_DEV_PORT, WakeSocketExchanger)  # create a listen thread
    lst.start()  # then start


class wake_tr(threading.Thread):
    def __init__(self):
        super().__init__()

    def run(self):
        startWake()


if __name__ == '__main__':
    startWake()
