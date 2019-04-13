import subprocess
import re
import os
import pexpect
import json
import time

Debug=False


def getData(data):
    contentDict = json.loads(data)
    username = contentDict['username']
    passwd = contentDict['passwd']
    session_id = getSession(username)
    return username,passwd,session_id


def decrypt(passwd):
    # TODO 解密接口
    return passwd


def checkLockScreen():
    '''
    :return: True for locked & False for unlocked
    '''
    # TODO 检测锁屏状态
    return True


def log(text):
    if Debug:
        print(text)


def getSession(username):
    '''
    :param username: username
    :return: None for invalid, session-id for valid
    '''
    log('检验用户名有效性')
    ret = subprocess.check_output(['/bin/loginctl','list-sessions'])
    result=ret.decode('utf-8').split('\n')

    first_flag=False
    session_id_index=None
    session_usr_index=None
    for usr in result:
        list=re.split(' +',usr.strip())
        if len(list)==5:
            if first_flag==False:
                session_id_index=list.index('SESSION')
                session_usr_index=list.index('USER')
                first_flag=True
                continue
            if list[session_usr_index]==username:
                return list[session_id_index]
    return None


def unlockStatus(session_id):
    '''
    :param session_id
    :return: no for locked , yes for unlocked
    '''
    ret = subprocess.check_output(['/bin/loginctl', 'show-session',session_id]).decode('utf-8').strip()
    return re.search('Active=[^\s]+',ret).group().split('=')[1]

def unlock(session_id):
    log('执行解锁指令')
    subprocess.Popen(['/bin/loginctl','unlock-session',session_id],stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)

def certificate(passwd,session_id):
    log("检验密码")
    passwd=decrypt(passwd)
    pro=pexpect.spawn('/bin/bash '+os.path.dirname(os.path.realpath(__file__))+'/check.sh')
    pro.sendline(passwd)
    time.sleep(3)
    text=str(pro.read().strip().strip(),encoding='utf8')
    if(text=='Y'):
        unlock(session_id)