package com.votesystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "blockchain_ledger")
public class BlockchainBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "block_index", nullable = false)
    private Long blockIndex;

    @Column(name = "previous_hash", nullable = false, length = 128)
    private String previousHash;

    @Column(name = "block_hash", nullable = false, length = 128)
    private String blockHash;

    @Column(name = "merkle_root", nullable = false, length = 128)
    private String merkleRoot;

    @Column(name = "vote_hash", length = 128)
    private String voteHash;

    @Column(name = "voter_identifier", length = 50)
    private String voterIdentifier;

    @Column(name = "data_payload", columnDefinition = "TEXT")
    private String dataPayload;

    @Column(nullable = false)
    private Long nonce = 0L;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public BlockchainBlock() {
        this.timestamp = LocalDateTime.now();
    }

    public BlockchainBlock(Long blockIndex, String previousHash, String blockHash, String merkleRoot, String voteHash, String voterIdentifier, String dataPayload, Long nonce) {
        this.blockIndex = blockIndex;
        this.previousHash = previousHash;
        this.blockHash = blockHash;
        this.merkleRoot = merkleRoot;
        this.voteHash = voteHash;
        this.voterIdentifier = voterIdentifier;
        this.dataPayload = dataPayload;
        this.nonce = nonce;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBlockIndex() {
        return blockIndex;
    }

    public void setBlockIndex(Long blockIndex) {
        this.blockIndex = blockIndex;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    public String getMerkleRoot() {
        return merkleRoot;
    }

    public void setMerkleRoot(String merkleRoot) {
        this.merkleRoot = merkleRoot;
    }

    public String getVoteHash() {
        return voteHash;
    }

    public void setVoteHash(String voteHash) {
        this.voteHash = voteHash;
    }

    public String getVoterIdentifier() {
        return voterIdentifier;
    }

    public void setVoterIdentifier(String voterIdentifier) {
        this.voterIdentifier = voterIdentifier;
    }

    public String getDataPayload() {
        return dataPayload;
    }

    public void setDataPayload(String dataPayload) {
        this.dataPayload = dataPayload;
    }

    public Long getNonce() {
        return nonce;
    }

    public void setNonce(Long nonce) {
        this.nonce = nonce;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Transient
    public String getOrganization() {
        if (dataPayload != null) {
            if (dataPayload.startsWith("[")) {
                int end = dataPayload.indexOf(']');
                if (end > 1) {
                    return dataPayload.substring(1, end).trim();
                }
            }
            if (dataPayload.toLowerCase().contains("infosys")) return "Infosys";
            if (dataPayload.toLowerCase().contains("crce")) return "CRCE College";
            if (dataPayload.toLowerCase().contains("wipro")) return "Wipro";
        }
        if (blockIndex != null && blockIndex == 0) {
            return "SYSTEM";
        }
        return "ALL";
    }
}
