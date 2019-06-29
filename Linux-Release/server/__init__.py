from unlockServer_nat_server import unlock_tr
from fileServer_nat_server import file_tr
from wakeOnLanServer_nat_server import wake_tr


if __name__ == '__main__':

    tr=[unlock_tr(),file_tr(),wake_tr()]

    for func_tr in tr:
        func_tr.start()

    for func_tr in tr:
        func_tr.join()

    exit(0)
