import socket
import uuid
import json
import time
import ipaddr


def get_host_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    finally:
        s.close()
    return ip


def getName():
    return socket.gethostname()


def getMacAddress():
    address = hex(uuid.getnode())[2:]
    return '-'.join(address[i:i + 2] for i in range(0, len(address), 2))


def getMask():
    mask = ipaddr.IPv4Network(get_host_ip())
    return str(mask.netmask)


PORT = 8970
BUFSIZE = 1024

if __name__ == '__main__':
    udpCliSock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    udpCliSock.bind(('', PORT))
    udpCliSock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    udpCliSock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)

    HOST = getMask()
    print(HOST)
    PORT = 8970
    BUFSIZE = 1024
    ADDR = (HOST, PORT)

    data = dict()
    data['host'] = getName()
    data['mac'] = getMacAddress()
    data['ip'] = get_host_ip()
    print(data)

    ADDR = ('<broadcast>', PORT)

    while True:
        if data:
            send_str = json.JSONEncoder().encode(data)
            print ("sending -> ",send_str)
            udpCliSock.sendto(send_str.encode('utf-8'),ADDR)
            time.sleep(1)

    # data, ADDR = udpCliSock.recvfrom(PORT)
    # if not data:
    #     print('failed')
    # print(data)
