package com.votesystem;

import com.votesystem.service.CryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CryptoServiceTest {

    private CryptoService cryptoService;

    @BeforeEach
    public void setUp() {
        cryptoService = new CryptoService();
    }

    @Test
    public void testAesEncryptionAndDecryption() {
        Long originalCandidateId = 18L;
        String encrypted = cryptoService.encryptVote(originalCandidateId);

        assertNotNull(encrypted);
        assertFalse(encrypted.isBlank());
        assertNotEquals(String.valueOf(originalCandidateId), encrypted);

        Long decrypted = cryptoService.decryptVote(encrypted);
        assertEquals(originalCandidateId, decrypted);
    }

    @Test
    public void testRsaSigningAndVerification() {
        Long candidateId = 18L;
        String encryptedVote = cryptoService.encryptVote(candidateId);

        String signature = cryptoService.signVote(encryptedVote);
        assertNotNull(signature);
        assertFalse(signature.isBlank());

        boolean valid = cryptoService.verifySignature(encryptedVote, signature);
        assertTrue(valid, "Signature should verify successfully with election public key");

        // Test with altered payload
        boolean tampered = cryptoService.verifySignature(encryptedVote + "altered", signature);
        assertFalse(tampered, "Altered payload should fail signature verification");
    }

    @Test
    public void testVoteHashCalculation() {
        Long voterId = 6L;
        Long candidateId = 18L;
        Long posId = 8L;
        String encrypted = cryptoService.encryptVote(candidateId);
        String sig = cryptoService.signVote(encrypted);

        String hash1 = cryptoService.calculateVoteHash(voterId, candidateId, posId, encrypted, sig);
        String hash2 = cryptoService.calculateVoteHash(voterId, candidateId, posId, encrypted, sig);

        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length(), "SHA-256 hash must be 64 hex characters");

        // Altering candidate ID changes hash
        String alteredHash = cryptoService.calculateVoteHash(voterId, 999L, posId, encrypted, sig);
        assertNotEquals(hash1, alteredHash);
    }
}
