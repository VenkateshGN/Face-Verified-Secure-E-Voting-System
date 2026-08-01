<?php
session_start();
include "includes/conn.php";

if(!isset($_SESSION['voter_id'])){
    echo "FAIL";
    exit();
}

// GET INPUT
$passkey = $_POST['passkey'] ?? '';

if(empty($passkey)){
    echo "FAIL";
    exit();
}

$voter_id = $_SESSION['voter_id'];

// CHECK PASSKEY IN DB
$sql = "SELECT passkey FROM voters WHERE voters_id='$voter_id'";
$result = mysqli_query($conn, $sql);

if(!$result || mysqli_num_rows($result) == 0){
    echo "FAIL";
    exit();
}

$row = mysqli_fetch_assoc($result);

// VERIFY
if($row['passkey'] === $passkey){
    echo "OK";
} else {
    echo "FAIL";
}
?>