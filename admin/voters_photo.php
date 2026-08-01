<?php
	include 'includes/session.php';

	if(isset($_POST['upload'])){
		// CSRF check
		if(!isset($_POST['csrf_token']) || $_POST['csrf_token'] !== $_SESSION['csrf_token']){
			$_SESSION['error'] = 'CSRF verification failed. Request denied.';
			header('location: voters.php');
			exit();
		}

		$id = $_POST['id'];
		$filename = $_FILES['photo']['name'];
		if(!empty($filename)){
			move_uploaded_file($_FILES['photo']['tmp_name'], '../images/'.$filename);	
		}
		
		$stmt = $conn->prepare("UPDATE voters SET photo = ? WHERE id = ?");
		$stmt->bind_param("si", $filename, $id);

		if($stmt->execute()){
			$_SESSION['success'] = 'Photo updated successfully';
		}
		else{
			$_SESSION['error'] = $stmt->error;
		}
		$stmt->close();

	}
	else{
		$_SESSION['error'] = 'Select voter to update photo first';
	}

	header('location: voters.php');
?>