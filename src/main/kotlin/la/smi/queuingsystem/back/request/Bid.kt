package la.smi.queuingsystem.back.request

data class Bid(val id: String): Comparable<Bid> {
    var timeOfGeneration: Double = -1.0
    var timeOfStartProcessing: Double = -1.0
    var timeOfEndProcessing: Double = -1.0
    var timeOfEntireBuffer: Double = -1.0
    var timeOfExitBuffer: Double = -1.0
    var timeOfDeleting: Double = -1.0

    var idOfBuf: Int = -1

    override fun equals(other: Any?): Boolean {
        if (other is Bid)
            return (timeOfGeneration == other.timeOfGeneration)
        return false
    }

    override fun compareTo(other: Bid): Int {
        if (this == other) return 0
        if (this.timeOfGeneration > other.timeOfGeneration)
            return 1
        else if (this.timeOfGeneration < other.timeOfGeneration)
            return -1
        else return 0
    }
}
