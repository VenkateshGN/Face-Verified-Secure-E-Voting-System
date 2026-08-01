<?php
include 'includes/conn.php';

function getIP() {
    return $_SERVER['REMOTE_ADDR'];
}

$user_agent = isset($_SERVER['HTTP_USER_AGENT']) ? $_SERVER['HTTP_USER_AGENT'] : 'Unknown';
$ip = getIP();

// Perform verification on all votes, logging tampering to DB, and collecting status
$sql = "SELECT votes.id AS vote_id, votes.voters_id AS votes_voter_id, votes.candidate_id AS votes_candidate_id, votes.position_id AS votes_pos_id, votes.encrypted_vote, votes.digital_signature, votes.vote_hash, positions.description AS pos_desc, candidates.firstname AS canfirst, candidates.lastname AS canlast, voters.firstname AS votfirst, voters.lastname AS votlast FROM votes LEFT JOIN positions ON positions.id=votes.position_id LEFT JOIN candidates ON candidates.id=votes.candidate_id LEFT JOIN voters ON voters.id=votes.voters_id ORDER BY positions.priority ASC";
$result = $conn->query($sql);

$votes_verified = [];
$total_votes = 0;
$valid_votes = 0;
$tampered_votes = 0;

if ($result) {
    while ($row = $result->fetch_assoc()) {
        $total_votes++;
        $v_row = [
            'voters_id' => $row['votes_voter_id'],
            'candidate_id' => $row['votes_candidate_id'],
            'position_id' => $row['votes_pos_id'],
            'encrypted_vote' => $row['encrypted_vote'],
            'digital_signature' => $row['digital_signature'],
            'vote_hash' => $row['vote_hash']
        ];
        $verify = verify_vote_integrity($conn, $v_row);
        
        if ($verify['valid']) {
            $valid_votes++;
            $status = 'Valid';
        } else {
            $tampered_votes++;
            $status = 'Tampered';
            
            // Log to tamper_logs if not already logged
            $vote_id = $row['vote_id'];
            $voter_id = $row['votes_voter_id'];
            
            $check = $conn->query("SELECT id FROM tamper_logs WHERE vote_id='$vote_id'");
            if ($check && $check->num_rows == 0) {
                $conn->query("
                    INSERT INTO tamper_logs 
                    (voter_id, vote_id, action, ip_address, user_agent) 
                    VALUES 
                    ('$voter_id', '$vote_id', 'Vote Tampering Detected', '$ip', '$user_agent')
                ");
            }
        }
        
        $votes_verified[] = [
            'row' => $row,
            'verify' => $verify,
            'status' => $status
        ];
    }
}
?>
<?php include 'includes/header.php'; ?>
<body class="hold-transition login-page">
<div class="container" style="margin-top: 40px; max-width: 1000px;">
    
    <!-- Title and Header -->
    <div class="row">
        <div class="col-md-12 text-center" style="margin-bottom: 30px;">
            <h1 style="font-weight: bold; text-shadow: 0 0 20px rgba(217, 70, 239, 0.4);">
                🛡️ VOTE INTEGRITY & RSA CRYPTO AUDIT 🛡️
            </h1>
            <p style="color: #555; font-size: 16px;">
                Checking vote hashes, AES-256 decrypted candidate matches, and RSA digital signatures using public key cryptography.
            </p>
            <hr style="border-top: 2px solid #ccc; width: 100px; margin: 20px auto;">
        </div>
    </div>

    <!-- Alert / Summary -->
    <?php if ($tampered_votes > 0): ?>
        <div class="alert alert-danger" style="border-radius: 8px; font-size: 16px; border: 1px solid #721C24;">
            <h4><i class="icon fa fa-warning"></i> <b>CRITICAL WARNING: Tampering Detected!</b></h4>
            We detected <b><?php echo $tampered_votes; ?></b> tampered vote(s) out of <b><?php echo $total_votes; ?></b> total votes.
            All tampered votes have been automatically logged and excluded from the election results tallies.
        </div>
    <?php else: ?>
        <div class="alert alert-success" style="border-radius: 8px; font-size: 16px; border: 1px solid #1E4620; background-color: #D4EDDA; color: #155724;">
            <h4><i class="icon fa fa-check-circle"></i> <b>All Votes Intact!</b></h4>
            All <b><?php echo $total_votes; ?></b> votes are verified successfully. Zero anomalies detected.
        </div>
    <?php endif; ?>

    <!-- Summary Widgets -->
    <div class="row" style="margin-bottom: 20px;">
        <div class="col-md-4">
            <div class="well text-center" style="background-color: #fff; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.05);">
                <span class="fa fa-envelope-o" style="font-size: 32px; color: #666;"></span>
                <h3><?php echo $total_votes; ?></h3>
                <p><b>Total Votes Checked</b></p>
            </div>
        </div>
        <div class="col-md-4">
            <div class="well text-center" style="background-color: #fff; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.05);">
                <span class="fa fa-check-circle" style="font-size: 32px; color: #28a745;"></span>
                <h3><?php echo $valid_votes; ?></h3>
                <p><b>Valid (Original) Votes</b></p>
            </div>
        </div>
        <div class="col-md-4">
            <div class="well text-center" style="background-color: #fff; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.05);">
                <span class="fa fa-warning" style="font-size: 32px; color: #dc3545;"></span>
                <h3><?php echo $tampered_votes; ?></h3>
                <p><b>Tampered Votes</b></p>
            </div>
        </div>
    </div>

    <!-- Verification Table -->
    <div class="row">
        <div class="col-md-12">
            <div class="box box-solid" style="border-radius: 8px; background-color: #d8d1bd; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                <div class="box-header with-border" style="background-color: #d8d1bd; border-radius: 8px 8px 0 0; border-bottom: 1px solid #ccc;">
                    <h3 class="box-title" style="color: black; font-weight: bold;">Audit Trail Log</h3>
                </div>
                <div class="box-body table-responsive">
                    <table class="table table-bordered" style="background-color: #fff; border-radius: 4px; overflow: hidden;">
                        <thead>
                            <tr style="background-color: #f1f1f1; color: black;">
                                <th style="width: 80px;">Vote ID</th>
                                <th>Position</th>
                                <th>Voter</th>
                                <th>Stored Candidate</th>
                                <th>Decrypted Candidate</th>
                                <th>Status</th>
                                <th>Details</th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php foreach ($votes_verified as $v): 
                                $row = $v['row'];
                                $verify = $v['verify'];
                                $status = $v['status'];
                                
                                if ($status === 'Valid') {
                                    $tr_style = "style='background-color: #E2F0D9; color: black;'";
                                    $status_badge = "<span class='label label-success' style='padding: 3px 8px;'><i class='fa fa-check'></i> Original</span>";
                                    $decrypted_name = $row['canfirst'] . ' ' . $row['canlast'];
                                } else {
                                    $tr_style = "style='background-color: #FCE4D6; color: black;'";
                                    $status_badge = "<span class='label label-danger' style='padding: 3px 8px;'><i class='fa fa-warning'></i> Tampered</span>";
                                    if (isset($verify['original_candidate_id'])) {
                                        $decrypted_name = get_candidate_name($conn, $verify['original_candidate_id']);
                                    } else {
                                        $decrypted_name = "Corrupted/Decryption Fail";
                                    }
                                }
                            ?>
                                <tr <?php echo $tr_style; ?>>
                                    <td><?php echo $row['vote_id']; ?></td>
                                    <td><?php echo htmlspecialchars($row['pos_desc']); ?></td>
                                    <td><?php echo htmlspecialchars($row['votfirst'] . ' ' . $row['votlast']); ?></td>
                                    <td><b><?php echo htmlspecialchars($row['canfirst'] . ' ' . $row['canlast']); ?></b></td>
                                    <td><b><?php echo htmlspecialchars($decrypted_name); ?></b></td>
                                    <td><?php echo $status_badge; ?></td>
                                    <td><small><?php echo htmlspecialchars($verify['details']); ?></small></td>
                                </tr>
                            <?php endforeach; ?>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
    
    <div class="row text-center" style="margin-top: 20px; margin-bottom: 50px;">
        <div class="col-md-12">
            <a href="index.php" class="btn btn-default btn-curve" style="background-color: #FFDEAD; color: black;"><i class="fa fa-arrow-left"></i> Back to Login</a>
            <a href="admin/index.php" class="btn btn-primary btn-curve" style="background-color: #3c8dbc; color: white;"><i class="fa fa-lock"></i> Go to Admin Panel</a>
        </div>
    </div>

</div>

<?php include 'includes/scripts.php'; ?>
</body>
</html>