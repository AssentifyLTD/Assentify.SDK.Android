package com.assentify.sdk.ScanPassport;

import com.assentify.sdk.Core.Constants.MrzKeys;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;


/**
 * ICAO 9303 MRZ parsing (TD3 passports, TD1 ID cards), check-digit validation, OCR repair,
 * and BAC key derivation.
 *
 * Works for any issuing country:
 *  - line 1 / the name line is validated structurally (it has no check digit), so text from the
 *    visual zone ("UNITED STATES DEPARTMENT OF STATE" -> country "ITE") can never be taken as line 1;
 *  - line 1 and line 2 are paired by content, not only by position, so OCR that returns the lines
 *    out of order, splits a line in two or merges both lines still works;
 *  - '<' read as 'K' in fillers is repaired; country codes are checked against ISO 3166 + ICAO codes
 *    ("D" for Germany, "GBD", "UNO", "XXA", ...);
 *  - TD1 document numbers longer than 9 characters (ICAO extended format) are supported.
 */
public final class Mrz {

    static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789<";
    static final int TD3_LEN = 44, TD1_LEN = 30;

    /** TD3 line 1 used when only line 2 was read (fails every line-1 check on purpose). */
    static final String TD3_PLACEHOLDER;

    static {
        StringBuilder ph = new StringBuilder("P<");
        while (ph.length() < TD3_LEN) ph.append('<');
        TD3_PLACEHOLDER = ph.toString();
    }

