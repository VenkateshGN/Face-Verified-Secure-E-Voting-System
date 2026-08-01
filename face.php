<?php
session_start();

/*
=====================================
SESSION CHECK
=====================================
*/
if (!isset($_SESSION['voter_id'])) {
    header("Location: index.php");
    exit();
}

/*
=====================================
STEP CONTROL
=====================================
*/
if (!isset($_SESSION['step'])) {
    $_SESSION['step'] = "face";
}

if ($_SESSION['step'] !== "face") {
    header("Location: index.php");
    exit();
}

/*
=====================================
EMERGENCY CAPTCHA KEY
=====================================
*/
if (!isset($_SESSION['captcha_key'])) {
    $_SESSION['captcha_key'] = rand(1000, 9999);
}
?>

<!DOCTYPE html>
<head>
    <title>Face Verification</title>
    <link rel="stylesheet" href="bower_components/bootstrap/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="dist/css/custom.css">
    <style>
        body {
            margin: 0;
            height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
        }

        video {
            width: 100%;
            border-radius: 12px;
            margin-bottom: 15px;
        }

        canvas {
            display: none;
        }

        .emergency {
            margin-top: 20px;
            padding-top: 15px;
            border-top: 1px solid var(--border-color) !important;
        }
    </style>
</head>

<body>

<div class="login-box" style="width: 450px !important;">

    <h2>Face Verification</h2>
    <br>

    <!-- CAMERA -->
    <video id="video" autoplay></video>
    <canvas id="canvas"></canvas>

    <!-- FORM 1: FACE -->
    <form method="POST" action="face_verify.php">
        <input type="hidden" name="csrf_token" value="<?php echo $_SESSION['csrf_token']; ?>">
        <input type="hidden" name="image" id="image">

        <button type="button" class="btn btn-warning btn-block" onclick="captureFace()" style="background-color: #f59e0b !important; border: none; color: #fff !important; font-weight: bold; border-radius: 12px; height: 46px; margin-bottom: 10px;">
            📸 Capture Face
        </button>

        <button type="submit" class="btn btn-primary btn-block" style="margin-bottom: 15px;">
            🔐 Verify Face
        </button>

    </form>

    <!-- EMERGENCY FALLBACK -->
    <div class="emergency">

        <h4>Emergency Access (Skip Face)</h4>

        <div class="captcha" style="margin-bottom: 15px; border-radius: 10px; padding: 10px; font-weight: bold;">
            Emergency Key: <?php echo $_SESSION['captcha_key']; ?>
        </div>

        <form method="POST" action="face_verify.php">
            <input type="hidden" name="csrf_token" value="<?php echo $_SESSION['csrf_token']; ?>">
            <input type="text" name="captcha_input" class="form-control" placeholder="Enter Emergency Key" required>

            <button type="submit" class="btn btn-primary btn-block" style="margin-top: 10px;">
                🚨 Skip Face & Continue
            </button>

        </form>

    </div>

    <div class="status" id="status" style="margin-top: 15px;"></div>

</div>

<script>
let video = document.getElementById("video");

navigator.mediaDevices.getUserMedia({ video: true })
.then(stream => {
    video.srcObject = stream;
})
.catch(err => {
    alert("Camera access denied");
});

function captureFace() {

    let canvas = document.getElementById("canvas");
    let ctx = canvas.getContext("2d");

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;

    ctx.drawImage(video, 0, 0);

    let imageData = canvas.toDataURL("image/png");

    document.getElementById("image").value = imageData;

    document.getElementById("status").innerText =
        "Face captured successfully ✔";
}
</script>

</body>
</html>