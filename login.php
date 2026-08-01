<?php
session_start();
include __DIR__ . "/includes/conn.php";

if (!isset($_POST['login'])) {
    header("Location: index.php");
    exit();
}

// CSRF check
if (!isset($_POST['csrf_token']) || $_POST['csrf_token'] !== $_SESSION['csrf_token']) {
    $_SESSION['error'] = "CSRF verification failed. Request denied.";
    header("Location: index.php");
    exit();
}

// 1. INPUTS
$voter_id = $_POST['voter'];
$password = $_POST['password'];

// 2. CHECK EMPTY INPUTS
if (empty($voter_id) || empty($password)) {
    $_SESSION['error'] = "All fields are required";
    header("Location: index.php");
    exit();
}

// 3. GET VOTER FROM DATABASE (USING PREPARED STATEMENT)
$stmt = $conn->prepare("SELECT * FROM voters WHERE voters_id = ?");
$stmt->bind_param("s", $voter_id);
$stmt->execute();
$result = $stmt->get_result();

// 4. VALIDATE USER
if (!$result || $result->num_rows == 0) {
    $_SESSION['error'] = "Invalid Voter ID";
    header("Location: index.php");
    exit();
}

$user = $result->fetch_assoc();
$stmt->close();

// 5. PASSWORD CHECK
if (!password_verify($password, $user['password'])) {
    $_SESSION['error'] = "Incorrect Password";
    header("Location: index.php");
    exit();
}

// 6. START SESSION (LOGIN SUCCESS)
$_SESSION['voter_id'] = $voter_id;

// 7. SET NEXT STEP (VERY IMPORTANT)
$_SESSION['step'] = "face";

// 8. CLEAR OLD OTP (safety)
unset($_SESSION['otp']);
unset($_SESSION['otp_time']);

// 9. REDIRECT TO FACE VERIFICATION PAGE
header("Location: face.php");
exit();
?>