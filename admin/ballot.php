<?php include 'includes/session.php'; ?>
<?php include 'includes/header.php'; ?>

<body class="hold-transition skin-blue sidebar-mini">
<div class="wrapper">

  <?php include 'includes/navbar.php'; ?>
  <?php include 'includes/menubar.php'; ?>

  <!-- Content Wrapper -->
  <div class="content-wrapper" style="background-color:#F1E9D2">

    <!-- Header -->
    <section class="content-header">
      <h1><b>Ballot Position</b></h1>

      <ol class="breadcrumb" style="color:black;font-size:17px;font-family:Times">
        <li><a href="#"><i class="fa fa-dashboard"></i> Home</a></li>
        <li class="active">Dashboard</li>
      </ol>
    </section>

    <!-- Content -->
    <section class="content">

      <?php
        if(isset($_SESSION['error'])){
          echo "
            <div class='alert alert-danger alert-dismissible'>
              <button type='button' class='close' data-dismiss='alert'>&times;</button>
              <b>Error!</b> ".$_SESSION['error']."
            </div>
          ";
          unset($_SESSION['error']);
        }

        if(isset($_SESSION['success'])){
          echo "
            <div class='alert alert-success alert-dismissible'>
              <button type='button' class='close' data-dismiss='alert'>&times;</button>
              <b>Success!</b> ".$_SESSION['success']."
            </div>
          ";
          unset($_SESSION['success']);
        }
      ?>

      <div class="row">
        <div class="col-xs-10 col-xs-offset-1" id="content">
          <!-- AJAX LOADS HERE -->
        </div>
      </div>

    </section>

  </div>

  <?php include 'includes/footer.php'; ?>
</div>

<?php include 'includes/scripts.php'; ?>

<script>
$(function(){

  fetch();

  // RESET SELECTION
  $(document).on('click', '.reset', function(e){
    e.preventDefault();
    var desc = $(this).data('desc');
    $('.'+desc).prop('checked', false);
  });

  // MOVE UP
  $(document).on('click', '.moveup', function(e){
    e.preventDefault();
    var id = $(this).data('id');

    $('#'+id).animate({ marginTop: "-100px" });

    $.ajax({
      type: 'POST',
      url: 'ballot_up.php',
      data: {id:id},
      dataType: 'json',
      success: function(res){
        fetch();
      }
    });
  });

  // MOVE DOWN
  $(document).on('click', '.movedown', function(e){
    e.preventDefault();
    var id = $(this).data('id');

    $('#'+id).animate({ marginTop: "+100px" });

    $.ajax({
      type: 'POST',
      url: 'ballot_down.php',
      data: {id:id},
      dataType: 'json',
      success: function(res){
        fetch();
      }
    });
  });

});

// LOAD BALLOT
function fetch(){
  $.ajax({
    type: 'POST',
    url: 'ballot_fetch.php',

    // IMPORTANT FIX:
    dataType: 'html',

    success: function(response){
      $('#content').html(response);

      // iCheck styling
      $('#content input').iCheck({
        checkboxClass: 'icheckbox_flat-green',
        radioClass: 'iradio_flat-green'
      });
    }
  });
}
</script>

</body>
</html>