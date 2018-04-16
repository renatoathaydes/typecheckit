package com.typecheck.linear;

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Log;

import javax.lang.model.element.TypeElement;
import java.util.List;

public class LinearTaskListener implements com.sun.source.util.TaskListener {

    private final JavacProcessingEnvironment processingEnvironment;
    private boolean hasInvokedTypeProcessingStart;
    private boolean hasInvokedTypeProcessingOver;
    private final TypeChecker typeChecker;


    public LinearTaskListener(JavacProcessingEnvironment processingEnvironment,
                              List<TypeChecker> typeCheckers) {
        this.processingEnvironment = processingEnvironment;
        this.typeChecker = new MultiTypeChecker(typeCheckers);
    }

    @Override
    public void started(TaskEvent e) {
        System.out.println("STARTED: " + e);
    }

    @Override
    public void finished(TaskEvent e) {
        System.out.println("FINISHED: " + e);
        if (e.getKind() != TaskEvent.Kind.ANALYZE) {
            return;
        }

        if (!hasInvokedTypeProcessingStart) {
            typeChecker.typeProcessingStart();
            hasInvokedTypeProcessingStart = true;
        }

        Log log = Log.instance(processingEnvironment.getContext());
        TypeElement typeElement = e.getTypeElement();
        TreePath treePath = Trees.instance(processingEnvironment).getPath(typeElement);

        typeChecker.typeCheck(typeElement, treePath, log);

        if (!hasInvokedTypeProcessingOver) {
            typeChecker.typeProcessingOver();
            hasInvokedTypeProcessingOver = true;
        }
    }

}
