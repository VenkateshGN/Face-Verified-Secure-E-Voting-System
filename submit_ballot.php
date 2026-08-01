<?php

session_start();
include 'includes/conn.php';
include 'includes/slugify.php';

if (!isset($_SESSION['voter_id'])) {
    $_SESSION['error'][] = "Please login first";
    header("location:index.php");
    exit();
}

// CSRF check
if (!isset($_POST['csrf_token']) || $_POST['csrf_token'] !== $_SESSION['csrf_token']) {
    $_SESSION['error'][] = "CSRF verification failed. Request denied.";
    header("location:home.php");
    exit();
}

$voter_session_id = $_SESSION['voter_id'];

$stmt_voter = $conn->prepare("SELECT * FROM voters WHERE voters_id = ?");
$stmt_voter->bind_param("s", $voter_session_id);
$stmt_voter->execute();
$vquery = $stmt_voter->get_result();
$voter = $vquery->fetch_assoc();
$stmt_voter->close();

if (!$voter) {
    $_SESSION['error'][] = "Voter not found";
    header("location:index.php");
    exit();
}

if (!isset($_POST['vote'])) {
    $_SESSION['error'][] = "Select candidates first";
    header("location:home.php");
    exit();
}

/*
CHECK ALREADY VOTED
*/
$stmt_check_vote = $conn->prepare("SELECT * FROM votes WHERE voters_id = ?");
$stmt_check_vote->bind_param("i", $voter['id']);
$stmt_check_vote->execute();
$cquery = $stmt_check_vote->get_result();
$stmt_check_vote->close();

if ($cquery->num_rows > 0) {
    $_SESSION['error'][] = "You have already voted";
    header("location:home.php");
    exit();
}

$post = $_POST;
unset($post['vote']);
unset($post['csrf_token']); // remove csrf_token so it's not counted in votes

if (count($post) == 0) {
    $_SESSION['error'][] = "No vote selected";
    header("location:home.php");
    exit();
}

$sql_positions = "SELECT * FROM positions ORDER BY priority ASC";
$query = $conn->query($sql_positions);

$error = false;

$stmt_check = $conn->prepare("SELECT * FROM candidates WHERE id = ? AND position_id = ?");
$stmt_insert = $conn->prepare("INSERT INTO votes (voters_id, candidate_id, position_id, encrypted_vote, digital_signature, vote_hash) VALUES (?, ?, ?, ?, ?, ?)");
$stmt_audit = $conn->prepare("INSERT INTO audit_logs(action, hash_value) VALUES ('Vote Submitted', ?)");

while ($row = $query->fetch_assoc()) {

    $position = slugify($row['description']);
    $pos_id = $row['id'];

    if (!isset($_POST[$position])) continue;

    $selected = $_POST[$position];

    if (!is_array($selected)) {
        $selected = [$selected];
    }

    if (count($selected) > $row['max_vote']) {
        $error = true;
        $_SESSION['error'][] = "Max vote limit exceeded for " . $row['description'];
        continue;
    }

    foreach ($selected as $candidate_id) {

        $stmt_check->bind_param("ii", $candidate_id, $pos_id);
        $stmt_check->execute();
        $ccquery = $stmt_check->get_result();

        if ($ccquery->num_rows == 0) {
            $error = true;
            $_SESSION['error'][] = "Invalid candidate selected";
            continue;
        }

        /*
        =========================
        ENCRYPT VOTE
        =========================
        */
        $secret_key = "ONLINE_VOTING_AES_SECRET_2026";
        $iv = substr($secret_key, 0, 16);

        $encrypted_vote = openssl_encrypt(
            $candidate_id,
            "AES-256-CBC",
            $secret_key,
            0,
            $iv
        );

        /*
        =========================
        DIGITAL SIGNATURE
        =========================
        */
        $private_key = openssl_pkey_get_private(
            file_get_contents('keys/private.pem')
        );

        openssl_sign(
            $encrypted_vote,
            $signature,
            $private_key,
            OPENSSL_ALGO_SHA256
        );

        $digital_signature = base64_encode($signature);

        /*
        =========================
        FINAL VOTE HASH (IMPORTANT)
        =========================
        */
        $vote_hash = hash(
            'sha256',
            $voter['id'] .
            $candidate_id .
            $pos_id .
            $encrypted_vote .
            $digital_signature
        );

        /*
        =========================
        INSERT VOTE
        =========================
        */
        $stmt_insert->bind_param("iiisss", $voter['id'], $candidate_id, $pos_id, $encrypted_vote, $digital_signature, $vote_hash);
        $stmt_insert->execute();

        /*
        =========================
        AUDIT LOG
        =========================
        */
        $stmt_audit->bind_param("s", $vote_hash);
        $stmt_audit->execute();
    }
}

$stmt_check->close();
$stmt_insert->close();
$stmt_audit->close();

if (!$error) {
    $_SESSION['success'] = "Ballot Submitted Successfully";
}

header("location:home.php");
exit();

?>