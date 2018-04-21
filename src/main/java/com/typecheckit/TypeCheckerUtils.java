package com.typecheckit;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Log;
import java.util.List;
import javax.annotation.processing.Messager;

import static java.util.stream.Collectors.toList;

public class TypeCheckerUtils {

    private final Log log;
    private final Trees trees;
    private final Messager messager;
    private final CompilationUnitTree compilationUnit;

    public TypeCheckerUtils( Log log, Messager messager, Trees trees,
                             CompilationUnitTree compilationUnit ) {
        this.log = log;
        this.trees = trees;
        this.messager = messager;
        this.compilationUnit = compilationUnit;
    }

    public Log getLog() {
        return log;
    }

    public Messager getMessager() {
        return messager;
    }

    public Trees getTrees() {
        return trees;
    }

    public CompilationUnitTree getCompilationUnit() {
        return compilationUnit;
    }

    public List<String> annotationNames( VariableTree var ) {
        return var.getModifiers().getAnnotations().stream()
                .map( a -> ( ( JCTree.JCIdent ) a.getAnnotationType() ).sym.toString() )
                .collect( toList() );
    }

}
