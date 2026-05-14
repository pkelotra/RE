import numpy as np
import requests
import time
from data_loader import load_data

CLOUD_URL = "http://13.62.228.71:5001/predict_raw"

X = np.load("X_test.npy")
y = np.load("y_test.npy")

def run_cloud_real():
    preds = []
    total_latency = 0
    total_bandwidth = 0

    for x in X:
        t0 = time.time()

        payload = {"eeg": x.tolist()}
        size = len(str(payload).encode("utf-8"))

        response = requests.post(CLOUD_URL, json=payload)
        pred = response.json()["prediction"]

        t1 = time.time()

        preds.append(pred)
        total_latency += (t1 - t0)
        total_bandwidth += size

    return preds, total_bandwidth, total_latency, y


if __name__ == "__main__":
    preds, bw, lat, y = run_cloud_real()

    print("\n☁️ REAL CLOUD DONE")
    print("Bandwidth (KB):", bw / 1024)
    print("Latency (ms):", (lat / len(preds)) * 1000)