<!-- SweetAlert2 -->
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<!-- jQuery 3 -->
<script src="bower_components/jquery/dist/jquery.min.js"></script>
<!-- Bootstrap 3.3.7 -->
<script src="bower_components/bootstrap/dist/js/bootstrap.min.js"></script>
<!-- iCheck 1.0.1 -->
<script src="plugins/iCheck/icheck.min.js"></script>
<!-- DataTables -->
<script src="bower_components/datatables.net/js/jquery.dataTables.min.js"></script>
<script src="bower_components/datatables.net-bs/js/dataTables.bootstrap.min.js"></script>
<!-- SlimScroll -->
<script src="bower_components/jquery-slimscroll/jquery.slimscroll.min.js"></script>
<!-- FastClick -->
<script src="bower_components/fastclick/lib/fastclick.js"></script>
<!-- AdminLTE App -->
<script src="dist/js/adminlte.min.js"></script>
<!-- Data Table Initialize -->
<script>
  $(function () {
    $('#example1').DataTable()
  	var bookTable = $('#booklist').DataTable({
      'paging'      : true,
      'lengthChange': false,
      'searching'   : true,
      'ordering'    : true,
      'info'        : false,
      'autoWidth'   : false
    })

    $('#searchBox').on('keyup', function(){
    	bookTable.search(this.value).draw();
	});

  })
</script>

<?php
if (isset($_SESSION['error'])) {
    $err_msg = is_array($_SESSION['error']) ? implode('<br>', $_SESSION['error']) : $_SESSION['error'];
    echo "
    <script>
    Swal.fire({
        icon: 'error',
        title: 'Error',
        html: '" . addslashes(nl2br($err_msg)) . "',
        confirmButtonColor: '#3085d6'
    });
    </script>
    ";
    unset($_SESSION['error']);
}
if (isset($_SESSION['success'])) {
    echo "
    <script>
    Swal.fire({
        icon: 'success',
        title: 'Success!',
        text: '" . addslashes($_SESSION['success']) . "',
        confirmButtonColor: '#3085d6'
    });
    </script>
    ";
    unset($_SESSION['success']);
}
?>