import os
import socket
import sys
import json
from Config import *

MAC_ADDR = None
IP = None
PC_NAME = None

OFFLINE = '-2'
OK = '0'


class bindID(threading.Thread):
    def __init__(self):
        super().__init__()

    def run(self) -> None:
        while True:
            time.sleep(3)
            print('侦测模块：正在监听网络广播')
            startBind()


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


def startBind():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        s.bind(('', UDP_PORT))
    except OSError:
        print('侦测模块：', UDP_PORT, '端口被占用')
        s.close()
        return
    print('侦测模块：正在监听', UDP_PORT, '端口')

    while True:
        data, address = s.recvfrom(65535)
        data = bytes.decode(data, encoding)
        data = json.loads(data)
        if data.get('macaddr', -1) != -1 and data.get('pcname', -1) != -1:
            f = open('config.ini', 'w')
            f.write(data['pcname'] + '\n' + data['macaddr'] + '\n' + address[0])
            f.close()
            return


def sendStatus(sock, status):
    json_tobe_send = dict()
    json_tobe_send.clear()
    json_tobe_send['status'] = status
    send_str = json.JSONEncoder().encode(json_tobe_send)
    sock.sendall(send_str)
