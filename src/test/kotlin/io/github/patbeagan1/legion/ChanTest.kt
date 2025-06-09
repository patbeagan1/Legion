package io.github.patbeagan1.legion

import io.github.patbeagan1.legion.ChanTest.Item.*
import io.github.patbeagan1.legion.ImpNode.BaseImp
import io.github.patbeagan1.legion.ImpNode.Imp
import io.github.patbeagan1.legion.Ingredient.*
import io.github.patbeagan1.legion.LegionScope.DebugLevel.ERROR
import io.github.patbeagan1.legion.LegionScope.DebugLevel.INFO
import io.github.patbeagan1.legion.joins.*
import io.github.patbeagan1.legion.nodes.*
import io.github.patbeagan1.legion.visualization.LinkCollection
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.system.measureNanoTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds


private interface I {
    val i: Int
}

private class A(override val i: Int) : I
private class B(override val i: Int) : I
private class C(override val i: Int) : I
private class D(override val i: Int) : I

class ChanTest {
    @Test
    fun `test basic chain with multiple links`() = runBlocking {
        val print = Imp("print") { it: String ->
            println(it)
        }
        val addS = Imp("addS") { it: String -> it.plus("s") }
        val addT = Imp("addT") { it: String -> it.plus("t") }
        val addR = Imp("addR") { it: String -> it.plus("r") }
        val addQ = Imp("addQ") { it: String -> it.plus("q") }

        withContext(Dispatchers.IO) {
            legion {
                start..addS
                start..addR
                start..addT
                start..addQ
                addS..addT
                addS..print
                addT..addR
                addT..print
                addR..print
                addQ..print
            }.accept("a")
        }
    }

    @Test
    fun `test chain using operator syntax`() = runBlocking {
        val addS = Imp("addS") { it: String -> it.plus("s") }
        val addT = Imp("addT") { it: String -> it.plus("t") }
        val addR = Imp("addR") { it: String -> it.plus("r") }
        val addQ = Imp("addQ") { it: String -> it.plus("q") }
        val print = Imp("print") { it: String ->
            println(it)
        }

        withContext(Dispatchers.IO) {
            legion {
                start..addR..addS..addT..print
            }.accept("a")
        }
    }

    @Test
    fun `test emit zero chain`() = runBlocking {
        legion<Unit> {
            start..Imp("zero") {
                A(0)
            }..Imp("a") {
                B(it.i)
            }..Imp("b") {
                runBlocking {
                    delay(500)
                }
                C(it.i)
            }..Imp("c") {
                println(it.i)
            }
        }.accept(Unit)
    }

    @Test
    fun `test chain with just functions`() = runBlocking {
        legion<String> {
            val code = start..{ it.map { it.code } }
            val p = imp<Int, Unit> { println(it) }

            listOf(code..{ it.sum() }, code..{ it.last() }, code..{ it.reduce { acc, i -> acc * i } }).forEach { it..p }
        }.run {
            runBlocking { accept("start") }
        }
    }

    class RedditRequest : BaseImp<String, RedditRequest.Out>() {
        private val client = HttpClient(CIO)
        override val name: String = "RedditRequest"

        override suspend fun action(input: String): Out = Out(
            client.get("https://www.reddit.com/r/$input/.json").bodyAsText()
        )

        data class Out(val outString: String)
    }

    @Test
    fun `test with network requests`() = runBlocking {
        val reddit = RedditRequest()

        val legion = legion {
            start..reddit..{ println(it) }
        }

        runBlocking {
            listOf(
                "PicsOfUnusualSlugs"
            ).forEach { i ->
                legion.accept(i)
            }
        }
    }

    class T : ImpNode<T.In, T.Out> by Imp("T", {
        Out(it.name + " t ")
    }) {
        data class In(val name: String)
        data class Out(val outName: String)
    }

    class S : BaseImp<S.In, S.Out>() {
        override suspend fun action(input: In): Out = Out(input.name + " s ")
        override val uuid: UUID = UUID.randomUUID()
        override val name: String = "S"
        override var isSubgraphEnd: Boolean = false

        override fun collectLinks(subgraphStack: LinkCollection.SubgraphStack, linkCollection: LinkCollection) {
            TODO("Not yet implemented")
        }

        data class In(val name: String)
        data class Out(val outName: String)

        object FromT : ImpNode<T.Out, In> by Imp("T to S", { In(it.outName) })
    }

