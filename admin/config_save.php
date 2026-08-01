<?php
	include 'includes/session.php';

	$return = 'home.php';
	if(isset($_GET['return'])){
		$return = $_GET['return'];
	}

	// Prevent Open Redirects
	if (preg_match('/[^a-zA-Z0-9_\-\.\/]/', $return) || strpos($return, ':') !== false || strpos($return, '//') !== false) {
		$return = 'home.php';
	}

	if(isset($_POST['save'])){
		// CSRF check
		if(!isset($_POST['csrf_token']) || $_POST['csrf_token'] !== $_SESSION['csrf_token']){
			$_SESSION['error'] = 'CSRF verification failed. Request denied.';
			header('location: '.$return);
			exit();
		}

		$title = $_POST['title'];

		$file = 'config.ini';
		$content = 'election_title = '.$title;

		file_put_contents($file, $content);

		$_SESSION['success'] = 'Election title updated successfully';
		
	}
	else{
		$_SESSION['error'] = "Fill up config form first";
	}

	header('location: '.$return);
?>