# Uncertainty-Aware Semantic Split Computing for EEG Seizure Detection

## Overview
This project implements an uncertainty-aware, split-computing pipeline for EEG seizure detection using the Bonn EEG dataset. It supports cloud-only inference, edge-only inference, and split inference with a semantic bottleneck, plus a feedback loop that switches between LOW/HIGH modes based on entropy.

Key assets:
- Dataset: [Bonn Univeristy Dataset/](Bonn%20Univeristy%20Dataset/)
- Implementation (hardware pipeline): [Implementation/](Implementation/)
- Simulation (software experiments): [Simulation/](Simulation/)
- Presentation: [Uncertainty-Aware_Semantic_Split_Computing_ppt.pdf](Uncertainty-Aware_Semantic_Split_Computing_ppt.pdf)
- Demo videos: [Demo_video.mp4](Demo_video.mp4), [NotebookLM_Project_Explanation_video.mp4](NotebookLM_Project_Explanation_video.mp4)

## Model Architecture
The CNN is split into an encoder (edge) and decoder (cloud) :

```text
Conv1 -> Pool
Conv2 -> Pool
Conv3 -> Pool
AdaptiveAvgPool1D 
FC1 -> FC2
```

Model definition: [Implementation/model.py](Implementation/model.py) and [Simulation/model.py](Simulation/model.py)

## Repository Structure
- [Bonn Univeristy Dataset/](Bonn%20Univeristy%20Dataset/): Bonn EEG dataset (folders Z, O, N, F, S)
- [Implementation/](Implementation/): Hardware pipeline scripts and ESP32 sketch
  - Cloud API: [Implementation/cloud_server.py](Implementation/cloud_server.py)
  - Gateway forwarder: [Implementation/edge.py](Implementation/edge.py)
  - Encoder streaming: [Implementation/sensor.py](Implementation/sensor.py)
  - ESP32 firmware: [Implementation/esp32.ino](Implementation/esp32.ino)
- [Simulation/](Simulation/): Local simulation of cloud, edge, and split inference
  - Cloud-only: [Simulation/cloud_only_simulation/](Simulation/cloud_only_simulation/)
  - Edge-only: [Simulation/edge_only_simulation/](Simulation/edge_only_simulation/)
  - Split computing: [Simulation/split_computing/](Simulation/split_computing/)
  - Metrics output: [Simulation/results.json](Simulation/results.json), [Simulation/results.csv](Simulation/results.csv)

## Code Files
Implementation:
- [Implementation/cloud_server.py](Implementation/cloud_server.py): Flask API that accepts tokens or raw EEG and returns prediction, entropy, and mode.
- [Implementation/data_loader.py](Implementation/data_loader.py): Loads Bonn EEG signals into normalized numpy arrays with a channel dimension.
- [Implementation/edge.py](Implementation/edge.py): Gateway socket server that receives tokens from ESP32 and forwards them to the cloud API.
- [Implementation/esp32.ino](Implementation/esp32.ino): ESP32 firmware that bridges laptop token stream to the Raspberry Pi over Wi-Fi.
- [Implementation/metrics_logger.py](Implementation/metrics_logger.py): Creates metrics dictionaries and saves results to JSON/CSV.
- [Implementation/model.py](Implementation/model.py): CNN model with `encode` (edge) and `decode` (cloud) split.
- [Implementation/run_all.py](Implementation/run_all.py): Runs cloud, edge, and split experiments and saves metrics (expects `X_test.npy` and `y_test.npy`).
- [Implementation/sensor.py](Implementation/sensor.py): Laptop-side encoder that streams semantic tokens to the ESP32 over TCP.

Simulation:
- [Simulation/cloud_only_simulation/cloud_client.py](Simulation/cloud_only_simulation/cloud_client.py): Sends raw EEG to the cloud endpoint and measures bandwidth/latency.
- [Simulation/cloud_only_simulation/cloud_server_full.py](Simulation/cloud_only_simulation/cloud_server_full.py): Flask server for full-cloud inference on raw EEG.
- [Simulation/data_loader.py](Simulation/data_loader.py): Loads and normalizes Bonn EEG signals for simulation.
- [Simulation/edge_only_simulation/edge_local.py](Simulation/edge_only_simulation/edge_local.py): Local edge-only inference baseline.
- [Simulation/metrics_logger.py](Simulation/metrics_logger.py): Creates metrics dictionaries and saves results to JSON/CSV.
- [Simulation/model.py](Simulation/model.py): CNN model with the same split architecture as the implementation.
- [Simulation/run_all.py](Simulation/run_all.py): Convenience runner for the three modes (imports scripts from subfolders).
- [Simulation/split_computing/cloud_local.py](Simulation/split_computing/cloud_local.py): Local cloud-side inference used for split experiments.
- [Simulation/split_computing/edge_client.py](Simulation/split_computing/edge_client.py): Split inference client with entropy feedback to switch LOW/HIGH modes.

