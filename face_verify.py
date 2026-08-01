import sys
import os

if len(sys.argv) < 3:
    print("NO_MATCH")
    sys.exit(1)

img1_path = sys.argv[1]
img2_path = sys.argv[2]

# Ensure the files exist
if not os.path.exists(img1_path) or not os.path.exists(img2_path):
    print("NO_MATCH")
    sys.exit(1)

# Method 1: Using face_recognition library (fast, lightweight, highly accurate, offline)
try:
    import face_recognition
    
    img1 = face_recognition.load_image_file(img1_path)
    img2 = face_recognition.load_image_file(img2_path)
    
    # Upsample the image 1 time to help detect faces in low-resolution / blurry images
    img1_locations = face_recognition.face_locations(img1, number_of_times_to_upsample=1)
    img2_locations = face_recognition.face_locations(img2, number_of_times_to_upsample=1)
    
    img1_encodings = face_recognition.face_encodings(img1, known_face_locations=img1_locations)
    img2_encodings = face_recognition.face_encodings(img2, known_face_locations=img2_locations)
    
    if len(img1_encodings) > 0 and len(img2_encodings) > 0:
        # Set tolerance to 0.7 to be more forgiving of low quality/lighting noise
        match = face_recognition.compare_faces([img1_encodings[0]], img2_encodings[0], tolerance=0.7)[0]
        if match:
            print("MATCH")
            sys.exit(0)
        else:
            print("NO_MATCH")
            sys.exit(0)
except Exception as e:
    # If face_recognition library fails or is not installed, fall back to DeepFace
    pass

# Method 2: Fallback to DeepFace (using Facenet512 model)
try:
    from deepface import DeepFace
    result = DeepFace.verify(
        img1_path=img1_path,
        img2_path=img2_path,
        model_name="Facenet512",
        enforce_detection=False
    )
    if result.get("verified", False):
        print("MATCH")
    else:
        print("NO_MATCH")
except Exception as e:
    print("NO_MATCH")