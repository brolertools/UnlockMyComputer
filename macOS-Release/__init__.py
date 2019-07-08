from fileServer_nat_dev import file_tr
from fileServer import file_local_tr
from wifi import wlan_unlock_local_tr


if __name__ == '__main__':

    tr = [file_tr(), file_local_tr(),wlan_unlock_local_tr()]

    for func_tr in tr:
        func_tr.start()

    for func_tr in tr:
        func_tr.join()
