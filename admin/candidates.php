<?php include 'includes/session.php'; ?>
<?php include 'includes/header.php'; ?>

<body class="hold-transition skin-blue sidebar-mini">
<div class="wrapper">

<?php include 'includes/navbar.php'; ?>
<?php include 'includes/menubar.php'; ?>

<div class="content-wrapper" style="background-color:#F1E9D2">

<section class="content-header">
  <h1><b>Candidates List</b></h1>
</section>

<section class="content">



<div class="box" style="background-color:#d8d1bd">

  <div class="box-header">
    <a href="#addnew" data-toggle="modal" class="btn btn-primary btn-sm">
      <i class="fa fa-plus"></i> New
    </a>
  </div>

  <div class="box-body">

    <table id="example1" class="table table-bordered table-striped">
      <thead>
        <tr>
          <th>Position</th>
          <th>Photo</th>
          <th>Symbol</th>
          <th>Firstname</th>
          <th>Lastname</th>
          <th>Platform</th>
          <th>Tools</th>
        </tr>
      </thead>

      <tbody>

      <?php
      // ✅ FIXED QUERY (IMPORTANT)
      $sql = "SELECT candidates.*, positions.description AS position_name
              FROM candidates
              LEFT JOIN positions ON positions.id = candidates.position_id
              ORDER BY candidates.id DESC";

      $query = $conn->query($sql);

      while($row = $query->fetch_assoc()){

        // PHOTO
        $photo = (!empty($row['photo']))
          ? '../images/'.$row['photo']
          : '../images/profile.jpg';

        // SYMBOL
        $symbol = (!empty($row['symbol']))
          ? '../images/'.$row['symbol']
          : '../images/profile.jpg';

        echo "
        <tr style='font-family:Times;font-size:15px;color:black'>

          <td>".$row['position_name']."</td>

          <td>
            <img src='".$photo."' width='40' height='40' style='border-radius:50%'>
          </td>

          <td>
            <img src='".$symbol."' width='40' height='40' style='border-radius:50%'>
          </td>

          <td>".$row['firstname']."</td>
          <td>".$row['lastname']."</td>

          <td>
            <a href='#platform' data-toggle='modal'
               class='btn btn-info btn-sm platform'
               data-id='".$row['id']."'>
              View
            </a>
          </td>

          <td>
            <button class='btn btn-success btn-sm edit'
              data-id='".$row['id']."'>Edit</button>

            <button class='btn btn-danger btn-sm delete'
              data-id='".$row['id']."'>Delete</button>
          </td>

        </tr>
        ";
      }
      ?>

      </tbody>
    </table>

  </div>
</div>

</section>
</div>

<?php include 'includes/footer.php'; ?>
<?php include 'includes/candidates_modal.php'; ?>
</div>

<?php include 'includes/scripts.php'; ?>

<script>
$(function(){

  $(document).on('click', '.edit', function(){
    $('#edit').modal('show');
    getRow($(this).data('id'));
  });

  $(document).on('click', '.delete', function(){
    $('#delete').modal('show');
    getRow($(this).data('id'));
  });

  $(document).on('click', '.platform', function(){
    $('#platform').modal('show');
    getRow($(this).data('id'));
  });

});

function getRow(id){
  $.ajax({
    type: 'POST',
    url: 'candidates_row.php',
    data: {id:id},
    dataType: 'json',
    success: function(response){

      $('.id').val(response.id);
      $('#edit_firstname').val(response.firstname);
      $('#edit_lastname').val(response.lastname);
      $('#edit_position').val(response.position_id);
      $('#edit_platform').val(response.platform);

      $('.fullname').html(response.firstname + " " + response.lastname);
      $('#desc').html(response.platform);
    }
  });
}
</script>

</body>
</html>