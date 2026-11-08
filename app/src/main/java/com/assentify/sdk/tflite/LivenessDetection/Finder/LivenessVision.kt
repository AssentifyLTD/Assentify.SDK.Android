package com.assentify.sdk.tflite.LivenessDetection.Finder

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Pixel maths of the rc17 document-liveness spec (02-core-spec), pure Kotlin (no OpenCV calls):
 *
 *  - rectify + corner roll ........ checks/rectify-colour.md  (makeBaseline, rectify, sRGB -> CIELAB)
 *  - laminate colours per frame ... checks/laminate.md        (iridescence: 12 hue sectors)
 *  - page response to the light ... checks/reflection.md      (pageResponse: spec / chroma / flare / veilLift)
 *  - sharpness and glare .......... checks/quality.md         (Laplacian variance + grey > 249 in the shrunk outline)
 *
 * All frames are the ANA (analysis) bitmap, upright. Quads are 8 doubles TL, TR, BR, BL.
 * Java callers: every function is @JvmStatic (LivenessVision.rectify(...)), fields are @JvmField (b.k, b.img).
 */
object LivenessVision {

    /** Width of the straightened measuring frame (vision-core.ts:40). */
    const val OPT_W = 480

    /** pageResponse sample layout: {specFrac, chromaFrac, flareFrac, veilLift}. */
    const val SPEC = 0
    const val CHROMA = 1
    const val FLARE = 2
    const val VEIL = 3

    // =============================================================================================
    // Result types
    // =============================================================================================

    /** The baseline choice: which corner is the document's top-left, and the straightened image. */
    class Baseline(@JvmField val k: Int, @JvmField val img: IntArray)

    /** Half-resolution Lab grid with the frame's colour cast (mean a*, b* of the ok cells) removed. */
    class Grid(@JvmField val gw: Int, @JvmField val gh: Int) {
        @JvmField val L = DoubleArray(gw * gh)
        @JvmField val A = DoubleArray(gw * gh)
        @JvmField val B = DoubleArray(gw * gh)
        @JvmField val ok = BooleanArray(gw * gh)
    }

    /** The lit flat reference: per cell, the strongest colour already there in its 7x7 neighbourhood. */
    class IridBase(@JvmField val gw: Int, @JvmField val gh: Int, @JvmField val own: DoubleArray)

    // =============================================================================================
    // Analysis frame (wiring/android.md §3): shrink by AREA AVERAGING, as OpenCV INTER_AREA / REF vision-client.ts:83.
    // Bitmap.createScaledBitmap(filter = true) is bilinear and is listed as an Android pitfall.
    // =============================================================================================

    /** Shrinks src to dw x dh by area averaging (each output pixel = the mean of the source area it covers). */
    @JvmStatic
    fun areaResize(src: Bitmap, dw: Int, dh: Int): Bitmap {
        val sw = src.width
        val sh = src.height
        if (dw >= sw && dh >= sh) return src
        val sx = sw.toDouble() / dw
        val sy = sh.toDouble() / dh

        // horizontal weights: for each output column, the source columns it covers and how much of each
        val colStart = IntArray(dw)
        val colW = arrayOfNulls<FloatArray>(dw)
        for (x in 0 until dw) {
            val x0 = x * sx
            val x1 = (x + 1) * sx
            val a = floor(x0).toInt()
            val b = min(sw, ceil(x1).toInt())
            colStart[x] = a
            colW[x] = FloatArray(b - a) { k ->
                val c = a + k
                ((min(x1, c + 1.0) - max(x0, c.toDouble())) / sx).toFloat()
            }
        }

        val row = IntArray(sw)
        val hr = FloatArray(dw * 3)                                   // one source row, resampled horizontally
        val acc = FloatArray(dw * 3)
        val out = IntArray(dw * dh)
        var cached = -1
        for (y in 0 until dh) {
            acc.fill(0f)
            val y0 = y * sy
            val y1 = (y + 1) * sy
            for (r in floor(y0).toInt() until min(sh, ceil(y1).toInt())) {
                val wy = ((min(y1, r + 1.0) - max(y0, r.toDouble())) / sy).toFloat()
                if (wy <= 0f) continue
                if (r != cached) {                                    // a source row is shared by at most two output rows
                    src.getPixels(row, 0, sw, 0, r, sw, 1)
                    for (x in 0 until dw) {
                        val ws = colW[x]!!
                        val a = colStart[x]
                        var rr = 0f; var gg = 0f; var bb = 0f
                        for (k in ws.indices) {
                            val p = row[a + k]
                            val w = ws[k]
                            rr += red(p) * w; gg += green(p) * w; bb += blue(p) * w
                        }
                        hr[3 * x] = rr; hr[3 * x + 1] = gg; hr[3 * x + 2] = bb
                    }
                    cached = r
                }
                for (i in acc.indices) acc[i] += wy * hr[i]
            }
            for (x in 0 until dw) {
                out[y * dw + x] = (0xFF shl 24) or (to8(acc[3 * x]) shl 16) or (to8(acc[3 * x + 1]) shl 8) or to8(acc[3 * x + 2])
            }
        }
        return Bitmap.createBitmap(out, dw, dh, Bitmap.Config.ARGB_8888)
    }

    // =============================================================================================
    // Rectify (checks/rectify-colour.md)
    // =============================================================================================

    @JvmStatic
    fun rectHeight(aspect: Double): Int = (OPT_W / aspect).roundToInt()

