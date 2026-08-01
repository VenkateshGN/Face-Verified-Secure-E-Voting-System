-- phpMyAdmin SQL Dump
-- version 5.0.2
-- https://www.phpmyadmin.net/

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

CREATE DATABASE IF NOT EXISTS votesystem;
USE votesystem;

-- =====================================================
-- ADMIN TABLE
-- =====================================================

CREATE TABLE `admin` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `firstname` varchar(50) NOT NULL,
  `lastname` varchar(50) NOT NULL,
  `photo` varchar(150) NOT NULL,
  `created_on` date NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

INSERT INTO `admin`
(`id`, `username`, `password`, `firstname`, `lastname`, `photo`, `created_on`)
VALUES
(
  1,
  'Venkatesh',
  '$2y$10$kLqXG4BAJrPbsOjJ/.B4eeZn6oojNhAb8l5/cb9eZvFnYU.pz2qni',
  'CRCE',
  'Admin',
  'admin.jpg',
  '2026-05-25'
);

-- =====================================================
-- POSITIONS TABLE
-- =====================================================

CREATE TABLE `positions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `description` varchar(50) NOT NULL,
  `max_vote` int(11) NOT NULL DEFAULT 1,
  `priority` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

INSERT INTO `positions`
(`id`, `description`, `max_vote`, `priority`)
VALUES
(8, 'MLA', 1, 1),
(9, 'MP', 1, 2);

-- =====================================================
-- CANDIDATES TABLE
-- =====================================================

CREATE TABLE `candidates` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `position_id` int(11) NOT NULL,
  `firstname` varchar(30) NOT NULL,
  `lastname` varchar(30) NOT NULL,
  `photo` varchar(150) NOT NULL,
  `platform` text NOT NULL,
  `symbol` varchar(150) NOT NULL,
  PRIMARY KEY (`id`),

  CONSTRAINT `fk_candidate_position`
  FOREIGN KEY (`position_id`)
  REFERENCES `positions`(`id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=latin1;

INSERT INTO `candidates`
(`id`, `position_id`, `firstname`, `lastname`, `photo`, `platform`, `symbol`)
VALUES
(
  18,
  8,
  'Venkatesh',
  'GN',
  '1779650286_IMG-20250129-WA0017.jpg',
  'Bright India',
  '1779650286_symbol_Cockroach.jpg'
);

-- =====================================================
-- VOTERS TABLE
-- =====================================================

CREATE TABLE `voters` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `voters_id` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `firstname` varchar(30) NOT NULL,
  `lastname` varchar(30) NOT NULL,
  `photo` varchar(150) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `otp` varchar(10) DEFAULT NULL,
  `otp_expiry` datetime DEFAULT NULL,

  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

INSERT INTO `voters`
(`id`, `voters_id`, `password`, `firstname`, `lastname`, `photo`, `email`)
VALUES
(
  6,
  'HrEn9AbXxtpo6jf',
  '$2y$10$AOxixFmJYaie5FtPTlePR.SofH5vBsevQD.Bbjm6oQt',
  'Venkatesh',
  'GN',
  'Screenshot.png',
  'venuv9162@gmail.com'
);

-- =====================================================
-- VOTES TABLE
-- =====================================================

CREATE TABLE `votes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `voters_id` int(11) NOT NULL,
  `candidate_id` int(11) NOT NULL,
  `position_id` int(11) NOT NULL,

  PRIMARY KEY (`id`),

  CONSTRAINT `fk_vote_voter`
  FOREIGN KEY (`voters_id`)
  REFERENCES `voters`(`id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE,

  CONSTRAINT `fk_vote_candidate`
  FOREIGN KEY (`candidate_id`)
  REFERENCES `candidates`(`id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE,

  CONSTRAINT `fk_vote_position`
  FOREIGN KEY (`position_id`)
  REFERENCES `positions`(`id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=latin1;

COMMIT;