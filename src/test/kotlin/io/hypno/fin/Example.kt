package io.hypno.fin

import kotlin.concurrent.thread

fun main(vararg args: String) {
    // Creating HomeController does most of the work
    val homeController = HomeController()
    thread {
        Thread.sleep(1000)
        synchronized(homeController, {
            // Wait some time and click a post
            homeController.postClicked(2)
        })
    }
}

// Example data type
data class Post(val id: Int)

// The state of your HomeController
data class HomeState(
        val loading: Boolean = false,
        val posts: List<Post>? = null
) : State

// An action representing a request
// to load posts. When posts is null
// make a new request, if it has data
// add it to the state.
data class LoadPostsAction(
        val posts: List<Post>? = null
) : Action {
    override val name = "Load Posts"
}

// This action is part of a "Router"
// not the HomeReducer, it is handled
// by middleware.
data class OpenPostAction(
        val postId: Int
) : Action {
    override val name = "Open Post"
}

// Require a dispatcher to send actions
class HomeReducer(
        val dispatcher: Dispatcher<Action>
) : SyncReducer<HomeState, Action> {

    // After pre middleware is called, we can handle the action
    override fun reduce(state: HomeState, action: Action): HomeState? {
        // Handle the actions that Home knows about
        return when (action) {
            is LoadPostsAction -> handleLoadPosts(state, action)
            else -> state // Unhandled state, maybe middleware will handle it
        }
    }

    fun handleLoadPosts(state: HomeState, action: LoadPostsAction): HomeState? {
        action.posts?.let {
            // We have new posts, display them and stop loading
            return state.copy(posts = it, loading = false)
        }

        // Fake async api call for data
        fetchPostsFromApi {
            // Posts received, dispatch an event to tell the state
            dispatcher.dispatch(LoadPostsAction(it))
        }

        // We have no data yet so set the state to loading
        return state.copy(loading = true)
    }
}

class HomeController {

    // Create a StateProcessor that HomeController will manage
    val stateProcessor = FinStateProcessor<HomeState, Action>(HomeState())
    // Create a HomeReducer and give it the state processor as a dispatcher
    val homeReducer = HomeReducer(stateProcessor)

    init {
        // Handle state changes
        stateProcessor.setStateChangeHandler { (loading, posts) ->
            posts?.let { showPosts(it) }
            setLoading(loading)
        }

        // Set our primary reducer
        stateProcessor.setReducer(homeReducer)

        // Add logging middleware to see state change
        stateProcessor.addMiddleware({ state, action ->
            // Before primary reducer
            state.apply {
                println("_______________")
                println("Current state: $state")
                println("Incoming action: $action")
            }
        }, { state, _ ->
            // After primary reducer
            state.apply {
                println("New state: $state")
                println(" ")
            }
        })

        // Add route handling middleware to open new screens
        stateProcessor.addPreMiddleware { state, action ->
            when (action) {
                is OpenPostAction -> {
                    println("Opening post id: ${action.postId}")
                    // Sending null tells the SyncStateProcessor
                    // that the Action is done and we can safely
                    // discard any state changes.
                    null
                }
                // This middleware ignores any other action type
                else -> state
            }
        }

        // Dispatch an initial action to get things going
        stateProcessor.dispatch(LoadPostsAction())
    }

    fun showPosts(posts: List<Post>) {
        println("showing $posts")
    }

    fun setLoading(isLoading: Boolean) {
        println("loading visible: $isLoading")
    }

    fun postClicked(postId: Int) {
        stateProcessor.dispatch(OpenPostAction(postId))
    }
}

fun fetchPostsFromApi(callback: (List<Post>) -> Unit) {
    thread {
        Thread.sleep(500)
        synchronized(callback, {
            callback(listOf(Post(1), Post(2), Post(3)))
        })
    }
}