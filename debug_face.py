from deepface import DeepFace
import sys

img1 = sys.argv[1]
img2 = sys.argv[2]

print("IMG1:", img1)
print("IMG2:", img2)

try:
    result = DeepFace.verify(
        img1_path=img1,
        img2_path=img2,
        enforce_detection=True
    )

    print("RESULT:", result)
except Exception as e:
    print("ERROR:", str(e))