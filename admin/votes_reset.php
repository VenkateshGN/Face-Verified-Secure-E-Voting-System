<?php
	include 'includes/session.php';

	if(isset($_POST['reset'])){
		// CSRF check
		if(!isset($_POST['csrf_token']) || $_POST['csrf_token'] !== $_SESSION['csrf_token']){
			$_SESSION['error'] = 'CSRF verification failed. Request denied.';
			header('location: votes.php');
			exit();
		}

		$sql = "DELETE FROM votes";
		if($conn->query($sql)){
			$conn->query("DELETE FROM audit_logs");
			$conn->query("DELETE FROM tamper_logs");
			$_SESSION['success'] = "Votes reset successfully";
		}
		else{
			$_SESSION['error'] = "Something went wrong in reseting";
		}
	}
	else{
		$_SESSION['error'] = "Submit reset form first";
	}

	header('location: votes.php');
?>