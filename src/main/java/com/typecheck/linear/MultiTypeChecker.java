package com.typecheck.linear;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.Log;

import javax.lang.model.element.TypeElement;
import java.util.List;

public class MultiTypeChecker implements TypeChecker {

    private final List<TypeChecker> typeCheckers;

    public MultiTypeChecker(List<TypeChecker> typeCheckers) {
        this.typeCheckers = typeCheckers;
    }

    @Override
    public void typeProcessingStart() {
        for (TypeChecker typeChecker : typeCheckers) {
            typeChecker.typeProcessingStart();
        }
    }

    @Override
    public void typeProcessingOver() {
        for (TypeChecker typeChecker : typeCheckers) {
            typeChecker.typeProcessingOver();
        }
    }

    @Override
    public void typeCheck(TypeElement element, TreePath treePath, Log log) {
        for (TypeChecker typeChecker : typeCheckers) {
            typeChecker.typeCheck(element, treePath, log);
        }
    }
}
