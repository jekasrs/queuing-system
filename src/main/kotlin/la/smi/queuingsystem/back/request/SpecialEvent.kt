package la.smi.queuingsystem.back.request

class SpecialEvent(val bid: Bid?, val cause: Cause, var time: Double): Comparable<SpecialEvent> {
    var idDevice: Int = 0
    var idBid: String = bid?.id ?: "-1"

    init {
        if (bid != null){
            if (cause == Cause.GENERATION)
                time = bid.timeOfGeneration
            if (cause == Cause.START_OF_PRODUCING)
                time = bid.timeOfStartProcessing
            if (cause == Cause.FINISH_OF_PRODUCING)
                time = bid.timeOfEndProcessing
            if (cause == Cause.ENTIRE_OF_BUFFERING)
                time = bid.timeOfEntireBuffer
            if (cause == Cause.EXIT_OF_BUFFERING)
                time = bid.timeOfExitBuffer
            if (cause == Cause.CANCEL)
                time = bid.timeOfDeleting
        }
    }
    override fun toString(): String {
        if (bid == null) return ""
        val timeS: Double
        val timeF: Double

        when (cause) {
            Cause.GENERATION -> {
                timeS = bid.timeOfGeneration
                return "$time: cause: $cause id: ${bid.id} | start:${"%.2f".format(timeS)}"
            }
            Cause.START_OF_PRODUCING -> {
                timeS = bid.timeOfStartProcessing
                return "$time: cause: $cause | idOfWorker: [${idDevice}] | id: ${bid.id} | start:${"%.2f".format(timeS)}"
            }
            Cause.ENTIRE_OF_BUFFERING -> {
                timeS = bid.timeOfEntireBuffer
                return "$time: cause: $cause | idOfWorker: [${idDevice}] |id: ${bid.id} | start:${"%.2f".format(timeS)}"
            }
            Cause.CANCEL -> {
                timeF = bid.timeOfDeleting
                return "$time: cause: $cause id: ${bid.id} | finish:${"%.2f".format(timeF)}"
            }
            Cause.FINISH_OF_PRODUCING -> {
                timeS = bid.timeOfStartProcessing
                timeF = bid.timeOfEndProcessing
                return "$time: cause: $cause | idOfWorker: [${idDevice}] | id: ${bid.id} | start:${"%.2f".format(timeS)} | end:${"%.2f".format(timeF)}"
            }
            Cause.EXIT_OF_BUFFERING -> {
                timeS = bid.timeOfEntireBuffer
                timeF = bid.timeOfExitBuffer
                return "$time: cause: | idOfWorker: [${idDevice}] |$cause id: ${bid.id} | start:${"%.2f".format(timeS)} | end:${"%.2f".format(timeF)}"
            }
            Cause.START -> return "Start"
            Cause.STOP -> return "Finish"
            else -> return ""
        }

    }
    override fun equals(other: Any?): Boolean {
        if (other is SpecialEvent){
            if ((this.time == other.time) && (this.cause == other.cause)) return true
            return false
        }
        else return false
    }
    override fun compareTo(other: SpecialEvent): Int {
        if (this.time > other.time) return 1
        else if (this.time < other.time) return -1
        else {
            if (this.cause == other.cause) return 0
            else {
                if (this.cause == Cause.GENERATION){
                    when (other.cause) {
                        Cause.START_OF_PRODUCING -> return -1
                        Cause.FINISH_OF_PRODUCING -> return -1
                        Cause.ENTIRE_OF_BUFFERING -> return -1
                        Cause.EXIT_OF_BUFFERING -> return -1
                        Cause.CANCEL -> return -1
                        else -> return 0
                    }
                }
                else if (this.cause == Cause.START_OF_PRODUCING){
                    when (other.cause) {
                        Cause.GENERATION -> return 1 //
                        Cause.FINISH_OF_PRODUCING -> return 1
                        Cause.ENTIRE_OF_BUFFERING -> return 1
                        Cause.EXIT_OF_BUFFERING -> return 1
                        Cause.CANCEL -> return 1
                        else -> return 0
                    }
                }
                else if (this.cause == Cause.FINISH_OF_PRODUCING){
                    when (other.cause) {
                        Cause.START_OF_PRODUCING -> return -1
                        Cause.GENERATION -> return -1
                        Cause.ENTIRE_OF_BUFFERING -> return 1
                        Cause.EXIT_OF_BUFFERING -> return -1
                        Cause.CANCEL -> return 1
                        else -> return 0
                    }
                }
                else if (this.cause == Cause.ENTIRE_OF_BUFFERING){
                    when (other.cause) {
                        Cause.START_OF_PRODUCING -> return -1
                        Cause.FINISH_OF_PRODUCING -> return -1
                        Cause.GENERATION -> return 1
                        Cause.EXIT_OF_BUFFERING -> return 1
                        Cause.CANCEL -> return 1
                        else -> return 0
                    }
                }
                else if (this.cause == Cause.EXIT_OF_BUFFERING){
                    when (other.cause) {
                        Cause.START_OF_PRODUCING -> return -1
                        Cause.FINISH_OF_PRODUCING -> return 1
                        Cause.ENTIRE_OF_BUFFERING -> return -1
                        Cause.GENERATION -> return 1
                        Cause.CANCEL -> return -1
                        else -> return 0
                    }
                }
                else if (this.cause == Cause.CANCEL){
                    when (other.cause) {
                        Cause.START_OF_PRODUCING -> return -1
                        Cause.FINISH_OF_PRODUCING -> return -1
                        Cause.ENTIRE_OF_BUFFERING -> return -1
                        Cause.EXIT_OF_BUFFERING -> return 1
                        Cause.GENERATION -> return 1
                        else -> return 0
                    }
                }
                else return 0
            }
        }
    }

    enum class Cause {
        START, STOP,
        GENERATION,
        START_OF_PRODUCING,
        FINISH_OF_PRODUCING,
        ENTIRE_OF_BUFFERING,
        EXIT_OF_BUFFERING,
        CANCEL
    }
}
