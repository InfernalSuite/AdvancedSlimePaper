package com.infernalsuite.asp.plugin.util;

public interface ThrowingSupplier<T> {

  T get() throws Exception;

}
