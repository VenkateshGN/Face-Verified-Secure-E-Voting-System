<?php include 'includes/session.php'; ?>

<!-- VIEW PLATFORM -->
<div class="modal fade" id="platform">
    <div class="modal-dialog">
        <div class="modal-content" style="background-color:#d8d1bd;color:black;font-size:15px;font-family:Times">

            <div class="modal-header">
                <button type="button" class="btn btn-close btn-curve pull-right" data-dismiss="modal">
                    <span aria-hidden="true">&times;</span>
                </button>
                <h4 class="modal-title"><b><span class="fullname"></span></b></h4>
            </div>

            <div class="modal-body">
                <h4><b>Platform:</b></h4>
                <p id="desc"></p>
            </div>

        </div>
    </div>
</div>

<!-- ADD CANDIDATE -->
<div class="modal fade" id="addnew">
    <div class="modal-dialog">
        <div class="modal-content" style="background-color:#d8d1bd;color:black;font-size:15px;font-family:Times">

            <div class="modal-header">
                <button type="button" class="btn btn-close btn-curve pull-right" data-dismiss="modal">
                    <span aria-hidden="true">&times;</span>
                </button>
                <h4 class="modal-title"><b>Add New Candidate</b></h4>
            </div>

            <div class="modal-body">
                <form class="form-horizontal" method="POST" action="candidates_add.php" enctype="multipart/form-data">
                    <input type="hidden" name="csrf_token" value="<?php echo $_SESSION['csrf_token']; ?>">

                    <!-- FIRSTNAME -->
                    <div class="form-group">
                        <label class="col-sm-3 control-label">Firstname</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="firstname" required>
                        </div>
                    </div>

                    <!-- LASTNAME -->
                    <div class="form-group">
                        <label class="col-sm-3 control-label">Lastname</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="lastname" required>
                        </div>
                    </div>

                    <!-- POSITION (TEXT INPUT FIXED) -->
                    <div class="form-group">
                        <label class="col-sm-3 control-label">Position</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" name="position" placeholder="e.g. President" required>
                        </div>
                    </div>

                    <!-- PHOTO -->
                    <div class="form-group">
                        <label class="col-sm-3 control-label">Photo</label>
                        <div class="col-sm-9">
                            <input type="file" name="photo" accept="image/*">
                        </div>
                    </div>

                    <!-- SYMBOL -->
                    <div class="form-group">
                        <label class="col-sm-3 control-label">Symbol Image</label>
                        <div class="col-sm-9">
                            <input type="file" name="symbol" accept="image/*">
                        </div>
                    </div>

                    <!-- PLATFORM (KEPT) -->
                    <div class="form-group">
                        <label class="col-sm-3 control-label">Platform</label>
                        <div class="col-sm-9">
                            <textarea class="form-control" name="platform" rows="5"></textarea>
                        </div>
                    </div>

            </div>

            <div class="modal-footer">
                <button type="button" class="btn btn-default btn-curve pull-left" data-dismiss="modal">Close</button>
                <button type="submit" class="btn btn-primary btn-curve" name="add">Save</button>
                </form>
            </div>

        </div>
    </div>
</div>

<!-- EDIT CANDIDATE -->
<div class="modal fade" id="edit">
    <div class="modal-dialog">
        <div class="modal-content" style="background-color:#d8d1bd;color:black;font-size:15px;font-family:Times">

            <div class="modal-header">
                <button type="button" class="btn btn-close btn-curve pull-right" data-dismiss="modal">
                    <span aria-hidden="true">&times;</span>
                </button>
                <h4 class="modal-title"><b>Edit Candidate</b></h4>
            </div>

            <div class="modal-body">
                <form class="form-horizontal" method="POST" action="candidates_edit.php" enctype="multipart/form-data">
                    <input type="hidden" name="csrf_token" value="<?php echo $_SESSION['csrf_token']; ?>">

                    <input type="hidden" class="id" name="id">

                    <!-- FIRSTNAME -->
                    <div class="form-group">
                        <label class="col-sm-3 control-label">Firstname</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" id="edit_firstname" name="firstname" required>
                        </div>
                    </div>

                    <!-- LASTNAME -->
                    <div class="form-group">
                        <label class="col-sm-3 control-label">Lastname</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" id="edit_lastname" name="lastname" required>
                        </div>
                    </div>

                    <!-- POSITION (TEXT INPUT MODE NOT DROPDOWN) -->
                    <div class="form-group">
                        <label class="col-sm-3 control-label">Position</label>
                        <div class="col-sm-9">
                            <input type="text" class="form-control" id="edit_position" name="position" required>
                        </div>
                    </div>

                    <!-- SYMBOL -->
                    <div class="form-group">
                        <label class="col-sm-3 control-label">Symbol Image</label>
                        <div class="col-sm-9">
                            <input type="file" name="symbol" accept="image/*">
                        </div>
                    </div>

                    <!-- PLATFORM -->
                    <div class="form-group">
                        <label class="col-sm-3 control-label">Platform</label>
                        <div class="col-sm-9">
                            <textarea class="form-control" id="edit_platform" name="platform" rows="5"></textarea>
                        </div>
                    </div>

            </div>

            <div class="modal-footer">
                <button type="button" class="btn btn-default btn-curve pull-left" data-dismiss="modal">Close</button>
                <button type="submit" class="btn btn-success btn-curve" name="edit">Update</button>
                </form>
            </div>

        </div>
    </div>
</div>

<!-- DELETE -->
<div class="modal fade" id="delete">
    <div class="modal-dialog">
        <div class="modal-content" style="background-color:#d8d1bd;color:black;font-size:15px;font-family:Times">

            <div class="modal-header">
                <button type="button" class="btn btn-close btn-curve pull-right" data-dismiss="modal">
                    <span aria-hidden="true">&times;</span>
                </button>
                <h4 class="modal-title"><b>Delete Candidate</b></h4>
            </div>

            <div class="modal-body">
                <form method="POST" action="candidates_delete.php">
                    <input type="hidden" name="csrf_token" value="<?php echo $_SESSION['csrf_token']; ?>">
                    <input type="hidden" class="id" name="id">

                    <div class="text-center">
                        <p>Are you sure you want to delete?</p>
                        <h3 class="fullname"></h3>
                    </div>

            </div>

            <div class="modal-footer">
                <button type="button" class="btn btn-default btn-curve pull-left" data-dismiss="modal">Close</button>
                <button type="submit" class="btn btn-danger btn-curve" name="delete">Delete</button>
                </form>
            </div>

        </div>
    </div>
</div>