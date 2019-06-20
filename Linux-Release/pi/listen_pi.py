import os
import socket
import sys
import json

sys.path.append('..')
from Config import *


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
    return (PC_NAME, MAC_ADDR, IP)


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
