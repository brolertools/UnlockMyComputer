from unlockServer_nat_server import unlock_tr
from fileServer_nat_server import file_tr
from wakeOnLanServer_nat_server import wake_tr


if __name__ == '__main__':
    tr_1 = unlock_tr()
    tr_2 = file_tr()
    tr_3 = wake_tr()

    tr_1.start()
    tr_2.start()
    tr_3.start()

    tr_1.join()
    tr_2.join()
    tr_3.join()

    exit(0)
