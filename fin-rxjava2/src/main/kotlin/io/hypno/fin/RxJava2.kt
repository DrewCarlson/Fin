package io.hypno.fin

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject

data class Reduction<S, A>(
        val state: S,
        val action: A
)

interface RxReducer<S> {
    fun reduce(state: S, action: Action): S
}

interface RxDispatcher {
    fun dispatch(action: Any): Any
}

interface BaseMiddleware<S> {

    fun dispatch(state: S, next: RxDispatcher, action: Any)
}

interface RxMiddleware<A : Action> {
    fun reduce(actions: Observable<A>): Observable<A>
}

open class RxJava2StateProcessor<S, A : Action>(
        val reducer: RxReducer<S>
) {

    private val actionSubject: PublishSubject<A> = PublishSubject.create()

    private val preMiddleware: MutableList<RxMiddleware<A>> = mutableListOf()
    private val postMiddleware: MutableList<RxMiddleware<A>> = mutableListOf()

    private val stateSubject: PublishSubject<S> = PublishSubject.create()
    var stateObservable: Observable<S> = Observable.empty<S>()
        private set

    fun dispatch(action: A) {
        actionSubject.onNext(action)
    }

    fun process(state: S, action: A) {
        //val preTransformers = Observable.fromIterable(preMiddleware)
        /*.map { middleware ->
            Function<Reduction<S, A>, Observable<S>> {
                middleware.reduce(state, action)
            }
        }*/
        //

        actionSubject
                .compose {
                    var middlewareChain = it
                    preMiddleware.forEach {
                        middlewareChain = it.reduce(middlewareChain)
                    }
                    middlewareChain
                }
                .zipWith<S, Pair<A, S>>(stateObservable, BiFunction { t1, t2 ->
                    Pair(t1, t2)
                })
                .map { reducer.reduce(it.second, it.first) }

    }

    fun reduce(state: S, action: A): S {
        TODO("not implemented")
    }

    fun addMiddleware(pre: RxMiddleware<A>.(Observable<A>) -> Observable<A>,
                      post: RxMiddleware<A>.(Observable<A>) -> Observable<A>) {
        TODO("not implemented")
    }

    fun addPreMiddleware(middleware: RxMiddleware<A>) {
        TODO("not implemented")
    }

    fun addPreMiddleware(middleware: RxMiddleware<A>.(Observable<A>) -> Observable<A>) {
        TODO("not implemented")
    }

    fun addPostMiddleware(middleware: RxMiddleware<A>) {
        TODO("not implemented")
    }

    fun addPostMiddleware(middleware: RxMiddleware<A>.(Observable<A>) -> Observable<A>) {
        TODO("not implemented")
    }
}