    /** Corners rolled so corner k becomes the document's top-left: [q_k, q_k+1, q_k+2, q_k+3]. */
    @JvmStatic
    fun roll(q: DoubleArray, k: Int): DoubleArray {
        val r = DoubleArray(8)
        for (i in 0 until 4) {
            val s = (i + k) % 4
            r[2 * i] = q[2 * s]
            r[2 * i + 1] = q[2 * s + 1]
        }
        return r
    }

    /**
     * Perspective warp of the quad to W x H (bilinear, border replicate), then the OpenCV 5x5 Gaussian
     * (sigma from size = kernel [1 4 6 4 1] / 16, border reflect-101). Returns packed 0xRRGGBB pixels.
     */
    @JvmStatic
    fun rectify(frame: Bitmap, quad: DoubleArray, W: Int, H: Int): IntArray? {
        val rect = doubleArrayOf(0.0, 0.0, W.toDouble(), 0.0, W.toDouble(), H.toDouble(), 0.0, H.toDouble())
        val hm = homography(rect, quad) ?: return null                // destination pixel -> frame pixel

        val fw = frame.width
        val fh = frame.height
        val minX = min(min(quad[0], quad[2]), min(quad[4], quad[6]))
        val maxX = max(max(quad[0], quad[2]), max(quad[4], quad[6]))
        val minY = min(min(quad[1], quad[3]), min(quad[5], quad[7]))
        val maxY = max(max(quad[1], quad[3]), max(quad[5], quad[7]))
        val x0 = clamp(floor(minX).toInt() - 1, 0, fw - 1)
        val y0 = clamp(floor(minY).toInt() - 1, 0, fh - 1)
        val x1 = clamp(ceil(maxX).toInt() + 1, x0, fw - 1)
        val y1 = clamp(ceil(maxY).toInt() + 1, y0, fh - 1)
        val rw = x1 - x0 + 1
        val rh = y1 - y0 + 1
        val px = IntArray(rw * rh)
        frame.getPixels(px, 0, rw, x0, y0, rw, rh)

        val n = W * H
        val r = FloatArray(n)
        val g = FloatArray(n)
        val b = FloatArray(n)
        for (v in 0 until H) {
            for (u in 0 until W) {
                val d = hm[6] * u + hm[7] * v + hm[8]
                val sx = (hm[0] * u + hm[1] * v + hm[2]) / d - x0
                val sy = (hm[3] * u + hm[4] * v + hm[5]) / d - y0
                val ix = floor(sx).toInt()
                val iy = floor(sy).toInt()
                val fx = (sx - ix).toFloat()
                val fy = (sy - iy).toFloat()
                val xa = clamp(ix, 0, rw - 1)                         // border replicate
                val xb = clamp(ix + 1, 0, rw - 1)
                val ya = clamp(iy, 0, rh - 1)
                val yb = clamp(iy + 1, 0, rh - 1)
                val p00 = px[ya * rw + xa]
                val p10 = px[ya * rw + xb]
                val p01 = px[yb * rw + xa]
                val p11 = px[yb * rw + xb]
                val i = v * W + u
                r[i] = bilinear(red(p00), red(p10), red(p01), red(p11), fx, fy)
                g[i] = bilinear(green(p00), green(p10), green(p01), green(p11), fx, fy)
                b[i] = bilinear(blue(p00), blue(p10), blue(p01), blue(p11), fx, fy)
            }
        }
        blur5(r, W, H)
        blur5(g, W, H)
        blur5(b, W, H)

        return IntArray(n) { i -> (to8(r[i]) shl 16) or (to8(g[i]) shl 8) or to8(b[i]) }
    }

    /**
     * makeBaseline for a profile without orient / staticHue (passports, licence):
     * candidates = corners whose next edge is a long edge; score = bandSD(0.8..0.96) - bandSD(0.04..0.2),
     * red channel only; the highest score wins (first wins ties).
     */
    @JvmStatic
    fun makeBaseline(frame: Bitmap, quad: DoubleArray, H: Int): Baseline? {
        val ks = ArrayList<Int>()
        for (k in 0 until 4) {
            val k1 = (k + 1) % 4
            val k2 = (k + 2) % 4
            val e1 = hypot(quad[2 * k1] - quad[2 * k], quad[2 * k1 + 1] - quad[2 * k + 1])
            val e2 = hypot(quad[2 * k2] - quad[2 * k1], quad[2 * k2 + 1] - quad[2 * k1 + 1])
            if (e1 >= e2) ks.add(k)
        }
        if (ks.isEmpty()) ks.add(0)

        var best: Baseline? = null
        var bestScore = -Double.MAX_VALUE
        for (k in ks) {
            val img = rectify(frame, roll(quad, k), OPT_W, H) ?: continue
            val score = bandSD(img, OPT_W, H, 0.8, 0.96) - bandSD(img, OPT_W, H, 0.04, 0.2)
            if (score > bestScore) {
                bestScore = score
                best = Baseline(k, img)
            }
        }
        return best
    }

    /** Population SD of the RED channel over the full width, rows round(y0*H) for max(1, round((y1-y0)*H)). */
    @JvmStatic
    fun bandSD(img: IntArray, W: Int, H: Int, y0: Double, y1: Double): Double {
        val r0 = Math.round(y0 * H).toInt()
        val rows = max(1, Math.round((y1 - y0) * H).toInt())
        val r1 = min(H, r0 + rows)
        var sum = 0.0
        var sq = 0.0
        var n = 0
        for (y in r0 until r1) {
            for (x in 0 until W) {
                val v = red(img[y * W + x]).toDouble()
                sum += v
                sq += v * v
                n++
            }
        }
        if (n == 0) return 0.0
        val mean = sum / n
        return sqrt(max(0.0, sq / n - mean * mean))
    }

