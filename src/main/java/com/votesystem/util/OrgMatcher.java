package com.votesystem.util;

public final class OrgMatcher {

    private OrgMatcher() {}

    /**
     * Checks if two organization names refer to the same organization,
     * ignoring case, whitespace differences, and minor spelling variations/typos
     * (such as "Inovation Hub" vs "Invation Hub" vs "Innovation Hub").
     */
    public static boolean matches(String org1, String org2) {
        if (org1 == null && org2 == null) return true;
        if (org1 == null || org2 == null) return false;

        String s1 = org1.trim();
        String s2 = org2.trim();

        if (s1.equalsIgnoreCase(s2)) return true;

        String n1 = normalize(s1);
        String n2 = normalize(s2);

        if (n1.isEmpty() && n2.isEmpty()) return true;
        if (n1.isEmpty() || n2.isEmpty()) return false;
        if (n1.equals(n2)) return true;

        // Levenshtein distance check for typos (e.g. 1 character difference)
        if (n1.length() >= 5 && n2.length() >= 5 && Math.abs(n1.length() - n2.length()) <= 1) {
            int dist = levenshteinDistance(n1, n2);
            if (dist <= 1) return true;
        }

        return false;
    }

    /**
     * Normalizes organization strings by removing punctuation, spaces,
     * and harmonizing known stem variations (e.g., inovation, invation -> innovation).
     */
    public static String normalize(String org) {
        if (org == null) return "";
        String s = org.toLowerCase().trim();
        // Remove non-alphanumeric characters
        s = s.replaceAll("[^a-z0-9]", "");
        // Harmonize known common typo variations
        s = s.replace("invation", "innovation");
        s = s.replace("inovation", "innovation");
        return s;
    }

    private static int levenshteinDistance(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }
}
