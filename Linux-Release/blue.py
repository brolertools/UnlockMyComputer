import bluetooth
from common import *


uuid = "4E5877C0-8297-4AAE-B7BD-73A8CBC1EDAF"


def startBT():
    while True:
        if checkLockScreen():
            server_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
            server_sock.bind(("", bluetooth.PORT_ANY))
            server_sock.listen(1)

            port = server_sock.getsockname()[1]

            bluetooth.advertise_service(server_sock, "Unlock", service_id=uuid)
            print("Waiting for connection on RFCOMM channel %d" % port)
            client_sock, client_info = server_sock.accept()
            print(client_info)

            data=None
            try:
                while True:
                    data = client_sock.recv(1024)
                    if len(data) == 0:
                        break
                    print("received [%s]" % data)
            except IOError:
                pass
            try:
                data=str(data,encoding='utf8')
            except:
                exit(-1)

            username, passwd, session_id=getData(data)
            if session_id!=None and unlockStatus(session_id)=='yes':
                certificate(passwd,session_id)
                client_sock.close()
                server_sock.close()
        else:
            time.sleep(3)	


if __name__ == "__main__":
    startBT()
