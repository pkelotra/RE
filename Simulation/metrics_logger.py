import csv
import json
from sklearn.metrics import classification_report

def create_result(name, y_true, preds, bw, lat):
    report = classification_report(y_true, preds, output_dict=True)

    return {
        "Experiment": name,
        "Accuracy": round(report["accuracy"], 4),
        "Bandwidth_KB": round(bw / 1024, 2),
        "Latency_ms": round((lat / len(preds)) * 1000, 2)
    }

def save(results):
    with open("results.json", "w") as f:
        json.dump(results, f, indent=4)

    with open("results.csv", "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=results[0].keys())
        writer.writeheader()
        writer.writerows(results)