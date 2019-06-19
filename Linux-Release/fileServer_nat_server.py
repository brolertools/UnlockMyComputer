import select
import socket
import ssl
from pathConvertor import *


# a read thread, read data from remote
class FILESocketExchanger(threading.Thread):
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
                        self.client.sendall(recv)

        self.client.close()
        self.master.close()


def startWLAN():
    lst = Listener(FILE_CLIENT_PORT, FILE_MASTER_PORT, FILESocketExchanger)  # create a listen thread
    lst.start()  # then start


if __name__ == '__main__':
    startWLAN()
