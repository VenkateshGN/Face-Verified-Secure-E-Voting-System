<?php
include 'includes/session.php';

if(isset($_POST['add'])){
    // CSRF check
    if(!isset($_POST['csrf_token']) || $_POST['csrf_token'] !== $_SESSION['csrf_token']){
        $_SESSION['error'] = 'CSRF verification failed. Request denied.';
        header('location: candidates.php');
        exit();
    }

    $firstname = $_POST['firstname'];
    $lastname = $_POST['lastname'];
    // USER ENTERS MLA / MP
    $position = trim($_POST['position']);
    $platform = $_POST['platform'];

    /*
    ========================
    FIND POSITION ID
    ========================
    */
    $stmt = $conn->prepare("SELECT * FROM positions WHERE description = ?");
    $stmt->bind_param("s", $position);
    $stmt->execute();
    $pquery = $stmt->get_result();

    if($pquery->num_rows == 0){
        $_SESSION['error'] = 'Position does not exist';
        $stmt->close();
        header('location: candidates.php');
        exit();
    }

    $prow = $pquery->fetch_assoc();
    $position_id = $prow['id'];
    $stmt->close();

    /*
    ========================
    PHOTO UPLOAD
    ========================
    */
    $photo = '';

    if(!empty($_FILES['photo']['name'])){
        $photo = time().'_'.$_FILES['photo']['name'];
        move_uploaded_file(
            $_FILES['photo']['tmp_name'],
            '../images/'.$photo
        );
    }

    /*
    ========================
    SYMBOL UPLOAD
    ========================
    */
    $symbol = '';

    if(!empty($_FILES['symbol']['name'])){
        $symbol = time().'_symbol_'.$_FILES['symbol']['name'];
        move_uploaded_file(
            $_FILES['symbol']['tmp_name'],
            '../images/'.$symbol
        );
    }

    /*
    ========================
    INSERT CANDIDATE
    ========================
    */
    $stmt = $conn->prepare("INSERT INTO candidates (position_id, firstname, lastname, photo, platform, symbol) VALUES (?, ?, ?, ?, ?, ?)");
    $stmt->bind_param("isssss", $position_id, $firstname, $lastname, $photo, $platform, $symbol);

    if($stmt->execute()){
        $_SESSION['success'] = 'Candidate added successfully';
    }
    else{
        $_SESSION['error'] = $stmt->error;
    }
    $stmt->close();

}
else{
    $_SESSION['error'] = 'Fill up form first';
}

header('location: candidates.php');
?>