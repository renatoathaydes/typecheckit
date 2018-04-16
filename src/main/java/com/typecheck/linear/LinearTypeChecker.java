package com.typecheck.linear;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.Log;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.AbstractElementVisitor8;

public class LinearTypeChecker implements TypeChecker {

    @Override
    public void typeCheck(TypeElement element, TreePath treePath, Log log) {
        System.out.println("Type checking @Linear element: " + element);
        element.accept(new AbstractElementVisitor8<Object, Object>() {
            @Override
            public Object visitPackage(PackageElement packageElement, Object p) {
                System.out.println("Visiting package " + packageElement);
                return null;
            }

            @Override
            public Object visitType(TypeElement typeElement, Object p) {
                System.out.println("Visiting type " + typeElement);

                return null;
            }

            @Override
            public Object visitVariable(VariableElement variableElement, Object p) {
                System.out.println("Visiting var " + variableElement);
                return null;
            }

            @Override
            public Object visitExecutable(ExecutableElement executableElement, Object p) {
                System.out.println("Visiting exe " + executableElement);
                return null;
            }

            @Override
            public Object visitTypeParameter(TypeParameterElement typeParameterElement, Object p) {
                System.out.println("Visiting type-param " + typeParameterElement);
                return null;
            }
        }, null);
    }

}
