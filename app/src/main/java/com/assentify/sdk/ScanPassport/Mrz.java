package com.assentify.sdk.ScanPassport;

import com.assentify.sdk.Core.Constants.MrzKeys;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/** ICAO 9303 MRZ parsing, check-digit validation, OCR repair, and BAC key derivation. */
public final class Mrz {

    static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789<";

    // ---------- OnBoardMe output property keys (IdentificationDocumentCapture step) ----------



    public static String clean(String line) {
        String s = line.toUpperCase()
                .replace("«", "<<").replace("‹", "<").replace("›", "<")
                .replace("≪", "<<").replace("≤", "<")
                .replaceAll("\\s", "");
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) if (CHARSET.indexOf(c) >= 0) b.append(c);
        return b.toString();
    }

    static int charVal(char c) {
        if (c == '<') return 0;
        if (c >= '0' && c <= '9') return c - '0';
        return c - 'A' + 10;
    }

    static boolean checkDigit(String data, char cd) {
        int[] w = {7, 3, 1};
        int sum = 0;
        for (int i = 0; i < data.length(); i++) sum += charVal(data.charAt(i)) * w[i % 3];
        if (cd == '<') return sum % 10 == 0;
        return cd >= '0' && cd <= '9' && sum % 10 == cd - '0';
    }

    // ---------- coercion of strictly-typed positions ----------

    static final String TO_D_FROM = "OQDIZSBG", TO_D_TO = "00012586";
    static final String TO_A_FROM = "012586", TO_A_TO = "OIZSBG";

    static String coerceRange(String ln, int from, int to, boolean toDigit) {
        char[] c = ln.toCharArray();
        for (int p = from; p < Math.min(to, c.length); p++) {
            if (c[p] == '<') continue;
            String f = toDigit ? TO_D_FROM : TO_A_FROM, t = toDigit ? TO_D_TO : TO_A_TO;
            int i = f.indexOf(c[p]);
            if (i >= 0) c[p] = t.charAt(i);
        }
        return new String(c);
    }

    static List<String> coerce(List<String> lines, String docType) {
        List<String> out = new ArrayList<>();
        if (docType.equals("td3")) {
            out.add(coerceRange(lines.get(0), 2, 44, false));
            String l2 = lines.get(1);
            l2 = coerceRange(l2, 9, 10, true);
            l2 = coerceRange(l2, 10, 13, false);
            l2 = coerceRange(l2, 13, 20, true);
            l2 = coerceRange(l2, 21, 28, true);
            l2 = coerceRange(l2, 42, 44, true);
            out.add(l2);
        } else {
            String l1 = coerceRange(coerceRange(lines.get(0), 2, 5, false), 14, 15, true);
            String l2 = lines.get(1);
            l2 = coerceRange(l2, 0, 7, true);
            l2 = coerceRange(l2, 8, 15, true);
            l2 = coerceRange(l2, 15, 18, false);
            l2 = coerceRange(l2, 29, 30, true);
            out.add(l1); out.add(l2); out.add(coerceRange(lines.get(2), 0, 30, false));
        }
        return out;
    }

    // ---------- check-digit-guided repair of alphanumeric fields (doc number) ----------

    /** OCR lookalike swaps, both directions. */
    static char alt(char c) {
        switch (c) {
            case 'O': case 'Q': case 'D': return '0';
            case '0': return 'O';
            case 'I': return '1';
            case '1': return 'I';
            case 'Z': return '2';
            case '2': return 'Z';
            case 'S': return '5';
            case '5': return 'S';
            case 'B': return '8';
            case '8': return 'B';
            case 'G': return '6';
            case '6': return 'G';
            default: return c;
        }
    }

    /**
     * If checkDigit(field, cd) fails, search lookalike-swap combinations for a variant
     * that passes. Returns the repaired field, or null when none found.
     */
    static String repairField(String field, char cd) {
        List<Integer> amb = new ArrayList<>();
        for (int i = 0; i < field.length(); i++)
            if (alt(field.charAt(i)) != field.charAt(i)) amb.add(i);
        int n = Math.min(amb.size(), 10);
        Integer[] masks = new Integer[(1 << n) - 1];
        for (int m = 1; m < (1 << n); m++) masks[m - 1] = m;
        Arrays.sort(masks, (a, b) -> Integer.bitCount(a) - Integer.bitCount(b)); // fewest swaps first
        for (int mask : masks) {
            char[] c = field.toCharArray();
            for (int b = 0; b < n; b++)
                if ((mask & (1 << b)) != 0) {
                    int p = amb.get(b);
                    c[p] = alt(c[p]);
                }
            String cand = new String(c);
            if (checkDigit(cand, cd)) return cand;
        }
        return null;
    }

    // ---------- result ----------

    public static class Result {
        public List<String> lines;
        public String docType;
        public boolean docNum, dob, expiry, optional, finalCd;
        public boolean line2Only = false;
        public boolean repaired = false;

        public boolean allValid() { return docNum && dob && expiry && optional && finalCd; }
        public boolean bacReady() { return docNum && dob && expiry; }
        public int score() {
            return (docNum ? 1 : 0) + (dob ? 1 : 0) + (expiry ? 1 : 0)
                    + (optional ? 1 : 0) + (finalCd ? 1 : 0) + (line2Only ? 0 : 1);
        }

        /**
         * True only when every OnBoardMe output field is actually present: all check
         * digits valid, AND (for the TD3 fallback path) line 1 was read so first/last
         * name exist. Use this -- not allValid() -- to decide whether a scan result can
         * be accepted, since allValid() alone can still pass with the name missing
         * (e.g. the TD3 line2Only fallback, which only ever carries line 2's data).
         */
        public boolean isComplete() {
            if (!allValid() || line2Only) return false;
            Map<String, Object> props = toOutputProperties();
            for (String key : new String[]{
                    MrzKeys.KEY_DOCUMENT_TYPE, MrzKeys.KEY_COUNTRY, MrzKeys.KEY_DOCUMENT_NUMBER,
                    MrzKeys.KEY_NATIONALITY, MrzKeys.KEY_BIRTH_DATE, MrzKeys.KEY_SEX,
                    MrzKeys.KEY_EXPIRY_DATE, MrzKeys.KEY_LAST_NAME, MrzKeys.KEY_FIRST_NAME}) {
                Object v = props.get(key);
                if (v == null || (v instanceof String && ((String) v).trim().isEmpty())) return false;
            }
            return true;
        }

        public String mrzInformation() {
            if (docType.equals("td3")) {
                String l2 = lines.get(1);
                return l2.substring(0, 10) + l2.substring(13, 20) + l2.substring(21, 28);
            }
            String l1 = lines.get(0), l2 = lines.get(1);
            return l1.substring(5, 15) + l2.substring(0, 7) + l2.substring(8, 15);
        }

        public String failSummary() {
            List<String> f = new ArrayList<>();
            if (!docNum) f.add("doc#");
            if (!dob) f.add("dob");
            if (!expiry) f.add("expiry");
            if (!optional) f.add("opt");
            if (!finalCd) f.add("final");
            String s = f.isEmpty() ? "all checks OK" : "fail: " + String.join(",", f);
            if (line2Only) s += " [line 1 not read]";
            if (repaired) s += " [auto-repaired]";
            return s;
        }

        public String[][] fields() {
            if (!docType.equals("td3")) {
                String l1 = lines.get(0), l2 = lines.get(1), l3 = lines.get(2);
                return new String[][]{
                        {"Document", l1.substring(0, 2).replace("<", "")},
                        {"Issuing state", l1.substring(2, 5).replace("<", "")},
                        {"Document no.", l1.substring(5, 14).replace("<", "")},
                        {"Birth date", fmtDate(l2.substring(0, 6))},
                        {"Sex", l2.substring(7, 8)},
                        {"Expiry date", fmtDate(l2.substring(8, 14))},
                        {"Nationality", l2.substring(15, 18).replace("<", "")},
                        {"Name", name(l3, 0)},
                };
            }
            String l1 = lines.get(0), l2 = lines.get(1);
            return new String[][]{
                    {"Document", l1.substring(0, 2).replace("<", "")},
                    {"Issuing state", l1.substring(2, 5).replace("<", "")},
                    {"Name", line2Only ? "(line 1 not read)" : name(l1, 5)},
                    {"Document no.", l2.substring(0, 9).replace("<", "")},
                    {"Nationality", l2.substring(10, 13).replace("<", "")},
                    {"Birth date", fmtDate(l2.substring(13, 19))},
                    {"Sex", l2.substring(20, 21)},
                    {"Expiry date", fmtDate(l2.substring(21, 27))},
                    {"Personal no.", l2.substring(28, 42).replace("<", " ").trim()},
            };
        }

        /**
         * Same underlying data as {@link #fields()}, but keyed by the OnBoardMe
         * IdentificationDocumentCapture output property keys (the Mrz.KEY_* constants)
         * instead of display labels, with surname/given name split into two separate
         * values instead of one combined "Surname, Given" string.
         *
         * On a TD1 back page that wasn't read (line2Only), first/last name are simply
         * omitted from the map rather than added with a placeholder value.
         */
        public Map<String, Object> toOutputProperties() {
            Map<String, Object> out = new HashMap<>();
            if (docType.equals("td3")) {
                String l1 = lines.get(0), l2 = lines.get(1);
                out.put(MrzKeys.KEY_DOCUMENT_TYPE, l1.substring(0, 2).replace("<", ""));
                out.put(MrzKeys.KEY_COUNTRY, l1.substring(2, 5).replace("<", ""));
                out.put(MrzKeys.KEY_DOCUMENT_NUMBER, l2.substring(0, 9).replace("<", ""));
                out.put(MrzKeys.KEY_NATIONALITY, l2.substring(10, 13).replace("<", ""));
                out.put(MrzKeys.KEY_BIRTH_DATE, isoDate(l2.substring(13, 19), false));
                out.put(MrzKeys.KEY_SEX, l2.substring(20, 21));
                out.put(MrzKeys.KEY_EXPIRY_DATE, isoDate(l2.substring(21, 27), true));
                if (!line2Only) {
                    String[] nameParts = splitNameParts(l1, 5);
                    out.put(MrzKeys.KEY_LAST_NAME, nameParts[0]);
                    out.put(MrzKeys.KEY_FIRST_NAME, nameParts[1]);
                }
            } else {
                String l1 = lines.get(0), l2 = lines.get(1);
                out.put(MrzKeys.KEY_DOCUMENT_TYPE, l1.substring(0, 2).replace("<", ""));
                out.put(MrzKeys.KEY_COUNTRY, l1.substring(2, 5).replace("<", ""));
                out.put(MrzKeys.KEY_DOCUMENT_NUMBER, l1.substring(5, 14).replace("<", ""));
                out.put(MrzKeys.KEY_BIRTH_DATE, isoDate(l2.substring(0, 6), false));
                out.put(MrzKeys.KEY_SEX, l2.substring(7, 8));
                out.put(MrzKeys.KEY_EXPIRY_DATE, isoDate(l2.substring(8, 14), true));
                out.put(MrzKeys.KEY_NATIONALITY, l2.substring(15, 18).replace("<", ""));
                if (!line2Only) {
                    String l3 = lines.get(2);
                    String[] nameParts = splitNameParts(l3, 0);
                    out.put(MrzKeys.KEY_LAST_NAME, nameParts[0]);
                    out.put(MrzKeys.KEY_FIRST_NAME, nameParts[1]);
                }
            }
            return out;
        }

        static String name(String line, int from) {
            String n = line.substring(from);
            String[] parts = n.split("<<", 2);
            String surname = parts[0].replace("<", " ").trim();
            String given = parts.length > 1 ? parts[1].replace("<", " ").trim() : "";
            return (surname + ", " + given).replaceAll("\\s+", " ").trim();
        }

        /** Surname/given name split apart (fields() combines them into one display string). */
        static String[] splitNameParts(String line, int from) {
            String n = line.substring(from);
            String[] parts = n.split("<<", 2);
            String surname = parts[0].replace("<", " ").trim().replaceAll("\\s+", " ");
            String given = parts.length > 1 ? parts[1].replace("<", " ").trim().replaceAll("\\s+", " ") : "";
            return new String[]{surname, given};
        }

        static String fmtDate(String yymmdd) {
            if (yymmdd.length() != 6) return yymmdd;
            return yymmdd.substring(0, 2) + "-" + yymmdd.substring(3, 5) + "-" + yymmdd.substring(6, 10)
                    + " (DD/MM/YYYY)";
        }

        /**
         * YYMMDD -> YYYY-MM-DD. MRZ years are two digits, so the century is a guess:
         * expiry dates are treated as 20xx; birth dates use (current year + 10) as the
         * pivot -- a yy above that is assumed 19xx, otherwise 20xx. Adjust the pivot if
         * your users skew noticeably older or younger than that.
         */
        static String isoDate(String yymmdd, boolean isExpiry) {
            if (yymmdd.length() != 6) return yymmdd;
            int yy = Integer.parseInt(yymmdd.substring(0, 2));
            int mm = Integer.parseInt(yymmdd.substring(2, 4));
            int dd = Integer.parseInt(yymmdd.substring(4, 6));
            int century;
            if (isExpiry) {
                century = 2000;
            } else {
                int pivot = (java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) % 100 + 10) % 100;
                century = (yy > pivot) ? 1900 : 2000;
            }
            return String.format("%02d/%02d/%04d", dd, mm, century + yy);
        }
    }

    // ---------- validation (with repair) ----------

    static Result validateTd3(List<String> lines) {
        String l2 = lines.get(1);
        Result r = new Result();
        r.lines = new ArrayList<>(lines); r.docType = "td3";
        r.docNum = checkDigit(l2.substring(0, 9), l2.charAt(9));
        if (!r.docNum) {
            String fixed = repairField(l2.substring(0, 9), l2.charAt(9));
            if (fixed != null) {
                l2 = fixed + l2.substring(9);
                r.lines.set(1, l2);
                r.docNum = true;
                r.repaired = true;
            }
        }
        r.dob = checkDigit(l2.substring(13, 19), l2.charAt(19));
        r.expiry = checkDigit(l2.substring(21, 27), l2.charAt(27));
        r.optional = checkDigit(l2.substring(28, 42), l2.charAt(42));
        String comp = l2.substring(0, 10) + l2.substring(13, 20) + l2.substring(21, 43);
        r.finalCd = checkDigit(comp, l2.charAt(43));
        return r;
    }

    static Result validateTd1(List<String> lines) {
        String l1 = lines.get(0), l2 = lines.get(1);
        Result r = new Result();
        r.lines = new ArrayList<>(lines); r.docType = "td1";
        r.docNum = checkDigit(l1.substring(5, 14), l1.charAt(14));
        if (!r.docNum) {
            String fixed = repairField(l1.substring(5, 14), l1.charAt(14));
            if (fixed != null) {
                l1 = l1.substring(0, 5) + fixed + l1.substring(14);
                r.lines.set(0, l1);
                r.docNum = true;
                r.repaired = true;
            }
        }
        r.dob = checkDigit(l2.substring(0, 6), l2.charAt(6));
        r.expiry = checkDigit(l2.substring(8, 14), l2.charAt(14));
        r.optional = true;
        String comp = l1.substring(5, 30) + l2.substring(0, 7) + l2.substring(8, 15) + l2.substring(18, 29);
        r.finalCd = checkDigit(comp, l2.charAt(29));
        return r;
    }

    // ---------- line-length normalization ----------

    /** Longest run of '<' in s: {start, length}, or null. */
    static int[] longestFillerRun(String s) {
        int bestStart = -1, bestLen = 0, i = 0;
        while (i < s.length()) {
            if (s.charAt(i) == '<') {
                int j = i;
                while (j < s.length() && s.charAt(j) == '<') j++;
                if (j - i > bestLen) { bestLen = j - i; bestStart = i; }
                i = j;
            } else i++;
        }
        return bestLen > 0 ? new int[]{bestStart, bestLen} : null;
    }

    /**
     * Variants of ln normalized to exactly len chars: trim/pad at the ends, and
     * shrink/grow the longest '<' run (fixes OCR inserting/dropping fillers mid-line).
     */
    static List<String> lengthVariants(String ln, int len) {
        List<String> out = new ArrayList<>();
        if (ln.length() == len) {
            out.add(ln);
            return out;
        }
        int[] run = longestFillerRun(ln);
        if (ln.length() > len) {
            int excess = ln.length() - len;
            if (run != null && run[1] > excess)  // collapse filler run
                out.add(ln.substring(0, run[0]) + ln.substring(run[0] + excess));
            out.add(ln.substring(0, len));        // trim back
            out.add(ln.substring(ln.length() - len)); // trim front
        } else {
            int missing = len - ln.length();
            if (run != null) {                    // grow filler run
                StringBuilder fill = new StringBuilder();
                for (int i = 0; i < missing; i++) fill.append('<');
                out.add(ln.substring(0, run[0]) + fill + ln.substring(run[0]));
            }
            StringBuilder b = new StringBuilder(ln); // pad end
            while (b.length() < len) b.append('<');
            out.add(b.toString());
        }
        // dedupe, keep order
        List<String> ded = new ArrayList<>();
        for (String s : out) if (!ded.contains(s)) ded.add(s);
        return ded;
    }

    // ---------- candidate search ----------

    static Result better(Result a, Result b) {
        if (a == null) return b;
        if (b == null) return a;
        return b.score() > a.score() ? b : a;
    }

    /** Best MRZ interpretation of recognized text, or null when no MRZ-like lines. */
    public static Result bestCandidate(String rawText) {
        List<String> raw = new ArrayList<>();
        for (String ln : rawText.split("\n")) {
            String c = clean(ln);
            if (c.length() >= 20) raw.add(c);
        }
        if (raw.isEmpty()) return null;
        Result best = null;
        boolean anyLong = false;
        for (String l : raw) if (l.length() >= 38) anyLong = true;
        String[] types = anyLong ? new String[]{"td3", "td1"} : new String[]{"td1", "td3"};

        for (String dt : types) {
            int n = dt.equals("td3") ? 2 : 3, len = dt.equals("td3") ? 44 : 30;
            for (int i = raw.size() - n; i >= 0; i--) {
                List<List<String>> perLine = new ArrayList<>();
                for (int j = 0; j < n; j++) perLine.add(lengthVariants(raw.get(i + j), len));
                int[] idx = new int[n];
                boolean more = true;
                while (more) {
                    List<String> cand = new ArrayList<>();
                    for (int j = 0; j < n; j++) cand.add(perLine.get(j).get(idx[j]));
                    for (List<String> c : Arrays.asList(cand, coerce(cand, dt))) {
                        try {
                            Result r = dt.equals("td3") ? validateTd3(c) : validateTd1(c);
                            best = better(best, r);
                            if (best.isComplete()) return best;
                        } catch (Exception ignored) { }
                    }
                    // advance cartesian index
                    int k = n - 1;
                    while (k >= 0 && ++idx[k] >= perLine.get(k).size()) { idx[k] = 0; k--; }
                    more = k >= 0;
                }
            }
        }

        // fallback: TD3 line 2 alone carries every check digit and the whole BAC input
        if (best == null || !best.bacReady()) {
            StringBuilder ph = new StringBuilder("P<");
            while (ph.length() < 44) ph.append('<');
            String placeholder = ph.toString();
            for (int i = raw.size() - 1; i >= 0; i--) {
                if (raw.get(i).length() < 40) continue;
                for (String v : lengthVariants(raw.get(i), 44)) {
                    List<String> cand = Arrays.asList(placeholder, v);
                    for (List<String> c : Arrays.asList(cand, coerce(cand, "td3"))) {
                        try {
                            Result r = validateTd3(c);
                            r.line2Only = true;
                            best = better(best, r);
                        } catch (Exception ignored) { }
                    }
                }
            }
        }
        return best;
    }

    // ---------- BAC key derivation (ICAO 9303 part 11) ----------

    public static String hex(byte[] b, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(String.format("%02X", b[i]));
        return sb.toString();
    }

    static byte[] adjustDesParity(byte[] key) {
        byte[] out = key.clone();
        for (int i = 0; i < out.length; i++) {
            int b = out[i] & 0xFE;
            out[i] = (byte) (b | (Integer.bitCount(b) % 2 == 0 ? 1 : 0));
        }
        return out;
    }

    static byte[] kdf(MessageDigest sha1, byte[] kseed, int c) {
        sha1.reset();
        sha1.update(kseed);
        sha1.update(new byte[]{0, 0, 0, (byte) c});
        return adjustDesParity(Arrays.copyOf(sha1.digest(), 16));
    }

    /** Returns {mrz_information, Kseed, Kenc, Kmac} as hex strings. */
    public static String[] bacKeys(Result r) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] kseed = Arrays.copyOf(sha1.digest(r.mrzInformation().getBytes("US-ASCII")), 16);
            return new String[]{r.mrzInformation(), hex(kseed, 16),
                    hex(kdf(sha1, kseed, 1), 16), hex(kdf(sha1, kseed, 2), 16)};
        } catch (Exception e) {
            return new String[]{"error: " + e, "", "", ""};
        }
    }

    private Mrz() { }
}