package la.smi.queuingsystem.back.manager

import la.smi.queuingsystem.back.component.Worker
import la.smi.queuingsystem.back.request.Bid
import la.smi.queuingsystem.back.request.SpecialEvent

class DispatcherWorkers(val N:Int, lambda: Double) {

    val workers: ArrayList<Worker> = ArrayList(N)
    private var pointer: Int = 0

    init {
        for (i in 1..N) workers.add(Worker.create(lambda))
    }

    fun getNext(): Worker {
        val w = workers[pointer]
        if (pointer+1 == N) pointer = 0 else pointer++
        return w
    }

    fun commit(systemTime: Double): ArrayList<SpecialEvent> {
        var event: SpecialEvent
        val events: ArrayList<SpecialEvent> = ArrayList()
        for (i in pointer until N) {
            if (workers[i].IsReleased(systemTime)){
                event = SpecialEvent(workers[i].bid, SpecialEvent.Cause.FINISH_OF_PRODUCING, workers[i].bid!!.timeOfEndProcessing)
                event.idDevice = i
                events.add(event)
            }
        }
        for (i in 0 until pointer) {
            if (workers[i].IsReleased(systemTime)){
                event = SpecialEvent(workers[i].bid, SpecialEvent.Cause.FINISH_OF_PRODUCING, workers[i].bid!!.timeOfEndProcessing)
                event.idDevice = i
                events.add(event)
            }
        }
        return events
    }

    fun getMaxTimeOfEnd(): Double {
        var tmp = 0.0
        var max = 0.0
        for (i in pointer until N) {
            tmp = workers[i].bid?.timeOfEndProcessing ?: 0.0
            if (tmp > max) max = tmp
        }
        for (i in 0 until pointer) {
            tmp = workers[i].bid?.timeOfEndProcessing ?: 0.0
            if (tmp > max) max = tmp
        }
        return max
    }





/*    fun loadBid(t: Double): SpecialEvent? {
//        val event: SpecialEvent?
//        for (i in pointer until N) {
//            if (workers[i].bid == null) {
//                if (i == N - 1) pointer = 0 else pointer = i + 1
//                event = SpecialEvent(null, SpecialEvent.Cause.SYSTEM_NOT_STARTED, -1.0)
//                event.idDevice = i
//                return event
//            }
//            if (workers[i].tryReleaseBid(t)) {
//                if (i == N - 1) pointer = 0 else pointer = i + 1
//                if (workers[i].bid != null) {
//                    event = SpecialEvent(
//                        workers[i].bid!!,
//                        SpecialEvent.Cause.FINISH_OF_PRODUCING,
//                        workers[i].bid!!.timeOfEndProcessing
//                    )
//                    event.idDevice = i
//                    return event
//                }
//            }
//        }
//        for (i in 0 until pointer) {
//            if (workers[i].bid == null) {
//                pointer = i + 1
//                event = SpecialEvent(null, SpecialEvent.Cause.SYSTEM_NOT_STARTED, -1.0)
//                event.idDevice = i
//                return event
//            }
//            if (workers[i].tryReleaseBid(t)) {
//                pointer = i + 1
//                if (workers[i].bid != null) {
//                    event = SpecialEvent(
//                        workers[i].bid!!,
//                        SpecialEvent.Cause.FINISH_OF_PRODUCING,
//                        workers[i].bid!!.timeOfEndProcessing
//                    )
//                    event.idDevice = i
//                    return event
//                }
//            }
//        }
//        return null
//    }
//
//    fun findPossibleTime(t: Double): Double? {
//        for (i in pointer until N) {
//            if (workers[i].bid == null) return null
//            if (workers[i].bid!!.timeOfEndProcessing >= t)
//                return workers[i].bid!!.timeOfEndProcessing
//        }
//        for (i in 0 until pointer) {
//            if (workers[i].bid == null) return null
//            if (workers[i].bid!!.timeOfEndProcessing >= t)
//                return workers[i].bid!!.timeOfEndProcessing
//        }
//        return null
*/

}
