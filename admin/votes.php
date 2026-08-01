<?php include 'includes/session.php'; ?>
<?php include 'includes/header.php'; ?>
<body class="hold-transition skin-blue sidebar-mini">
<div class="wrapper">

  <?php include 'includes/navbar.php'; ?>
  <?php include 'includes/menubar.php'; ?>

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <section class="content-header">
      <h1>
        VOTES
      </h1>
      <ol class="breadcrumb">
        <li><a href="#"><i class="fa fa-dashboard" ></i> Home</a></li>
        <li class="active">Dashboard</li>
      </ol>
    </section>
    <!-- Main content -->
    <section class="content">

      <?php
        // Fetch all votes for counts
        $total_votes_count = 0;
        $original_votes_count = 0;
        $tampered_votes_count = 0;

        $sql = "SELECT voters_id, candidate_id, position_id, encrypted_vote, digital_signature, vote_hash FROM votes";
        $count_query = $conn->query($sql);
        if ($count_query) {
            while ($c_row = $count_query->fetch_assoc()) {
                $total_votes_count++;
                $verify = verify_vote_integrity($conn, $c_row);
                if ($verify['valid']) {
                    $original_votes_count++;
                } else {
                    $tampered_votes_count++;
                }
            }
        }
      ?>
      <!-- Summary Widgets -->
      <div class="row" style="font-family:Times;">
        <div class="col-lg-4 col-xs-6">
          <div class="small-box" style="background-color: #B0C4DE; color: black; border-radius: 8px;">
            <div class="inner" style="padding: 15px;">
              <h3><?php echo $total_votes_count; ?></h3>
              <p><b>Total Votes Cast</b></p>
            </div>
            <div class="icon" style="top: 10px; right: 15px;">
              <i class="fa fa-envelope-o" style="font-size: 70px; opacity: 0.15;"></i>
            </div>
          </div>
        </div>
        <div class="col-lg-4 col-xs-6">
          <div class="small-box" style="background-color: #C2E0C6; color: #1E4620; border-radius: 8px;">
            <div class="inner" style="padding: 15px;">
              <h3><?php echo $original_votes_count; ?></h3>
              <p><b>Original (Valid) Votes</b></p>
            </div>
            <div class="icon" style="top: 10px; right: 15px;">
              <i class="fa fa-check-circle-o" style="font-size: 70px; opacity: 0.15;"></i>
            </div>
          </div>
        </div>
        <div class="col-lg-4 col-xs-6">
          <div class="small-box" style="background-color: rgba(239, 68, 68, 0.1) !important; color: var(--danger-color) !important; border: 1px solid rgba(239, 68, 68, 0.2) !important;">
            <div class="inner" style="padding: 15px;">
              <h3 style="color: var(--danger-color) !important; font-weight: 800 !important; font-size: 38px !important; margin: 0 0 10px 0 !important;"><?php echo $tampered_votes_count; ?></h3>
              <p style="color: var(--danger-color) !important; font-weight: 600 !important;">Tampered (Invalid) Votes</p>
            </div>
            <div class="icon" style="top: 10px; right: 15px; color: rgba(239, 68, 68, 0.1) !important;">
              <i class="fa fa-warning" style="font-size: 70px;"></i>
            </div>
          </div>
        </div>
      </div>

      <div class="row">
        <div class="col-xs-12">
          <div class="box">
            <div class="box-header with-border">
              <a href="#reset" data-toggle="modal" class="btn btn-danger btn-sm btn-curve"><i class="fa fa-refresh"></i> Reset</a>
            </div>
            <div class="box-body">
              <table id="example1" class="table table-bordered">
                <thead>
                  <th class="hidden"></th>
                  <th>Position</th>
                  <th>Voter</th>
                  <th>Stored Candidate</th>
                  <th>Original Candidate (Decrypted)</th>
                  <th>Status</th>
                  <th>Verification Details</th>
                </thead>
                <tbody>
                  <?php
                    $sql = "SELECT votes.id AS vote_id, votes.voters_id AS votes_voter_id, votes.candidate_id AS votes_candidate_id, votes.position_id AS votes_pos_id, votes.encrypted_vote, votes.digital_signature, votes.vote_hash, positions.description AS pos_desc, candidates.firstname AS canfirst, candidates.lastname AS canlast, voters.firstname AS votfirst, voters.lastname AS votlast FROM votes LEFT JOIN positions ON positions.id=votes.position_id LEFT JOIN candidates ON candidates.id=votes.candidate_id LEFT JOIN voters ON voters.id=votes.voters_id ORDER BY positions.priority ASC";
                    $query = $conn->query($sql);
                    while($row = $query->fetch_assoc()){
                      $v_row = [
                          'voters_id' => $row['votes_voter_id'],
                          'candidate_id' => $row['votes_candidate_id'],
                          'position_id' => $row['votes_pos_id'],
                          'encrypted_vote' => $row['encrypted_vote'],
                          'digital_signature' => $row['digital_signature'],
                          'vote_hash' => $row['vote_hash']
                      ];
                      $verify = verify_vote_integrity($conn, $v_row);
                      
                      if($verify['valid']){
                          $status_label = "<span class='label label-success' style='font-size: 11px; padding: 3px 8px; border-radius: 4px;'><i class='fa fa-check-circle'></i> Original</span>";
                          $row_style = "style='color:black ; font-size: 14px; font-family:Times; background-color: #E2F0D9;'";
                          $original_candidate_name = $row['canfirst'].' '.$row['canlast'];
                      } else {
                          $status_label = "<span class='label label-danger' style='font-size: 11px; padding: 3px 8px; border-radius: 4px;'><i class='fa fa-warning'></i> Tampered</span>";
                          $row_style = "style='color:black ; font-size: 14px; font-family:Times; background-color: #FCE4D6;'";
                          if(isset($verify['original_candidate_id'])){
                              $original_candidate_name = get_candidate_name($conn, $verify['original_candidate_id']);
                          } else {
                              $original_candidate_name = "<span class='text-danger'><b>Corrupted Payload</b></span>";
                          }
                      }
                      
                      echo "
                        <tr ".$row_style.">
                          <td class='hidden'></td>
                          <td>".htmlspecialchars($row['pos_desc'])."</td>
                          <td>".htmlspecialchars($row['votfirst'].' '.$row['votlast'])."</td>
                          <td><b>".htmlspecialchars($row['canfirst'].' '.$row['canlast'])."</b></td>
                          <td><b>".htmlspecialchars($original_candidate_name)."</b></td>
                          <td>".$status_label."</td>
                          <td><small>".htmlspecialchars($verify['details'])."</small></td>
                        </tr>
                      ";
                    }
                  ?>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </section>   
  </div>
    
  <?php include 'includes/footer.php'; ?>
  <?php include 'includes/votes_modal.php'; ?>
</div>
<?php include 'includes/scripts.php'; ?>
</body>
</html>