    // =============================================================================================
    // CIELAB (checks/rectify-colour.md)
    // =============================================================================================

    private val LIN = DoubleArray(256) { v ->
        val c = v / 255.0
        if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun f(t: Double): Double = if (t > 0.008856) cbrt(t) else 7.787 * t + 16.0 / 116.0

    /** sRGB (D65) -> {L*, a*, b*} written into out. */
    @JvmStatic
    fun lab(rgb: Int, out: DoubleArray) {
        val r = LIN[red(rgb)]
        val g = LIN[green(rgb)]
        val b = LIN[blue(rgb)]
        val x = (0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / 0.95047
        val y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b
        val z = (0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / 1.08883
        val fx = f(x)
        val fy = f(y)
        val fz = f(z)
        out[0] = 116 * fy - 16
        out[1] = 500 * (fx - fy)
        out[2] = 200 * (fy - fz)
    }

    // =============================================================================================
    // Laminate colours (checks/laminate.md, vision-core.ts:1187)
    // =============================================================================================

    @JvmStatic
    fun chromaGrid(img: IntArray, W: Int, H: Int): Grid {
        val gr = Grid(W shr 1, H shr 1)
        val lab = DoubleArray(3)
        var sumA = 0.0
        var sumB = 0.0
        var okN = 0
        for (y in 0 until gr.gh) {
            for (x in 0 until gr.gw) {
                val p = img[(2 * y) * W + 2 * x]
                val j = y * gr.gw + x
                lab(p, lab)
                gr.L[j] = lab[0]
                gr.A[j] = lab[1]
                gr.B[j] = lab[2]
                gr.ok[j] = min(min(red(p), green(p)), blue(p)) < 235
                if (gr.ok[j]) {
                    sumA += lab[1]
                    sumB += lab[2]
                    okN++
                }
            }
        }
        if (okN > 0) {                                                // remove the frame's colour cast
            val ma = sumA / okN
            val mb = sumB / okN
            for (j in gr.A.indices) {
                gr.A[j] -= ma
                gr.B[j] -= mb
            }
        }
        return gr
    }

    @JvmStatic
    fun iridBase(litImg: IntArray, W: Int, H: Int): IridBase {
        val g = chromaGrid(litImg, W, H)
        val chroma = DoubleArray(g.gw * g.gh) { j -> hypot(g.A[j], g.B[j]) }
        val own = DoubleArray(g.gw * g.gh)
        for (y in 0 until g.gh) {
            for (x in 0 until g.gw) {
                var m = 0.0
                for (dy in -3..3) {
                    val yy = y + dy
                    if (yy < 0 || yy >= g.gh) continue
                    for (dx in -3..3) {
                        val xx = x + dx
                        if (xx < 0 || xx >= g.gw) continue
                        m = max(m, chroma[yy * g.gw + xx])
                    }
                }
                own[y * g.gw + x] = m
            }
        }
        return IridBase(g.gw, g.gh, own)
    }

    /** 12 hue-sector shares of NEW saturated colour on the page (null when no usable cell). */
    @JvmStatic
    fun iridescence(base: IridBase?, cur: Grid?): DoubleArray? {
        if (base == null || cur == null || base.gw != cur.gw || base.gh != cur.gh) return null
        val gw = cur.gw
        val gh = cur.gh
        val bx = Math.round(0.04 * gw).toInt()
        val by = Math.round(0.04 * gh).toInt()
        val count = DoubleArray(12)
        var usable = 0
        for (y in by until gh - by) {
            for (x in bx until gw - bx) {
                val j = y * gw + x
                if (!cur.ok[j]) continue
                usable++
                val a = cur.A[j]
                val b = cur.B[j]
                if (cur.L[j] < 55 || hypot(a, b) - base.own[j] <= 12) continue   // not bright / no new colour
                val deg = Math.toDegrees(atan2(b, a))
                if (deg > -15 && deg < 75) continue                              // skin and warm paper tones
                val sector = floor(((deg + 360) % 360) / 30).toInt()
                count[min(11, sector)]++
            }
        }
        if (usable == 0) return null
        for (i in 0 until 12) count[i] /= usable.toDouble()
        return count
    }

    // =============================================================================================
    // Page response to the light (checks/reflection.md, vision-core.ts:1280)
    // =============================================================================================

    @JvmStatic
    fun pageResponse(B: IntArray, C: IntArray, W: Int, H: Int): DoubleArray? {
        val cell = 8
        val edge = 2
        val cols = W / cell - 2 * edge
        val rows = H / cell - 2 * edge
        if (cols <= 0 || rows <= 0) return null
        val n = cols * rows

        // mean RGB per cell (every 2nd pixel in x and y), baseline and now
        val bR = DoubleArray(n); val bG = DoubleArray(n); val bB = DoubleArray(n)
        val cR = DoubleArray(n); val cG = DoubleArray(n); val cB = DoubleArray(n)
        for (j in 0 until n) {
            val ox = (j % cols + edge) * cell
            val oy = (j / cols + edge) * cell
            var sbr = 0.0; var sbg = 0.0; var sbb = 0.0
            var scr = 0.0; var scg = 0.0; var scb = 0.0
            var k = 0
            for (dy in 0 until cell step 2) {
                for (dx in 0 until cell step 2) {
                    val i = (oy + dy) * W + ox + dx
                    val pb = B[i]
                    val pc = C[i]
                    sbr += red(pb); sbg += green(pb); sbb += blue(pb)
                    scr += red(pc); scg += green(pc); scb += blue(pc)
                    k++
                }
            }
            bR[j] = sbr / k; bG[j] = sbg / k; bB[j] = sbb / k
            cR[j] = scr / k; cG[j] = scg / k; cB[j] = scb / k
        }

        // per-channel gain: median of c/b over cells with b > 25 and c < 250
        val gain = doubleArrayOf(channelGain(bR, cR), channelGain(bG, cG), channelGain(bB, cB))

        // log-ratio of brightness, then a lighting plane over the calm cells
        val lrs = ArrayList<DoubleArray>()                            // {lr, x, y}
        for (j in 0 until n) {
            val lb = (bR[j] + bG[j] + bB[j]) / 3
            val lc = (cR[j] + cG[j] + cB[j]) / 3
            val clip = min(min(cR[j], cG[j]), cB[j]) >= 250
            if (lb > 25 && lc > 5 && !clip) {
                lrs.add(doubleArrayOf(ln(lc / lb), (j % cols) / cols.toDouble(), (j / cols) / rows.toDouble()))
            }
        }
        var m0 = 0.0
        if (lrs.isNotEmpty()) {
            val v = DoubleArray(lrs.size) { lrs[it][0] }
            v.sort()
            m0 = v[v.size shr 1]
        }
        var p0 = m0
        var p1 = 0.0
        var p2 = 0.0
        var calm = 0
        var s11 = 0.0; var s1x = 0.0; var s1y = 0.0; var sxx = 0.0; var sxy = 0.0; var syy = 0.0
        var t1 = 0.0; var tx = 0.0; var ty = 0.0
        for (e in lrs) {
            if (abs(e[0] - m0) >= 0.2) continue
            val x = e[1] - 0.5
            val y = e[2] - 0.5
            val l = e[0]
            calm++
            s11 += 1.0; s1x += x; s1y += y; sxx += x * x; sxy += x * y; syy += y * y
            t1 += l; tx += l * x; ty += l * y
        }
        if (calm >= 50) {
            val det = det3(s11, s1x, s1y, s1x, sxx, sxy, s1y, sxy, syy)
            if (abs(det) > 1e-9) {
                p0 = det3(t1, s1x, s1y, tx, sxx, sxy, ty, sxy, syy) / det
                p1 = det3(s11, t1, s1y, s1x, tx, sxy, s1y, ty, syy) / det
                p2 = det3(s11, s1x, t1, s1x, sxx, tx, s1y, sxy, ty) / det
            }
        }

        // specular / colour-shifted cells, and the brightness pairs for the veil lift
        var spec = 0
        var chroma = 0
        val pairs = ArrayList<DoubleArray>()                          // {lin(lum b), lin(lum c)}
        for (j in 0 until n) {
            val lb = (bR[j] + bG[j] + bB[j]) / 3
            val lc = (cR[j] + cG[j] + cB[j]) / 3
            val clip = min(min(cR[j], cG[j]), cB[j]) >= 250
            val x = (j % cols) / cols.toDouble()
            val y = (j / cols) / rows.toDouble()
            val expected = lb * exp(p0 + p1 * (x - 0.5) + p2 * (y - 0.5))
            val gbR = bR[j] * gain[0]
            val gbG = bG[j] * gain[1]
            val gbB = bB[j] * gain[2]
            val sc = cR[j] + cG[j] + cB[j]
            val sg = gbR + gbG + gbB
            val shift = if (sc == 0.0 || sg == 0.0) 0.0 else hypot(cR[j] / sc - gbR / sg, cG[j] / sc - gbG / sg)
            if (if (clip) expected < 240 else (shift <= 0.05 && lc > 1.18 * expected + 10)) {
                spec++
            } else if (!clip && shift > 0.05 && lb > 40 && lc > 40) {
                chroma++
            }
            if (lb < 236 && lc < 236 && lb > 4) pairs.add(doubleArrayOf(gamma(lb), gamma(lc)))
        }

        // flare: pixels inside the 16 px ring that white out under the light but were not white before
        var flare = 0
        var total = 0
        val ring = cell * edge
        for (yy in ring until H - ring step 2) {
            for (xx in ring until W - ring step 2) {
                val i = yy * W + xx
                val pc = C[i]
                val pb = B[i]
                val minC = min(min(red(pc), green(pc)), blue(pc))
                val minB = min(min(red(pb) * gain[0], green(pb) * gain[1]), blue(pb) * gain[2])
                total++
                if (minC >= 242 && minB < 236) flare++
            }
        }

        // veil lift: how much more the dark third brightened than the bright third (added light)
        var veil = 1.0
        if (pairs.size >= 200) {
            pairs.sortBy { it[0] }
            val third = floor(0.3 * pairs.size).toInt()
            if (third > 0) {
                val dark = partRatio(pairs.subList(0, third))
                val bright = partRatio(pairs.subList(pairs.size - third, pairs.size))
                veil = dark / max(bright, 0.001)
            }
        }

        return doubleArrayOf(
            spec / n.toDouble(),
            chroma / n.toDouble(),
            if (total == 0) 0.0 else flare / total.toDouble(),
            veil
        )
    }

    private fun channelGain(b: DoubleArray, c: DoubleArray): Double {
        val v = ArrayList<Double>()
        for (j in b.indices) {
            if (b[j] > 25 && c[j] < 250) v.add(c[j] / b[j])
        }
        if (v.isEmpty()) return 1.0
        v.sort()
        return v[v.size shr 1]
    }

    private fun partRatio(part: List<DoubleArray>): Double {
        val b = DoubleArray(part.size) { part[it][0] }
        val c = DoubleArray(part.size) { part[it][1] }
        b.sort()
        c.sort()
        return c[c.size shr 1] / max(b[b.size shr 1], 0.001)
    }

    private fun gamma(v: Double): Double = 255 * (v / 255).pow(2.2)

    private fun det3(
        a: Double, b: Double, c: Double,
        d: Double, e: Double, f: Double,
        g: Double, h: Double, i: Double
    ): Double = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g)

    // =============================================================================================
    // Quality (checks/quality.md, vision-core.ts:1016)
    // =============================================================================================

    /** {sharpness (Laplacian variance, population), glareFrac (grey > 249)} inside the outline shrunk by 4%. */
    @JvmStatic
    fun quality(frame: Bitmap, quad: DoubleArray): DoubleArray {
        val cx = (quad[0] + quad[2] + quad[4] + quad[6]) / 4
        val cy = (quad[1] + quad[3] + quad[5] + quad[7]) / 4
        val m = DoubleArray(8)
        for (i in 0 until 4) {
            m[2 * i] = Math.round(cx + (quad[2 * i] - cx) * 0.96).toDouble()
            m[2 * i + 1] = Math.round(cy + (quad[2 * i + 1] - cy) * 0.96).toDouble()
        }
        val fw = frame.width
        val fh = frame.height
        val x0 = clamp(floor(min(min(m[0], m[2]), min(m[4], m[6]))).toInt(), 0, fw - 1)
        val x1 = clamp(ceil(max(max(m[0], m[2]), max(m[4], m[6]))).toInt(), x0, fw - 1)
        val y0 = clamp(floor(min(min(m[1], m[3]), min(m[5], m[7]))).toInt(), 0, fh - 1)
        val y1 = clamp(ceil(max(max(m[1], m[3]), max(m[5], m[7]))).toInt(), y0, fh - 1)

        // grey of the box plus a 1 px border (the Laplacian's neighbours), reflect-101 at the frame edge
        val bx0 = max(0, x0 - 1)
        val by0 = max(0, y0 - 1)
        val bx1 = min(fw - 1, x1 + 1)
        val by1 = min(fh - 1, y1 + 1)
        val bw = bx1 - bx0 + 1
        val bh = by1 - by0 + 1
        val px = IntArray(bw * bh)
        frame.getPixels(px, 0, bw, bx0, by0, bw, bh)
        val grey = IntArray(px.size) { i ->                           // OpenCV RGBA2GRAY fixed point
            val p = px[i]
            (red(p) * 4899 + green(p) * 9617 + blue(p) * 1868 + 8192) shr 14
        }

        var sum = 0.0
        var sq = 0.0
        var area = 0
        var bright = 0
        for (y in y0..y1) {
            for (x in x0..x1) {
                if (!insideConvex(m, x.toDouble(), y.toDouble())) continue
                val g = grey[(y - by0) * bw + (x - bx0)]
                val up = grey[(reflect(y - 1, fh) - by0) * bw + (x - bx0)]
                val dn = grey[(reflect(y + 1, fh) - by0) * bw + (x - bx0)]
                val lf = grey[(y - by0) * bw + (reflect(x - 1, fw) - bx0)]
                val rt = grey[(y - by0) * bw + (reflect(x + 1, fw) - bx0)]
                val lap = up + dn + lf + rt - 4.0 * g
                sum += lap
                sq += lap * lap
                area++
                if (g > 249) bright++
            }
        }
        if (area == 0) return doubleArrayOf(0.0, 0.0)
        val mean = sum / area
        return doubleArrayOf(sq / area - mean * mean, bright / area.toDouble())
    }

    /** Point (pixel centre) inside or on the edge of a convex quad. */
    private fun insideConvex(q: DoubleArray, x: Double, y: Double): Boolean {
        var pos = false
        var neg = false
        for (i in 0 until 4) {
            val j = (i + 1) % 4
            val cross = (q[2 * j] - q[2 * i]) * (y - q[2 * i + 1]) - (q[2 * j + 1] - q[2 * i + 1]) * (x - q[2 * i])
            if (cross > 0) pos = true else if (cross < 0) neg = true
            if (pos && neg) return false
        }
        return true
    }

    // =============================================================================================
    // Cards (03-documents/civil-id, checks/emblem.md, checks/overlay-cut-edge.md, rectify-colour.md)
    // =============================================================================================

    /** A profile rectangle in RECT pixels (vision-core.ts:1048 px): round, at least 1 px. Returns {x, y, w, h}. */
    @JvmStatic
    fun px(r: DoubleArray, h: Int): IntArray = intArrayOf(
        Math.round(r[0] * OPT_W).toInt(), Math.round(r[1] * h).toInt(),
        max(1, Math.round(r[2] * OPT_W).toInt()), max(1, Math.round(r[3] * h).toInt()),
    )

    /** CIELAB b* of one sRGB pixel (vision-core.ts:128 labB). */
    @JvmStatic
    fun labB(r: Int, g: Int, b: Int): Double {
        val rr = LIN[r]
        val gg = LIN[g]
        val bb = LIN[b]
        return 200 * (f(0.2126729 * rr + 0.7151522 * gg + 0.072175 * bb) -
                f((0.0193339 * rr + 0.119192 * gg + 0.9503041 * bb) / 1.08883))
    }

    /**
     * The cedar emblem (vision-core.ts:1242): how much yellower (b*) the cell is than the plain ring around it, now and in
     * the reference, at 9 small offsets; the offset where the emblem stands out most NOW is used, with the reference read at
     * that same place. Returns {now, base} or null. cell / inner / outer are fractions {x, y, w, h} of the upright card.
     */
    @JvmStatic
    fun emblemReading(B: IntArray, C: IntArray, W: Int, H: Int,
                      cellR: DoubleArray, innerR: DoubleArray, outerR: DoubleArray): DoubleArray? {
        val cell = px(cellR, H)
        val inner = px(innerR, H)
        val outer = px(outerR, H)
        fun inside(r: IntArray, x: Int, y: Int) = x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3]
        fun read(img: IntArray, dx: Int, dy: Int): Double? {
            var sc = 0.0; var nc = 0; var sr = 0.0; var nr = 0
            var y = outer[1]
            while (y < outer[1] + outer[3]) {
                var x = outer[0]
                while (x < outer[0] + outer[2]) {
                    val inCell = inside(cell, x, y)
                    if (inCell || !inside(inner, x, y)) {                // the rest of the mark: neither sample
                        val px = x + dx
                        val py = y + dy
                        if (px >= 0 && py >= 0 && px < W && py < H) {
                            val p = img[py * W + px]
                            val r = red(p); val g = green(p); val b = blue(p)
                            if (!(r >= 235 && g >= 235 && b >= 235)) {
                                val v = labB(r, g, b)
                                if (inCell) { sc += v; nc++ } else { sr += v; nr++ }
                            }
                        }
                    }
                    x += 2
                }
                y += 2
            }
            return if (nc >= 20 && nr >= 20) sc / nc - sr / nr else null
        }
        var best: DoubleArray? = null
        for (dy in intArrayOf(-7, 0, 7)) {
            for (dx in intArrayOf(-9, 0, 9)) {
                val now = read(C, dx, dy) ?: continue
                if (best != null && now <= best[0]) continue
                val base = read(B, dx, dy)
                if (base != null) best = doubleArrayOf(now, base)
            }
        }
        return best
    }

    /** OpenCV 8-bit HSV of one pixel: {H 0..180, S 0..255}. */
    private fun hsv(p: Int): IntArray {
        val r = red(p); val g = green(p); val b = blue(p)
        val v = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val d = v - mn
        val s = if (v == 0) 0 else Math.round(255.0 * d / v).toInt()
        var h = if (d == 0) 0.0 else when (v) {
            r -> 60.0 * (g - b) / d
            g -> 120.0 + 60.0 * (b - r) / d
            else -> 240.0 + 60.0 * (r - g) / d
        }
        if (h < 0) h += 360.0
        return intArrayOf(Math.round(h / 2).toInt(), s)
    }

    /** rectify-colour.md hueShare: every 2nd pixel of the rect with hue in [lo, hi] and saturation >= minSat. */
    private fun hueShare(img: IntArray, W: Int, r: IntArray, lo: Double, hi: Double, minSat: Double): Double {
        var n = 0
        var y = r[1]
        while (y < r[1] + r[3]) {
            var x = r[0]
            while (x < r[0] + r[2]) {
                if (x in 0 until W && y >= 0 && y * W + x < img.size) {
                    val hs = hsv(img[y * W + x])
                    if (hs[0] >= lo && hs[0] <= hi && hs[1] >= minSat) n++
                }
                x += 2
            }
            y += 2
        }
        val cells = ((r[3] + 1) / 2) * ((r[2] + 1) / 2)
        return if (cells == 0) 0.0 else n / cells.toDouble()
    }

    /** rectify-colour.md staticFeature frac: share of the feature ROI with the resting hue (every pixel). */
    private fun staticFrac(img: IntArray, W: Int, r: IntArray, lo: Double, hi: Double, minSat: Double): Double {
        var n = 0; var all = 0
        for (y in r[1] until r[1] + r[3]) for (x in r[0] until r[0] + r[2]) {
            if (x !in 0 until W || y < 0 || y * W + x >= img.size) continue
            all++
            val hs = hsv(img[y * W + x])
            if (hs[0] >= lo && hs[0] <= hi && hs[1] >= minSat) n++
        }
        return if (all == 0) 0.0 else n / all.toDouble()
    }

    /**
     * makeBaseline for a profile WITH an orientation mark (the ID front, vision-core.ts:1074): of the corners whose next
     * edge is long, the one whose straightened card shows the mark's hue in orient.roi (+ 0.01 x the resting cedar share).
     * orient = {x, y, w, h, hueLo, hueHi, minSat}; feature = {x, y, w, h, hueLo, hueHi, minSat} or null.
     */
    @JvmStatic
    fun makeBaselineOriented(frame: Bitmap, quad: DoubleArray, H: Int, orient: DoubleArray, feature: DoubleArray?): Baseline? {
        val ks = ArrayList<Int>()
        for (k in 0 until 4) {
            val k1 = (k + 1) % 4
            val k2 = (k + 2) % 4
            val e1 = hypot(quad[2 * k1] - quad[2 * k], quad[2 * k1 + 1] - quad[2 * k + 1])
            val e2 = hypot(quad[2 * k2] - quad[2 * k1], quad[2 * k2 + 1] - quad[2 * k1 + 1])
            if (e1 >= e2) ks.add(k)
        }
        if (ks.isEmpty()) ks.add(0)
        var best: Baseline? = null
        var bestScore = -Double.MAX_VALUE
        for (k in ks) {
            val img = rectify(frame, roll(quad, k), OPT_W, H) ?: continue
            val o = px(orient.copyOfRange(0, 4), H)
            var score = hueShare(img, OPT_W, o, orient[4], orient[5], orient[6])
            if (feature != null) {
                val fr = px(feature.copyOfRange(0, 4), H)
                score += 0.01 * staticFrac(img, OPT_W, fr, feature[4], feature[5], feature[6])
            }
            if (score > bestScore) {
                bestScore = score
                best = Baseline(k, img)
            }
        }
        return best
    }

    /** The overlay cut edge is measured on the card straightened to CUT_W x CUT_H from the FULL camera frame. */
    const val CUT_W = 856
    const val CUT_H = 540

    /**
     * cut-edge.ts cutEdgeOf: the score of the straightest near-vertical line to the right of the emblem (an overlaid print's
     * cut edge). frame = the FULL upright camera frame; quad = the card there, top left first, clockwise.
     */
    @JvmStatic
    fun cutEdgeOf(frame: Bitmap, quad: DoubleArray): Double {
        // grey of the card's bounding box + 8%, (77R + 150G + 29B) >> 8
        val fw = frame.width
        val fh = frame.height
        val minX = min(min(quad[0], quad[2]), min(quad[4], quad[6]))
        val maxX = max(max(quad[0], quad[2]), max(quad[4], quad[6]))
        val minY = min(min(quad[1], quad[3]), min(quad[5], quad[7]))
        val maxY = max(max(quad[1], quad[3]), max(quad[5], quad[7]))
        val mx = 0.08 * (maxX - minX)
        val my = 0.08 * (maxY - minY)
        val x0 = clamp(floor(minX - mx).toInt(), 0, fw - 1)
        val y0 = clamp(floor(minY - my).toInt(), 0, fh - 1)
        val x1 = clamp(ceil(maxX + mx).toInt(), x0, fw - 1)
        val y1 = clamp(ceil(maxY + my).toInt(), y0, fh - 1)
        val bw = x1 - x0 + 1
        val bh = y1 - y0 + 1
        val px = IntArray(bw * bh)
        frame.getPixels(px, 0, bw, x0, y0, bw, bh)
        val grey = IntArray(px.size) { i -> (red(px[i]) * 77 + green(px[i]) * 150 + blue(px[i]) * 29) shr 8 }

        // cardBand: unit square -> quad homography, bilinear, outside = 255, clamped and rounded half to even
        val hq = unitToQuad(quad)
        val card = DoubleArray(CUT_W * CUT_H)
        for (y in 0 until CUT_H) {
            val v = (y + 0.5) / CUT_H
            for (x in 0 until CUT_W) {
                val u = (x + 0.5) / CUT_W
                val w = hq[6] * u + hq[7] * v + hq[8]
                val sx = (hq[0] * u + hq[1] * v + hq[2]) / w - 0.5 - x0
                val sy = (hq[3] * u + hq[4] * v + hq[5]) / w - 0.5 - y0
                val ix = floor(sx).toInt()
                val iy = floor(sy).toInt()
                val o = y * CUT_W + x
                if (ix < 0 || iy < 0 || ix + 1 >= bw || iy + 1 >= bh) { card[o] = 255.0; continue }
                val fx = sx - ix
                val fy = sy - iy
                val i = iy * bw + ix
                val top = grey[i] + (grey[i + 1] - grey[i]) * fx
                val bottom = grey[i + bw] + (grey[i + bw + 1] - grey[i + bw]) * fx
                card[o] = Math.rint(min(255.0, max(0.0, top + (bottom - top) * fy)))
            }
        }
        return cutEdgeScore(card, CUT_W, CUT_H)
    }

    /** cut-edge.ts cutEdgeScore on a straightened grey card w x h. */
    @JvmStatic
    fun cutEdgeScore(card: DoubleArray, w: Int, h: Int): Double {
        // blur3: [1 2 1]/4 horizontally then vertically, edges replicated
        val tmp = DoubleArray(w * h)
        val b = DoubleArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val i = y * w + x
            val l = if (x > 0) card[i - 1] else card[i]
            val r = if (x < w - 1) card[i + 1] else card[i]
            tmp[i] = (l + 2 * card[i] + r) / 4
        }
        for (y in 0 until h) for (x in 0 until w) {
            val i = y * w + x
            val u = if (y > 0) tmp[i - w] else tmp[i]
            val d = if (y < h - 1) tmp[i + w] else tmp[i]
            b[i] = (u + 2 * tmp[i] + d) / 4
        }
        // |Sobel x|, interior only
        val gx = DoubleArray(w * h)
        for (y in 1 until h - 1) for (x in 1 until w - 1) {
            val i = y * w + x
            gx[i] = abs((b[i - w + 1] - b[i - w - 1]) + 2 * (b[i + 1] - b[i - 1]) + (b[i + w + 1] - b[i + w - 1]))
        }
        val leftW = floor(0.45 * w).toInt()
        val left = DoubleArray(h * leftW)
        var n0 = 0
        for (y in 0 until h) for (x in 0 until leftW) left[n0++] = gx[y * w + x]
        val base = percentile(left, 0.5) + 1
        val y0 = floor(0.04 * h).toInt()
        val y1 = floor(0.30 * h).toInt()
        val n = y1 - y0
        val col = DoubleArray(n)
        var best = 0.0
        for (x0 in floor(0.12 * w).toInt() until floor(0.34 * w).toInt()) {
            for (s in doubleArrayOf(-0.06, -0.03, 0.0, 0.03, 0.06)) {
                for (k in 0 until n) {
                    val yy = y0 + k
                    val xx = min(w - 2, max(1, Math.round(x0 + s * k).toInt()))
                    val i = yy * w + xx
                    col[k] = max(gx[i], max(gx[i - 1], gx[i + 1]))
                }
                best = max(best, percentile(col.copyOf(), 0.2))
            }
        }
        return best / base
    }

