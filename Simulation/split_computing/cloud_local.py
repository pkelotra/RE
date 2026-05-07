import torch
import time
import numpy as np

def run_cloud(model, X, device):
    preds, bw, lat = [], 0, 0

    for x in X:
        x_tensor = torch.tensor(x, dtype=torch.float32).unsqueeze(0).to(device)

        size = x.nbytes

        t0 = time.time()
        output = model(x_tensor)
        t1 = time.time()

        preds.append(torch.argmax(output).item())

        bw += size
        lat += (t1 - t0)

    return preds, bw, lat