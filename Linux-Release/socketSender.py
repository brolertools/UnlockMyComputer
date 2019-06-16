import json


def send(client, json_str):
    send_str = json.JSONEncoder().encode(json_str)
    if client.sendall(send_str.encode('utf-8')) is None:
        return True
    return False
