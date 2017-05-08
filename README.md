# Fin
A pure Kotlin state management and action processor blueprint for every situation.

Fin provides a few classes and interfaces to define a contract for state management and processing.
It is inspired by Redux but adds no weight or opinions to your application.

Fin is a friend to all frameworks and architectures, simply implement a StateProcessor around
whatever it is that modifies and reads your application state.

# The Key
Fin does not manage your state, actions or reducers!
Fin leaves the heavy lifting to your application design, it has no architectural opinions (other than complete immutability).
This is why Fin works everywhere in your application UI, data, or wherever else you might want to manage state.
With that said, Fin does provide FinStateProcessor and SyncStateProcessor to help you get started right away.

# Example
Define a State:
```kotlin
// The state of our HomeController
data class HomeState(
        val loading: Boolean = false,
        val posts: List<Post>? = null
) : State
```
Define some Actions:
```kotlin
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
```
Implement your reducer:
```kotlin
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
```
Wire a StateProcessor and Reducer to our Application:
```kotlin
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
```

Now to run the program:
```kotlin
fun main(vararg args: String) {
    // Creating HomeController does most of the work
    val homeController = HomeController()
    async(1000) {
        // Wait some time and click a post
        homeController.postClicked(2)
    }
}
```

    _______________
    Current state: HomeState(loading=false, posts=null)
    Incoming action: LoadPostsAction(posts=null)
    New state: HomeState(loading=true, posts=null)

    loading visible: true
    _______________
    Current state: HomeState(loading=true, posts=null)
    Incoming action: LoadPostsAction(posts=[Post(id=1), Post(id=2), Post(id=3)])
    New state: HomeState(loading=false, posts=[Post(id=1), Post(id=2), Post(id=3)])

    showing [Post(id=1), Post(id=2), Post(id=3)]
    loading visible: false
    _______________
    Current state: HomeState(loading=false, posts=[Post(id=1), Post(id=2), Post(id=3)])
    Incoming action: OpenPostAction(postId=2)
    Opening post id: 2
    Rejected OpenPostAction(postId=2) with HomeState(loading=false, posts=[Post(id=1), Post(id=2), Post(id=3)])