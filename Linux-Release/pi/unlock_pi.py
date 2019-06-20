# udp_gb_client.py
'''树莓派设置py'''

import binascii
import json
import re
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
    return True


def startBind():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)

    s.bind(('', UDP_PORT))
    print('Listening for broadcast at ', s.getsockname())

    while True:
        data, address = s.recvfrom(65535)
        data = bytes.decode(data, encoding)
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
                s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                ssl_sock = ssl.wrap_socket(s, ca_certs="cacert.pem", cert_reqs=ssl.CERT_REQUIRED)
                ssl_sock.connect((IP, UNLOCK_PORT))
                ssl_sock.sendall(data)
                print('发送成功')
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
            startBind()
        else:
            tr = Connector(UNLOCK_DEV_PORT, Reader)
            tr.start()
            tr.join()


if __name__ == '__main__':
    s = main_t()
    s.start()
    s.join()
