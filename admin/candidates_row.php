<?php 
include 'includes/session.php';

if(isset($_POST['id'])){

    $id = $_POST['id'];

    $sql = "SELECT candidates.*, candidates.id AS canid, positions.description 
            FROM candidates 
            LEFT JOIN positions ON positions.id = candidates.position_id 
            WHERE candidates.id = '$id'";

    $query = $conn->query($sql);

    if($query && $query->num_rows > 0){

        $row = $query->fetch_assoc();

        /* =========================
           FIX PHOTO PATH
        ========================= */
        $row['photo'] = (!empty($row['photo']))
            ? '../images/' . $row['photo']
            : '../images/profile.jpg';

        /* =========================
           FIX SYMBOL PATH
        ========================= */
        $row['symbol'] = (!empty($row['symbol']))
            ? '../images/' . $row['symbol']
            : '../images/profile.jpg';

        /* =========================
           RETURN JSON
        ========================= */
        echo json_encode($row);

    } else {

        echo json_encode([
            "error" => true,
            "message" => "Candidate not found"
        ]);
    }
}
?>