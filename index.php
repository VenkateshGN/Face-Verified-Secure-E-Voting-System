<?php
session_start();

if (!isset($_SESSION['csrf_token'])) {
    $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
}

// Admin redirect
if (isset($_SESSION['admin'])) {
    header('Location: admin/home.php');
    exit();
}

// Voter redirect after full process
if (isset($_SESSION['voter_id']) && isset($_SESSION['step']) && $_SESSION['step'] == "voted") {
    header('Location: home.php');
    exit();
}
?>

<?php include 'includes/header.php'; ?>

<body class="hold-transition login-page">

<div class="login-box">

    <div class="login-logo">
        <b>Online Voting System</b>
    </div>

    <div class="login-box-body">

        <p class="login-box-msg">
            Sign in to start your voting session
        </p>



        <form action="login.php" method="POST">
            <input type="hidden" name="csrf_token" value="<?php echo $_SESSION['csrf_token']; ?>">

            <input type="text"
                   name="voter"
                   class="form-control"
                   placeholder="Voter's ID"
                   required>

            <br>

            <input type="password"
                   name="password"
                   class="form-control"
                   placeholder="Password"
                   required>

            <br>

            <button type="submit"
                    name="login"
                    class="btn btn-primary btn-block">
                🔐 Sign In
            </button>

        </form>

    </div>
</div>

<?php include 'includes/scripts.php'; ?>

</body>
</html>