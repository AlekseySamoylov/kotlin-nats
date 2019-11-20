package com.alekseysamoylov.dealer


import car.Message
import io.nats.client.Connection
import io.nats.client.Dispatcher
import io.nats.client.Nats
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock

private const val orderSubject = "order.jvm.service"
private const val deliverySubject = "delivery.jvm.service"
private const val carAmount = 500_000
private val logger = LoggerFactory.getLogger("JvmMain")

var deliveryTimeout = LocalDateTime.now().plusSeconds(5)
var deliverySum = AtomicInteger(0)
lateinit var connection: Connection
lateinit var dispatcher: Dispatcher
fun main() {
    logger.info("JVM Dialer started")
    TimeUnit.SECONDS.sleep(5)

    connection = Nats.connect()
    try {
        subscribeToCarDelivery()
        var carAmountCorrection = 0
        while (true) {
            approveOrder(carAmountCorrection)
            carAmountCorrection = checkDelivery()

            logger.info("JVM Number of delivered LADA VAZ 2105: ${deliverySum.get()}")
        }
    } finally {
        connection.closeDispatcher(dispatcher)
        connection.close()
    }
}

fun checkDelivery(): Int {
    var previousDeliveryCarAmount = 0
    while (true) {
        if ((LocalDateTime.now().isAfter(deliveryTimeout) || deliverySum.get() % carAmount == 0) && previousDeliveryCarAmount == deliverySum.get()) {
            break
        }
        previousDeliveryCarAmount = deliverySum.get()
        TimeUnit.MILLISECONDS.sleep(500)
    }

    deliveryTimeout = LocalDateTime.now().plusSeconds(5)
    val deliveredForOrderOrZero = deliverySum.get() % carAmount
    return if (deliveredForOrderOrZero == 0) {
        0
    } else {
        carAmount - deliveredForOrderOrZero
    }
}


fun subscribeToCarDelivery() {
        dispatcher = connection.createDispatcher() {
            val carDelivery = Message.Delivery.parseFrom(it.data)
            if (carDelivery.model == "LADA VAZ 2105") {
                deliverySum.incrementAndGet()
            }
        }
        dispatcher.subscribe(deliverySubject)
}

fun approveOrder(carAmountCorrection: Int) {
    val order = Message.Order.newBuilder()
        .setId(UUID.randomUUID().toString())
        .setAmount(carAmount + carAmountCorrection)
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
}
