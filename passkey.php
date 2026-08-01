<?php
session_start();

if(!isset($_SESSION['voter_id'])){
    header("location: index.php");
    exit();
}
?>

<!DOCTYPE html>
<html>
<head>
    <title>Passkey Verification</title>

    <style>
        body{
            font-family: Arial;
            background:#f1e9d2;
            text-align:center;
        }

        .box{
            margin-top:100px;
            background:white;
            width:400px;
            margin:auto;
            padding:20px;
            border-radius:10px;
        }

        input, button{
            padding:10px;
            width:80%;
            margin-top:10px;
        }
    </style>
</head>

<body>

<div class="box">

    <h2>Backup Verification</h2>
    <p>Enter your passkey</p>

    <form action="passkey_verify.php" method="POST">

        <input type="text" name="passkey" placeholder="Enter Passkey" required>

        <button type="submit">Verify</button>

    </form>

</div>

</body>
</html>