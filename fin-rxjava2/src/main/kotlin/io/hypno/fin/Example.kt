package io.hypno.fin

import io.reactivex.Observable


fun main(vararg args: String) {
    val homeController = HomeController()

    homeController.postClicked(2)
}

data class Post(val id: Int)

data class HomeState(
        val loading: Boolean = false,
        val posts: List<Post>? = null
) : State

data class LoadPostsAction(
        val posts: List<Post>? = null,
        val refresh: Boolean = false
) : Action {
    override val name = "Load Posts"
}

data class LoadPostsErrorAction(
        val error: String
) : Action {
    override val name = "Load Posts Error"
}

data class OpenPostAction(
        val postId: Int
) : Action {
    override val name = "Open Post"
}

class HomeReducer : RxReducer<HomeState> {

    override fun reduce(state: HomeState, action: Action): HomeState {
        return when (action) {
            is LoadPostsAction -> handleLoadPosts(state, action)
            else -> state
        }
    }

    fun handleLoadPosts(state: HomeState, action: LoadPostsAction): HomeState {
        action.posts?.let {
            return state.copy(posts = it, loading = false)
        }
        return state.copy(loading = true)
    }
}

data class User(
        val username: String,
        val isAdmin: Boolean
)

class HomeController {
    val homeReducer = HomeReducer()
    val stateProcessor = RxJava2StateProcessor<HomeState, Action>(homeReducer)

    init {
        data class AuthAction(
                val username: String,
                val password: String
        ) : Action {
            override val name = "Auth"
        }

        data class AuthSuccessAction(
                val user: User
        ) : Action {
            override val name = "Auth Success"
        }

        data class AuthFailedAction(
                val error: String
        ) : Action {
            override val name = "Auth Failed"
        }

        stateProcessor.addPreMiddleware { actions: Observable<Action> ->
            actions.ofType(AuthAction::class.java)
                    .switchMap {
                        authenticateUser(it.username, it.password)
                                .map<Action> { AuthSuccessAction(it) }
                                .onErrorReturn {
                                    AuthFailedAction(it.message ?: "Unknown error")
                                }
                    }
        }

        stateProcessor.addPreMiddleware { actions: Observable<Action> ->
            actions.ofType(LoadPostsAction::class.java)
                    .filter { it.refresh || it.posts == null }
                    .switchMap {
                        fetchPostsFromApi()
                                .map<Action> { LoadPostsAction(it) }
                                .onErrorReturn {
                                    LoadPostsErrorAction(it.message ?: "Unknown error")
                                }
                    }
        }

        stateProcessor.addMiddleware({
            it.doOnNext {
                println("_______________")
                println("Incoming action: $it")
            }
        }, {
            it.doOnNext {
                println("Completed action: $it")
                println(" ")
            }
        })

        stateProcessor.addPreMiddleware {
            it.ofType(OpenPostAction::class.java)
                    .doOnNext { (id) ->
                        println("Opening post id: $id")
                    }
                    .cast(Action::class.java)
        }

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

fun authenticateUser(username: String, password: String): Observable<User> {
    return Observable.just(User("test", true))
}

fun fetchPostsFromApi(): Observable<List<Post>> {
    return Observable.just(listOf(Post(1), Post(2), Post(3)))
}