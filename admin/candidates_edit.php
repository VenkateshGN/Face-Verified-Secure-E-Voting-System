<?php
	include 'includes/session.php';

	if(isset($_POST['edit'])){
		// CSRF check
		if(!isset($_POST['csrf_token']) || $_POST['csrf_token'] !== $_SESSION['csrf_token']){
			$_SESSION['error'] = 'CSRF verification failed. Request denied.';
			header('location: candidates.php');
			exit();
		}

		$id = $_POST['id'];
		$firstname = $_POST['firstname'];
		$lastname = $_POST['lastname'];
		$position = trim($_POST['position']);
		$platform = $_POST['platform'];

		// Find position ID by description (MLA / MP)
		$stmt = $conn->prepare("SELECT id FROM positions WHERE description = ?");
		$stmt->bind_param("s", $position);
		$stmt->execute();
		$pquery = $stmt->get_result();

		if($pquery->num_rows == 0){
			$_SESSION['error'] = 'Position does not exist';
			$stmt->close();
			header('location: candidates.php');
			exit();
		}

		$prow = $pquery->fetch_assoc();
		$position_id = $prow['id'];
		$stmt->close();

		// Handle optional symbol image update
		$symbol_filename = "";
		if(!empty($_FILES['symbol']['name'])){
			$symbol_filename = time().'_symbol_'.$_FILES['symbol']['name'];
			move_uploaded_file($_FILES['symbol']['tmp_name'], '../images/'.$symbol_filename);
		}

		if(!empty($symbol_filename)){
			$stmt = $conn->prepare("UPDATE candidates SET firstname = ?, lastname = ?, position_id = ?, platform = ?, symbol = ? WHERE id = ?");
			$stmt->bind_param("ssissi", $firstname, $lastname, $position_id, $platform, $symbol_filename, $id);
		} else {
			$stmt = $conn->prepare("UPDATE candidates SET firstname = ?, lastname = ?, position_id = ?, platform = ? WHERE id = ?");
			$stmt->bind_param("ssisi", $firstname, $lastname, $position_id, $platform, $id);
		}

		if($stmt->execute()){
			$_SESSION['success'] = 'Candidate updated successfully';
		}
		else{
			$_SESSION['error'] = $stmt->error;
		}
		$stmt->close();
	}
	else{
		$_SESSION['error'] = 'Fill up edit form first';
	}

	header('location: candidates.php');
?>