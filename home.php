<?php
session_start();
include 'includes/conn.php';

/*
========================================
STEP PROTECTION
========================================
*/
if (
    !isset($_SESSION['voter_id']) ||
    !isset($_SESSION['step']) ||
    $_SESSION['step'] !== "done"
){
    header("Location: index.php");
    exit();
}

$voter_id = $_SESSION['voter_id'];

/*
========================================
FETCH VOTER DETAILS
========================================
*/
$stmt = $conn->prepare("SELECT * FROM voters WHERE voters_id = ?");
$stmt->bind_param("s", $voter_id);
$stmt->execute();

$result = $stmt->get_result();
$voter = $result->fetch_assoc();

if(!$voter){
    session_destroy();
    header("Location: index.php");
    exit();
}
?>

<?php include 'includes/header.php'; ?>

<body class="hold-transition skin-blue layout-top-nav">

<div class="wrapper">

    <?php include 'includes/navbar.php'; ?>

    <div class="content-wrapper" style="background-color:#F1E9D2;">

        <div class="container">

            <section class="content">

                <?php
                $parse = parse_ini_file('admin/config.ini', FALSE, INI_SCANNER_RAW);
                $title = $parse['election_title'];
                ?>

                <h1 class="page-header text-center title">
                    <b><?php echo strtoupper($title); ?></b>
                </h1>

                <div class="row">

                    <div class="col-sm-10 col-sm-offset-1">

                        <!-- ERROR MESSAGE -->


                        <div class="alert alert-danger" id="alert" style="display:none;">
                            <span class="message"></span>
                        </div>

                        <?php
                        /*
                        ========================================
                        CHECK IF ALREADY VOTED
                        ========================================
                        */
                        $checkVote = "
                            SELECT *
                            FROM votes
                            WHERE voters_id = '".$voter['id']."'
                        ";

                        $voteQuery = $conn->query($checkVote);

                        if($voteQuery->num_rows > 0){
                        ?>

                            <div class="text-center"
                                 style="font-size:24px;
                                        color: var(--text-primary);
                                        margin-top: 50px;">

                                <h3>You have already voted for this election.</h3>
                                <br>

                                <a href="#view"
                                   data-toggle="modal"
                                   class="btn btn-primary btn-lg btn-curve">

                                    View Ballot

                                </a>
                            </div>

                        <?php
                        }
                        else{
                        ?>

                        <!-- ========================================
                             BALLOT FORM
                        ========================================= -->
                        <form method="POST"
                              action="submit_ballot.php"
                              id="ballotForm">
                            <input type="hidden" name="csrf_token" value="<?php echo $_SESSION['csrf_token']; ?>">

                            <?php
                            include 'includes/slugify.php';

                            $sql = "
                                SELECT *
                                FROM positions
                                ORDER BY priority ASC
                            ";

                            $query = $conn->query($sql);

                            while($row = $query->fetch_assoc()){

                                $position_id = $row['id'];

                                $candidateHTML = '';

                                /*
                                ========================================
                                FETCH CANDIDATES
                                ========================================
                                */
                                $sql2 = "
                                    SELECT *
                                    FROM candidates
                                    WHERE position_id = '$position_id'
                                ";

                                $cquery = $conn->query($sql2);

                                while($crow = $cquery->fetch_assoc()){

                                    $slug = slugify($row['description']);

                                    /*
                                    ========================================
                                    PHOTO
                                    ========================================
                                    */
                                    $photo = (!empty($crow['photo']))
                                        ? 'images/'.$crow['photo']
                                        : 'images/profile.jpg';

                                    /*
                                    ========================================
                                    SYMBOL
                                    ========================================
                                    */
                                    $symbol = (!empty($crow['symbol']))
                                        ? 'images/'.$crow['symbol']
                                        : 'images/profile.jpg';

                                    /*
                                    ========================================
                                    INPUT TYPE
                                    ========================================
                                    */
                                    if($row['max_vote'] > 1){

                                        $input = '
                                        <input type="checkbox"
                                               class="'.$slug.'"
                                               name="'.$slug.'[]"
                                               value="'.$crow['id'].'">
                                        ';
                                    }
                                    else{

                                        $input = '
                                        <input type="radio"
                                               class="'.$slug.'"
                                               name="'.$slug.'"
                                               value="'.$crow['id'].'">
                                        ';
                                    }

                                    /*
                                    ========================================
                                    CANDIDATE CARD
                                    ========================================
                                    */
                                    $candidateHTML .= '

                                    <li style="
                                        list-style:none;
                                        margin-bottom:20px;
                                        padding:15px;
                                        border-bottom:1px solid #ccc;
                                    ">

                                        '.$input.'

                                        <img src="'.$photo.'"
                                             width="80"
                                             height="80"
                                             style="
                                                margin-left:10px;
                                                border-radius:10px;
                                                object-fit:cover;
                                             ">

                                        <img src="'.$symbol.'"
                                             width="60"
                                             height="60"
                                             style="
                                                margin-left:10px;
                                                border-radius:10px;
                                                object-fit:cover;
                                             ">

                                        <span style="
                                            margin-left:15px;
                                            font-size:18px;
                                            font-weight:bold;
                                        ">
                                            '.$crow['firstname'].' '.$crow['lastname'].'
                                        </span>

                                        <div style="
                                            margin-left:120px;
                                            margin-top:10px;
                                        ">
                                            <b>Party / Platform:</b>
                                            '.$crow['platform'].'
                                        </div>

                                    </li>
                                    ';
                                }

                                /*
                                ========================================
                                INSTRUCTIONS
                                ========================================
                                */
                                if($row['max_vote'] > 1){
                                    $instruction =
                                    'You may select up to '.$row['max_vote'].' candidates';
                                }
                                else{
                                    $instruction =
                                    'Select only one candidate';
                                }

                                /*
                                ========================================
                                POSITION BOX
                                ========================================
                                */
                                echo '

                                <div class="box box-solid"
                                     style="
                                        background:#d8d1bd;
                                        margin-bottom:20px;
                                     ">

                                    <div class="box-header with-border">

                                        <h3 class="box-title">
                                            <b>'.$row['description'].'</b>
                                        </h3>

                                    </div>

                                    <div class="box-body">

                                        <p style="font-weight:bold;">
                                            '.$instruction.'
                                        </p>

                                        <ul style="padding-left:0;">
                                            '.$candidateHTML.'
                                        </ul>

                                    </div>

                                </div>
                                ';
                            }
                            ?>

                            <!-- ========================================
                                 BUTTONS
                            ========================================= -->
                            <div class="text-center"
                                 style="margin-top:20px;">

                                <button type="button"
                                        id="preview"
                                        class="btn btn-success btn-lg">

                                    Preview

                                </button>

                                <button type="submit"
                                        name="vote"
                                        class="btn btn-primary btn-lg">

                                    Submit Vote

                                </button>

                            </div>

                        </form>

                        <?php } ?>

                    </div>

                </div>

            </section>

        </div>

    </div>

    <?php include 'includes/footer.php'; ?>
    <?php include 'includes/ballot_modal.php'; ?>

</div>

<?php include 'includes/scripts.php'; ?>

<script>

$(function(){

    /*
    ========================================
    PREVIEW
    ========================================
    */
    $('#preview').click(function(e){

        e.preventDefault();

        var form = $('#ballotForm').serialize();

        if(form == ''){

            $('.message').text('Select at least one candidate');
            $('#alert').show();

            return;
        }

        $.ajax({

            type: 'POST',
            url: 'preview.php',
            data: form,
            dataType: 'json',

            success: function(response){

                if(response.error){

                    $('.message').text('Error in selection');
                    $('#alert').show();
                }
                else{

                    $('#preview_modal').modal('show');
                    $('#preview_body').html(response.list);
                }
            }
        });

    });

});

</script>

</body>
</html>