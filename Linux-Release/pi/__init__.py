import threading
import time

from unlock_pi import main_t as main1
from wake_on_lan_pi import main_t as main2

if __name__ == '__main__':
    tr1 = main1()
    tr2 = main2()
    try:
        tr1.start()
        tr2.start()
        while threading.activeCount() < 2:
            time.sleep(10)
    except KeyboardInterrupt:
        exit(0)
