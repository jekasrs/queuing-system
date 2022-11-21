package la.smi.queuingsystem.back.manager

import la.smi.queuingsystem.back.component.Buffer
import la.smi.queuingsystem.back.request.Bid
import la.smi.queuingsystem.back.request.SpecialEvent

class DispatcherBuffer (var S:Int) {
    private val buffer: Buffer = Buffer(S)

    fun isEmpty() = buffer.state == Buffer.State.EMPTY
    fun isFull() = buffer.state == Buffer.State.FULL

    fun add(bid: Bid, systemTime: Double): SpecialEvent {
        buffer.add(bid)
        val event = SpecialEvent(bid, SpecialEvent.Cause.ENTIRE_OF_BUFFERING, systemTime)
        event.idDevice = bid.idOfBuf
        return event
    }

    fun getNext(systemTime: Double) = buffer.getNextByIdDevice(systemTime)

    fun cancel(systemTime: Double): SpecialEvent {
        val bid = buffer.cancel(systemTime)
        val event = SpecialEvent(bid, SpecialEvent.Cause.CANCEL, systemTime)
        event.idDevice = bid.idOfBuf
        return event
    }
}
