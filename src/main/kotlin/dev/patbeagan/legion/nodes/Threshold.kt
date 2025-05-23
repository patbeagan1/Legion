package dev.patbeagan.legion.nodes

import dev.patbeagan.legion.ImpNode
import dev.patbeagan.legion.LegionScope
import java.util.concurrent.atomic.DoubleAccumulator

fun <T : Number, EIn : Number, EOut> LegionScope<*>.threshold(
    thresholdMin: T? = null,
    thresholdMax: T? = null,
    action: (EIn) -> EOut
): ImpNode.Imp<EIn, EOut?> {
    val accumulator = DoubleAccumulator({ left, right -> left + right }, 0.0)
    return imp { eIn ->
        accumulator.accumulate(eIn.toDouble())

        val currentAccumulation = accumulator.get().also {
            log("acc: $it")
        }

        val greaterThanMin = currentAccumulation > (thresholdMin ?: Double.MIN_VALUE).toDouble()
        val lesserThanMax = currentAccumulation < (thresholdMax ?: Double.MAX_VALUE).toDouble()

        if (greaterThanMin && lesserThanMax) {
            action(eIn)
        } else {
            null
        }
    }
}