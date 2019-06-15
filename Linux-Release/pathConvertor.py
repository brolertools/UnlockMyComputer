import platform as pf
import string
import os
from Config import *


def pathAdjtor(path):
    """
    解决Win和mac和Linux到路径不兼容情况,app默认使用/.作为根结点，需要对win做兼容
    :param path: 传入到path字符串
    :return: 系统兼容到路径
    """
    sysstr = pf.system()
    if sysstr == 'Windows':
        if path == '/.':
            return win_lst_disk_path()
        else:
            return path[3:]
    return path


def win_lst_disk_path():
    json_tobe_send = dict()
    json_tobe_send.clear()
    json_tobe_send['current_folder'] = '/.'
    json_tobe_send['detail'] = []

    for c in string.ascii_uppercase:
        detail = dict()
        detail.clear()

        disk = c + ':'
        if os.path.isdir(disk):
            detail['file_name'] = disk
            detail['attributes'] = FOLDER
            json_tobe_send['detail'].append(detail.copy())
    return json_tobe_send