    @Test
    fun `with custom class`() = runBlocking {
        val t = T()
        val s = S()
        legion {
            start..t..S.FromT..s..{ it.outName }..{ println(it) }
        }.accept(T.In("Hello world"))
    }

    class CountToThree : ImpNode<Int, Int> {
        private var i: Int = 0
        private val delegate = Imp<Int, Int>("Count to three") {
            if (i < 3) i++ else null
        }

        override val name: String = delegate.name
        override var isSubgraphEnd: Boolean = false
        override fun collectLinks(subgraphStack: LinkCollection.SubgraphStack, linkCollection: LinkCollection) {
            TODO("Not yet implemented")
        }

        override suspend fun accept(legionScope: LegionScope<*>, coroutineScope: CoroutineScope, e: Int) =
            delegate.accept(legionScope, coroutineScope, e)

        override fun <EventOther> LegionScope<*>.link(c: ImpNode<Int, EventOther>): ImpNode<Int, EventOther> =
            delegate..(c)

        override val uuid: UUID = UUID.randomUUID()
    }

    @Test
    fun `testing loop`() = runBlocking {
        val p = impGlobal<Int, Unit> { println(it) }

        val legion = legion {
            val count = CountToThree()
            start..count..count..count..p
        }

        runBlocking { legion.accept(0) }

        val legion2 = legion {
            val count = CountToThree()
            repeat(3) {
                start..count..p
            }
        }

        runBlocking { legion2.accept(0) }
    }

    @Test
    fun `test complex chain with ImplPartial4`() = runBlocking {
        val adder = impJoinAllTagged("add", 4) {
            val a = it.findByTag<Int>("a") ?: error("'a' required")
            val b = it.findByTag<Int>("b") ?: error("'b' required")
            val c = it.findByTag<Int>("c") ?: error("'c' required")
            val d = it.findByTag<Int>("d") ?: error("'d' required")
            a.value + b.value + c.value + d.value
        }

        val multiplier = impJoinAllTagged("mult", 4) {
            val a = it.findByTag<Int>("a") ?: error("'a' required")
            val b = it.findByTag<Int>("b") ?: error("'b' required")
            val c = it.findByTag<Int>("c") ?: error("'c' required")
            val d = it.findByTag<Int>("d") ?: error("'d' required")
            a.value * b.value * c.value * d.value
        }

        val emitFirst = Imp<Unit, Int>("first") { 3 }
        val emitSecond = Imp<Unit, Int>("second") { 7 }
        val emitThird = Imp<Unit, Int>("third") { 1 }
        val emitFourth = Imp<Unit, Int>("fourth") { 9 }


        val printImp = Imp<Int, Unit>("print_impl") { println("Adder result: $it") }

        legion {
            start..emitFirst
            emitFirst..containerize("a")..adder
            emitFirst..containerize("a")..multiplier

            start..emitSecond
            emitSecond..containerize("b")..adder
            emitSecond..containerize("b")..multiplier

            start..emitThird
            emitThird..containerize("c")..adder
            emitThird..containerize("c")..multiplier

            start..emitFourth
            emitFourth..containerize("d")..adder
            emitFourth..containerize("d")..multiplier

            adder..printImp
            multiplier..printImp
        }.accept(Unit)
    }

    @Test
    fun `test null will stop the chain`() = runBlocking {
        val legion = legion<Unit> {
            start..{ println("first") }..{ println("second"); null }..{ println("third") }..{ println("fourth") }
            start..{ println("first") }..{ println("second"); null }..{ println("third") }..{ println("fourth") }


        }
        val job = launch(Dispatchers.IO) {
            legion.listen {
                println(it)
            }
        }
        legion.accept(Unit)

        delay(1000)
        job.cancel()
    }