## Hardware Requirements
- Edge/control device: ESP32-WROOM-32
- Gateway: Raspberry Pi 3 Model B (or similar)
- Cloud: AWS EC2 (Ubuntu Server 24.04 LTS, t2.micro)
- Dev machine: Laptop/Desktop with PyTorch
- Connectivity: USB cable for ESP32, breadboard, optional LEDs/resistors, internet

## Software Requirements
- Python 3.x
- Arduino IDE + ESP32 Board Package + CP2102 USB-to-UART Driver
- Python libraries: `torch`, `numpy`, `scikit-learn`, `flask`, `requests`, `pyserial`

## Dataset Preparation
Scripts expect `X_test.npy` and `y_test.npy` in the working directory. You can generate them from the Bonn EEG dataset using [Simulation/data_loader.py](Simulation/data_loader.py) or [Implementation/data_loader.py](Implementation/data_loader.py).

Example (run from the Simulation folder):

```bash
cd Simulation
python - <<'PY'
import numpy as np
from data_loader import load_data

X, y = load_data("../Bonn Univeristy Dataset")
# Simple train/test split (80/20)
split = int(0.8 * len(X))
X_train, X_test = X[:split], X[split:]
y_train, y_test = y[:split], y[split:]

np.save("X_test.npy", X_test)
np.save("y_test.npy", y_test)
PY
```

## Simulation Runs
Run the individual modes from their subfolders. Update the cloud URL in the script if you are not using the default AWS endpoint.

Cloud-only:
```bash
cd Simulation/cloud_only_simulation
python cloud_server_full.py
python cloud_client.py
```

Edge-only:
```bash
cd Simulation/edge_only_simulation
python edge_local.py
```

Split computing (semantic tokens + entropy feedback):
```bash
cd Simulation/split_computing
python edge_client.py
```

Notes:
- [Simulation/run_all.py](Simulation/run_all.py) is a convenience runner but imports files that live in subfolders. If you want a single-entry run, update its imports or move the scripts into the same directory.

## Hardware Pipeline (Edge -> Gateway -> Cloud)
1. Cloud server (AWS EC2): update and run [Implementation/cloud_server.py](Implementation/cloud_server.py).
2. Gateway (Raspberry Pi): set `CLOUD_URL` in [Implementation/edge.py](Implementation/edge.py) and run it.
3. ESP32: update Wi-Fi and Pi settings in [Implementation/esp32.ino](Implementation/esp32.ino), then flash via Arduino IDE.
4. Encoder stream (Laptop): update `ESP32_IP` and `DATASET_PATH` in [Implementation/sensor.py](Implementation/sensor.py) and run it.

Current behavior:
- [Implementation/cloud_server.py](Implementation/cloud_server.py) returns `prediction`, `entropy`, and `mode`.
- [Implementation/edge.py](Implementation/edge.py) forwards tokens to the cloud and prints responses.

## Results for Simulation
Recorded metrics are available in [Simulation/results.json](Simulation/results.json) and [Simulation/results.csv](Simulation/results.csv).

Example results:

| Experiment | Accuracy | Precision | Recall | F1_score | Bandwidth_KB | Latency_ms |
| ---------- | -------- | --------- | ------ | -------- | ------------ | ---------- |
| Cloud      | 0.948    | 0.9512    | 0.948  | 0.9449   | 16003.91     | 27.34      |
| Edge       | 0.948    | 0.9512    | 0.948  | 0.9449   | 0.49         | 1.17       |
| Split      | 0.948    | 0.9512    | 0.948  | 0.9449   | 500.00       | 1.84       |

## References
- Bonn EEG dataset from the University of Bonn (seizure vs non-seizure classification)
