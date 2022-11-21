package la.smi.queuingsystem.back.component

import la.smi.queuingsystem.back.request.Bid
import kotlin.random.Random

class Generator private constructor(private val a: Double, private val b: Double,
                                    val id: Int) {
    private var NO = 0
    private var whenLastGenerated: Double = 0.0
    private var whenPreLastGenerated: Double = 0.0

    // равномерный закон распределения
    private fun getRandomUniDistribution(): Double {
        val fx = Random.nextDouble()
        return fx * (b - a) + a
    }

    fun generateBid(): Bid {
        NO++
        val bid = Bid("$id.$NO")
        whenPreLastGenerated = whenLastGenerated
        bid.timeOfGeneration = whenLastGenerated + getRandomUniDistribution()
        whenLastGenerated = bid.timeOfGeneration
        return bid
    }

    fun cancelLastBid() {
        NO--
        whenLastGenerated = whenPreLastGenerated
    }

    companion object FactoryGenerators {
        private var ID = 0
        fun create(a: Double, b: Double): Generator {
            return Generator(a, b, ID++)
        }
    }
}
