蓝牙客户端 For Linux using systemd+loginctl

## 使用方法(python3)

- blue.py 开启蓝牙单模式
- wifi.py 开启WiFi单模式
- \_\_init\_\_.py 开启蓝牙+WiFi双通道模式


## 使用说明
- 使用到的python套件
    - json
    - pybluez
    - pexpect
    - re
    - subprocess
    - socket
    - threading
- 使用loginctl解锁
- 使用
- 可注册为systemd组件
- 实时监听WLAN、蓝牙接口

** PS:blue.py可以在root和普通用户下执行，普通用户需要更改rfcomm的执行权限(Linux下普通用户默认没有执行此可执行文件的权限)，使用```sudo chmod u+x /usr/bin/rfcomm```即可. **
