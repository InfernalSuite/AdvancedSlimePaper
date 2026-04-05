package com.infernalsuite.asp.util;

public interface ThrowingConsumer<T> {

    public void accept(T value) throws Exception;

}
