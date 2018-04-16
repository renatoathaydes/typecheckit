package com.typecheck.linear;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.Log;

import javax.lang.model.element.TypeElement;

public interface TypeChecker {

    default void typeProcessingStart() {
    }

    default void typeProcessingOver() {
    }

    void typeCheck(TypeElement element, TreePath treePath, Log log);

}
