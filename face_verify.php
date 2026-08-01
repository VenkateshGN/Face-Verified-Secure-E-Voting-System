<?php
session_start();
include 'includes/conn.php';

if (!isset($_SESSION['voter_id'])) {
    header("Location: index.php");
    exit();
}

// CSRF check
if (!isset($_POST['csrf_token']) || $_POST['csrf_token'] !== $_SESSION['csrf_token']) {
    $_SESSION['error'] = 'CSRF verification failed. Request denied.';
    header("Location: face.php");
    exit();
}

/*
=====================================
EMERGENCY KEY CHECK
=====================================
*/
if (isset($_POST['captcha_input'])) {
    if ($_POST['captcha_input'] == $_SESSION['captcha_key']) {
        $_SESSION['step'] = "otp";
        header("Location: otp.php");
        exit();
    } else {
        $_SESSION['error'] = "Invalid Emergency Key";
        header("Location: face.php");
        exit();
    }
}

/*
=====================================
FACE FLOW
=====================================
*/
if (!isset($_POST['image']) || empty($_POST['image'])) {
    $_SESSION['error'] = "Please capture your face first.";
    header("Location: face.php");
    exit();
}

// 1. Decode captured webcam image
$image = $_POST['image'];
$image = str_replace('data:image/png;base64,', '', $image);
$image = str_replace(' ', '+', $image);
$imgData = base64_decode($image);

if (!$imgData) {
    $_SESSION['error'] = "Failed to decode captured face image.";
    header("Location: face.php");
    exit();
}

// Ensure captured_faces directory exists
if (!is_dir('captured_faces')) {
    mkdir('captured_faces', 0777, true);
}

// Save the captured image to a file
$voter_id = $_SESSION['voter_id'];
$capturedPath = "captured_faces/" . $voter_id . ".png";
file_put_contents($capturedPath, $imgData);

// 2. Fetch registered photo from DB (USING PREPARED STATEMENT)
$stmt = $conn->prepare("SELECT photo FROM voters WHERE voters_id = ?");
$stmt->bind_param("s", $voter_id);
$stmt->execute();
$result = $stmt->get_result();

if (!$result || $result->num_rows == 0) {
    $_SESSION['error'] = "Voter record not found.";
    $stmt->close();
    header("Location: face.php");
    exit();
}

$row = $result->fetch_assoc();
$stmt->close();
$registeredImage = "images/" . $row['photo'];

// Validate that registered image exists
if (empty($row['photo']) || !file_exists($registeredImage)) {
    $_SESSION['error'] = "Registered photo not found. Please contact admin.";
    header("Location: face.php");
    exit();
}

// 3. Execute AI face verification script
$python = "python";
$script = "face_verify.py";
$command = escapeshellcmd("$python $script " . escapeshellarg($registeredImage) . " " . escapeshellarg($capturedPath));

$output = shell_exec($command);
$output = trim($output);

if ($output === "MATCH") {
    // Face matched successfully, clean up captured image
    if (file_exists($capturedPath)) {
        unlink($capturedPath);
    }
    $_SESSION['step'] = "otp";
    header("Location: otp.php");
    exit();
} else {
    $_SESSION['error'] = "Face verification failed. Please try again.";
    header("Location: face.php");
    exit();
}
?>