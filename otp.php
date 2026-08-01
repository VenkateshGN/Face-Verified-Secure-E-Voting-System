<?php
session_start();

/*
========================================
STEP CHECK (must come from face.php)
========================================
*/
if (!isset($_SESSION['voter_id']) || $_SESSION['step'] !== "otp") {
    header("Location: index.php");
    exit();
}

/*
Set step properly if not set
(prevents redirect loops)
*/
if (!isset($_SESSION['step'])) {
    $_SESSION['step'] = "otp";
}

/*
Block direct access
*/
if ($_SESSION['step'] !== "otp") {
    header("Location: index.php");
    exit();
}

/*
========================================
GENERATE OTP ONLY ONCE
========================================
*/
if (!isset($_SESSION['otp'])) {
    $_SESSION['otp'] = rand(100000, 999999);
    $_SESSION['otp_time'] = time();
}

/*
========================================
OTP EXPIRY (5 minutes)
========================================
*/
$otp_expired = false;

if (isset($_SESSION['otp_time'])) {
    if (time() - $_SESSION['otp_time'] > 300) {
        $otp_expired = true;
    }
}

/*
========================================
HANDLE OTP SUBMIT
========================================
*/
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    // CSRF check
    if (!isset($_POST['csrf_token']) || $_POST['csrf_token'] !== $_SESSION['csrf_token']) {
        $_SESSION['error'] = "CSRF verification failed. Request denied.";
        header("Location: index.php");
        exit();
    }

    $entered_otp = $_POST['otp'];

    if ($otp_expired) {

        $_SESSION['error'] = "OTP expired. Please login again.";

        // clear session safely
        session_unset();
        session_destroy();

        header("Location: index.php");
        exit();
    }

    if ($entered_otp == $_SESSION['otp']) {

        /*
        SUCCESS → FINAL STEP
        */
        $_SESSION['step'] = "done";

        unset($_SESSION['otp']);
        unset($_SESSION['otp_time']);

        header("Location: home.php");
        exit();

    } else {
        $_SESSION['error'] = "Invalid OTP. Try again.";
        header("Location: otp.php");
        exit();
    }
}
?>

<!DOCTYPE html>
<html>
<head>
    <title>OTP Verification</title>
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
    </style>
</head>

<body class="hold-transition login-page">

<div class="login-box">

    <h2>OTP Verification</h2>
    <br>

    <p class="login-box-msg">
        Your OTP: <b style="color: var(--accent-color); font-size: 18px;"><?php echo $_SESSION['otp']; ?></b>
    </p>

    <?php if ($otp_expired): ?>
        <p class="error text-center" style="color: var(--danger-color);">OTP expired. Please login again.</p>
        <a href="index.php" class="btn btn-default btn-block">Go Back</a>
    <?php else: ?>

        <form method="POST">
            <input type="hidden" name="csrf_token" value="<?php echo $_SESSION['csrf_token']; ?>">
            <input type="text" name="otp" class="form-control text-center" placeholder="Enter OTP" required style="font-size: 18px; letter-spacing: 2px; height: 46px;">
            <button type="submit" class="btn btn-primary btn-block" style="margin-top: 15px;">Verify OTP</button>
        </form>

    <?php endif; ?>

</div>

<?php include 'includes/scripts.php'; ?>
</body>
</html>