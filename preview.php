<?php
include 'includes/session.php';
include 'includes/slugify.php';

$output = array('error'=>false);

if(!isset($_POST)){
    $output['error'] = true;
}
else{

    $sql = "SELECT * FROM positions ORDER BY priority ASC";
    $query = $conn->query($sql);

    $output['list'] = '';

    while($row = $query->fetch_assoc()){

        $position = slugify($row['description']);

        if(isset($_POST[$position])){

            $output['list'] .= "
                <div style='margin-bottom:15px;padding:10px;border-bottom:1px solid #ccc;'>
                    <h4><b>".$row['description']."</b></h4>
            ";

            if($row['max_vote'] > 1){

                foreach($_POST[$position] as $candidate_id){

                    $sql2 = "SELECT * FROM candidates WHERE id='$candidate_id'";
                    $cquery = $conn->query($sql2);
                    $crow = $cquery->fetch_assoc();

                    $output['list'] .= "
                        <p>".$crow['firstname']." ".$crow['lastname']."</p>
                    ";
                }
            }
            else{

                $candidate_id = $_POST[$position];

                $sql2 = "SELECT * FROM candidates WHERE id='$candidate_id'";
                $cquery = $conn->query($sql2);
                $crow = $cquery->fetch_assoc();

                $output['list'] .= "
                    <p>".$crow['firstname']." ".$crow['lastname']."</p>
                ";
            }

            $output['list'] .= "</div>";
        }
    }
}

echo json_encode($output);
?>