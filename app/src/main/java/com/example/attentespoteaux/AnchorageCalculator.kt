package com.example.attentespoteaux

import kotlin.math.max
import kotlin.math.min

data class CalculationInput(
    val columnWidthCm: Double,
    val columnDepthCm: Double,
    val barDiameterMm: Double,
    val numBars: Int,
    val fc28MPa: Double,
    val feMPa: Double,
    val isHighAdherence: Boolean,
    val footingHeightCm: Double,
    val footingWidthCm: Double,
    val coverCm: Double,
    val lapCoeff: Double = 1.0
)

data class CalculationResult(
    val ft28MPa: Double,
    val tauSuMPa: Double,
    val fsuMPa: Double,
    val lsMm: Double,
    val lsRetainedCm: Double,
    val lsMinCm: Double,
    val straightAnchorageCm: Double,
    val hook90TotalCm: Double,
    val hook180TotalCm: Double,
    val lapLengthCm: Double,
    val embedDepthRequiredCm: Double,
    val totalWaitLengthCm: Double,
    val warning: String?,
    val isValid: Boolean
)

object AnchorageCalculator {
    fun calculate(input: CalculationInput): CalculationResult {
        val phi = input.barDiameterMm
        val fc28 = input.fc28MPa
        val fe = input.feMPa
        val gammaS = 1.15

        val ft28 = 0.6 + 0.06 * fc28
        val psi = if (input.isHighAdherence) 1.5 else 1.0
        val tauSu = 0.6 * psi * psi * ft28
        val fsu = fe / gammaS
        val ls = (phi * fsu) / (4 * tauSu)
        val lsMin = max(40 * phi, 200.0)
        val lsRetained = max(ls, lsMin)

        val straightPart90 = max(0.4 * lsRetained, 8 * phi)
        val hook90Total = straightPart90 + 5 * phi

        val straightPart180 = max(0.3 * lsRetained, 5 * phi)
        val hook180Total = straightPart180 + 8 * phi

        val lapLength = max(input.lapCoeff * lsRetained, 20 * phi)

        val availableDepthMm = (input.footingHeightCm * 10) - (input.coverCm * 10)
        val embedRequired = if (availableDepthMm >= lsRetained) lsRetained
                            else min(hook90Total, hook180Total)

        var warning: String? = null
        var isValid = true

        if (availableDepthMm < hook180Total) {
            warning = "⚠️ Hauteur de semelle insuffisante. Augmentez l'épaisseur ou réduisez le Ø."
            isValid = false
        } else if (availableDepthMm < lsRetained && availableDepthMm >= hook90Total) {
            warning = "ℹ️ Utilisez un crochet 90° (${"%.1f".format(hook90Total/10)} cm)."
        } else if (availableDepthMm < lsRetained) {
            warning = "ℹ️ Utilisez un crochet 180° (${"%.1f".format(hook180Total/10)} cm)."
        }

        val totalWait = embedRequired + lapLength

        return CalculationResult(
            ft28MPa = ft28,
            tauSuMPa = tauSu,
            fsuMPa = fsu,
            lsMm = ls,
            lsRetainedCm = lsRetained / 10,
            lsMinCm = lsMin / 10,
            straightAnchorageCm = lsRetained / 10,
            hook90TotalCm = hook90Total / 10,
            hook180TotalCm = hook180Total / 10,
            lapLengthCm = lapLength / 10,
            embedDepthRequiredCm = embedRequired / 10,
            totalWaitLengthCm = totalWait / 10,
            warning = warning,
            isValid = isValid
        )
    }
}
