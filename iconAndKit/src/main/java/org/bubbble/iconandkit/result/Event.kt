
package org.bubbble.iconandkit.result

import androidx.lifecycle.Observer

/**
 * 用作通过LiveData表示事件的数据公开的包装。
 */
open class Event<out T>(private val content: T) {

    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * 返回内容并阻止其再次使用。
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * 返回内容，即使已经处理过。
     */
    fun peekContent(): T = content
}

/**
 * [Event]的[Observer]，简化了检查[Event]内容是否包含内容的模式已经处理。
 *
 * [onEventUnhandledContent] 仅在未处理[Event]的内容时调用。
 */
class EventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<Event<T>> {
    override fun onChanged(event: Event<T>?) {
        event?.getContentIfNotHandled()?.let { value ->
            onEventUnhandledContent(value)
        }
    }
}
