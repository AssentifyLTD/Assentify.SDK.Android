package com.assentify.sdk.ScanPassport;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Combines the MRZ readings of several camera frames into one result.
 *
 * Why: a single frame can be wrong in a way NO check digit detects (e.g. "25DI68178" read as
 * "2S016B178" passes both the document and the composite check digit by coincidence).
 * OCR mistakes like S/5 or D/0 rarely hit the same position in every frame, so a per-character
 * majority vote across frames removes them. All distinct readings are also kept, ranked by
 * votes, so the NFC step can let the chip decide when frames disagree.
 */
final class MrzConsensus {

    static final class Outcome {
        /** Result to use for the passport step */
        @NonNull final Mrz.Result result;
        /** Index (in the readings list) of a frame whose reading matches [result] */
        final int frameIndex;
        /** Distinct MRZ texts ("line1\nline2[\nline3]"), best first, for the NFC access key */
        @NonNull final List<String> candidates;
        /** Short human-readable summary for logs */
        @NonNull final String summary;

        Outcome(@NonNull Mrz.Result result, int frameIndex, @NonNull List<String> candidates, @NonNull String summary) {
            this.result = result;
            this.frameIndex = frameIndex;
            this.candidates = candidates;
            this.summary = summary;
        }
    }

    /** At most this many readings are handed to NFC (each wrong key slows the chip down) */
    private static final int MAX_CANDIDATES = 3;

    @Nullable
    static Outcome decide(@NonNull List<Mrz.Result> readings) {
        if (readings.isEmpty()) return null;

        // 1. Keep the document type most frames agree on
        String type = majorityType(readings);
        List<Mrz.Result> same = new ArrayList<>();
        List<Integer> sameIdx = new ArrayList<>();
        for (int i = 0; i < readings.size(); i++) {
            if (type.equals(readings.get(i).docType)) {
                same.add(readings.get(i));
                sameIdx.add(i);
            }
        }

        // 2. Per-character vote, line by line
        int lineCount = same.get(0).lines.size();
        List<String> voted = new ArrayList<>();
        boolean line2Only = false;
        for (int li = 0; li < lineCount; li++) {
            List<Mrz.Result> voters = new ArrayList<>();
            for (Mrz.Result r : same) {
                // TD3 line 1 of a "line 2 only" reading is a placeholder: it must not vote
                if (li == 0 && type.equals("td3") && r.line2Only) continue;
                voters.add(r);
            }
            if (voters.isEmpty()) {
                voted.add(same.get(0).lines.get(li)); // only placeholders available
                line2Only = true;
            } else {
                voted.add(voteLine(voters, li));
            }
        }

        Mrz.Result consensus = null;
        try {
            consensus = type.equals("td3") ? Mrz.validateTd3(voted) : Mrz.validateTd1(voted);
            consensus.line2Only = line2Only;
        } catch (Exception ignored) {
        }

        // 3. Distinct readings (by the BAC-relevant part), ranked by how many frames produced them
        Map<String, Integer> votes = new LinkedHashMap<>();
        Map<String, Mrz.Result> byKey = new HashMap<>();
        Map<String, Integer> firstIndex = new HashMap<>();
        for (int i = 0; i < same.size(); i++) {
            Mrz.Result r = same.get(i);
            if (!r.bacReady()) continue;
            String key = safeMrzInformation(r);
            if (key == null) continue;
            Integer old = votes.get(key);
            votes.put(key, old == null ? 1 : old + 1);
            if (!byKey.containsKey(key)) {
                byKey.put(key, r);
                firstIndex.put(key, sameIdx.get(i));
            }
        }
        List<String> ranked = new ArrayList<>(votes.keySet());
        ranked.sort((a, b) -> {
            int c = Integer.compare(votes.get(b), votes.get(a));
            if (c != 0) return c;
            return Integer.compare(byKey.get(b).score(), byKey.get(a).score());
        });

        // 4. Pick the result: the vote if it passes every check, else the most-voted valid reading
        Mrz.Result chosen;
        String how;
        if (consensus != null && consensus.allValid()) {
            chosen = consensus;
            how = "per-character vote of " + same.size() + " frame(s)";
        } else {
            chosen = null;
            for (String k : ranked) {
                if (byKey.get(k).allValid()) { chosen = byKey.get(k); break; }
            }
            if (chosen == null) chosen = bestByScore(same);
            how = "best single frame (vote " + (consensus == null ? "invalid" : consensus.failSummary()) + ")";
        }

        // 5. Candidates for NFC: chosen first, then the other distinct readings by votes
        List<String> candidates = new ArrayList<>();
        addCandidate(candidates, chosen);
        for (String k : ranked) {
            if (candidates.size() >= MAX_CANDIDATES) break;
            addCandidate(candidates, byKey.get(k));
        }

        String chosenKey = safeMrzInformation(chosen);
        int frame = chosenKey != null && firstIndex.containsKey(chosenKey) ? firstIndex.get(chosenKey) : sameIdx.get(0);

        String summary = how + "; " + ranked.size() + " distinct reading(s), votes="
                + votes.values() + "; chosen: " + chosen.failSummary();
        return new Outcome(chosen, frame, candidates, summary);
    }

    private static String voteLine(List<Mrz.Result> voters, int li) {
        int len = voters.get(0).lines.get(li).length();
        StringBuilder sb = new StringBuilder(len);
        for (int p = 0; p < len; p++) {
            Map<Character, Integer> count = new LinkedHashMap<>(); // insertion order = most recent frame first
            for (Mrz.Result r : voters) {
                String l = r.lines.get(li);
                if (p >= l.length()) continue;
                char c = l.charAt(p);
                Integer old = count.get(c);
                count.put(c, old == null ? 1 : old + 1);
            }
            char best = '<';
            int bestCount = -1;
            for (Map.Entry<Character, Integer> e : count.entrySet()) {
                if (e.getValue() > bestCount) { best = e.getKey(); bestCount = e.getValue(); }
            }
            sb.append(best);
        }
        return sb.toString();
    }

    private static String majorityType(List<Mrz.Result> readings) {
        int td3 = 0;
        for (Mrz.Result r : readings) if ("td3".equals(r.docType)) td3++;
        return td3 * 2 >= readings.size() ? "td3" : "td1";
    }

    private static Mrz.Result bestByScore(List<Mrz.Result> list) {
        Mrz.Result best = list.get(0);
        for (Mrz.Result r : list) if (r.score() > best.score()) best = r;
        return best;
    }

    @Nullable
    private static String safeMrzInformation(Mrz.Result r) {
        try {
            return r.mrzInformation();
        } catch (Exception e) {
            return null;
        }
    }

    private static void addCandidate(List<String> out, Mrz.Result r) {
        if (r == null || !r.bacReady()) return;
        String text = String.join("\n", r.lines);
        if (!out.contains(text) && out.size() < MAX_CANDIDATES) out.add(text);
        // Document number taken from the visual zone: the other plausible readings (and the MRZ as
        // read, last) stay available so the chip can decide
        if (r.alternativeLines != null) {
            for (List<String> alt : r.alternativeLines) {
                String text2 = String.join("\n", alt);
                if (!out.contains(text2) && out.size() < MAX_CANDIDATES) out.add(text2);
            }
        }
    }

    private MrzConsensus() { }
}