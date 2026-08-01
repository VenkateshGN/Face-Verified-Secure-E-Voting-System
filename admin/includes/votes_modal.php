<!-- Reset -->
<div class="modal fade" id="reset">
    <div class="modal-dialog">
        <div class="modal-content" style="background-color: #d8d1bd ;color:black ; font-size: 15px; font-family:Times ">
            <div class="modal-header">
              <button type="button" class=" btn btn-close btn-curve pull-right" data-dismiss="modal" aria-label="Close">
                  <span aria-hidden="true">&times;</span></button>
              <h4 class="modal-title"><b>Reseting...</b></h4>
            </div>
            <form method="POST" action="votes_reset.php">
                <input type="hidden" name="csrf_token" value="<?php echo $_SESSION['csrf_token']; ?>">
                <div class="modal-body">
                  <div class="text-center">
                      <p>RESET VOTES</p>
                      <h4>This will delete all votes and counting back to 0.</h4>
                  </div>
                </div>
                <div class="modal-footer">
                  <button type="button" class="btn btn-default btn-curve pull-left"style='background-color:  #FFDEAD  ;color:black ; font-size: 12px; font-family:Times' data-dismiss="modal"><i class="fa fa-close"></i> Close</button>
                  <button type="submit" name="reset" class="btn btn-danger btn-curve"style='background-color:  #ff8e88  ;color:black ; font-size: 12px; font-family:Times' ><i class="fa fa-refresh"></i> Reset</button>
                </div>
            </form>
        </div>
    </div>
</div>