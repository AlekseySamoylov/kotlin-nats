package com.alekseysamoylov.dealer


import car.Message
import io.nats.client.Dispatcher
import io.nats.client.Nats
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock

private const val orderSubject = "order.jvm.service"
private const val deliverySubject = "delivery.jvm.service"
private const val carAmount = 500_000
//private var cyclicBarrier = CyclicBarrier(0)
private val lock = ReentrantReadWriteLock()
private val readLock: ReentrantReadWriteLock.ReadLock = lock.readLock()
private val writeLock: ReentrantReadWriteLock.WriteLock = lock.writeLock()
private val logger = LoggerFactory.getLogger("JvmMain")
private val objLock = Object()

fun main() {
    logger.info("JVM Dialer started")
//    TimeUnit.SECONDS.sleep(5)

    val connection = Nats.connect()
    var dispatcher: Dispatcher? = null
    try {
        val deliveryCounter = AtomicInteger(0)
        dispatcher = connection.createDispatcher() {
            val carDelivery = Message.Delivery.parseFrom(it.data)
            if (carDelivery.model == "Ford Mustang Shelby GT350") {
//                synchronized(objLock) {
                    deliveryCounter.incrementAndGet()
                    objLock.notify()
//                }
//                cyclicBarrier.await()
            }
        }
        dispatcher.subscribe(deliverySubject)
        while (true) {
//            if (cyclicBarrier.await(5, TimeUnit.SECONDS) > 0) {
//                logger.info("Timed out waiting for car delivery")
//            }
//            writeLock.tryLock(10, TimeUnit.SECONDS)
//            cyclicBarrier.reset()

//            TimeUnit.SECONDS.sleep(5)
            val order = Message.Order.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setAmount(carAmount)
                .setSubject(deliverySubject)
                .build()
            val future = connection.request(orderSubject, order.toByteArray())
            try {
                val message = future.get(500, TimeUnit.MILLISECONDS)
                val orderAccepted = Message.OrderAccepted.parseFrom(message.data)
                if (orderAccepted.orderId == order.id) {
                    logger.info("JVM Order accepted")
                }
            } catch (ex: Exception) {
                logger.info("Cannot get order accept")
            }

            while (deliveryCounter.get() % carAmount != 0) {
                synchronized(objLock) {
                    objLock.wait(5000)
                }
            }

            logger.info("JVM Number of delivered Mustang Shelby GT350: ${deliveryCounter.get()}")
        }
    } finally {
        connection.closeDispatcher(dispatcher)
        connection.close()
    }
}

//fun getCurrentCountDownLatch(): CountDownLatch {
//    return cyclicBarrier
//}
