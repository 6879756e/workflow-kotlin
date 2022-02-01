package com.squareup.tracing

import com.squareup.tracing.ChromeTraceEvent.Companion.INSTANT_SCOPE_GLOBAL
import com.squareup.tracing.ChromeTraceEvent.Companion.INSTANT_SCOPE_PROCESS
import com.squareup.tracing.ChromeTraceEvent.Companion.INSTANT_SCOPE_THREAD
import com.squareup.tracing.ChromeTraceEvent.Phase.ASYNC_BEGIN
import com.squareup.tracing.ChromeTraceEvent.Phase.ASYNC_END
import com.squareup.tracing.ChromeTraceEvent.Phase.COUNTER
import com.squareup.tracing.ChromeTraceEvent.Phase.DURATION_BEGIN
import com.squareup.tracing.ChromeTraceEvent.Phase.DURATION_END
import com.squareup.tracing.ChromeTraceEvent.Phase.INSTANT
import com.squareup.tracing.ChromeTraceEvent.Phase.OBJECT_CREATED
import com.squareup.tracing.ChromeTraceEvent.Phase.OBJECT_DESTROYED
import com.squareup.tracing.ChromeTraceEvent.Phase.OBJECT_SNAPSHOT
import com.squareup.tracing.TraceEvent.AsyncDurationBegin
import com.squareup.tracing.TraceEvent.AsyncDurationEnd
import com.squareup.tracing.TraceEvent.Counter
import com.squareup.tracing.TraceEvent.DurationBegin
import com.squareup.tracing.TraceEvent.DurationEnd
import com.squareup.tracing.TraceEvent.Instant
import com.squareup.tracing.TraceEvent.Instant.InstantScope.GLOBAL
import com.squareup.tracing.TraceEvent.Instant.InstantScope.PROCESS
import com.squareup.tracing.TraceEvent.Instant.InstantScope.THREAD
import com.squareup.tracing.TraceEvent.ObjectCreated
import com.squareup.tracing.TraceEvent.ObjectDestroyed
import com.squareup.tracing.TraceEvent.ObjectSnapshot

/**
 * Represents a single event in a trace.
 */
public sealed class TraceEvent {

  public open val category: String? get() = null

  public data class DurationBegin(
    val name: String,
    val args: Map<String, Any?> = emptyMap(),
    override val category: String? = null
  ) : TraceEvent()

  public data class DurationEnd(
    val name: String,
    val args: Map<String, Any?> = emptyMap(),
    override val category: String? = null
  ) : TraceEvent()

  public data class Instant(
    val name: String,
    val args: Map<String, Any?> = emptyMap(),
    val scope: InstantScope = THREAD,
    override val category: String? = null
  ) : TraceEvent() {
    public enum class InstantScope {
      THREAD,
      PROCESS,
      GLOBAL
    }
  }

  public data class AsyncDurationBegin(
    val id: Any,
    val name: String,
    val args: Map<String, Any?> = emptyMap(),
    override val category: String? = null
  ) : TraceEvent()

  public data class AsyncDurationEnd(
    val id: Any,
    val name: String,
    val args: Map<String, Any?> = emptyMap(),
    override val category: String? = null
  ) : TraceEvent()

  public data class ObjectCreated(
    val id: Long,
    val objectType: String
  ) : TraceEvent()

  public data class ObjectDestroyed(
    val id: Long,
    val objectType: String
  ) : TraceEvent()

  public data class ObjectSnapshot(
    val id: Long,
    val objectType: String,
    val snapshot: Any
  ) : TraceEvent()

  public data class Counter(
    val name: String,
    val series: Map<String, Number>,
    val id: Long? = null
  ) : TraceEvent()
}

internal fun TraceEvent.toChromeTraceEvent(
  threadId: Int,
  processId: Int,
  nowMicros: Long
): ChromeTraceEvent = when (this) {
  is DurationBegin -> ChromeTraceEvent(
    phase = DURATION_BEGIN,
    name = name,
    category = category,
    args = args,
    threadId = threadId,
    processId = processId,
    timestampMicros = nowMicros
  )
  is DurationEnd -> ChromeTraceEvent(
    phase = DURATION_END,
    name = name,
    category = category,
    args = args,
    threadId = threadId,
    processId = processId,
    timestampMicros = nowMicros
  )
  is Instant -> ChromeTraceEvent(
    phase = INSTANT,
    name = name,
    category = category,
    scope = when (scope) {
      THREAD -> INSTANT_SCOPE_THREAD
      PROCESS -> INSTANT_SCOPE_PROCESS
      GLOBAL -> INSTANT_SCOPE_GLOBAL
    },
    args = args,
    threadId = threadId,
    processId = processId,
    timestampMicros = nowMicros
  )
  is AsyncDurationBegin -> ChromeTraceEvent(
    phase = ASYNC_BEGIN,
    id = id,
    name = name,
    category = category,
    args = args,
    threadId = threadId,
    processId = processId,
    timestampMicros = nowMicros
  )
  is AsyncDurationEnd -> ChromeTraceEvent(
    phase = ASYNC_END,
    id = id,
    name = name,
    category = category,
    threadId = threadId,
    processId = processId,
    timestampMicros = nowMicros
  )
  is ObjectCreated -> ChromeTraceEvent(
    phase = OBJECT_CREATED,
    id = id.toHex(),
    name = objectType,
    category = category,
    threadId = threadId,
    processId = processId,
    timestampMicros = nowMicros
  )
  is ObjectDestroyed -> ChromeTraceEvent(
    phase = OBJECT_DESTROYED,
    id = id.toHex(),
    name = objectType,
    category = category,
    threadId = threadId,
    processId = processId,
    timestampMicros = nowMicros
  )
  is ObjectSnapshot -> ChromeTraceEvent(
    phase = OBJECT_SNAPSHOT,
    id = id.toHex(),
    name = objectType,
    category = category,
    args = mapOf("snapshot" to snapshot),
    threadId = threadId,
    processId = processId,
    timestampMicros = nowMicros
  )
  is Counter -> ChromeTraceEvent(
    phase = COUNTER,
    id = id?.toHex(),
    name = name,
    args = series,
    threadId = threadId,
    processId = processId,
    timestampMicros = nowMicros
  )
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Long.toHex() = toString(16)
