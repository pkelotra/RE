import torch
import torch.nn as nn
import torch.nn.functional as F

class EEGNet(nn.Module):
    def __init__(self):
        super().__init__()

        # EDGE
        self.conv1 = nn.Conv1d(1, 16, 5)
        self.conv2 = nn.Conv1d(16, 32, 5)
        self.conv3 = nn.Conv1d(32, 64, 5)
        self.pool = nn.MaxPool1d(2)

        # BOTTLENECK
        self.bottleneck = nn.AdaptiveAvgPool1d(8)

        # CLOUD
        self.fc1 = nn.Linear(64 * 8, 64)
        self.fc2 = nn.Linear(64, 2)

    def encode(self, x):
        x = self.pool(F.relu(self.conv1(x)))
        x = self.pool(F.relu(self.conv2(x)))
        x = self.pool(F.relu(self.conv3(x)))
        x = self.bottleneck(x)
        return x

    def decode(self, x):
        x = x.view(x.size(0), -1)
        x = F.relu(self.fc1(x))
        return self.fc2(x)

    def forward(self, x):
        return self.decode(self.encode(x))