package com.votesystem.service;

import com.votesystem.model.BlockchainBlock;
import com.votesystem.repository.BlockchainBlockRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BlockchainService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainService.class);
    public static final String GENESIS_PREV_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final BlockchainBlockRepository blockchainRepo;
    private final CryptoService cryptoService;

    public BlockchainService(BlockchainBlockRepository blockchainRepo, CryptoService cryptoService) {
        this.blockchainRepo = blockchainRepo;
        this.cryptoService = cryptoService;
    }

    @PostConstruct
    public void initGenesisBlock() {
        if (blockchainRepo.count() == 0) {
            log.info("Initializing Blockchain with Genesis Block...");
            String merkleRoot = cryptoService.sha256("GENESIS_VOTING_CONSENSUS_BLOCK_0");
            String blockHash = calculateBlockHash(0L, GENESIS_PREV_HASH, merkleRoot, 0L);

            BlockchainBlock genesis = new BlockchainBlock(
                    0L,
                    GENESIS_PREV_HASH,
                    blockHash,
                    merkleRoot,
                    "GENESIS_HASH",
                    "SYSTEM",
                    "Genesis Block: E-Voting Immutable Cryptographic Ledger Initialized",
                    0L
            );
            genesis.setTimestamp(LocalDateTime.now());
            blockchainRepo.save(genesis);
            log.info("Genesis Block created with Hash: {}", blockHash);
        }
    }

    /**
     * Appends a new vote transaction block into the immutable ledger.
     */
    @Transactional
    public synchronized BlockchainBlock addVoteBlock(String voteHash, String maskedVoterId, String summaryPayload) {
        Optional<BlockchainBlock> latestOpt = blockchainRepo.findTopByOrderByBlockIndexDesc();
        long nextIndex = latestOpt.map(b -> b.getBlockIndex() + 1).orElse(1L);
        String prevHash = latestOpt.map(BlockchainBlock::getBlockHash).orElse(GENESIS_PREV_HASH);

        String merkleRoot = cryptoService.sha256(voteHash + ":" + summaryPayload);
        long nonce = System.currentTimeMillis() % 100000;
        String blockHash = calculateBlockHash(nextIndex, prevHash, merkleRoot, nonce);

        BlockchainBlock block = new BlockchainBlock(
                nextIndex,
                prevHash,
                blockHash,
                merkleRoot,
                voteHash,
                maskedVoterId,
                summaryPayload,
                nonce
        );
        return blockchainRepo.save(block);
    }

    public String calculateBlockHash(Long index, String prevHash, String merkleRoot, Long nonce) {
        return cryptoService.sha256(index + prevHash + merkleRoot + nonce);
    }

    /**
     * Complete blockchain verification algorithm.
     */
    public BlockchainValidationResult validateBlockchain() {
        List<BlockchainBlock> chain = blockchainRepo.findAllByOrderByBlockIndexAsc();
        if (chain.isEmpty()) {
            return new BlockchainValidationResult(true, 0, "No blocks in ledger", new ArrayList<>());
        }

        List<String> discrepancies = new ArrayList<>();
        boolean valid = true;

        for (int i = 0; i < chain.size(); i++) {
            BlockchainBlock current = chain.get(i);

            // 1. Check Genesis Block
            if (i == 0) {
                if (!GENESIS_PREV_HASH.equals(current.getPreviousHash())) {
                    valid = false;
                    discrepancies.add("Genesis block has invalid previous hash: " + current.getPreviousHash());
                }
            } else {
                BlockchainBlock previous = chain.get(i - 1);
                // 2. Check Previous Hash Link
                if (!current.getPreviousHash().equals(previous.getBlockHash())) {
                    valid = false;
                    discrepancies.add("Broken link at Block #" + current.getBlockIndex() + ": expected prevHash " + previous.getBlockHash() + " but found " + current.getPreviousHash());
                }
            }

            // 3. Verify Block Hash Recalculation
            String recalculated = calculateBlockHash(current.getBlockIndex(), current.getPreviousHash(), current.getMerkleRoot(), current.getNonce());
            if (!recalculated.equals(current.getBlockHash())) {
                valid = false;
                discrepancies.add("Block #" + current.getBlockIndex() + " has corrupted hash! Stored: " + current.getBlockHash() + " vs Computed: " + recalculated);
            }
        }

        return new BlockchainValidationResult(valid, chain.size(), valid ? "All blocks cryptographically verified and intact" : "Blockchain tampering detected!", discrepancies);
    }

    public List<BlockchainBlock> getAllBlocks() {
        return blockchainRepo.findAllByOrderByBlockIndexAsc();
    }

    public static class BlockchainValidationResult {
        private final boolean valid;
        private final int totalBlocks;
        private final String message;
        private final List<String> issues;

        public BlockchainValidationResult(boolean valid, int totalBlocks, String message, List<String> issues) {
            this.valid = valid;
            this.totalBlocks = totalBlocks;
            this.message = message;
            this.issues = issues;
        }

        public boolean isValid() { return valid; }
        public int getTotalBlocks() { return totalBlocks; }
        public String getMessage() { return message; }
        public List<String> getIssues() { return issues; }
    }
}
