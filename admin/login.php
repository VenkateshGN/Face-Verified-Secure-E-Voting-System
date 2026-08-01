<?php
	session_start();
	include 'includes/conn.php';

	if(isset($_POST['login'])){
		// CSRF check
		if(!isset($_POST['csrf_token']) || $_POST['csrf_token'] !== $_SESSION['csrf_token']){
			$_SESSION['error'] = 'CSRF verification failed. Request denied.';
			header('location: index.php');
			exit();
		}

		$username = $_POST['username'];
		$password = $_POST['password'];

		$stmt = $conn->prepare("SELECT * FROM admin WHERE username = ?");
		$stmt->bind_param("s", $username);
		$stmt->execute();
		$query = $stmt->get_result();

		if($query->num_rows < 1){
			$_SESSION['error'] = 'Cannot find account with the username';
		}
		else{
			$row = $query->fetch_assoc();
			if(password_verify($password, $row['password'])){
				$_SESSION['admin'] = $row['id'];
			}
			else{
				$_SESSION['error'] = 'Incorrect password';
			}
		}
		$stmt->close();
	}
	else{
		$_SESSION['error'] = 'Input admin credentials first';
	}

	header('location: index.php');
?>