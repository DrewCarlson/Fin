package io.hypno.fin

/**
 * The state of your application.
 */
interface State

/**
 * Actions that can be applied to your application state.
 */
interface Action {
    val name: String
}

/**
 * Middleware can be placed before or after a primary reducer.
 */
interface Middleware<in S, in A, out R> : Reducer<S, A, R>

/**
 * A reducer handles producing a new state from an
 * action in order to receive a new state.
 */
interface Reducer<in S, in A, out R> {
    fun reduce(state: S, action: A): R
}

/**
 * A handler to delegate actions to a StateProcessor.
 * Your dispatcher can control Action flow in any way
 * so long as no two Actions would process at once.
 */
interface Dispatcher<in A> {
    fun dispatch(action: A)
}

/**
 * A way to dispatch actions, apply middleware, and reduce the state.
 */
interface StateProcessor<S, A, R, M>
    : Reducer<S, A, R>, Dispatcher<A> {

    /**
     * Processing occurs when your Dispatcher is provided
     * a new Action and the Dispatcher determines that
     * it is an appropriate to process a new action.
     */
    fun process(state: S, action: A)

    fun addMiddleware(pre: M.(S, A) -> R,
                      post: M.(S, A) -> R)

    fun addPreMiddleware(middleware: M)
    fun addPreMiddleware(middleware: M.(S, A) -> R)
    fun addPostMiddleware(middleware: M)
    fun addPostMiddleware(middleware: M.(S, A) -> R)
}

interface SyncMiddleware<S : State, in A : Action> : Middleware<S, A, S?>
interface SyncReducer<S : State, in A : Action> : Reducer<S, A, S?>

/**
 * A SyncStateProcessor implementation to get started with.
 */
class FinStateProcessor<S : State, A : Action>(
        initialState: S
) : SyncStateProcessor<S, A>(initialState) {
    override fun rejected(state: S, action: A) {
        System.out.println("Rejected $action with $state")
    }
}

/**
 * An outline for a simple synchronous state processor.
 */
abstract class SyncStateProcessor<S : State, A : Action>(
        initialState: S
) : StateProcessor<S, A, S?, SyncMiddleware<S, A>> {

    private val preMiddleware = mutableListOf<SyncMiddleware<S, A>>()
    private val postMiddleware = mutableListOf<SyncMiddleware<S, A>>()

    private lateinit var reducer: SyncReducer<S, A>
    private var stateChangeHandler: (S) -> Unit = { }

    var state: S = initialState
        private set(value) {
            field = value
            stateChangeHandler(value)
        }

    fun setReducer(reducer: SyncReducer<S, A>) {
        this.reducer = reducer
    }

    fun setStateChangeHandler(handler: (S) -> Unit) {
        stateChangeHandler = handler
    }

    final override fun dispatch(action: A) {
        process(state, action)
    }

    final override fun reduce(state: S, action: A): S? {
        return reducer.reduce(state, action)
    }

    final override fun process(state: S, action: A) {
        var newState: S = state

        runMiddleware(preMiddleware, newState, action) ?: return rejected(newState, action)

        try {
            newState = reduce(newState, action) ?: return rejected(newState, action)
        } catch (e: Throwable) {
            return e.printStackTrace()
        }

        runMiddleware(postMiddleware, newState, action) ?: return rejected(newState, action)

        this.state = newState
    }

    private fun runMiddleware(middleware: List<SyncMiddleware<S, A>>, state: S, action: A): S? {
        var newState: S? = state

        middleware.forEach {
            try {
                newState = it.reduce(newState!!, action)
                if (newState == null) return null
            } catch(e: Throwable) {
                e.printStackTrace()
            }
        }
        return newState
    }

    abstract fun rejected(state: S, action: A)

    final override fun addMiddleware(pre: SyncMiddleware<S, A>.(S, A) -> S?,
                                     post: SyncMiddleware<S, A>.(S, A) -> S?) {
        addPreMiddleware(pre)
        addPostMiddleware(post)
    }

    final override fun addPreMiddleware(middleware: SyncMiddleware<S, A>) {
        preMiddleware.add(middleware)
    }

    final override fun addPreMiddleware(middleware: SyncMiddleware<S, A>.(S, A) -> S?) {
        addPreMiddleware(object : SyncMiddleware<S, A> {
            override fun reduce(state: S, action: A): S? {
                return middleware(state, action)
            }
        })
    }

    final override fun addPostMiddleware(middleware: SyncMiddleware<S, A>) {
        postMiddleware.add(middleware)
    }

    final override fun addPostMiddleware(middleware: SyncMiddleware<S, A>.(S, A) -> S?) {
        addPostMiddleware(object : SyncMiddleware<S, A> {
            override fun reduce(state: S, action: A): S? {
                return middleware(state, action)
            }
        })
    }
}
