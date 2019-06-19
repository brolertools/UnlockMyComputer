# udp_gb_client.py
'''树莓派设置py'''

import socket
import json
import os
import threading
import ssl
import binascii
import re
import time
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

    if MAC_ADDR=="":
        return False
    print('载入配置文件成功')
    return True


def startBind():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)

    s.bind(('', PORT))
    print('Listening for broadcast at ', s.getsockname())

    while True:
        data, address = s.recvfrom(65535)
        data = json.loads(data)
        if data.get('macaddr', -1) != -1 and data.get('pcname', -1) != -1:
            f = open('config.ini', 'w')
            f.write(data['pcname'] + '\n' + data['macaddr'] + '\n' + address[0])
            f.close()
            return


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


# a listen thread, listen remote connect
# when a remote machine request to connect, it will create a read thread to handle
class Connector(threading.Thread):
    def __init__(self, port):
        threading.Thread.__init__(self)
        self.port = port
        # SSL
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.ssl_sock = ssl.wrap_socket(self.socket, ca_certs="cacert.pem", cert_reqs=ssl.CERT_REQUIRED)


    def run(self):
        print("Connector started")
        while True:
            if not self.ssl_sock._connected:
                try:
                    self.ssl_sock.connect((NAT_SERVER, self.port))
                    print('连接成功')
                    Reader(self.ssl_sock).start()
                except ConnectionRefusedError:
                    # 重新初始化
                    self.__init__(self.port)
                    time.sleep(3)
                    print('3s后尝试连接NAT公网代理服务器')
                    continue
                Reader(self.ssl_sock).start()
            if self.ssl_sock._closed:
                print('重新连接')
                self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                self.ssl_sock = ssl.wrap_socket(self.socket, ca_certs="cacert.pem", cert_reqs=ssl.CERT_REQUIRED)


if __name__ == '__main__':
    if not loadConfig():
        startBind()
    else:
        Connector(WAKE_ON_LAN_DEV_PORT).start()
