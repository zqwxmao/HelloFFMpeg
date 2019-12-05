package com.michael.libplayer.opengl.core;


public interface IObservable<Type> {

    void addObserver(IObserver<Type> observer);

    void notify(Type type);

}
