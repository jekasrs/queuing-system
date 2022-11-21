package la.smi.queuingsystem.back.manager

import la.smi.queuingsystem.back.component.Generator
import la.smi.queuingsystem.back.component.Worker
import la.smi.queuingsystem.back.request.Bid
import la.smi.queuingsystem.back.request.SpecialEvent

class MainDispatcher(val N_BIDS: Int,
                     val N_GENS: Int, val N_WORK: Int, val S_BUFF: Int,
                     alpha: Double, beta: Double, lambda: Double) {

    private val generators: ArrayList<Generator> = ArrayList(N_GENS)
    private val dispatcherWorkers: DispatcherWorkers
    private val dispatcherBuffer: DispatcherBuffer

    private var queue: ArrayList<SpecialEvent> = ArrayList()

    init {
        for (i in 1..N_GENS) generators.add(Generator.create(alpha, beta))
        dispatcherWorkers = DispatcherWorkers(N_WORK, lambda)
        dispatcherBuffer = DispatcherBuffer(S_BUFF)
    }

    private fun isFinishedModelling(): Boolean {
        var counter = 0

        var newQueue = ArrayList<SpecialEvent>()
        queue.distinct().forEach {
            if (!newQueue.contains(it)){
                newQueue.add(it)
                if (it.cause==SpecialEvent.Cause.FINISH_OF_PRODUCING) counter++
            }
        }
        queue.clear()
        queue.addAll(newQueue)
        return counter >= N_BIDS
    }
    private fun generate(): ArrayList<SpecialEvent> {
        var event: SpecialEvent?
        val tmp: ArrayList<SpecialEvent> = ArrayList(generators.size)
        var bid: Bid
        var maxBid: Bid = generators[0].generateBid()
        var timeOfMaxWorker: Double = dispatcherWorkers.getMaxTimeOfEnd()
        tmp.add(SpecialEvent(maxBid, SpecialEvent.Cause.GENERATION, maxBid.timeOfGeneration))
        for (gen in generators) {
            bid = gen.generateBid()
            event = SpecialEvent(bid, SpecialEvent.Cause.GENERATION, bid.timeOfGeneration)
            event.idDevice = gen.id
            tmp.add(event)
            if (bid.timeOfGeneration > maxBid.timeOfGeneration) maxBid = bid
        }
        for (gen in generators) {
            var tmpBid: Bid = gen.generateBid()
            while (tmpBid.timeOfGeneration < (maxBid.timeOfGeneration + timeOfMaxWorker)) {
                event = SpecialEvent(tmpBid, SpecialEvent.Cause.GENERATION, tmpBid.timeOfGeneration)
                event.idDevice = gen.id
                tmp.add(event)
                tmpBid = gen.generateBid()
            }
            gen.cancelLastBid()
        }

        queue.addAll(tmp)
        tmp.sortBy { it.bid!!.timeOfGeneration }
        return tmp
    }
    fun getResults(): ArrayList<SpecialEvent> {
        val results: ArrayList<SpecialEvent> = ArrayList(queue.size)
        var amountOfProducedBids = 0
        queue.sortWith(Comparator.naturalOrder())
        for (e in queue.distinct()){
            if (e.cause == SpecialEvent.Cause.FINISH_OF_PRODUCING && (amountOfProducedBids!=N_BIDS)) {
                amountOfProducedBids++
                results.add(e)
            }
            else if (amountOfProducedBids!=N_BIDS){
                results.add(e)
            }
        }
        results.sortWith(Comparator.naturalOrder())
        return results
    }

    private fun putBidInBuffer(bid: Bid) {
        /** 2.1. if Buffer is Full **/
        if (dispatcherBuffer.isFull()) {
            /** 2.1.1 cancel bid **/
            queue.add(dispatcherBuffer.cancel(bid.timeOfGeneration))
            /** 2.1.2 put generated bid in Buffer **/
            queue.add(dispatcherBuffer.add(bid, bid.timeOfGeneration))
        }
        /** 2.2 if Buffer is not Full **/
        else {
            /** 2.2.1 put generated bid in Buffer **/
            queue.add(dispatcherBuffer.add(bid, bid.timeOfGeneration))
        }
    }

    private fun recursiveCheckWorkers(bid: Bid, w: Worker) {
        if (bid.timeOfGeneration >= w.bid!!.timeOfEndProcessing) {
            /** 1.1. buffer is not EMPTY **/
            if (!dispatcherBuffer.isEmpty()) {
                /** 1.1.3. pull out bid from Buffer **/
                val b = dispatcherBuffer.getNext(w.bid!!.timeOfEndProcessing)
                if (b.timeOfGeneration >= w.bid!!.timeOfEndProcessing) {
                    b.timeOfExitBuffer = -1.0
                    dispatcherBuffer.add(b, b.timeOfGeneration)
                } else {
                    var event = SpecialEvent(b, SpecialEvent.Cause.EXIT_OF_BUFFERING, b.timeOfExitBuffer)
                    event.idDevice = b.idOfBuf
                    queue.add(event)

                    /** 1.1.4. put bid in Worker **/
                    putBidInWorker(b, w)
                    val events = dispatcherWorkers.commit(systemTime)
                    queue.addAll(events)
                    recursiveCheckWorkers(bid, w)
                }
            }
            /** 1.2. buffer is EMPTY **/
            else {
                /** 1.2.3. put generated bid in Worker **/
                putBidInWorker(bid, w)
                val events = dispatcherWorkers.commit(systemTime)
                queue.addAll(events)
            }
            /** 1.3 commit FINISH_OF_PRODUCING **/
            val events = dispatcherWorkers.commit(systemTime)
            queue.addAll(events)
        }
    }

    private var systemTime: Double = 0.0
    fun run() {
        while (!isFinishedModelling()) {
            for (e in generate()) {
                systemTime = e.bid!!.timeOfGeneration

                /** commit FINISH_OF_PRODUCING **/
                var events = dispatcherWorkers.commit(systemTime)
                queue.addAll(events)

                /** for each in workers **/
                var counter = 0
                while ((counter != dispatcherWorkers.N) && ((e.bid.timeOfStartProcessing == -1.0) && (e.bid.timeOfEntireBuffer == -1.0))) {
                    val w = dispatcherWorkers.getNext()
                    if (w.bid == null) {
                        /** 1.2.3. put generated bid in Worker **/
                        putBidInWorker(e.bid, w)
                        events = dispatcherWorkers.commit(systemTime)
                        queue.addAll(events)
                        counter = dispatcherWorkers.N-1
                    } else recursiveCheckWorkers(e.bid, w)
                    counter++
                    if (counter == dispatcherWorkers.N) {
                        if (e.bid.timeOfStartProcessing == -1.0) putBidInBuffer(e.bid)
                    }
                }
            }
        }
    }


//    private fun putBidInBuffer(bid: Bid) {
//        /** 2.1. if Buffer is Full **/
//        if (dispatcherBuffer.isFull()) {
//            /** 2.1.1 cancel bid **/
//            queue.add(dispatcherBuffer.cancel(bid.timeOfGeneration))
//            /** 2.1.2 put generated bid in Buffer **/
//            queue.add(dispatcherBuffer.add(bid, bid.timeOfGeneration))
//        }
//        /** 2.2 if Buffer is not Full **/
//        else {
//            /** 2.2.1 put generated bid in Buffer **/
//            queue.add(dispatcherBuffer.add(bid, bid.timeOfGeneration))
//        }
//    }
    private fun putBidInWorker(bid: Bid, w: Worker) {
        w.load(bid)
        val time = if (bid.timeOfGeneration < bid.timeOfExitBuffer) bid.timeOfExitBuffer else bid.timeOfGeneration
        val event = SpecialEvent(bid, SpecialEvent.Cause.START_OF_PRODUCING, time)
        event.idDevice = w.Id
        queue.add(event)
    }
//
//    fun run2() {
//        while (!isFinishedModelling()) {
//            for (e in generate()) {
//                /** commit FINISH_OF_PRODUCING **/
//                var events = dispatcherWorkers.commit(e.bid!!.timeOfGeneration)
//                queue.addAll(events)
//
//                // 1. В буфере есть заявки
//                if (!dispatcherBuffer.isEmpty()) {
//                    var counterWorkers = 0
//                    var counterBuffers = 0
//
//                    // 1.1 Проходим Буфер
//                    while (counterBuffers != (dispatcherBuffer.S)) {
//                        if (dispatcherBuffer.isEmpty()) break
//
//                        // Берем заявку из буфера
//                        val b = dispatcherBuffer.getNext(-1.0)
//
//                        var w = dispatcherWorkers.getNext()
//                        // 1.2 Находим свободный прибор, по указателю
//                        while ((counterWorkers != (dispatcherWorkers.N))) {
//                            // Возвращаем заявку обратно в буфер
//                            if (w.bid == null) {
//                                putBidInWorker(b, w)
//                                break
//                            }
//                            else if (e.bid.timeOfGeneration > w.bid!!.timeOfEndProcessing && b.timeOfGeneration < w.bid!!.timeOfEndProcessing) {
//                                val event = SpecialEvent(b, SpecialEvent.Cause.EXIT_OF_BUFFERING, w.bid!!.timeOfEndProcessing)
//                                event.idDevice = b.idOfBuf
//                                queue.add(event)
//                                putBidInWorker(b, w)
//                                break
//                            }
//                            else {
//                                counterWorkers++
//                                w = dispatcherWorkers.getNext() // Берем прибор
//                            }
//                        }
//
//                        if (b.timeOfStartProcessing != -1.0) {
//                            counterBuffers++
//                        }
//                        else {
//                            b.timeOfExitBuffer = -1.0
//                            dispatcherBuffer.add(b, b.timeOfGeneration)
//                            break
//                        }
//                    }
//                }
//
//                // 2. Находим свободный прибор, по указателю
//                var counter = 0
//                var w = dispatcherWorkers.getNext()
//                while ((counter != (dispatcherWorkers.N))) {
//                    // 2.1 Прибор свободен - ни разу не использовался
//                    if (w.bid == null) {
//                        putBidInWorker(e.bid, w)
//                        break
//                    }
//                    // 2.2 Прибор свободен после работы
//                    else if (w.bid!!.timeOfEndProcessing < e.bid.timeOfGeneration){
//                        putBidInWorker(e.bid, w)
//                        break
//                    }
//                    counter++
//                    w = dispatcherWorkers.getNext()
//                }
//
//                // 3. Нет свободных приборов
//                if (e.bid.timeOfStartProcessing == -1.0) putBidInBuffer(e.bid)
//            }
//        }
//    }
}



