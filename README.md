通过手机解锁我的电脑！

> 作者：Kingtous

功能：
- 解锁电脑
- Wake On LAN
- 文件传输~~(暂未开发)~~
- 内网穿透(开发中)
- 锁上电脑(暂未开发)

# 下载地址
[Release](https://github.com/Kingtous/UnlockMyComputer/releases)

# 时间轴

## June 15,2019

- 支持文件传输(python端)
  - 其中文件传输支持Win,macOS,Linux三端

- 修复若干影响使用的bug

## May 4,2019

正式发布1.0版本，有安卓、Windows、Linux三端

## Apr 13,2019
更新WLAN、蓝牙客户端 For Linux (using systemd)
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

** PS:blue.py（蓝牙模块）可以在root和普通用户下执行，普通用户需要更改rfcomm的执行权限(Linux下普通用户默认没有执行此可执行文件的权限)，使用```sudo chmod u+x /usr/bin/rfcomm```即可. **

## Apr 7,2019

完善WiFi模块

## Mar 26, 2019

迁移至本仓库

## Mar 11, 2019

完善蓝牙模块

## Feb 23, 2019

Demo阶段，开发中