    @Test
    fun `testing cohorts`() = runBlocking(Dispatchers.IO) {
        val legion = legion<String>(debugLevel = ERROR) {

            val cohort = cohort<String, String>("reverse appended to capitalized") {
                val adder = impJoinAllTagged("add", 4) {
                    val a = it.findByTag<String>("P1") ?: error("'a' required")
                    val b = it.findByTag<String>("P2") ?: error("'b' required")
                    val c = it.findByTag<String>("P3") ?: error("'c' required")
                    val d = it.findByTag<String>("P4") ?: error("'d' required")
                    a.value + b.value + c.value + d.value
                }

                val count = impJoinAllTagged("count", 4) {
                    val a = it.findByTag<String>("P1") ?: error("'P1' required")
                    val b = it.findByTag<String>("P2") ?: error("'P2' required")
                    val c = it.findByTag<String>("P3") ?: error("'P3' required")
                    val d = it.findByTag<String>("P4") ?: error("'P4' required")
                    a.value.length + b.value.length + c.value.length + d.value.length
                }

                startCohort..{ (it.reversed()) }..containerize("P1")..listOf(adder, count)
                startCohort..{ (it.uppercase()) }..containerize("P2")..listOf(adder, count)
                startCohort..{ (it.reversed()) }..containerize("P3")..listOf(adder, count)
                startCohort..{ (it.uppercase()) }..containerize("P4")..listOf(adder, count)

                count..{ println(it) }
                adder..endCohort
            }

            start..cohort..{ println(it) }
        }

        legion.asGraphviz().also { println(it) }

        val job = launch(Dispatchers.IO) {
            legion.listen { println(it) }
        }

        legion.accept("Hello")

        delay(1000)
        job.cancel()
    }

    @Test
    fun `testing parallelization of only cohorts`() = runBlocking {
        val legion = legion<String>(debugLevel = ERROR) {

            val cohort = cohort<String, Int>(
                "reverse appended to capitalized",
                Dispatchers.IO
            ) {
                val count = impJoinAllTagged("count", 4) {
                    val a = it.findByTag<String>("P1") ?: error("'P1' required")
                    val b = it.findByTag<String>("P2") ?: error("'P2' required")
                    val c = it.findByTag<String>("P3") ?: error("'P3' required")
                    val d = it.findByTag<String>("P4") ?: error("'P4' required")
                    a.value.length + b.value.length + c.value.length + d.value.length
                }

                startCohort..{ (it.reversed()).also { println("c1") } }..containerize("P1")..count
                startCohort..{ (it.uppercase()).also { println("c2") } }..containerize("P2")..count
                startCohort..{ (it.reversed()).also { println("c3") } }..containerize("P3")..count
                startCohort..{ (it.uppercase()).also { println("c4") } }..containerize("P4")..count

                count..endCohort
            }

            start..cohort..{ println(it) }
            start..{ "1" }..{ println(it) }
            start..{ "2" }..{ println(it) }
            start..{ "3" }..{ println(it) }
            start..{ "4" }..{ println(it) }
        }

        val job = launch(Dispatchers.IO) {
            legion.listen { println(it) }
        }

        legion.accept("Hello")

        delay(1000)
        job.cancel()
    }

    @Test
    fun `test memo`() {
        runBlocking {
            val legion = legion<Int>(debugLevel = ERROR) {
                start..
                        memo {
                            delay(1)
                            it + 5
                        }..
                        { println(it) }
            }

            val job = launch(Dispatchers.IO) {
                var first: Long? = null
                legion.listen {
                    if (first == null) first = it.timeStamp
//                    println("$it ${it.timeStamp - first!!}")
                }
            }

            repeat(20) {
                measureNanoTime {
                    legion.accept(1)
                }.also { println(it.toDouble() / 1_000_000) }
                if (it % 5 == 0) delay(1)
            }

            delay(1000)
            job.cancel()
        }
    }

    @Test
    fun `test threshold`() = runBlocking {
        val legion = legion<String> {
            start..
                    { s -> s.split(" ").count().also { println(it) } }..
                    listOf(
                        threshold(thresholdMin = 20) { println("20 words quota reached") },
                        threshold(thresholdMax = 10) { println("Lesser than 10 words written") }
                    )

        }
        runBlocking {
            repeat(10) {
                legion.accept("Adding more content")
            }
        }
    }


    @Test
    fun `test gate`() = runBlocking {
        val legion = legion<String> {
            start..
                    { println(it) }

            start..
                    gate { it.startsWith("A") }..
                    { println(it.plus("A")) }

            start..
                    gate { it.startsWith("B") }..
                    { println(it.plus("B")) }
        }

        runBlocking {
            legion.accept("Apple")
            legion.accept("Banana")
        }
    }

