<?php include 'includes/session.php'; ?>
<?php include 'includes/slugify.php'; ?>
<?php include 'includes/header.php'; ?>
<body class="hold-transition skin-blue sidebar-mini">
<div class="wrapper">

  <?php include 'includes/navbar.php'; ?>
  <?php include 'includes/menubar.php'; ?>

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <section class="content-header">
      <h1><b>
      Dashboard
      </b></h1>
      <ol class="breadcrumb">
        <li><a href="#"><i class="fa fa-dashboard" ></i> Home</a></li>
        <li class="active">Dashboard</li>
      </ol>
    </section>

    <!-- Main content -->
    <section class="content">
      <?php
        if(isset($_SESSION['error'])){
          echo "
            <div class='alert alert-danger alert-dismissible'>
              <button type='button' class='close' data-dismiss='alert' aria-hidden='true'>&times;</button>
              <h4><i class='icon fa fa-warning'></i> Error!</h4>
              ".$_SESSION['error']."
            </div>
          ";
          unset($_SESSION['error']);
        }
        if(isset($_SESSION['success'])){
          echo "
            <div class='alert alert-success alert-dismissible'>
              <button type='button' class='close' data-dismiss='alert' aria-hidden='true'>&times;</button>
              <h4><i class='icon fa fa-check'></i> Success!</h4>
              ".$_SESSION['success']."
            </div>
          ";
          unset($_SESSION['success']);
        }
      ?>
          <!-- Small boxes (Stat box) -->
      <div class="row">
        <div class="col-lg-3 col-xs-6">
          <!-- small box -->
          <div class="small-box">
            <div class="inner">
              <?php
                $sql = "SELECT * FROM positions";
                $query = $conn->query($sql);

                echo "<h3>".$query->num_rows."</h3>";
              ?>

              <p>No. of Positions</p>
            </div>
            <div class="icon">
              <i class="fa fa-cog"></i>
            </div>
            <a href="positions.php" class="small-box-footer">More info <i class="fa fa-arrow-right"></i></a>
          </div>
        </div>
        <!-- ./col -->
        <div class="col-lg-3 col-xs-6">
          <!-- small box -->
          <div class="small-box">
            <div class="inner">
              <?php
                $sql = "SELECT * FROM candidates";
                $query = $conn->query($sql);

                echo "<h3>".$query->num_rows."</h3>";
              ?>
          
              <p>No. of Candidates</p>
            </div>
            <div class="icon">
             <i class="fa fa-black-tie"></i>
            </div>
            <a href="candidates.php" class="small-box-footer">More info <i class="fa fa-arrow-right"></i></a>
          </div>
        </div>
        <!-- ./col -->
        <div class="col-lg-3 col-xs-6">
          <!-- small box -->
          <div class="small-box">
            <div class="inner">
              <?php
                $sql = "SELECT * FROM voters";
                $query = $conn->query($sql);

                echo "<h3>".$query->num_rows."</h3>";
              ?>
             
              <p>Total Voters</p>
            </div>
            <div class="icon">
              <i class="fa fa-users"></i>
            </div>
            <a href="voters.php" class="small-box-footer">More info <i class="fa fa-arrow-right"></i></a>
          </div>
        </div>
        <!-- ./col -->
        <div class="col-lg-3 col-xs-6">
          <!-- small box -->
          <div class="small-box">
            <div class="inner">
              <?php
                $sql = "SELECT * FROM votes GROUP BY voters_id";
                $query = $conn->query($sql);

                echo "<h3>".$query->num_rows."</h3>";
              ?>

              <p>Voters Voted</p>
            </div>
            <div class="icon">
              <i class="fa fa-edit"></i>
            </div>
            <a href="votes.php" class="small-box-footer">More info <i class="fa fa-arrow-right"></i></a>
          </div>
        </div></div>

      <!-- Main Dashboard Grid -->
      <div class="row" style="margin-top: 20px;">
        
        <!-- Left Column: Votes Tally Charts -->
        <div class="col-md-8">
          <div class="box box-solid">
            <div class="box-header with-border">
              <h3 class="box-title"><b>📊 Live Election Results (Votes Tally)</b></h3>
              <div class="box-tools pull-right">
                <a href="print.php" class="btn btn-success btn-xs btn-curve"><span class="glyphicon glyphicon-print"></span> Print Results</a>
              </div>
            </div>
            <div class="box-body" style="max-height: 480px; overflow-y: auto; padding: 20px;">
              <?php
                $sql = "SELECT * FROM positions ORDER BY priority ASC";
                $query = $conn->query($sql);
                while($row = $query->fetch_assoc()){
                  echo "
                    <div style='margin-bottom: 30px;'>
                      <h4 style='color: var(--text-primary); font-weight: 700; margin-bottom: 12px;'>".$row['description']."</h4>
                      <div class='chart' style='height: 160px; position: relative;'>
                        <canvas id='".slugify($row['description'])."'></canvas>
                      </div>
                    </div>
                  ";
                }
              ?>
            </div>
          </div>
        </div>

        <!-- Right Column: Security Auditing Log -->
        <div class="col-md-4">
          <div class="box">
            <div class="box-header with-border" style="background: rgba(239, 68, 68, 0.03) !important; border-bottom: 1px solid rgba(239, 68, 68, 0.1) !important;">
              <h3 class="box-title" style="color: var(--danger-color) !important;"><b>🚨 System Tamper Audit Log</b></h3>
            </div>
            <div class="box-body" style="max-height: 480px; overflow-y: auto; padding: 15px;">
              <div class="table-responsive">
                <table class="table table-condensed table-striped" style="font-size: 12px;">
                  <thead>
                    <tr>
                      <th>Voter</th>
                      <th>Database vs Decrypted</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    <?php
                      $sql = "SELECT votes.id AS vote_id, votes.voters_id AS votes_voter_id, votes.candidate_id AS votes_candidate_id, votes.position_id AS votes_pos_id, votes.encrypted_vote, votes.digital_signature, votes.vote_hash, positions.description AS pos_desc, candidates.firstname AS canfirst, candidates.lastname AS canlast, voters.firstname AS votfirst, voters.lastname AS votlast FROM votes LEFT JOIN positions ON positions.id=votes.position_id LEFT JOIN candidates ON candidates.id=votes.candidate_id LEFT JOIN voters ON voters.id=votes.voters_id";
                      $vquery = $conn->query($sql);
                      $tampered_found = false;
                      if ($vquery) {
                        while($vrow = $vquery->fetch_assoc()){
                          $v_row = [
                              'voters_id' => $vrow['votes_voter_id'],
                              'candidate_id' => $vrow['votes_candidate_id'],
                              'position_id' => $vrow['votes_pos_id'],
                              'encrypted_vote' => $vrow['encrypted_vote'],
                              'digital_signature' => $vrow['digital_signature'],
                              'vote_hash' => $vrow['vote_hash']
                          ];
                          $verify = verify_vote_integrity($conn, $v_row);
                          if(!$verify['valid']){
                            $tampered_found = true;
                            if(isset($verify['original_candidate_id'])){
                              $original_name = get_candidate_name($conn, $verify['original_candidate_id']);
                            } else {
                              $original_name = "Corrupted Payload";
                            }
                            echo "
                              <tr class='bg-danger' style='color: var(--danger-color);'>
                                <td><b>".htmlspecialchars($vrow['votfirst'].' '.$vrow['votlast'])."</b></td>
                                <td>DB: ".htmlspecialchars($vrow['canfirst'].' '.$vrow['canlast'])."<br>Decrypted: ".htmlspecialchars($original_name)."</td>
                                <td><span class='label label-danger'>TAMPERED</span></td>
                              </tr>
                            ";
                          }
                        }
                      }
                      if(!$tampered_found){
                        echo "
                          <tr>
                            <td colspan='3' class='text-center text-success' style='padding: 40px 10px; font-size: 13px;'>
                              <i class='fa fa-check-circle' style='font-size: 32px;'></i><br><br><b>All votes verified. No database tampering detected.</b>
                            </td>
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

      </div>

      </section>
      <!-- right col -->
    </div>
  	<?php include 'includes/footer.php'; ?>

</div>
<!-- ./wrapper -->

<?php include 'includes/scripts.php'; ?>
<?php
  $sql = "SELECT * FROM positions ORDER BY priority ASC";
  $query = $conn->query($sql);
  while($row = $query->fetch_assoc()){
    $sql = "SELECT * FROM candidates WHERE position_id = '".$row['id']."'";
    $cquery = $conn->query($sql);
    $carray = array();
    $rarray = array(); // Stored / DB counts (raw)
    $oarray = array(); // Original decrypted counts (before tampering)
    while($crow = $cquery->fetch_assoc()){
      array_push($carray, $crow['lastname']);
      
      // 1. Calculate Stored / DB counts (raw count in DB)
      $sql = "SELECT COUNT(*) AS raw_count FROM votes WHERE candidate_id = '".$crow['id']."'";
      $rquery = $conn->query($sql);
      $rrow = $rquery->fetch_assoc();
      $raw_count = $rrow['raw_count'];
      array_push($rarray, $raw_count);

      // 2. Calculate Original counts (decrypted from encrypted vote payload)
      $sql = "SELECT voters_id, candidate_id, position_id, encrypted_vote, digital_signature, vote_hash FROM votes";
      $vquery = $conn->query($sql);
      $original_count = 0;
      if ($vquery) {
        while($vrow = $vquery->fetch_assoc()){
          $verify = verify_vote_integrity($conn, $vrow);
          if (isset($verify['original_candidate_id']) && $verify['original_candidate_id'] == $crow['id']) {
            $original_count++;
          }
        }
      }
      array_push($oarray, $original_count);
    }
    $carray = json_encode($carray);
    $rarray = json_encode($rarray);
    $oarray = json_encode($oarray);
    ?>
    <script>
    $(function(){
      var description = '<?php echo slugify($row['description']); ?>';
      var ctx = document.getElementById(description).getContext('2d');
      new Chart(ctx, {
        type: 'bar',
        data: {
          labels: <?php echo $carray; ?>,
          datasets: [
            {
              label: 'Stored / DB Votes (With Tampering)',
              data: <?php echo $rarray; ?>,
              backgroundColor: 'rgba(239, 68, 68, 0.85)',
              borderColor: '#ef4444',
              borderWidth: 1
            },
            {
              label: 'Original Votes (Before Tampering)',
              data: <?php echo $oarray; ?>,
              backgroundColor: 'rgba(16, 185, 129, 0.85)',
              borderColor: '#10b981',
              borderWidth: 1
            }
          ]
        },
        options: {
          indexAxis: 'y',
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              position: 'top',
              labels: {
                font: {
                  family: "'Outfit', sans-serif",
                  size: 12
                }
              }
            }
          },
          scales: {
            x: {
              beginAtZero: true,
              ticks: {
                precision: 0
              }
            }
          }
        }
      });
    });
    </script>
    <?php
  }
?>
</body>
</html>
