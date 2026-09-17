# Tong hop ket qua benchmark DL4J (Windows)

## Kich ban 1: WorkspaceMode NONE vs ENABLED
| mode | epochs | batchSize | elapsedMs | heapUsedBytes | nonHeapUsedBytes | nativeUsedBytes |
| --- | --- | --- | --- | --- | --- | --- |
| NONE | 2 | 64 | 33627 | 1481433600 | 62502336 | 30508704 |
| ENABLED | 2 | 64 | 32870 | 1049328000 | 61234136 | 9803964 |

## Kich ban 2: Spark Local Scaling
| threads | batchSize | elapsedMs | throughputRecPerSec |
| --- | --- | --- | --- |
| 1 | 64 | 163725 | 1391.6323102763781 |
| 2 | 64 | 100246 | 2272.858767432117 |
| 4 | 64 | 80202 | 2840.889254632054 |
| 6 | 64 | 74288 | 3067.049860004308 |
| 8 | 64 | 71565 | 3183.7490393348703 |

## Kich ban 3a: Inference Latency (In-process)
| mode | batchSize | meanMs | p50Ms | p99Ms | minMs | maxMs | sampleCount |
| --- | --- | --- | --- | --- | --- | --- | --- |
| in-process | 1 | 1.574 | 1.441 | 4.208 | 0.355 | 5.356 | 200 |
| in-process | 8 | 1.971 | 1.782 | 7.019 | 1.121 | 11.533 | 200 |

## Kich ban 3b: Inference Latency (Remote)
| mode | batchSize | meanMs | p50Ms | p99Ms | minMs | maxMs | sampleCount |
| --- | --- | --- | --- | --- | --- | --- | --- |
| remote | 1 | 7.644 | 6.827 | 18.317 | 2.774 | 20.649 | 200 |
| remote | 8 | 5.874 | 4.669 | 16.834 | 3.02 | 21.651 | 200 |


