import threading
import time
from listen_pi import bindID
from unlock_pi import main_t as main1
from wake_on_lan_pi import main_t as main2

if __name__ == '__main__':
    tr1 = main1()
    tr2 = main2()

    checkBind = bindID()
    checkBind.setDaemon(True)

    try:
        tr1.start()
        tr2.start()

        checkBind.start()
        while threading.activeCount() < 2:
            time.sleep(10)
    except KeyboardInterrupt:
        exit(0)
