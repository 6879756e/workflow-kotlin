package com.squareup.tracing

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import okio.BufferedSink
import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.TimeSource

/**
 * Encodes and writes [trace events][TraceEvent] to an Okio [BufferedSink].
 *
 * @param scope The [CoroutineScope] that defines the lifetime for the encoder. When the scope is
 * cancelled or fails, the sink returned from [sinkProvider] will be closed.
 * @param start The [TimeMark] to consider the beginning timestamp of the trace. All trace events'
 * timestamps are relative to this mark.
 * [TimeSource.Monotonic].[markNow][TimeSource.Monotonic.markNow] by default.
 * @param ioDispatcher The [CoroutineDispatcher] to use to execute all IO operations.
 * [IO] by default.
 * @param sinkProvider Returns the [BufferedSink] to use to write trace events to. Called on a
 * background thread.
 */
@OptIn(ObsoleteCoroutinesApi::class)
public class TraceEncoder(
  scope: CoroutineScope,
  private val start: TimeMark = TraceEncoderTimeMark,
  ioDispatcher: CoroutineDispatcher = IO,
  private val sinkProvider: () -> BufferedSink
) : Closeable {

  private val processIdCounter = AtomicInteger(0)
  private val threadIdCounter = AtomicInteger(0)

  private val events: SendChannel<List<ChromeTraceEvent>> =
    scope.actor(ioDispatcher, capacity = UNLIMITED) {
      sinkProvider().use { sink ->
        // Start the JSON array. Doesn't need to be closed.
        sink.writeUtf8("[\n")

        @Suppress("EXPERIMENTAL_API_USAGE")
        consumeEach { eventBatch ->
          eventBatch.forEach { event ->
            event.writeTo(sink)
            sink.writeUtf8(",\n")
          }
          sink.flush()
        }
      }
    }

  /**
   * Allocates a new thread ID named [threadName] and returns a [TraceLogger] that will log all
   * events under that thread ID.
   *
   * Note this does not do anything with _actual_ threads, it just affects the thread ID used in
   * trace events.
   */
  public fun createLogger(
    processName: String = "",
    threadName: String = ""
  ): TraceLogger {
    val processId = processIdCounter.getAndIncrement()
    val threadId = threadIdCounter.getAndIncrement()

    // Log metadata to set thread and process names.
    val timestamp = getTimestampNow()
    val processNameEvent = createProcessNameEvent(processName, processId, timestamp)
    val threadNameEvent = createThreadNameEvent(threadName, processId, threadId, timestamp)
    events.trySend(listOf(processNameEvent, threadNameEvent))

    return object : TraceLogger {
      override fun log(eventBatch: List<TraceEvent>) = log(processId, threadId, eventBatch)
      override fun log(event: TraceEvent) = log(processId, threadId, event)
      override fun toString(): String =
        " TraceLogger(" +
          "processName=$processName, processId=$processId, " +
          "threadName=$threadName, threadId=$threadId)"
    }
  }

  override fun close() {
    events.close()
  }

  internal fun log(
    processId: Int,
    threadId: Int,
    eventBatch: List<TraceEvent>
  ) {
    val timestampMicros = getTimestampNow()
    val chromeTraceEvents = eventBatch.map {
      it.toChromeTraceEvent(threadId, processId, timestampMicros)
    }
    events.trySend(chromeTraceEvents)
  }

  internal fun log(
    processId: Int,
    threadId: Int,
    event: TraceEvent
  ) {
    val timestampMicros = getTimestampNow()
    val chromeTraceEvents = event.toChromeTraceEvent(threadId, processId, timestampMicros)
    events.trySend(listOf(chromeTraceEvents))
  }

  private fun getTimestampNow(): Long = start.elapsedNow
}

/**
 * A [TimeMark] that invokes [System.nanoTime] to calculate its start point as well as elapsed time.
 */
private object TraceEncoderTimeMark : TimeMark {
  /**
   * The moment at which which this [TraceEncoderTimeMark] was instantiated.
   */
  val start: Long = System.nanoTime()

  override val elapsedNow: Long
    get() = (System.nanoTime() - start).inWholeMicroseconds()

  private fun Long.inWholeMicroseconds(): Long = (this / 1000)
}
