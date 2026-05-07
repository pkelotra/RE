import socket
import numpy as np
import os
import time
import json
import torch
from model import EEGNet

# ===== CONFIG =====
ESP32_IP = ""   #  your ESP IP
PORT = 1234
DELAY = 0.05
DATASET_PATH = "Dataset"

# ===== LOAD MODEL =====
DEVICE = torch.device("cpu")

model = EEGNet().to(DEVICE)
model.load_state_dict(torch.load("model.pth", map_location=DEVICE))
model.eval()

print("Encoder loaded on laptop")

# ===== LOAD DATA =====
def load_all_files(folder):
    signals = []
    for root, dirs, files in os.walk(folder):
        for file in files:
            if file.endswith(".txt"):
                path = os.path.join(root, file)
                signal = np.loadtxt(path)
                signals.append(signal)
    return signals

signals = load_all_files(DATASET_PATH)
print(f"Loaded {len(signals)} EEG signals")

# ===== CONNECT =====
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect((ESP32_IP, PORT))
print("Connected to ESP32")

# ===== STREAM TOKENS =====
for signal in signals:

    signal = (signal - np.mean(signal)) / np.std(signal)

    x = torch.tensor(signal).float().unsqueeze(0).unsqueeze(0)

    with torch.no_grad():
        token = model.encode(x)

    token = token.squeeze().numpy().tolist()

    # 🔥 ADD DELIMITER HERE
    message = json.dumps(token) + "\n"
    sock.sendall(message.encode())

    time.sleep(DELAY)

print("Streaming finished")
sock.close()