    @Test
    fun `test gateXor`() = runBlocking {
        val legion = legion {
            start..gateXor(
                listOf(
                    Pair({ it.length == 1 }, imp<String, Unit> { println("one") }.also {
                        it..{ println("first") }
                    }),
                    Pair({ it.length == 2 }, imp { println("two") }),
                    Pair({ it.length == 3 }, imp { println("three") }),
                    Pair({ true }, imp { println("other") }),
                )
            )
        }

        runBlocking {
            legion.accept("AA")
            legion.accept("A")
            legion.accept("AAAA")
            legion.accept("AAA")
        }
    }

    @Test
    fun `test complex gateXor`() = runBlocking {
        val legion = legion {
            start..gateXor(
                listOf(
                    Pair(
                        { it.length < 5 },
                        cohort<String, Unit>("less than 5", Dispatchers.IO) {
                            startCohort..{ it.length }..{ println("less than 5! : $it") }
                            startCohort..{ println(it.uppercase()) }
                        }
                    ),
                    Pair(
                        { it.length < 10 },
                        cohort<String, Unit>("less than 10", Dispatchers.IO) {
                            startCohort..{ it.length }..{ println("less than 10! : $it") }
                            startCohort..{ println(it.uppercase()) }
                        }
                    ),
                    Pair({ true }, imp { println("default") })
                )
            )
        }

        runBlocking {
            legion.accept("12345")
            legion.accept("1234")
            legion.accept("1234567890123")
            legion.accept("123456789")
            legion.accept("1234567890")
            legion.accept("1234567890123456")

            delay(10000)
        }
    }

    @Test
    fun `test xor gates cannot be linked`(): Unit = runBlocking {
        assertFailsWith(IllegalAccessError::class) {
            legion<String> {
                start..gateXor(
                    listOf(
                        Pair({ true }, imp { println("A") })
                    )
                )..{ println("A") }
            }
        }
    }

    @Test
    fun `test factorio assembler`() = runBlocking {
        val assembler = legion(debugLevel = INFO) {

            // Define crafting logic
            val transportBeltAssembler = impJoinAllQuantified(
                name = "belt builder",
                quantity1 = Quantity(1),
                quantity2 = Quantity(2),
                quantity3 = Quantity(1),
                quantity4 = Quantity(1)
            ) { params: ImplPartialQuantified4Params<IronPlate, IronPlate2, IronPlate3, Gear> ->
                println("Crafting Transport Belt using $params")
                delay(Random.nextLong(100, 300)) // Simulate crafting time
                TransportBelt()
            }

            // Define crafting logic
            val transportBeltAssembler2 = impJoinAllQuantified(
                "belt builder",
                Quantity<IronPlate>(1),
                Quantity<IronPlate2>(2),
                Quantity<IronPlate3>(1),
                Quantity<Gear>(1)
            ) { params ->
                println("Crafting Transport Belt using $params")
                delay(200) // Simulate crafting time
                TransportBelt()
            }


            // Input stages for crafting
            start..
                    { println("Received input: $it"); it }..
                    transportBeltAssembler2..
                    { println("Crafted item: $it") }
        }

        val inputs = listOf(
            IronPlate(),
            Gear(),
            IronPlate(),
            Gear(),
            IronPlate2(),
            IronPlate2(),
            IronPlate3(),
            IronPlate2(),
            IronPlate3(),
            IronPlate3(),
            IronPlate(),
            Gear() // Enough for 3 TransportBelts
        )

        // Feed items into the assembler
        withContext(Dispatchers.IO) {
            inputs.forEach { assembler.accept(it) }
        }
        delay(2000) // Allow processing time before exiting
    }

    // Base item class
    sealed class Item {
        class IronPlate : Item()
        class IronPlate2 : Item()
        class IronPlate3 : Item()
        class Gear : Item()
        class TransportBelt : Item()
    }

//    @Test
//    fun `baking a cake`() {
//        val legion = legion<Unit> {
//            start..cohort("gather ingredients") {
//                listOf(
//                    Egg(),
//                    Flour(),
//                    Egg(),
//                    Milk(),
//                    Milk(),
//                    Sugar(),
//                    Egg(),
//                ).forEach { ingredient ->
//                    startCohort..{ ingredient }..this.endCohort
//                }
//            }..impJoinAllQuantified(
//                "bake cake",
//                Quantity<Egg>(3),
//                Quantity<Sugar>(1),
//                Quantity<Flour>(1),
//                Quantity<Milk>(1)
//            ) {
//                Cake()
//            }..{
//                println(it)
//            }
//        }.also { println(it.asGraphviz()) }
//
//        runBlocking {
//            startLegionWithTimeout(legion, Unit, timeout = 2000, dispatcher = Dispatchers.IO)
//        }
//    }

