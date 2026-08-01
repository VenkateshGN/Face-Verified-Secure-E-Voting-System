<?php
	include 'includes/session.php';

	if(isset($_POST['edit'])){
		// CSRF check
		if(!isset($_POST['csrf_token']) || $_POST['csrf_token'] !== $_SESSION['csrf_token']){
			$_SESSION['error'] = 'CSRF verification failed. Request denied.';
			header('location: voters.php');
			exit();
		}

		$id = $_POST['id'];
		$firstname = $_POST['firstname'];
		$lastname = $_POST['lastname'];
		$email = $_POST['email'];
		$password = $_POST['password'];

		$stmt = $conn->prepare("SELECT * FROM voters WHERE id = ?");
		$stmt->bind_param("i", $id);
		$stmt->execute();
		$result = $stmt->get_result();

		if ($result && $result->num_rows > 0) {
			$row = $result->fetch_assoc();
			if($password == $row['password']){
				$password = $row['password'];
			}
			else{
				$password = password_hash($password, PASSWORD_DEFAULT);
			}
			$stmt->close();

			$stmt = $conn->prepare("UPDATE voters SET firstname = ?, lastname = ?, email = ?, password = ? WHERE id = ?");
			$stmt->bind_param("ssssi", $firstname, $lastname, $email, $password, $id);
			if($stmt->execute()){
				$_SESSION['success'] = 'Voter updated successfully';
			}
			else{
				$_SESSION['error'] = $stmt->error;
			}
		} else {
			$_SESSION['error'] = 'Voter not found';
		}
		$stmt->close();
	}
	else{
		$_SESSION['error'] = 'Fill up edit form first';
	}

	header('location: voters.php');
?>