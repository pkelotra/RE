import os
import numpy as np

def load_data(path):
    X, y = [], []

    label_map = {'Z':0, 'O':0, 'N':0, 'F':0, 'S':1}

    for folder in os.listdir(path):
        folder_path = os.path.join(path, folder)

        if not os.path.isdir(folder_path):
            continue

        label = label_map.get(folder, None)
        if label is None:
            continue

        for file in os.listdir(folder_path):
            try:
                signal = np.loadtxt(os.path.join(folder_path, file))
                X.append(signal)
                y.append(label)
            except:
                continue

    X = np.array(X)
    y = np.array(y)

    # Normalize
    X = (X - X.mean(axis=1, keepdims=True)) / (X.std(axis=1, keepdims=True) + 1e-8)

    # Add channel dim
    X = X[:, np.newaxis, :]

    return X, y