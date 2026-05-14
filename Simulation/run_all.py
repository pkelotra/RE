import torch
import numpy as np
from sklearn.metrics import accuracy_score

from model import EEGNet
from metrics_logger import create_result, save

from edge_local import run_edge
from edge_client import run_split
from cloud_client import run_cloud_real  

DEVICE = torch.device("cpu")

# ===== LOAD TEST DATA  =====
X = np.load("X_test.npy")
y = np.load("y_test.npy")

# ===== LOAD MODEL =====
model = EEGNet().to(DEVICE)
model.load_state_dict(torch.load("model.pth", map_location=DEVICE))
model.eval()

# ===== RUN EXPERIMENTS =====
print("Running Cloud (REAL AWS)...")
c_preds, c_bw, c_lat, y_true = run_cloud_real()

print("Running Edge (LOCAL)...")
e_preds, e_bw, e_lat = run_edge(model, X, DEVICE)

print("Running Split (REAL)...")
s_preds, s_bw, s_lat, _ = run_split()

# ===== RESULTS =====
results = [
    create_result("Cloud", y_true, c_preds, c_bw, c_lat),
    create_result("Edge", y, e_preds, e_bw, e_lat),
    create_result("Split", y, s_preds, s_bw, s_lat),
]

# ===== PRINT =====
for r in results:
    print(r)

# ===== SAVE =====
save(results)