    @Test
    fun `baking a cake - minimal syntax`() = runBlocking {
        val legion = legion(debugLevel = INFO) {
            start..
                    impJoinAllQuantified(
                        "bake cake",
                        Quantity<Egg>(3),
                        Quantity<Sugar>(1),
                        Quantity<Flour>(1),
                        Quantity<Milk>(1)
                    ) { Cake() }..
                    { println(it) }
        }

        runBlocking(Dispatchers.IO) {
            listOf(
                Egg(),
                Flour(),
                Egg(),
                Milk(),
                Milk(),
                Sugar(),
                Egg(),
            ).forEach {
                legion.accept(it)
            }
        }
    }

    @Test
    fun testingRateLimit() = runBlocking {
        val startTime = System.currentTimeMillis()
        fun l() = (System.currentTimeMillis() - startTime).toString().padStart(5, '0')
        val legion = legion<String>(
//            debugLevel = LegionScope.DebugLevel.DEBUG,
            scope = CoroutineScope(Dispatchers.IO)
        ) {
            start..
                    rateLimit(1.seconds) { "1-- delay $it" }..
                    { println("${l()} $it ${Thread.currentThread().name}") }
            start..
                    rateLimit(3.seconds) { "-3- delay $it" }..
                    { println("${l()} $it ${Thread.currentThread().name}") }
            start..
                    rateLimit(5.seconds) { "--5 delay $it" }..
                    { println("${l()} $it ${Thread.currentThread().name}") }
        }
        val legion2 = legion<String>(
//            debugLevel = LegionScope.DebugLevel.DEBUG,
//            scope = CoroutineScope(Dispatchers.IO)
        ) {
            start..
                    rateLimit(1.seconds) { "1-- delay $it" }..
                    { println("${l()} $it ${Thread.currentThread().name}") }
            start..
                    rateLimit(3.seconds) { "-3- delay $it" }..
                    { println("${l()} $it ${Thread.currentThread().name}") }
            start..
                    rateLimit(5.seconds) { "--5 delay $it" }..
                    { println("${l()} $it ${Thread.currentThread().name}") }
        }

        runBlocking {

//            val listen = launch { legion.listen { println(it) } }
            val job = launch { delay(40_000) }

            legion.accept("Hello 0   ")
            legion.accept("Hello  1  ")
            legion.accept("Hello   2 ")
            legion.accept("Hello    3")

            delay(20000)
            println()

            legion2.accept("Hello 0   ")
            legion2.accept("Hello  1  ")
            legion2.accept("Hello   2 ")
            legion2.accept("Hello    3")

            job.join()
//            listen.cancel()
        }
    }

    @Test
    fun `test signals`() = runBlocking {
        val legion = legion<Unit>(scope = CoroutineScope(Dispatchers.IO)) {
            start..{
                println("before signal")
                val signal = awaitMessage<Message1>("test")
                println("after signal: ${signal.value}")
            }..{
                println("end")
            }

            start..{
                println("before signal2")
                val signal = awaitMessage<Message2>("other")
                println("after signal2: ${signal.value}")
            }..{
                println("end2")
            }

//            start..{
//                println("before signal2")
//                val signal = awaitMessage<Message1>("other")
//                println("after signal2: ${signal.value}")
//            }..{
//                println("end")
//            }
        }

        val j = launch(Dispatchers.IO) {
            legion.listen {
                println(it)
            }
        }

        legion.accept(Unit)
        delay(100)
        println("1")
        legion.sendMessage("other", Message2(3))
        println("2")
        legion.sendMessage("false", Message1(5))
        println("3")
        legion.sendMessage("test", Message1(10))
        delay(100)

        j.cancel()
    }

    suspend fun <T> printWithTag(msg: String, action: suspend () -> T): T {
        println("$msg: starting")
        val v = action()
        println("$msg: ending")
        return v
    }

    @Test
    fun `communicating across imps`(): Unit = runBlocking {
        val legion = legion<Unit>(debugLevel = INFO) {
            start..{
                printWithTag("a") {
                    awaitMessage<Message1>("a")
                }
            }..{
                println("a received $it")
            }

            start..{
                printWithTag("b") {
                    delay(300)
                    sendMessage("a", Message1(5))
                }
            }
        }

        legion.accept(Unit)
    }

