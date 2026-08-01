<?php
$conn = new mysqli('127.0.0.1', 'root', '', 'votesystem');

if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

if (session_status() === PHP_SESSION_NONE) {
    session_start();
}
if (!isset($_SESSION['csrf_token'])) {
    $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
}

if (!function_exists('verify_vote_integrity')) {
    function verify_vote_integrity($conn, $row) {
        $hash_ok = false;
        $decrypt_ok = false;
        $sig_ok = false;
        $original_candidate_id = null;
        $reasons = [];
        $details_parts = [];

        // 1. Hash Check
        $calculated_hash = hash('sha256',
            $row['voters_id'] .
            $row['candidate_id'] .
            $row['position_id'] .
            $row['encrypted_vote'] .
            $row['digital_signature']
        );
        if ($calculated_hash === $row['vote_hash']) {
            $hash_ok = true;
            $details_parts[] = "SHA256 Hash matches";
        } else {
            $reasons[] = "Hash mismatch";
            $details_parts[] = "Hash mismatch (Calc: " . substr($calculated_hash, 0, 8) . "... vs Stored: " . substr($row['vote_hash'], 0, 8) . "...)";
        }

        // 2. Decryption Check
        $secret_key = "ONLINE_VOTING_AES_SECRET_2026";
        $iv = substr($secret_key, 0, 16);
        $decrypted_candidate_id = openssl_decrypt(
            $row['encrypted_vote'],
            "AES-256-CBC",
            $secret_key,
            0,
            $iv
        );
        if ($decrypted_candidate_id !== false) {
            $original_candidate_id = $decrypted_candidate_id;
            if ($decrypted_candidate_id == $row['candidate_id']) {
                $decrypt_ok = true;
                $details_parts[] = "Candidate ID matches decrypted value";
            } else {
                $reasons[] = "Candidate ID mismatch (Cheated vote)";
                $details_parts[] = "Candidate mismatch (DB ID: " . $row['candidate_id'] . " vs Decrypted ID: " . $decrypted_candidate_id . ")";
            }
        } else {
            $reasons[] = "AES Decryption failed";
            $details_parts[] = "AES Decryption failed";
        }

        // 3. RSA Signature check using public key
        $pubkey_path = '';
        if (file_exists('keys/public.pem')) {
            $pubkey_path = 'keys/public.pem';
        } elseif (file_exists('../keys/public.pem')) {
            $pubkey_path = '../keys/public.pem';
        } elseif (file_exists('../../keys/public.pem')) {
            $pubkey_path = '../../keys/public.pem';
        }

        if ($pubkey_path) {
            $pubkey = file_get_contents($pubkey_path);
            $public_key = openssl_pkey_get_public($pubkey);
            if ($public_key) {
                $signature_bytes = base64_decode($row['digital_signature']);
                $ok = openssl_verify($row['encrypted_vote'], $signature_bytes, $public_key, OPENSSL_ALGO_SHA256);
                if ($ok === 1) {
                    $sig_ok = true;
                    $details_parts[] = "RSA Signature verified";
                } else {
                    $reasons[] = "RSA Signature failed";
                    $details_parts[] = "RSA Signature verification failed";
                }
            } else {
                $reasons[] = "Invalid public key format";
                $details_parts[] = "Invalid public key format";
            }
        } else {
            $reasons[] = "Public key file public.pem missing";
            $details_parts[] = "Public key file public.pem not found";
        }

        $valid = ($hash_ok && $decrypt_ok && $sig_ok);
        return [
            'valid' => $valid,
            'reason' => $valid ? 'Valid' : implode(', ', $reasons),
            'details' => implode('. ', $details_parts) . '.',
            'original_candidate_id' => $original_candidate_id
        ];
    }
}

if (!function_exists('get_candidate_name')) {
    function get_candidate_name($conn, $candidate_id) {
        if (!$candidate_id) return 'None';
        $sql = "SELECT firstname, lastname FROM candidates WHERE id = '" . mysqli_real_escape_string($conn, $candidate_id) . "'";
        $query = $conn->query($sql);
        if ($query && $query->num_rows > 0) {
            $row = $query->fetch_assoc();
            return $row['firstname'] . ' ' . $row['lastname'];
        }
        return 'Unknown Candidate (ID: ' . $candidate_id . ')';
    }
}

?>