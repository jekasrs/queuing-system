package la.smi.queuingsystem.back.component

import la.smi.queuingsystem.back.request.Bid
import kotlin.math.ln
import kotlin.random.Random

class Worker private constructor(private val lambda: Double, val Id: Int) {
    var bid: Bid? = null

    /** Экспоненциальный закон распределения **/
    private fun getRandomUniDistribution(): Double {
        val fx = Random.nextDouble()
        return (-1 / lambda) * ln(1 - fx)
    }

    fun load(b: Bid) {
        bid = b
        bid!!.timeOfStartProcessing = if (bid!!.timeOfExitBuffer>= bid!!.timeOfGeneration) bid!!.timeOfExitBuffer else bid!!.timeOfGeneration
        bid!!.timeOfEndProcessing = bid!!.timeOfStartProcessing + getRandomUniDistribution()
    }

    fun IsReleased(t: Double): Boolean {
        if (bid == null)
            return false
        return bid!!.timeOfEndProcessing <= t
    }

    companion object FactoryWorkers {
        var ID: Int = 0
        fun create(lambda: Double): Worker {
            return Worker(lambda, ID++)
        }
    }
}