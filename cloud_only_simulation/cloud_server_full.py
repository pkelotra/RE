from flask import Flask, request, jsonify
import torch
import numpy as np
from model import EEGNet

app = Flask(__name__)
DEVICE = torch.device("cpu")

model = EEGNet().to(DEVICE)
model.load_state_dict(torch.load("model.pth", map_location=DEVICE))
model.eval()

@app.route("/predict_raw", methods=["POST"])
def predict_raw():
    print("\n☁️ FULL CLOUD REQUEST RECEIVED")

    data = request.json

    # Raw EEG
    x = np.array(data["eeg"], dtype=np.float32)
    x = torch.tensor(x).unsqueeze(0).to(DEVICE)

    print("EEG shape:", x.shape)

    with torch.no_grad():
        output = model(x)
        pred = torch.argmax(output, dim=1).item()

    return jsonify({"prediction": pred})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001)