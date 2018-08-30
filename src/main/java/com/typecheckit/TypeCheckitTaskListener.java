package com.typecheckit;

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Log;
import com.typecheckit.util.TypeCheckerUtils;

import java.util.List;
import javax.annotation.processing.Messager;

public class TypeCheckitTaskListener implements com.sun.source.util.TaskListener {

    private final JavacProcessingEnvironment processingEnvironment;
    private boolean hasInvokedTypeProcessingStart;
    private boolean hasInvokedTypeProcessingOver;
    private final List<TypeChecker> typeCheckers;

    public TypeCheckitTaskListener( JavacProcessingEnvironment processingEnvironment,
                                    List<TypeChecker> typeCheckers ) {
        this.processingEnvironment = processingEnvironment;
        this.typeCheckers = typeCheckers;
    }

    @Override
    public void started( TaskEvent e ) {
    }

    @Override
    public void finished( TaskEvent e ) {
        if ( e.getKind() != TaskEvent.Kind.ANALYZE ) {
            return;
        }

        if ( !hasInvokedTypeProcessingStart ) {
            for ( TypeChecker typeChecker : typeCheckers ) {
                typeChecker.start();
            }
            hasInvokedTypeProcessingStart = true;
        }

        Messager messager = processingEnvironment.getMessager();
        Log log = Log.instance( processingEnvironment.getContext() );
        Trees trees = Trees.instance( processingEnvironment );
        TreePath treePath = trees.getPath( e.getTypeElement() );
        TypeCheckerUtils utils = new TypeCheckerUtils( log, messager, trees, e.getCompilationUnit() );

        for ( TypeChecker typeChecker : typeCheckers ) {
            typeChecker.scan( treePath, utils );
        }

        if ( !hasInvokedTypeProcessingOver ) {
            for ( TypeChecker typeChecker : typeCheckers ) {
                typeChecker.stop();
            }
            hasInvokedTypeProcessingOver = true;
        }
    }

}