    /** Sorted ascending, linear interpolation between floor(k) and ceil(k), k = (n - 1) p. Sorts its argument. */
    private fun percentile(v: DoubleArray, p: Double): Double {
        v.sort()
        val k = (v.size - 1) * p
        val lo = floor(k).toInt()
        val hi = ceil(k).toInt()
        return v[lo] + (v[hi] - v[lo]) * (k - lo)
    }

    /** barcode-prep.ts unitToQuad: (u, v) in the unit square -> the quad (TL, TR, BR, BL). */
    private fun unitToQuad(q: DoubleArray): DoubleArray {
        val x0 = q[0]; val y0 = q[1]; val x1 = q[2]; val y1 = q[3]; val x2 = q[4]; val y2 = q[5]; val x3 = q[6]; val y3 = q[7]
        val dx1 = x1 - x2; val dx2 = x3 - x2; val dy1 = y1 - y2; val dy2 = y3 - y2
        val sx = x0 - x1 + x2 - x3; val sy = y0 - y1 + y2 - y3
        val den = dx1 * dy2 - dx2 * dy1
        val g = if (den == 0.0) 0.0 else (sx * dy2 - dx2 * sy) / den
        val h = if (den == 0.0) 0.0 else (dx1 * sy - sx * dy1) / den
        return doubleArrayOf(x1 - x0 + g * x1, x3 - x0 + h * x3, x0, y1 - y0 + g * y1, y3 - y0 + h * y3, y0, g, h, 1.0)
    }