//class MainDispatcher(private val N_BIDS: Int,
//                     val N_GENS: Int, val N_WORK: Int, val S_BUFF: Int,
//                     alpha: Double, beta: Double, lambda: Double) {
//
//    private val generators: ArrayList<Generator> = ArrayList(N_GENS)
//    private val dispatcherWorkers: DispatcherWorkers
//    private val dispatcherBuffer: DispatcherBuffer
//
//    private var queue: ArrayList<SpecialEvent> = ArrayList()
//
//    init {
//        for (i in 1..N_GENS) generators.add(Generator.create(alpha, beta))
//        dispatcherWorkers = DispatcherWorkers(N_WORK, lambda)
//        dispatcherBuffer = DispatcherBuffer(S_BUFF)
//    }
//
//    private fun isFinishedModelling(): Boolean {
//        var counter = 0
//
//        var newQueue = ArrayList<SpecialEvent>()
//        queue.distinct().forEach {
//            if (!newQueue.contains(it)){
//                newQueue.add(it)
//                if (it.cause==SpecialEvent.Cause.FINISH_OF_PRODUCING) counter++
//            }
//        }
//        queue.clear()
//        queue.addAll(newQueue)
//        return counter >= N_BIDS
//    }
////    private fun generate(): ArrayList<SpecialEvent> {
////        var event: SpecialEvent?
////        val tmp: ArrayList<SpecialEvent> = ArrayList(generators.size)
////        var bid: Bid
////        var maxBid: Bid = generators[0].generateBid()
////        tmp.add(SpecialEvent(maxBid, SpecialEvent.Cause.GENERATION, maxBid.timeOfGeneration))
////        for (gen in generators) {
////            bid = gen.generateBid()
////            event = SpecialEvent(bid, SpecialEvent.Cause.GENERATION, bid.timeOfGeneration)
////            event.idDevice = gen.id
////            tmp.add(event)
////            if (bid.timeOfGeneration > maxBid.timeOfGeneration) maxBid = bid
////        }
////        for (gen in generators) {
////            var tmpBid: Bid = gen.generateBid()
////            while (tmpBid.timeOfGeneration < maxBid.timeOfGeneration) {
////                event = SpecialEvent(tmpBid, SpecialEvent.Cause.GENERATION, tmpBid.timeOfGeneration)
////                event.idDevice = gen.id
////                tmp.add(event)
////                tmpBid = gen.generateBid()
////            }
////            gen.cancelLastBid()
////        }
////
////        queue.addAll(tmp)
////        tmp.sortBy { it.bid!!.timeOfGeneration }
////        return tmp
////    }
//    fun getResults(): ArrayList<SpecialEvent> {
//        val results: ArrayList<SpecialEvent> = ArrayList(queue.size)
//        var amountOfProducedBids = 0
//        queue.sortWith(Comparator.naturalOrder())
//        for (e in queue.distinct()){
//            if (e.cause == SpecialEvent.Cause.FINISH_OF_PRODUCING && (amountOfProducedBids!=N_BIDS)) {
//                amountOfProducedBids++
//                results.add(e)
//            }
//            else if (amountOfProducedBids!=N_BIDS) results.add(e)
//        }
//        results.sortWith(Comparator.naturalOrder())
//        return results
//    }
//
//    private var systemTime: Double = 0.0
//
////    fun run() {
////        while (!isFinishedModelling()) {
////            for (e in generate()) {
////                systemTime = e.bid!!.timeOfGeneration
////
////                /** commit FINISH_OF_PRODUCING **/
////                val events = dispatcherWorkers.commit(systemTime)
////                queue.addAll(events)
////
////                /** for each in workers
////                 * N workers
////                 * counter считает сколько workers прошли
////                 * **/
////                var counter = 0
////                var isBeginProducingOrIputInBuffer = false
////                while (counter!=dispatcherWorkers.N && !isBeginProducingOrIputInBuffer){
////                    val w = dispatcherWorkers.getNext()
////                    if (w.bid == null) {
////                        /** 1.2.3. put generated bid in Worker **/
////                        w.load(e.bid)
////                        val event = SpecialEvent(e.bid, SpecialEvent.Cause.START_OF_PRODUCING, systemTime)
////                        event.idDevice = w.Id
////                        queue.add(event)
////                        isBeginProducingOrIputInBuffer=true
////                    }
////                    /** 1. worker is free **/
////                    else if (systemTime >= w.bid!!.timeOfEndProcessing) {
////                        /** 1.1. buffer is not EMPTY **/
////                        if (!dispatcherBuffer.isEmpty()) {
////                            /** 1.1.3. pull out bid from Buffer **/
////                            val bid = dispatcherBuffer.getNext(w.bid!!.timeOfEndProcessing)
////                            var event = SpecialEvent(bid, SpecialEvent.Cause.EXIT_OF_BUFFERING, systemTime)
////                            event.idDevice = dispatcherBuffer.getLastPlace()
////                            queue.add(event)
////
////                            /** 1.1.4. put bid in Worker **/
////                            w.load(bid)
////                            event = SpecialEvent(bid, SpecialEvent.Cause.START_OF_PRODUCING, systemTime)
////                            event.idDevice = w.Id
////                            queue.add(event)
////
//////                            /** 1.1.5. Buffer is FULL **/
//////                            if (dispatcherBuffer.isFull()) {
//////                                /** 1.1.5.1 cancel bid **/
//////                                queue.add(dispatcherBuffer.cancel(systemTime))
//////
//////                                /** 1.1.5.1 put generated bid in Buffer **/
//////                                queue.add(dispatcherBuffer.add(e.bid, systemTime))
//////                            }
//////                            /** 1.1.6 Buffer is not EMPTY **/
//////                            else {
//////                                /** 1.1.6.1 put generated bid in Buffer **/
//////                                queue.add(dispatcherBuffer.add(e.bid, systemTime))
//////                            }
////                            isBeginProducingOrIputInBuffer=true
////                        }
////                        /** 1.2. buffer is empty **/
////                        else {
////                            /** 1.2.3. put generated bid in Worker **/
////                            w.load(e.bid)
////                            val event = SpecialEvent(e.bid, SpecialEvent.Cause.START_OF_PRODUCING, systemTime)
////                            event.idDevice = w.Id
////                            queue.add(event)
////                            isBeginProducingOrIputInBuffer=true
////                        }
////                    }
////                    counter++
////                }
////
////                /** 2. all Workers are busy **/
////                if (e.bid.timeOfStartProcessing == -1.0) {
////                    /** 2.1. if Buffer is Full **/
////                    if (dispatcherBuffer.isFull()) {
////                        /** 2.1.1 cancel bid **/
////                        queue.add(dispatcherBuffer.cancel(systemTime))
////
////                        /** 2.1.2 put generated bid in Buffer **/
////                        queue.add(dispatcherBuffer.add(e.bid, systemTime))
////                    }
////                    /** 2.2 if Buffer is not Full **/
////                    else {
////                        /** 2.2.1 put generated bid in Buffer **/
////                        queue.add(dispatcherBuffer.add(e.bid, systemTime))
////                    }
////                }
////            }
////        }
////    }
//}
/*package manager
//
//import component.Generator
//import request.Bid
//import request.SpecialEvent
//
//class MainDispatcher(private val N_BIDS: Int, N_GENS: Int,
//                     N_WORK: Int, S_BUFF: Int,
//                     alpha: Double, beta: Double, lambda: Double) {
//
//    private val generators: ArrayList<Generator> = ArrayList(N_GENS)
//    private val dispatcherWorkers: DispatcherWorkers
//    private val dispatcherBuffer: DispatcherBuffer
//
//    private var queue: ArrayList<SpecialEvent> = ArrayList()
//
//    init {
//        for (i in 1..N_GENS) generators.add(Generator.create(alpha, beta))
//        dispatcherWorkers = DispatcherWorkers(N_WORK, lambda)
//        dispatcherBuffer = DispatcherBuffer(S_BUFF)
//    }
//
//    private fun isProducedAllBids(): Boolean {
//        var counter = 0
//        for (e in queue)
//            if (e.cause == SpecialEvent.Cause.FINISH_OF_PRODUCING) counter++
//        return counter >= N_BIDS
//    }
//    private fun generate(): ArrayList<SpecialEvent> {
//        var event: SpecialEvent?
//        val tmp: ArrayList<SpecialEvent> = ArrayList(generators.size)
//
//        var bid: Bid
//        var maxBid: Bid = generators[0].generateBid()
//        tmp.add(SpecialEvent(maxBid, SpecialEvent.Cause.GENERATION, maxBid.timeOfGeneration))
//        for (gen in generators) {
//            bid = gen.generateBid()
//            event = SpecialEvent(bid, SpecialEvent.Cause.GENERATION, bid.timeOfGeneration)
//            tmp.add(event)
//            if (bid.timeOfGeneration > maxBid.timeOfGeneration) maxBid = bid
//        }
//        for (gen in generators) {
//            var tmpBid: Bid = gen.generateBid()
//            while (tmpBid.timeOfGeneration < maxBid.timeOfGeneration) {
//                event = SpecialEvent(tmpBid, SpecialEvent.Cause.GENERATION, tmpBid.timeOfGeneration)
//                tmp.add(event)
//                tmpBid = gen.generateBid()
//            }
//            gen.cancelLastBid()
//        }
//
//        if (!dispatcherBuffer.isEmpty()){
//            for (buffered in dispatcherBuffer.getAllBidsInEvents()) {
//                if (buffered.bid!!.timeOfGeneration <= maxBid.timeOfGeneration)
//                    tmp.add(buffered)
//            }
//        }
//        queue.addAll(tmp)
//        val set: HashSet<SpecialEvent> = HashSet(queue)
//        queue.clear()
//        queue.addAll(set)
//
//        tmp.sortBy { it.bid!!.timeOfGeneration }
//        return tmp
//    }
//
//    fun run() {
//        var event: SpecialEvent?
//        while (!isProducedAllBids()) {
//            for (e in generate()) {
//                if (e.cause == SpecialEvent.Cause.GENERATION) {
//                    event = dispatcherWorkers.loadBid(e.bid!!.timeOfGeneration)
//                    // ALL BUSY {MAKE CANCEL AND ADD IN BUFF}
//                    if (event == null) {
//                        if (dispatcherBuffer.isFull()) { // если весь буфер занят
//                            event = dispatcherBuffer.cancel(e.bid.timeOfGeneration)
//                            queue.add(event)
//                        } // добавляем в буфер
//                        event = dispatcherBuffer.add(e.bid)
//                        queue.add(event)
//                    }
//                    // IF WORKER IS NOT PRODUCING {FIRST INIT}
//                    else if (event.cause == SpecialEvent.Cause.SYSTEM_NOT_STARTED){
//                        dispatcherWorkers.workers[event.idDevice].loadBid(e.bid)
//                        val tmpIdDevice = event.idDevice
//                        event = SpecialEvent(e.bid,
//                            SpecialEvent.Cause.START_OF_PRODUCING,
//                            dispatcherWorkers.workers[event.idDevice].bid!!.timeOfStartProcessing)
//                        event.idDevice = tmpIdDevice
//                        queue.add(event)
//                    }
//                    // RELEASED {THERE IS FREE WORKER}
//                    else {
//                        val tmpIdDevice = event.idDevice
//                        queue.add(event)
//                        dispatcherWorkers.workers[event.idDevice].loadBid(e.bid)
//                        event = SpecialEvent(e.bid,
//                            SpecialEvent.Cause.START_OF_PRODUCING,
//                            dispatcherWorkers.workers[event.idDevice].bid!!.timeOfStartProcessing)
//                        event.idDevice = tmpIdDevice
//                        queue.add(event)
//                    }
//                }
//                if (e.cause == SpecialEvent.Cause.ENTIRE_OF_BUFFERING) {
//                    val exitTimeOfBuffer = dispatcherWorkers.findPossibleTime(e.bid!!.timeOfEntireBuffer) ?: continue
//                    if (!dispatcherBuffer.isLast(e.bid)) continue
//                    queue.add(dispatcherBuffer.getNext(exitTimeOfBuffer)) // выход из буфера
//                    event = dispatcherWorkers.loadBid(exitTimeOfBuffer)   // поступление на прибор
//                    queue.add(event!!)
//                    val tmpIdDevice = event.idDevice
//                    dispatcherWorkers.workers[tmpIdDevice].loadBid(e.bid)
//                    event = SpecialEvent(e.bid,
//                        SpecialEvent.Cause.START_OF_PRODUCING,
//                        dispatcherWorkers.workers[tmpIdDevice].bid!!.timeOfStartProcessing)
//                    event.idDevice = tmpIdDevice
//                    queue.add(event)
//                }
//            }
//        }
//    }
//
//    fun getResults(): ArrayList<SpecialEvent> {
//        val results: ArrayList<SpecialEvent> = ArrayList(queue.size)
//        val set: HashSet<SpecialEvent> = HashSet(queue)
//        queue.clear()
//        queue.addAll(set)
//        queue.sort()
//        var amountOfBids = 0
//        for (e in queue) {
//            if (e.cause == SpecialEvent.Cause.FINISH_OF_PRODUCING)
//                amountOfBids++
//            results.add(e)
//            if (amountOfBids == N_BIDS) break
//        }
//        return results
//    }
*/