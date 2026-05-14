import socket
import requests
import json

HOST = "0.0.0.0"
PORT = 5000

CLOUD_URL = ""

server = socket.socket()
server.bind((HOST, PORT))
server.listen(1)

print("Waiting for ESP32...")

conn, addr = server.accept()
print("Connected from:", addr)

buffer = b""

while True:
    data = conn.recv(4096)
    if not data:
        break

    buffer += data

    # HANDLE MULTIPLE TOKENS PROPERLY
    while b"\n" in buffer:
        line, buffer = buffer.split(b"\n", 1)

        try:
            token = json.loads(line.decode())

            res = requests.post(CLOUD_URL, json={"token": token})

            print("Prediction:", res.json())

        except Exception as e:
            print("Error:", e)