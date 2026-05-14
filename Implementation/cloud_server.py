from flask import Flask, request, jsonify
import torch
import numpy as np
import torch.nn.functional as F
from model import EEGNet

app = Flask(__name__)
DEVICE = torch.device("cpu")

# Load model
model = EEGNet().to(DEVICE)
model.load_state_dict(torch.load("model.pth", map_location=DEVICE))
model.eval()


@app.route("/predict", methods=["POST"])
def predict():
    print("\n🔥 REQUEST RECEIVED")

    data = request.json

    # =========================
    # CASE 1: TOKEN (LOW MODE)
    # =========================
    if "token" in data:
        token = np.array(data["token"], dtype=np.float32)
        token = torch.tensor(token).unsqueeze(0).to(DEVICE)

        with torch.no_grad():
            logits = model.decode(token)

        input_type = "TOKEN"

    # =========================
    # CASE 2: RAW EEG (HIGH MODE)
    # =========================
    elif "eeg" in data:
        x = np.array(data["eeg"], dtype=np.float32)
        x = torch.tensor(x).unsqueeze(0).to(DEVICE)

        with torch.no_grad():
            logits = model(x)

        input_type = "RAW EEG"

    else:
        return jsonify({"error": "Invalid input"}), 400

    # =========================
    # COMMON PART
    # =========================
    probs = F.softmax(logits, dim=1)

    pred = torch.argmax(probs, dim=1).item()

    # 🔥 ENTROPY
    entropy = -torch.sum(probs * torch.log(probs + 1e-8)).item()

    # 🔥 FEEDBACK LOGIC
    THRESHOLD = 0.5

    if entropy > THRESHOLD:
        mode = "HIGH"
    else:
        mode = "LOW"

    print(f"Input: {input_type}, Entropy: {entropy:.4f}, Mode: {mode}")

    return jsonify({
        "prediction": pred,
        "entropy": entropy,
        "mode": mode
    })


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)