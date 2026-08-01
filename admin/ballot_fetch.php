<?php
include 'includes/session.php';
include 'includes/slugify.php';

$output = '';

$sql = "SELECT * FROM positions ORDER BY priority ASC";
$query = $conn->query($sql);

while($row = $query->fetch_assoc()){

    $posid = $row['id'];
    $slug = slugify($row['description']);

    $candidate = '';

    // FETCH CANDIDATES
    $csql = "SELECT * FROM candidates WHERE position_id='$posid'";
    $cquery = $conn->query($csql);

    // CHECK IF EMPTY
    if($cquery->num_rows == 0){
        $candidate .= '
        <li style="list-style:none;padding:15px;color:red;font-weight:bold;">
            No candidates added for this position
        </li>
        ';
    }

    while($crow = $cquery->fetch_assoc()){

        // RADIO / CHECKBOX
        if($row['max_vote'] > 1){
            $input = '
            <input type="checkbox"
                   class="flat-red '.$slug.'"
                   name="'.$slug.'[]"
                   value="'.$crow['id'].'">';
        }
        else{
            $input = '
            <input type="radio"
                   class="flat-red '.$slug.'"
                   name="'.$slug.'"
                   value="'.$crow['id'].'">';
        }

        // PHOTO PATH
        $photo = (!empty($crow['photo']))
            ? '../images/'.$crow['photo']
            : '../images/profile.jpg';

        // SYMBOL PATH
        $symbol = (!empty($crow['symbol']))
            ? '../images/'.$crow['symbol']
            : '../images/profile.jpg';

        // UI
        $candidate .= '
        <li style="
            list-style:none;
            margin-bottom:20px;
            padding:15px;
            border-bottom:1px solid #bbb;
            background:#f9f9f9;
            border-radius:10px;
        ">

            <div style="display:flex;align-items:center;">

                '.$input.'

                <img src="'.$photo.'"
                     width="90"
                     height="90"
                     style="
                        margin-left:15px;
                        border-radius:10px;
                        object-fit:cover;
                        border:2px solid #ccc;
                     ">

                <img src="'.$symbol.'"
                     width="65"
                     height="65"
                     style="
                        margin-left:15px;
                        border-radius:10px;
                        object-fit:cover;
                        border:2px solid #ccc;
                     ">

                <div style="margin-left:20px;">

                    <div style="
                        font-size:20px;
                        font-weight:bold;
                        color:#222;
                    ">
                        '.$crow['firstname'].' '.$crow['lastname'].'
                    </div>

                    <div style="
                        margin-top:8px;
                        font-size:15px;
                        color:#444;
                    ">
                        <b>Party / Platform:</b>
                        '.$crow['platform'].'
                    </div>

                </div>

            </div>

        </li>
        ';
    }

    // INSTRUCTION
    if($row['max_vote'] > 1){
        $instruction = 'You may select up to '.$row['max_vote'].' candidates';
    }
    else{
        $instruction = 'Select only one candidate';
    }

    // POSITION BOX
    $output .= '
    <div class="box box-solid"
         style="
            background-color:#d8d1bd;
            margin-bottom:25px;
            border-radius:10px;
         ">

        <div class="box-header with-border"
             style="background:#cfc6ab;">

            <h3 class="box-title">
                <b>'.$row['description'].'</b>
            </h3>

        </div>

        <div class="box-body">

            <p style="
                font-weight:bold;
                font-size:15px;
                color:#333;
            ">
                '.$instruction.'
            </p>

            <ul style="padding-left:0;">
                '.$candidate.'
            </ul>

        </div>

    </div>
    ';
}

echo $output;
?>