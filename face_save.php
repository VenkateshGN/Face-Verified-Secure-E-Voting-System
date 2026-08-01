<?php
session_start();
include "includes/conn.php";

// CHECK LOGIN STEP
if(!isset($_SESSION['voter_id'])){
    echo "NO_MATCH";
    exit();
}

// READ JSON IMAGE FROM FRONTEND
$data = json_decode(file_get_contents("php://input"), true);

if(!isset($data['image'])){
    echo "NO_MATCH";
    exit();
}

// REMOVE BASE64 PREFIX
$image = $data['image'];
$image = str_replace('data:image/png;base64,', '', $image);
$image = str_replace(' ', '+', $image);

// SAVE CAPTURED IMAGE
$imgData = base64_decode($image);

$capturedPath = "captured_faces/live.jpg";
file_put_contents($capturedPath, $imgData);

// GET VOTER IMAGE FROM DB
$voter_id = $_SESSION['voter_id'];

$sql = "SELECT photo FROM voters WHERE voters_id='$voter_id'";
$result = mysqli_query($conn, $sql);

if(!$result || mysqli_num_rows($result) == 0){
    echo "NO_MATCH";
    exit();
}

$row = mysqli_fetch_assoc($result);

$registeredImage = "images/" . $row['photo'];

// CHECK FILE EXISTS
if(!file_exists($registeredImage)){
    echo "NO_MATCH";
    exit();
}

// CALL PYTHON FACE MODEL
$python = "python";
$script = "face_verify.py";

$command = "$python $script $registeredImage $capturedPath";

$output = shell_exec($command);

// CLEAN OUTPUT
$output = trim($output);

// RETURN RESULT ONLY
if($output == "MATCH"){
    echo "MATCH";
} else {
    echo "NO_MATCH";
}
?>