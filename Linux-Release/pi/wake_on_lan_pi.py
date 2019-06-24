# udp_gb_client.py
'''树莓派设置py'''

import binascii
import json
import os
import re
import sys
from listen_pi import *

sys.path.append('..')
from Config import *

MAC_ADDR = None
IP = None
PC_NAME = None
PORT = 8970


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
        while True:
            # 发送目标机器的MAC ADDR
            if MAC_ADDR is not None:
                self.client.sendall(generateJsonBytesForMAC(MAC_ADDR))
            else:
                # 发送缺省值
                self.client.sendall(DEFAULT_MAC_ADDR.encode(encoding))
            data = self.client.recv(BUFSIZE)
            if data:
                broadcast_address = '255.255.255.255'
                port = 9
                # ======================================
                send_data = create_magic_packet(MAC_ADDR)

                s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                s.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
                for i in range(5):
                    s.sendto(send_data, (broadcast_address, port))
                    s.sendto(send_data, (IP, port))
                s.close()
                print('发送成功')
            else:
                break


def main():
    global PC_NAME, MAC_ADDR, IP
    t = loadConfig()
    while True:
        if t is None:
            startBind()
        else:
            (PC_NAME, MAC_ADDR, IP) = loadConfig()
            conn = Connector(WAKE_ON_LAN_DEV_PORT, Reader)
            conn.start()
            conn.join()


class main_t(threading.Thread):
    def __init__(self):
        super().__init__()

    def run(self):
        main()


if __name__ == '__main__':
    s = main_t()
    s.start()
    s.join()
