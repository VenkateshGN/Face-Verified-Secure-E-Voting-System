<?php

	include 'includes/session.php';

	if(isset($_POST['add'])){
		// CSRF check
		if(!isset($_POST['csrf_token']) || $_POST['csrf_token'] !== $_SESSION['csrf_token']){
			$_SESSION['error'] = 'CSRF verification failed. Request denied.';
			header('location: voters.php');
			exit();
		}

		$firstname = $_POST['firstname'];
		$lastname = $_POST['lastname'];
		$email = $_POST['email'];
		$password = password_hash($_POST['password'], PASSWORD_DEFAULT);

		$filename = $_FILES['photo']['name'];

		// Enforce voter photo as required
		if(empty($filename)){
			$_SESSION['error'] = 'Voter photo is required for face verification';
			header('location: voters.php');
			exit();
		}

		move_uploaded_file(
			$_FILES['photo']['tmp_name'],
			'../images/'.$filename
		);

		// Generate voter ID
		$set = '123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
		$voter = substr(str_shuffle($set), 0, 15);

		// Prepared Statement
		$stmt = $conn->prepare(
			"INSERT INTO voters
			(voters_id, password,
			firstname, lastname,
			email, photo)
			VALUES (?, ?, ?, ?, ?, ?)"
		);

		$stmt->bind_param(
			"ssssss",
			$voter,
			$password,
			$firstname,
			$lastname,
			$email,
			$filename
		);

		if($stmt->execute()){
			$_SESSION['success'] = 'Voter added successfully';
		}
		else{
			$_SESSION['error'] = $stmt->error;
		}
		$stmt->close();
	}
	else{
		$_SESSION['error'] = 'Fill up add form first';
	}

	header('location: voters.php');

?>