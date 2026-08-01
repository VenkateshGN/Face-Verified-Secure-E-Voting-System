<?php
	include 'includes/session.php';

	if(isset($_POST['add'])){
		// CSRF check
		if(!isset($_POST['csrf_token']) || $_POST['csrf_token'] !== $_SESSION['csrf_token']){
			$_SESSION['error'] = 'CSRF verification failed. Request denied.';
			header('location: positions.php');
			exit();
		}

		$description = $_POST['description'];
		$max_vote = $_POST['max_vote'];

		$sql = "SELECT * FROM positions ORDER BY priority DESC LIMIT 1";
		$query = $conn->query($sql);
		$row = $query->fetch_assoc();

		$priority = ($row) ? ($row['priority'] + 1) : 1;
		
		$stmt = $conn->prepare("INSERT INTO positions (description, max_vote, priority) VALUES (?, ?, ?)");
		$stmt->bind_param("sii", $description, $max_vote, $priority);

		if($stmt->execute()){
			$_SESSION['success'] = 'Position added successfully';
		}
		else{
			$_SESSION['error'] = $stmt->error;
		}
		$stmt->close();

	}
	else{
		$_SESSION['error'] = 'Fill up add form first';
	}

	header('location: positions.php');
?>