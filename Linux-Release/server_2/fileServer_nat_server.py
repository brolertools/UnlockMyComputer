import daemon, select, sys, threading

sys.path.append('..')
from pathConvertor import *


# a read thread, read data from remote
class FILESocketExchanger(threading.Thread):
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
            self.master.sendall(req)
            while True:
                try:
                    pre_read, pre_write, err = select.select([self.master, ], [self.master, ], [], 5)
                except select.error:
                    self.master.shutdown(2)
                    self.master.close()
                    break
                if len(pre_read) > 0:
                    recv = self.master.recv(BUFSIZE)
                    if recv == b'':
                        break
                    elif recv:
                        try:
                            self.client.sendall(recv)
                        except ConnectionResetError:
                            print('客户端在传输过程中中断')
                            break
        self.client.close()
        self.master.close()


def startWLAN():
    # 无限监听，并accept
    lst = daemon.MasterHolderd(FILE_MASTER_PORT)  # create a listen thread
    lst.start()  # then start

    # 接收客户端连接请求并与服务端对接
    rcv = daemon.ClientHolderd(FILE_MASTER_PORT, FILE_CLIENT_PORT, FILESocketExchanger)
    rcv.start()

    d = daemon.reportd(FILE_MASTER_PORT)
    d.setDaemon(True)
    d.start()


class file_tr(threading.Thread):
    def __init__(self):
        super().__init__()

    def run(self):
        startWLAN()


if __name__ == '__main__':
    startWLAN()
