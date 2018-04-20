package com.typecheck.linear;

import com.sun.source.util.Trees;
import com.sun.tools.javac.util.Log;

public class TypeCheckerUtils {

    private final Log log;
    private final Trees trees;

    public TypeCheckerUtils( Log log, Trees trees ) {
        this.log = log;
        this.trees = trees;
    }

    public Log getLog() {
        return log;
    }

    public Trees getTrees() {
        return trees;
    }
}