    public static String clean(String line) {
        String s = line.toUpperCase(Locale.US)
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

    // ---------- country codes (ISO 3166 alpha-3 + ICAO 9303 special codes) ----------

    static final Set<String> COUNTRY_CODES = buildCountryCodes();

    private static Set<String> buildCountryCodes() {
        Set<String> s = new HashSet<>();
        for (String a2 : Locale.getISOCountries()) {
            try {
                String a3 = new Locale("", a2).getISO3Country();
                if (a3 != null && a3.length() == 3) s.add(a3.toUpperCase(Locale.US));
            } catch (MissingResourceException ignored) {
            }
        }
        Collections.addAll(s,
                "D",                                        // Germany
                "GBD", "GBN", "GBO", "GBP", "GBS",          // British nationality categories
                "UNO", "UNA", "UNK",                        // United Nations
                "XXA", "XXB", "XXC", "XXX",                 // stateless / refugee / unspecified
                "EUE",                                      // European Union
                "XBA", "XIM", "XCC", "XCE", "XCO", "XEC", "XES", "XMP", "XOM", "XPO", "XDC",
                "RKS", "XKX",                               // Kosovo
                "ANT", "NTZ", "ZIM", "BUR", "YUG", "SCG", "CSK", "DDR", "SUN");   // older codes
        return s;
    }

    static boolean isKnownCountry(String field) {
        String c = field.replace("<", "");
        return !c.isEmpty() && COUNTRY_CODES.contains(c);
    }

    /** Country field without fillers; Germany's one-letter "D" becomes "DEU". */
    static String normalizeCountry(String field) {
        String c = field.replace("<", "");
        return c.equals("D") ? "DEU" : c;
    }

    // ---------- structural checks for the lines without check digits ----------

    static boolean isAlphaOrFiller(String s, int from, int to) {
        for (int i = from; i < to; i++) {
            char c = s.charAt(i);
            if (c != '<' && (c < 'A' || c > 'Z')) return false;
        }
        return true;
    }

    /**
     * A name zone: letters and '<' only, at least 2 letters, and no name after a run of 4+ fillers
     * (real names are separated by "<<" or "<"; the run of fillers only comes at the end).
     */
    static boolean namePlausible(String zone) {
        int letters = 0, run = 0;
        boolean longRun = false;
        for (int i = 0; i < zone.length(); i++) {
            char c = zone.charAt(i);
            if (c == '<') {
                if (++run >= 4) longRun = true;
                continue;
            }
            if (c < 'A' || c > 'Z' || longRun) return false;
            run = 0;
            letters++;
        }
        return letters >= 2;
    }

    /** TD3 line 1: "P" + type + issuing state + name zone. */
    static boolean td3Line1Plausible(String l1) {
        if (l1 == null || l1.length() != TD3_LEN || l1.charAt(0) != 'P') return false;
        if (!isAlphaOrFiller(l1, 1, 5) || l1.charAt(2) == '<') return false;
        return namePlausible(l1.substring(5));
    }

    /** Count of digits at the given positions (dates must be digits, after coercion). */
    static int digitsAt(String s, int[][] ranges) {
        int n = 0;
        for (int[] r : ranges)
            for (int i = r[0]; i < r[1] && i < s.length(); i++) {
                char c = s.charAt(i);
                if (c >= '0' && c <= '9') n++;
            }
        return n;
    }

    /**
     * TD3 line 2: birth date + check digit and expiry date + check digit (14 positions) are digits,
     * nationality is letters / '<'. Page text ("DENEPASLEPLIERLEPERFORER...", any language) fails here.
     */
    static boolean td3Line2Plausible(String l2) {
        if (l2 == null || l2.length() != TD3_LEN) return false;
        if (digitsAt(l2, new int[][]{{13, 20}, {21, 28}}) < 12) return false;
        return isAlphaOrFiller(l2, 10, 13);
    }

    /** TD1 line 2: birth date + check digit and expiry date + check digit are digits. */
    static boolean td1Line2Plausible(String l2) {
        if (l2 == null || l2.length() != TD1_LEN) return false;
        if (digitsAt(l2, new int[][]{{0, 7}, {8, 15}}) < 12) return false;
        return isAlphaOrFiller(l2, 15, 18);
    }

    /** TD1: line 1 starts with I / A / C + issuing state, line 3 is a name. */
    static boolean td1HeaderAndNamePlausible(List<String> lines) {
        String l1 = lines.get(0), l3 = lines.get(2);
        if (l1.length() != TD1_LEN || l3.length() != TD1_LEN) return false;
        char t = l1.charAt(0);
        if (t != 'I' && t != 'A' && t != 'C') return false;
        if (!isAlphaOrFiller(l1, 1, 5) || l1.charAt(2) == '<') return false;
        return namePlausible(l3);
    }

    // ---------- coercion of strictly-typed positions ----------

    static final String TO_D_FROM = "OQDILZSBGT", TO_D_TO = "0001125867";
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

    /**
     * OCR often reads the filler '<' as 'K'. In [from, to): a run of 2+ 'K' touching a filler, or a
     * single 'K' with "<<" on both sides, is turned back into fillers. A one-letter initial
     * ("JOHN<K<<<") and names containing K ("JACK<<<") are kept.
     */
    static String fixFillers(String ln, int from, int to) {
        char[] c = ln.toCharArray();
        int end = Math.min(to, c.length);
        int i = Math.max(0, from);
        while (i < end) {
            if (c[i] != 'K') {
                i++;
                continue;
            }
            int j = i;
            while (j < end && c[j] == 'K') j++;
            boolean leftFill = i > from && c[i - 1] == '<';
            boolean rightFill = j >= end || c[j] == '<';
            boolean leftDouble = i - 2 >= from && c[i - 1] == '<' && c[i - 2] == '<';
            boolean rightDouble = j >= end || (j + 1 < end && c[j] == '<' && c[j + 1] == '<');
            if ((j - i >= 2 && (leftFill || rightFill)) || (leftDouble && rightDouble)) {
                for (int k = i; k < j; k++) c[k] = '<';
            }
            i = j;
        }
        return new String(c);
    }

    /** Sex: M, F, X or '<'; fixes the usual misreads. */
    static String coerceSex(String ln, int p) {
        if (p >= ln.length()) return ln;
        char s = ln.charAt(p), f;
        switch (s) {
            case 'H': case 'N': f = 'M'; break;
            case 'E': case 'P': f = 'F'; break;
            case 'K': f = '<'; break;
            default: return ln;
        }
        return ln.substring(0, p) + f + ln.substring(p + 1);
    }

    static String coerceTd3Line1(String l1) {
        String s = coerceRange(l1, 1, TD3_LEN, false);
        s = fixFillers(s, 5, TD3_LEN);
        // 'P' misread (only lines that contain '<' get here, so visual-zone text is never changed)
        if (!s.isEmpty() && "RFB".indexOf(s.charAt(0)) >= 0) s = 'P' + s.substring(1);
        return s;
    }

    static String coerceTd3Line2(String l2) {
        l2 = coerceRange(l2, 9, 10, true);
        l2 = coerceRange(l2, 10, 13, false);
        l2 = coerceRange(l2, 13, 20, true);
        l2 = coerceSex(l2, 20);
        l2 = coerceRange(l2, 21, 28, true);
        l2 = coerceRange(l2, 42, 44, true);
        return l2;
    }

    static List<String> coerce(List<String> lines, String docType) {
        List<String> out = new ArrayList<>();
        if (docType.equals("td3")) {
            out.add(coerceTd3Line1(lines.get(0)));
            out.add(coerceTd3Line2(lines.get(1)));
        } else {
            String l1 = coerceRange(lines.get(0), 2, 5, false);
            if (td1LongNumber(l1) == null) l1 = coerceRange(l1, 14, 15, true);
            String l2 = lines.get(1);
            l2 = coerceRange(l2, 0, 7, true);
            l2 = coerceSex(l2, 7);
            l2 = coerceRange(l2, 8, 15, true);
            l2 = coerceRange(l2, 15, 18, false);
            l2 = coerceRange(l2, 29, 30, true);
            String l3 = fixFillers(coerceRange(lines.get(2), 0, TD1_LEN, false), 0, TD1_LEN);
            out.add(l1);
            out.add(l2);
            out.add(l3);
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

    /** At most this many characters are swapped to repair a field: with more, a single check digit
     *  (1 chance in 10 to pass by luck) "repairs" any text into a valid-looking number. */
    static final int MAX_REPAIR_SWAPS = 2;

    /**
     * If checkDigit(field, cd) fails, search lookalike-swap combinations (at most
     * MAX_REPAIR_SWAPS swaps) for a variant that passes. Returns the repaired field, or null.
     */
    static String repairField(String field, char cd) {
        if (cd < '0' || cd > '9') return null;                  // no real check digit to repair against
        List<Integer> amb = new ArrayList<>();
        for (int i = 0; i < field.length(); i++)
            if (alt(field.charAt(i)) != field.charAt(i)) amb.add(i);
        int n = Math.min(amb.size(), 10);
        if (n == 0) return null;
        List<Integer> masks = new ArrayList<>();
        for (int m = 1; m < (1 << n); m++) if (Integer.bitCount(m) <= MAX_REPAIR_SWAPS) masks.add(m);
        masks.sort((a, b) -> Integer.bitCount(a) - Integer.bitCount(b)); // fewest swaps first
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

    // ---------- TD1 extended document number (more than 9 characters) ----------

    /**
     * ICAO 9303 part 5: when the number is longer than 9 characters, position 15 is '<' and the rest
     * of the number plus its check digit continue in the optional data. Returns {number, checkDigit}
     * or null for an ordinary 9-character number.
     */
    static String[] td1LongNumber(String l1) {
        if (l1 == null || l1.length() < TD1_LEN || l1.charAt(14) != '<' || l1.charAt(15) == '<') return null;
        int end = l1.indexOf('<', 15);
        if (end < 0) end = TD1_LEN;
        if (end - 1 <= 15) return null;
        return new String[]{l1.substring(5, 14) + l1.substring(15, end - 1), String.valueOf(l1.charAt(end - 1))};
    }

    // ---------- cross-check with the visual zone (VIZ) ----------

    /** Characters OCR confuses in the MRZ font (OCR-B), as groups */
    static final String[] LOOKALIKE_GROUPS = {"0ODQ", "1IL", "2Z", "4A", "5S", "6G", "7T", "8B"};

    static boolean lookalike(char a, char b) {
        if (a == b) return true;
        for (String g : LOOKALIKE_GROUPS) if (g.indexOf(a) >= 0 && g.indexOf(b) >= 0) return true;
        return false;
    }

    /** The other members of c's lookalike group */
    static List<Character> lookalikesOf(char c) {
        List<Character> out = new ArrayList<>();
        for (String g : LOOKALIKE_GROUPS) {
            if (g.indexOf(c) < 0) continue;
            for (char o : g.toCharArray()) if (o != c) out.add(o);
        }
        return out;
    }

    /**
     * Variants of s with exactly k lookalike swaps, for k = 0..maxSwaps (list index = k).
     * Each position is swapped at most once.
     */
    static List<List<String>> lookalikeVariants(String s, int maxSwaps) {
        List<List<String>> byCount = new ArrayList<>();
        List<String> zero = new ArrayList<>();
        zero.add(s);
        byCount.add(zero);
        for (int k = 1; k <= maxSwaps; k++) {
            List<String> next = new ArrayList<>();
            for (String v : byCount.get(k - 1)) {
                for (int i = 0; i < v.length(); i++) {
                    if (v.charAt(i) != s.charAt(i)) continue;
                    for (char c : lookalikesOf(v.charAt(i))) {
                        String w = v.substring(0, i) + c + v.substring(i + 1);
                        if (!next.contains(w)) next.add(w);
                    }
                }
            }
            byCount.add(next);
        }
        return byCount;
    }

    static String padRight(String s, int len) {
        StringBuilder b = new StringBuilder(s);
        while (b.length() < len) b.append('<');
        return b.toString();
    }

    /** Pairs that page fonts themselves draw alike (French passports print "I" like "1") */
    static final String[] PAGE_AMBIGUOUS = {"1I", "0O"};

    /** How far a candidate is from the printed number: overriding a character the page font
     *  draws ambiguously costs 1, overriding any other printed character costs 2. */
    static int swapPenalty(String cand, String printed) {
        int p = 0;
        for (int i = 0; i < cand.length(); i++) {
            char a = cand.charAt(i), b = printed.charAt(i);
            if (a == b) continue;
            boolean pageAmbiguous = false;
            for (String g : PAGE_AMBIGUOUS) if (g.indexOf(a) >= 0 && g.indexOf(b) >= 0) pageAmbiguous = true;
            p += pageAmbiguous ? 1 : 2;
        }
        return p;
    }

    /** Number of separate letter groups ("25DI68178" = 1, "25D168I78" = 2) */
    static int letterBlocks(String s) {
        int blocks = 0;
        boolean inLetters = false;
        for (char c : s.toCharArray()) {
            boolean letter = c >= 'A' && c <= 'Z';
            if (letter && !inLetters) blocks++;
            inLetters = letter;
        }
        return blocks;
    }

    /**
     * The document number is also printed in the visual zone of the page. A misread MRZ number can
     * pass its check digit by coincidence ("25DI68178" read as "2S016B178"), and the composite check
     * digit cannot help: changing only the number shifts both sums by the same amount.
     *
     * Country-independent rule:
     *  - take every number on the page that differs from the MRZ number only by lookalikes
     *    (0/O/D/Q, 1/I/L, 5/S, 8/B, 2/Z, 6/G, 7/T, 4/A);
     *  - allow up to 2 more lookalike swaps, because the page font can mislead too
     *    (French passports print "I" like "1");
     *  - keep the versions that pass the check digit, ranked by: fewest overrides of the page
     *    (overriding it where it disagrees with the MRZ costs more), then fewest letter groups.
     * The best one is used; the others and the MRZ as read are kept, best first, in
     * {@link Result#alternativeLines} so the NFC step lets the chip decide.
     */
    static Result crossCheckWithVisualZone(Result r, String rawText) {
        if (r == null || rawText == null || r.lines == null) return r;
        try {
            boolean td3 = r.docType.equals("td3");
            if (!td3 && td1LongNumber(r.lines.get(0)) != null) return r;   // extended numbers: not handled here
            int lineIndex = td3 ? 1 : 0;
            int from = td3 ? 0 : 5;
            String docLine = r.lines.get(lineIndex);
            String field = docLine.substring(from, from + 9);
            char cd = docLine.charAt(from + 9);
            String doc = field.replace("<", "");
            if (doc.length() < 6) return r;

            // candidate number -> {penalty, letterBlocks}
            Map<String, int[]> scored = new HashMap<>();
            for (String line : rawText.split("\n")) {
                // Skip the MRZ lines themselves: only the visual zone is an independent reading
                String cleaned = clean(line);
                if (cleaned.indexOf('<') >= 0 || cleaned.length() >= 30) continue;

                // Single words, plus two neighbouring words glued together ("25DI 68178")
                String[] words = line.toUpperCase(Locale.US).split("[^A-Z0-9]+");
                List<String> tokens = new ArrayList<>(Arrays.asList(words));
                for (int w = 0; w + 1 < words.length; w++) tokens.add(words[w] + words[w + 1]);

                for (String token : tokens) {
                    if (token.length() != doc.length()) continue;
                    boolean similar = true;
                    for (int i = 0; i < token.length() && similar; i++) {
                        similar = lookalike(token.charAt(i), doc.charAt(i));
                    }
                    if (!similar) continue;

                    // The MRZ reading competes too (when its check digit holds): a correct MRZ must win
                    List<String> pool = new ArrayList<>();
                    for (List<String> level : lookalikeVariants(token, 2)) pool.addAll(level);
                    if (r.docNum) pool.add(doc);
                    for (String cand : pool) {
                        if (!checkDigit(padRight(cand, 9), cd)) continue;
                        int[] score = {swapPenalty(cand, token), letterBlocks(cand), cand.equals(doc) ? 0 : 1};
                        int[] old = scored.get(cand);
                        if (old == null || compareScore(score, old) < 0) scored.put(cand, score);
                    }
                }
            }
            if (scored.isEmpty()) return r;

            List<String> ranked = new ArrayList<>(scored.keySet());
            ranked.sort((a, b) -> {
                int c = compareScore(scored.get(a), scored.get(b));
                return c != 0 ? c : a.compareTo(b);
            });
            // The MRZ reading is the best explanation of the page: keep it unchanged
            if (ranked.get(0).equals(doc)) return r;

            List<List<String>> alternatives = new ArrayList<>();
            Result best = null;
            for (String cand : ranked) {
                if (cand.equals(doc)) continue; // added at the end as "the MRZ as read"
                List<String> lines = new ArrayList<>(r.lines);
                lines.set(lineIndex, docLine.substring(0, from) + padRight(cand, 9) + docLine.substring(from + 9));
                Result v = td3 ? validateTd3(lines) : validateTd1(lines);
                if (!v.bacReady() || (r.finalCd && !v.finalCd)) continue;
                if (best == null) best = v;
                else if (alternatives.size() < MAX_ALTERNATIVES - 1) alternatives.add(lines);
            }
            if (best == null) return r;
            alternatives.add(new ArrayList<>(r.lines)); // the MRZ as read stays as the last option

            best.line2Only = r.line2Only;
            best.repaired = true;
            best.vizCorrected = true;
            best.mrzOriginalLines = new ArrayList<>(r.lines);
            best.alternativeLines = alternatives;
            return best;
        } catch (Exception e) {
            return r;
        }
    }

    static final int MAX_ALTERNATIVES = 2;

    /** penalty, then letter groups, then "is the MRZ reading" (ties go to the MRZ) */
    static int compareScore(int[] a, int[] b) {
        for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return Integer.compare(a[i], b[i]);
        return 0;
    }

    // ---------- result ----------

    public static class Result {
        public List<String> lines;
        public String docType;
        public boolean docNum, dob, expiry, optional, finalCd;
        public boolean line2Only = false;
        public boolean repaired = false;
        /** The lines without check digits (TD3 line 1, TD1 header + name line) are structurally valid */
        public boolean line1Ok = false;
        /** Issuing state / nationality are real ISO 3166 or ICAO codes */
        public boolean countryKnown = false, nationalityKnown = false;
        /** Document number taken from the visual zone instead of the (misread) MRZ */
        public boolean vizCorrected = false;
        /** The MRZ lines as read, when the visual zone corrected them; null otherwise */
        public List<String> mrzOriginalLines = null;
        /** Other plausible readings (best first, MRZ as read last) for the NFC step; null if none */
        public List<List<String>> alternativeLines = null;

        public boolean allValid() { return docNum && dob && expiry && optional && finalCd; }
        public boolean bacReady() { return docNum && dob && expiry; }

        /**
         * Check digits dominate: each BAC field (6) outweighs everything about line 1 together (5),
         * so a nice line 1 can never make up for a failed date or document number.
         */
        public int score() {
            return 6 * ((docNum ? 1 : 0) + (dob ? 1 : 0) + (expiry ? 1 : 0))
                    + 2 * ((optional ? 1 : 0) + (finalCd ? 1 : 0))
                    + (line2Only ? 0 : 1) + (line1Ok ? 2 : 0)
                    + (countryKnown ? 1 : 0) + (nationalityKnown ? 1 : 0);
        }

        /**
         * True only when the reading can be accepted as it is: all check digits valid, line 1 read
         * and structurally valid, and every required output field present. (A person with a single
         * name has no given name, so the first name is not required.)
         */
        public boolean isComplete() {
            if (!allValid() || line2Only || !line1Ok) return false;
            Map<String, Object> props = toOutputProperties();
            for (String key : new String[]{
                    MrzKeys.KEY_DOCUMENT_TYPE, MrzKeys.KEY_COUNTRY, MrzKeys.KEY_DOCUMENT_NUMBER,
                    MrzKeys.KEY_NATIONALITY, MrzKeys.KEY_BIRTH_DATE, MrzKeys.KEY_SEX,
                    MrzKeys.KEY_EXPIRY_DATE, MrzKeys.KEY_LAST_NAME}) {
                Object v = props.get(key);
                if (v == null || (v instanceof String && ((String) v).trim().isEmpty())) return false;
            }
            return true;
        }

        /** The document number, including the extended part of a long TD1 number. */
        public String documentNumber() {
            if (docType.equals("td3")) return lines.get(1).substring(0, 9).replace("<", "");
            String[] ln = td1LongNumber(lines.get(0));
            return ln != null ? ln[0] : lines.get(0).substring(5, 14).replace("<", "");
        }

        public String mrzInformation() {
            if (docType.equals("td3")) {
                String l2 = lines.get(1);
                return l2.substring(0, 10) + l2.substring(13, 20) + l2.substring(21, 28);
            }
            String l1 = lines.get(0), l2 = lines.get(1);
            String[] ln = td1LongNumber(l1);
            String doc = ln != null ? ln[0] + ln[1] : l1.substring(5, 15);
            return doc + l2.substring(0, 7) + l2.substring(8, 15);
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
            else if (!line1Ok) s += " [line 1 invalid]";
            if (repaired) s += " [auto-repaired]";
            if (vizCorrected) s += " [doc# from visual zone]";
            return s;
        }

        public String[][] fields() {
            if (!docType.equals("td3")) {
                String l1 = lines.get(0), l2 = lines.get(1), l3 = lines.get(2);
                return new String[][]{
                        {"Document", l1.substring(0, 2).replace("<", "")},
                        {"Issuing state", normalizeCountry(l1.substring(2, 5))},
                        {"Document no.", documentNumber()},
                        {"Birth date", fmtDate(l2.substring(0, 6))},
                        {"Sex", l2.substring(7, 8)},
                        {"Expiry date", fmtDate(l2.substring(8, 14))},
                        {"Nationality", normalizeCountry(l2.substring(15, 18))},
                        {"Name", line1Ok ? name(l3, 0) : "(name not read)"},
                };
            }
            String l1 = lines.get(0), l2 = lines.get(1);
            boolean nameOk = !line2Only && line1Ok;
            return new String[][]{
                    {"Document", l1.substring(0, 2).replace("<", "")},
                    {"Issuing state", nameOk ? normalizeCountry(l1.substring(2, 5)) : ""},
                    {"Name", nameOk ? name(l1, 5) : "(line 1 not read)"},
                    {"Document no.", documentNumber()},
                    {"Nationality", normalizeCountry(l2.substring(10, 13))},
                    {"Birth date", fmtDate(l2.substring(13, 19))},
                    {"Sex", l2.substring(20, 21)},
                    {"Expiry date", fmtDate(l2.substring(21, 27))},
                    {"Personal no.", l2.substring(28, 42).replace("<", " ").trim()},
            };
        }

        /**
         * Same underlying data as {@link #fields()}, keyed by the OnBoardMe
         * IdentificationDocumentCapture output property keys, with surname / given name split.
         *
         * Issuing state and names come from the line without check digits: when that line was not
         * read or is not valid, the issuing state is "" and the names are omitted, never guessed.
         */
        public Map<String, Object> toOutputProperties() {
            Map<String, Object> out = new HashMap<>();
            if (docType.equals("td3")) {
                String l1 = lines.get(0), l2 = lines.get(1);
                boolean nameOk = !line2Only && line1Ok;
                out.put(MrzKeys.KEY_DOCUMENT_TYPE, l1.substring(0, 2).replace("<", ""));
                out.put(MrzKeys.KEY_COUNTRY, nameOk ? normalizeCountry(l1.substring(2, 5)) : "");
                out.put(MrzKeys.KEY_DOCUMENT_NUMBER, documentNumber());
                out.put(MrzKeys.KEY_NATIONALITY, normalizeCountry(l2.substring(10, 13)));
                out.put(MrzKeys.KEY_BIRTH_DATE, isoDate(l2.substring(13, 19), false));
                out.put(MrzKeys.KEY_SEX, l2.substring(20, 21));
                out.put(MrzKeys.KEY_EXPIRY_DATE, isoDate(l2.substring(21, 27), true));
                if (nameOk) {
                    String[] nameParts = splitNameParts(l1, 5);
                    out.put(MrzKeys.KEY_LAST_NAME, nameParts[0]);
                    out.put(MrzKeys.KEY_FIRST_NAME, nameParts[1]);
                }
            } else {
                String l1 = lines.get(0), l2 = lines.get(1);
                out.put(MrzKeys.KEY_DOCUMENT_TYPE, l1.substring(0, 2).replace("<", ""));
                out.put(MrzKeys.KEY_COUNTRY, line1Ok ? normalizeCountry(l1.substring(2, 5)) : "");
                out.put(MrzKeys.KEY_DOCUMENT_NUMBER, documentNumber());
                out.put(MrzKeys.KEY_BIRTH_DATE, isoDate(l2.substring(0, 6), false));
                out.put(MrzKeys.KEY_SEX, l2.substring(7, 8));
                out.put(MrzKeys.KEY_EXPIRY_DATE, isoDate(l2.substring(8, 14), true));
                out.put(MrzKeys.KEY_NATIONALITY, normalizeCountry(l2.substring(15, 18)));
                if (!line2Only && line1Ok) {
                    String[] nameParts = splitNameParts(lines.get(2), 0);
                    out.put(MrzKeys.KEY_LAST_NAME, nameParts[0]);
                    out.put(MrzKeys.KEY_FIRST_NAME, nameParts[1]);
                }
            }
            return out;
        }

        static String name(String line, int from) {
            String[] p = splitNameParts(line, from);
            return (p[0] + ", " + p[1]).replaceAll("\\s+", " ").trim();
        }

        /** Surname / given names split apart. */
        static String[] splitNameParts(String line, int from) {
            String n = line.substring(from);
            String[] parts = n.split("<<", 2);
            String surname = parts[0].replace("<", " ").trim().replaceAll("\\s+", " ");
            String given = parts.length > 1 ? parts[1].replace("<", " ").trim().replaceAll("\\s+", " ") : "";
            return new String[]{surname, given};
        }

        /** YYMMDD -> "DD/MM/YY" */
        static String fmtDate(String yymmdd) {
            if (yymmdd.length() != 6) return yymmdd;
            return yymmdd.substring(4, 6) + "/" + yymmdd.substring(2, 4) + "/" + yymmdd.substring(0, 2);
        }

        /**
         * YYMMDD -> DD/MM/YYYY. MRZ years have two digits, so the century is inferred:
         *  - birth: a year after the current one is 19xx, otherwise 20xx;
         *  - expiry: up to 20 years ahead is 20xx, otherwise 19xx (an old expired passport).
         * Unknown parts ("<<", allowed by ICAO for an unknown day or month) return the field as read.
         */
        static String isoDate(String yymmdd, boolean isExpiry) {
            if (yymmdd.length() != 6) return yymmdd;
            int yy, mm, dd;
            try {
                yy = Integer.parseInt(yymmdd.substring(0, 2));
                mm = Integer.parseInt(yymmdd.substring(2, 4));
                dd = Integer.parseInt(yymmdd.substring(4, 6));
            } catch (NumberFormatException e) {
                return yymmdd;
            }
            int now = Calendar.getInstance().get(Calendar.YEAR) % 100;
            int century = isExpiry ? (yy <= now + 20 ? 2000 : 1900) : (yy <= now ? 2000 : 1900);
            return String.format(Locale.US, "%02d/%02d/%04d", dd, mm, century + yy);
        }
    }

    // ---------- validation (with repair) ----------

    static Result validateTd3(List<String> lines) {
        String l1 = lines.get(0), l2 = lines.get(1);
        Result r = new Result();
        r.lines = new ArrayList<>(lines);
        r.docType = "td3";
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
        if (!r.optional) {                                      // '<' read as 'K' in the optional data
            String fixed = fixFillers(l2, 28, 42);
            if (!fixed.equals(l2) && checkDigit(fixed.substring(28, 42), fixed.charAt(42))) {
                l2 = fixed;
                r.lines.set(1, l2);
                r.optional = true;
                r.repaired = true;
            }
        }
        String comp = l2.substring(0, 10) + l2.substring(13, 20) + l2.substring(21, 43);
        r.finalCd = checkDigit(comp, l2.charAt(43));
        r.line1Ok = td3Line1Plausible(l1);
        r.countryKnown = r.line1Ok && isKnownCountry(l1.substring(2, 5));
        r.nationalityKnown = isKnownCountry(l2.substring(10, 13));
        return r;
    }

    static Result validateTd1(List<String> lines) {
        String l1 = lines.get(0), l2 = lines.get(1);
        Result r = new Result();
        r.lines = new ArrayList<>(lines);
        r.docType = "td1";
        String[] longNo = td1LongNumber(l1);
        if (longNo != null) {
            r.docNum = checkDigit(longNo[0], longNo[1].charAt(0));
        } else {
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
        }
        r.dob = checkDigit(l2.substring(0, 6), l2.charAt(6));
        r.expiry = checkDigit(l2.substring(8, 14), l2.charAt(14));
        r.optional = true;
        String comp = l1.substring(5, 30) + l2.substring(0, 7) + l2.substring(8, 15) + l2.substring(18, 29);
        r.finalCd = checkDigit(comp, l2.charAt(29));
        r.line1Ok = td1HeaderAndNamePlausible(r.lines);
        r.countryKnown = r.line1Ok && isKnownCountry(l1.substring(2, 5));
        r.nationalityKnown = isKnownCountry(l2.substring(15, 18));
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
                if (j - i > bestLen) {
                    bestLen = j - i;
                    bestStart = i;
                }
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
        List<String> ded = new ArrayList<>();
        for (String s : out) if (!ded.contains(s)) ded.add(s);
        return ded;
    }

    // ---------- OCR lines ----------

    /** A cleaned OCR line and its position in the text (halves of a split line get +-0.5). */
    static final class Line {
        final String text;
        final double pos;

        Line(String text, double pos) {
            this.text = text;
            this.pos = pos;
        }
    }

    /**
     * Cleaned OCR lines that could be MRZ lines, in reading order. Two MRZ lines read as one are
     * split (line 2 is the last 44 characters: line 1 is the one that loses fillers), and a line
     * read in two pieces is also offered joined.
     */
    static List<Line> mrzLines(String rawText) {
        String[] src = rawText.split("\n");
        List<String> cleaned = new ArrayList<>();
        for (String s : src) cleaned.add(clean(s));

        List<Line> out = new ArrayList<>();
        for (int i = 0; i < cleaned.size(); i++) {
            String c = cleaned.get(i);
            if (c.length() < 10) continue;
            if (c.length() >= TD3_LEN + 15) {                   // no single MRZ line is this long
                out.add(new Line(c.substring(0, c.length() - TD3_LEN), i - 0.5));
                out.add(new Line(c.substring(c.length() - TD3_LEN), i + 0.5));
            } else {
                out.add(new Line(c, i));
            }
            if (i + 1 < cleaned.size()) {
                String n = cleaned.get(i + 1);
                int sum = c.length() + n.length();
                if (c.length() >= 10 && n.length() >= 10 && c.length() < 38 && n.length() < 38
                        && sum >= TD3_LEN - 2 && sum <= TD3_LEN + 3) {
                    out.add(new Line(c + n, i + 0.75));
                }
            }
        }
        out.sort((a, b) -> Double.compare(a.pos, b.pos));
        return out;
    }

    // ---------- candidate search ----------

    static Result better(Result a, Result b) {
        if (a == null) return b;
        if (b == null) return a;
        return b.score() > a.score() ? b : a;
    }

    private static List<String> distinct(String a, String b) {
        return a.equals(b) ? Collections.singletonList(a) : Arrays.asList(a, b);
    }

    /** Best MRZ interpretation of recognized text, or null when no MRZ-like lines. */
    public static Result bestCandidate(String rawText) {
        // The document number is cross-checked with the visual zone of the page before returning
        return crossCheckWithVisualZone(bestMrzCandidate(rawText), rawText);
    }

    /** Best MRZ interpretation from the MRZ lines alone */
    static Result bestMrzCandidate(String rawText) {
        if (rawText == null) return null;
        List<Line> lines = mrzLines(rawText);
        if (lines.isEmpty()) return null;

        boolean anyLong = false;
        for (Line l : lines) if (l.text.length() >= 38) anyLong = true;

        Result td3 = anyLong ? bestTd3(lines) : null;
        if (td3 != null && td3.isComplete()) return td3;
        return better(td3, bestTd1(lines));
    }

    /**
     * TD3: every line that can be line 2 (check digits) is paired with every line that can be
     * line 1 (structure), not only the line just above it. Best score wins; on a tie, the pair
     * closest to "line 1 directly above line 2". Line 2 alone is a candidate too (line2Only).
     */
    static Result bestTd3(List<Line> lines) {
        Result best = null;
        double bestDist = Double.MAX_VALUE;

        for (Line a : lines) {
            if (a.text.length() < 38) continue;
            for (String v2 : lengthVariants(a.text, TD3_LEN)) {
                for (String c2 : distinct(v2, coerceTd3Line2(v2))) {
                    if (!td3Line2Plausible(c2)) continue;               // page text, not an MRZ line
                    Result probe;
                    try {
                        probe = validateTd3(Arrays.asList(TD3_PLACEHOLDER, c2));
                    } catch (Exception e) {
                        continue;
                    }
                    probe.line2Only = true;
                    if (best == null || probe.score() > best.score()) {
                        best = probe;
                        bestDist = Double.MAX_VALUE;
                    }
                    if (!probe.docNum && !probe.dob && !probe.expiry) continue;   // not a line 2

                    for (Line b : lines) {
                        // line 1 always has fillers; visual-zone text never does
                        if (b == a || b.text.indexOf('<') < 0 || b.text.length() > TD3_LEN + 6) continue;
                        for (String v1 : lengthVariants(b.text, TD3_LEN)) {
                            for (String c1 : distinct(v1, coerceTd3Line1(v1))) {
                                if (!td3Line1Plausible(c1)) continue;
                                Result r;
                                try {
                                    r = validateTd3(Arrays.asList(c1, c2));
                                } catch (Exception e) {
                                    continue;
                                }
                                double dist = Math.abs(a.pos - b.pos - 1);
                                if (r.score() > best.score() || (r.score() == best.score() && dist < bestDist)) {
                                    best = r;
                                    bestDist = dist;
                                }
                                if (bestDist == 0 && best.isComplete()) return best;
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    /** TD1: three consecutive lines of ~30 characters. */
    static Result bestTd1(List<Line> all) {
        List<String> raw = new ArrayList<>();
        for (Line l : all) if (l.text.length() >= 20 && l.text.length() <= 40) raw.add(l.text);
        Result best = null;
        for (int i = raw.size() - 3; i >= 0; i--) {
            List<List<String>> perLine = new ArrayList<>();
            for (int j = 0; j < 3; j++) perLine.add(lengthVariants(raw.get(i + j), TD1_LEN));
            int[] idx = new int[3];
            boolean more = true;
            while (more) {
                List<String> cand = new ArrayList<>();
                for (int j = 0; j < 3; j++) cand.add(perLine.get(j).get(idx[j]));
                for (List<String> c : Arrays.asList(cand, coerce(cand, "td1"))) {
                    if (!td1Line2Plausible(c.get(1))) continue;
                    try {
                        best = better(best, validateTd1(c));
                        if (best.isComplete()) return best;
                    } catch (Exception ignored) {
                    }
                }
                int k = 2;
                while (k >= 0 && ++idx[k] >= perLine.get(k).size()) {
                    idx[k] = 0;
                    k--;
                }
                more = k >= 0;
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