    // =============================================================================================
    // Helpers
    // =============================================================================================

    /** 3x3 homography (9 numbers) mapping the 4 'from' points onto the 4 'to' points. */
    @JvmStatic
    fun homography(from: DoubleArray, to: DoubleArray): DoubleArray? {
        val a = Array(8) { DoubleArray(9) }
        for (i in 0 until 4) {
            val x = from[2 * i]
            val y = from[2 * i + 1]
            val tx = to[2 * i]
            val ty = to[2 * i + 1]
            a[2 * i] = doubleArrayOf(x, y, 1.0, 0.0, 0.0, 0.0, -tx * x, -tx * y, tx)
            a[2 * i + 1] = doubleArrayOf(0.0, 0.0, 0.0, x, y, 1.0, -ty * x, -ty * y, ty)
        }
        for (col in 0 until 8) {                                      // Gauss-Jordan, partial pivoting
            var piv = col
            for (r in col + 1 until 8) {
                if (abs(a[r][col]) > abs(a[piv][col])) piv = r
            }
            if (abs(a[piv][col]) < 1e-12) return null
            val t = a[col]
            a[col] = a[piv]
            a[piv] = t
            for (r in 0 until 8) {
                if (r == col) continue
                val f = a[r][col] / a[col][col]
                for (k in col until 9) a[r][k] -= f * a[col][k]
            }
        }
        val h = DoubleArray(9)
        for (i in 0 until 8) h[i] = a[i][8] / a[i][i]
        h[8] = 1.0
        return h
    }

