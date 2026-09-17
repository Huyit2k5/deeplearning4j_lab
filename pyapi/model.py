"""
MLP cung kien truc voi model DL4J (30 -> 32 -> 16 -> 2, ReLU/ReLU/Softmax) de dam bao
chi phi forward-pass tuong duong khi so sanh do tre serving giua FastAPI (Python) va
Spring Boot (Java). Trong so khoi tao random (khong train lai) - benchmark nay chi do
DO TRE cua tang serving (network + serialize + framework overhead), khong danh gia do
chinh xac, nen khong can trong so da train.
"""
import torch
import torch.nn as nn

NUM_FEATURES = 30
NUM_CLASSES = 2


class FraudMlp(nn.Module):
    def __init__(self):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(NUM_FEATURES, 32),
            nn.ReLU(),
            nn.Linear(32, 16),
            nn.ReLU(),
            nn.Linear(16, NUM_CLASSES),
            nn.Softmax(dim=1),
        )

    def forward(self, x):
        return self.net(x)


def load_model():
    torch.manual_seed(42)
    model = FraudMlp()
    model.eval()
    return model