    @Test
    fun `communicating across imps, queued`(): Unit = runBlocking {
        val legion = legion<Unit>(debugLevel = INFO) {
            start..{
                printWithTag("a") {
                    delay(300)
                    awaitMessageQueued<Message1>("a")
                }
            }..{
                println("a received $it")
            }

            start..{
                printWithTag("b") {
                    sendMessageQueued("a", Message1(5))
                }
            }
        }

        legion.accept(Unit)
    }

    @Test
    fun `test timer`() = runBlocking {
        legion<Unit> {
            start..
                    { "payload" }..
                    timer(3.seconds)..
                    { println("3 secs over: $it") }
        }.accept(Unit)
    }

    @Test
    fun `test visualization`() = runBlocking {
        legion<Unit> {
            start..
                    imp("payload") { "payload" }..
                    timer(3.seconds)..
                    imp("done") { println("3 secs over: $it") }
        }.asGraphviz().also { println(it) }

        Unit
    }

    @Test
    fun `test visualiztion 2`() = runBlocking {
        withContext(Dispatchers.IO) {
            legion {
                val print = imp<String, Unit>("print") { println(it) }
                val addS = imp<String, String>("addS") { it.plus("s") }
                val addT = imp<String, String>("addT") { it.plus("t") }
                val addR = imp<String, String>("addR") { it.plus("r") }
                val addQ = imp<String, String>("addQ") { it.plus("q") }

                start..addS
                start..addR
                start..addT
                start..addQ
                addS..addT
                addS..print
                addT..addR
                addT..print
                addR..print
                addQ..print
            }.asGraphviz().also { println(it) }
        }

        Unit
    }

    @Test
    fun `test visualiztion 3`() = runBlocking {
        withContext(Dispatchers.IO) {
            legion<Unit> {
                start..
                        cohort("gather ingredients") {
                            listOf(
                                Egg(),
                                Flour(),
                                Egg(),
                                Milk(),
                                Milk(),
                                Sugar(),
                                Egg(),
                            ).forEach { ingredient ->
                                startCohort..imp(ingredient::class.java.simpleName) { ingredient }..this.endCohort
                            }
                        }..
                        impJoinAllQuantified(
                            "bake cake",
                            Quantity<Egg>(3),
                            Quantity<Sugar>(1),
                            Quantity<Flour>(1),
                            Quantity<Milk>(1)
                        ) {
                            Cake()
                        }..
                        imp("Print") { println(it) }
            }.asGraphviz().also { println(it) }
        }
        Unit
    }


    @Test
    fun `test visualization with gates`() = runBlocking {
        withContext(Dispatchers.IO) {
            legion<String> {
                start..gate { it.startsWith("A") }..imp("Process A") { println("Processing A: $it") }
                start..gate { it.startsWith("B") }..imp("Process B") { println("Processing B: $it") }
                start..gateXor(
                    listOf(
                        Pair({ it.length < 5 }, imp("Short") { println("Short string: $it") }),
                        Pair({ it.length >= 5 }, imp("Long") { println("Long string: $it") })
                    )
                )
            }.asGraphviz().also { println(it) }
        }
        Unit
    }

    @Test
    fun `test join by tag`() = runBlocking {
        val legion = legion<Unit> {
            val join = impJoinAllTagged("joinAll", 3) {
                printWithTag("joining") {
                    it.also { println(it) }
                }
            }
            start..{ "first" }..containerize("test1")..join
            start..{ "second" }..containerize("test2")..join
            start..{ 3 }..containerize("test3")..join
            join..{
                println("test3: " + it.findByTag<Int>("test3"))
                println("test1: " + it.findByTag<String>("test1"))
                println("test2: " + it.findByTag<String>("test2"))
            }

            join..{ it.findByTag<Int>("test3")?.value }..{ it!!; println(it + 10) }
        }

        val j = launch(Dispatchers.IO) {
            legion.listen {
                println(it)
            }
        }

        legion.accept(Unit)

        j.cancel()
    }