    private fun bilinear(p00: Int, p10: Int, p01: Int, p11: Int, fx: Float, fy: Float): Float {
        val top = p00 + (p10 - p00) * fx
        val bot = p01 + (p11 - p01) * fx
        return top + (bot - top) * fy
    }

    /** Separable [1 4 6 4 1]/16 blur, border reflect-101 (OpenCV GaussianBlur 5x5 with sigma 0). */
    private fun blur5(ch: FloatArray, W: Int, H: Int) {
        val k0 = 6f / 16
        val k1 = 4f / 16
        val k2 = 1f / 16
        val tmp = FloatArray(ch.size)
        for (y in 0 until H) {
            val row = y * W
            for (x in 0 until W) {
                tmp[row + x] = k0 * ch[row + x] +
                        k1 * (ch[row + reflect(x - 1, W)] + ch[row + reflect(x + 1, W)]) +
                        k2 * (ch[row + reflect(x - 2, W)] + ch[row + reflect(x + 2, W)])
            }
        }
        for (y in 0 until H) {
            for (x in 0 until W) {
                ch[y * W + x] = k0 * tmp[y * W + x] +
                        k1 * (tmp[reflect(y - 1, H) * W + x] + tmp[reflect(y + 1, H) * W + x]) +
                        k2 * (tmp[reflect(y - 2, H) * W + x] + tmp[reflect(y + 2, H) * W + x])
            }
        }
    }

    private fun reflect(index: Int, n: Int): Int {
        if (n == 1) return 0
        var i = index
        while (i < 0 || i >= n) {
            if (i < 0) i = -i
            if (i >= n) i = 2 * n - 2 - i
        }
        return i
    }

    private fun red(p: Int): Int = (p shr 16) and 255
    private fun green(p: Int): Int = (p shr 8) and 255
    private fun blue(p: Int): Int = p and 255

    private fun to8(v: Float): Int = max(0, min(255, Math.round(v)))

    private fun clamp(v: Int, lo: Int, hi: Int): Int = max(lo, min(hi, v))
}