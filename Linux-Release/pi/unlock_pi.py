# udp_gb_client.py
'''树莓派设置py'''

import binascii
import json
import re
import os
import sys

from listen_pi import *

sys.path.append('..')
from Config import *

MAC_ADDR = None
IP = None
PC_NAME = None


def loadConfig():
    global MAC_ADDR, IP, PC_NAME
    if not os.path.exists('config.ini'):
        return False
    f = open('config.ini', 'r')
    PC_NAME = f.readline().strip()
    MAC_ADDR = f.readline().strip()
    IP = f.readline().strip()

    if MAC_ADDR == "":
        return False
    print('载入配置文件成功')
    f.close()

    return (PC_NAME, MAC_ADDR, IP)


def create_magic_packet(mac):
    pro_data = 'FF' * 6 + mac * 16
    send_data = binascii.unhexlify(pro_data)
    return send_data


def format_mac(mac):
    mac_re = re.compile(r'''
                      (^([0-9A-F]{1,2}[-]){5}([0-9A-F]{1,2})$
                      |^([0-9A-F]{1,2}[:]){5}([0-9A-F]{1,2})$
                      |^([0-9A-F]{1,2}[.]){5}([0-9A-F]{1,2})$
                      )''', re.VERBOSE | re.IGNORECASE)
    # print(re.match(mac_re, mac))
    if re.match(mac_re, mac):
        if mac.count(':') == 5 or mac.count('-') == 5 or mac.count('.'):
            sep = mac[2]
            mac_fm = mac.replace(sep, '')
            return mac_fm
    else:
        raise ValueError('Incorrect MAC format')


class Reader(threading.Thread):
    def __init__(self, client):
        threading.Thread.__init__(self)
        self.client = client

    def run(self):
        global MAC_ADDR, IP, PC_NAME

        if MAC_ADDR is not None:
            self.client.sendall(generateJsonBytesForMAC(MAC_ADDR))
        else:
            # 发送缺省值
            self.client.sendall(generateJsonBytesForMAC(DEFAULT_MAC_ADDR))

        while True:
            data = self.client.recv(BUFSIZE)
            if data:
                try:
                    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                    ssl_sock = ssl.wrap_socket(s, ca_certs="cacert.pem", cert_reqs=ssl.CERT_REQUIRED)
                    ssl_sock.connect((IP, UNLOCK_PORT))
                    ssl_sock.sendall(data)
                    sendStatus(OK)
                    print('发送成功')
                except ConnectionRefusedError:
                    print('所控制的设备还未上线')
                    sendStatus(OFFLINE)

            else:
                break


# a listen thread, listen remote connect
# when a remote machine request to connect, it will create a read thread to handle


class main_t(threading.Thread):
    def __init__(self):
        super().__init__()

    def run(self):
        main()


def main():
    while True:
        if not loadConfig():
            print('解锁模块：没有配置文件，正在等待侦测')
            time.sleep(3)
        else:
            tr = Connector(UNLOCK_DEV_PORT, Reader)
            tr.start()
            tr.join()


if __name__ == '__main__':
    s = main_t()
    s.start()
    s.join()
