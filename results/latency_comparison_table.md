# So sanh do tre In-Process vs Remote (Spring Boot & FastAPI)

## Remote: Spring Boot (Java)

| Batch (B) | In-Process p50 (ms) | Remote p50 (ms) | Ty so chenh lech p50 | Chenh lech mean | Thong luong In-Process (mau/s) | Thong luong Remote (mau/s) |
| --- | --- | --- | --- | --- | --- | --- |
| B=1 | 0,172 | 5,485 | 31,89x | 22,93x | 3.634,0 | 158,6 |
| B=8 | 0,738 | 5,560 | 7,53x | 7,07x | 9.791,9 | 1.385,7 |
| B=16 | 0,381 | 5,447 | 14,30x | 12,79x | 36.471,9 | 2.849,7 |
| B=32 | 0,322 | 5,370 | 16,68x | 14,72x | 82.332,7 | 5.586,5 |
| B=64 | 0,626 | 6,028 | 9,63x | 8,33x | 81.732,8 | 9.813,9 |

## Remote: FastAPI (Python)

| Batch (B) | In-Process p50 (ms) | Remote p50 (ms) | Ty so chenh lech p50 | Chenh lech mean | Thong luong In-Process (mau/s) | Thong luong Remote (mau/s) |
| --- | --- | --- | --- | --- | --- | --- |
| B=1 | 0,172 | 3,239 | 18,83x | 13,24x | 3.634,0 | 274,6 |
| B=8 | 0,738 | 3,036 | 4,11x | 3,92x | 9.791,9 | 2.496,0 |
| B=16 | 0,381 | 2,967 | 7,79x | 6,85x | 36.471,9 | 5.324,7 |
| B=32 | 0,322 | 3,596 | 11,17x | 11,05x | 82.332,7 | 7.442,0 |
| B=64 | 0,626 | 4,104 | 6,56x | 5,31x | 81.732,8 | 15.404,2 |

## So sanh truc tiep 3 tang theo p50 (ms)

| Batch (B) | In-Process | Spring Boot (Java) | FastAPI (Python) |
| --- | --- | --- | --- |
| B=1 | 0,172 | 5,485 | 3,239 |
| B=8 | 0,738 | 5,560 | 3,036 |
| B=16 | 0,381 | 5,447 | 2,967 |
| B=32 | 0,322 | 5,370 | 3,596 |
| B=64 | 0,626 | 6,028 | 4,104 |