    class FailedImpException() : Exception()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test splitter works correctly`() {
        // Given
        val inputString = "test"
        val outputChars = mutableListOf<Char>()
        val outputFinal = mutableListOf<String>()
        val results = MutableSharedFlow<String>()

        val legion = legion<String>(scope = CoroutineScope(Dispatchers.IO)) {
            start..cohort("test") {
                startCohort..{ println(it) }..endCohort
            }..{}

            start..{
                it.toCharArray().toList()
            }..splitter {
                startSplitter..{
                    outputChars.add(it)
                    it
                }..endSplitter
            }..{
                val result = it.joinToString("")
                outputFinal.add(result)
                result
            }..sink(results)
        }.also { println(it.asGraphviz()) }

        // When
//        startLegionWithTimeout(legion, inputString, dispatcher = Dispatchers.IO)

        runTest {
            legion.accept(inputString)
            delay(2000)
        }

        assertEquals(listOf('e', 's', 't', 't').groupBy { it }, outputChars.groupBy { it })
        assertEquals(listOf("estt"), outputFinal.map { it.toList().sorted().joinToString("") })
        assertEquals(inputString.length, outputChars.size)
        assertEquals(1, outputFinal.size)
        println("done")
    }

    @Test
    fun `cohort catches exceptions`() {
        val legion = legion<String> {
            start..cohort("test") {
                startCohort..{ println("1") }..endCohort
            }..cohort("exception") {
                startCohort..{
                    throw FailedImpException()
                }..endCohort
                errorCatch..{
                    println("resuming from error")
                }..{
                    "2"
                }..endCohort
            }..{
                println("3: finish")
            }
        }.also { println(it.asGraphviz()) }

        runBlocking {
            legion.accept("test")
            delay(10_000)
        }
    }

//    @Test
//    fun `test splitter works correctly - url example`() = runBlocking {
//        val inputString = "https://example.com,https://google.com"
//
//        val legion = legion<String> {
//            start..{ it.split(",") }..splitter {
//                startSplitter..{ Url(it) }..endSplitter
//            }..{
//                println(it)
//            }
//        }.also { println(it.asGraphviz()) }
//
//        startLegionWithTimeout(legion, inputString, dispatcher = Dispatchers.IO)
//    }

    private suspend fun <T> CoroutineScope.startLegionWithTimeout(
        legion: Legion<T>,
        input: T,
        shouldListen: Boolean = false,
        timeout: Long = 10_000,
        dispatcher: CoroutineDispatcher
    ) {
        if (shouldListen) {
            val j = launch(dispatcher) { legion.listen { println(it) } }

            legion.accept(input)

            delay(timeout)
            j.cancel()
        } else {
            legion.accept(input)
            delay(timeout)
            cancel()
        }
    }

    @Test
    fun `cli commands work in the graph`() {
        val legion = legion<String> {
            start..
                    { it.split(" ") }..
                    cli()..
                    { it.output.uppercase() }..
                    { println(it) }
        }

        runBlocking {
            legion.accept("echo hello")
            delay(200)
        }
    }

    @Test
    fun `cli commands work in the graph - complex`() {
        val legion = legion<Int> {
            val forward: Imp<Int, Int> = imp { it }

            for (i in 1..10) {
                start..{ i * 2 }..forward
            }

            forward..
                    { "echo $it" }..
                    { it.split(" ") }..
                    cli()..
                    { println(it) }
        }

        runBlocking {
            legion.accept(1)
            delay(200)
        }
    }

    @Test
    fun `test failed imp will retry`() = runBlocking {
        val legion = legion<Unit> {
            val tries = AtomicInteger()
            start..imp {
                /* idea - cohort will be the one handling failure.
                 It will be able to create a new coroutine scope, and react gracefully if anything below it dies
                 a built in supervisor.

                 To do that,
                 */
                if (tries.incrementAndGet() == 1) {
                    throw FailedImpException()
                }
            }
        }

        val j = launch(Dispatchers.IO) {
            legion.listen {
                println(it)
            }
        }

        legion.accept(Unit)

        j.cancel()
    }

    @Test
    fun `testing sinks work`() {
        val s = MutableSharedFlow<Int>()

        val legion = legion<String> {
            val sink = sink(s)
            start..{ it.count() }..sink
            start..{ 3 }..sink
        }

        runBlocking {
            val jobs = buildList {
                add(launch { s.collect { println(it) } })
                add(launch { legion.accept("test") })
            }
            delay(1_000)
            jobs.forEach { it.cancel() }
        }
    }
}

data class Message2(val value: Int) : LegionScope.Message
data class Message1(val value: Int) : LegionScope.Message

