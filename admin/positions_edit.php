<?php
	include 'includes/session.php';

	if(isset($_POST['edit'])){
		// CSRF check
		if(!isset($_POST['csrf_token']) || $_POST['csrf_token'] !== $_SESSION['csrf_token']){
			$_SESSION['error'] = 'CSRF verification failed. Request denied.';
			header('location: positions.php');
			exit();
		}

		$id = $_POST['id'];
		$description = $_POST['description'];
		$max_vote = $_POST['max_vote'];

		$stmt = $conn->prepare("UPDATE positions SET description = ?, max_vote = ? WHERE id = ?");
		$stmt->bind_param("sii", $description, $max_vote, $id);

		if($stmt->execute()){
			$_SESSION['success'] = 'Position updated successfully';
		}
		else{
			$_SESSION['error'] = $stmt->error;
		}
		$stmt->close();
	}
	else{
		$_SESSION['error'] = 'Fill up edit form first';
	}

	header('location: positions.php');
?>