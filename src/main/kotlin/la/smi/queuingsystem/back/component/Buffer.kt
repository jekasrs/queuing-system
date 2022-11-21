package la.smi.queuingsystem.back.component

import la.smi.queuingsystem.back.request.Bid
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class Buffer (private val S: Int) {
    var state = State.EMPTY
    var array: ArrayList<Bid> = ArrayList(S)
    var amount = 0

    fun add(bid: Bid) {
        bid.timeOfEntireBuffer = bid.timeOfGeneration
        array.add(bid)
        array.sort()
        array.reverse()
        bid.idOfBuf = array.lastIndex
        amount++
        state = if (amount == S) State.FULL else State.NOT_EMPTY
    }

    fun getNext(t: Double): Bid {
        array.sort()
        array.reverse()
        val bid = array[0]
        amount--
        bid.timeOfExitBuffer = t
        array.remove(bid)
        state = if (amount!=0) State.NOT_EMPTY else State.EMPTY
        return bid
    }

    fun getNextByIdDevice(t: Double): Bid {
        array.sort()
        array.reverse()
        val bid: Bid = array[getLastIndex()]
        amount--
        bid.timeOfExitBuffer = t
        array.remove(bid)
        state = if (amount!=0) State.NOT_EMPTY else State.EMPTY
        return bid
    }

    fun cancel(t: Double): Bid {
        array.sort()
        array.reverse()
        val bid = array[getLastIndex()]
        amount--
        bid.timeOfDeleting = t
        array.remove(bid)
        state = if (amount!=0) State.NOT_EMPTY else State.EMPTY
        return bid
    }

    fun getLastIndex(): Int {
        var bidMax = array[0].idOfBuf
        for (a in array) {
            if (a.idOfBuf > bidMax) {
                bidMax = a.idOfBuf
            }
        }
        var i = 0
        for (a in array){
            if (a.idOfBuf == bidMax){
                return i
            }
            i++
        }
        return i
    }

    enum class State {
        FULL,
        NOT_EMPTY,
        EMPTY
    }
}
