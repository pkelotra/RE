import torch
import numpy as np
import requests
import time
from model import EEGNet

DEVICE = torch.device("cpu")

# Single unified cloud endpoint
CLOUD_URL = "http://13.62.228.71:5000/predict"

# Load model
model = EEGNet().to(DEVICE)
model.load_state_dict(torch.load("model.pth", map_location=DEVICE))
model.eval()

# Load test data
X = np.load("X_test.npy")
y = np.load("y_test.npy")


def run_split():
    preds = []
    total_latency = 0
    total_bandwidth = 0

    mode = "LOW"  # starting in low power mode

    for x in X:
        x_tensor = torch.tensor(x, dtype=torch.float32).unsqueeze(0).to(DEVICE)

        t0 = time.time()

        # =========================
        # LOW POWER → SEND TOKEN
        # =========================
        if mode == "LOW":
            with torch.no_grad():
                token = model.encode(x_tensor)

            token_np = token.detach().cpu().numpy().astype(np.float16)
            payload = {"token": token_np.tolist()}

        # =========================
        # HIGH POWER → SEND RAW EEG
        # =========================
        else:
            payload = {"eeg": x.tolist()}

        # BANDWIDTH
        size = len(str(payload).encode("utf-8"))

        # SEND REQUEST
        response = requests.post(CLOUD_URL, json=payload)
        res = response.json()

        pred = res["prediction"]

        # FEEDBACK LOOP
        if "mode" in res:
            mode = res["mode"]

        t1 = time.time()

        print(f"Mode: {mode}, Prediction: {pred}")

        preds.append(pred)
        total_latency += (t1 - t0)
        total_bandwidth += size

    return preds, total_bandwidth, total_latency, y


if __name__ == "__main__":
    preds, bw, lat, y = run_split()

    print("\n✅ DONE")
    print("Predictions:", preds[:10])
    print("Bandwidth (KB):", bw / 1024)
    print("Avg Latency (ms):", (lat / len(preds)) * 1000)