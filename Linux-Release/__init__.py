from blue import *
from wifi import *
import _thread
import time

if __name__ == '__main__':
    thr1 = _thread.start_new_thread(startWLAN, ())
    thr2 = _thread.start_new_thread(startBT, ())
    while (threading.activeCount() > 0):
        time.sleep(10)
