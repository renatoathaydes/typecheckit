package com.typecheckit;

import com.athaydes.osgiaas.javac.internal.DefaultClassLoaderContext;
import com.athaydes.osgiaas.javac.internal.compiler.OsgiaasJavaCompiler;

import java.util.Optional;

import static java.util.Arrays.asList;

public class TestUtils {

    private static OsgiaasJavaCompiler compiler =
            new OsgiaasJavaCompiler(
                    DefaultClassLoaderContext.INSTANCE,
                    asList( "-processor",
                            "com.typecheckit.TypeCheckitProcessor" ) );

    public static Optional<Class<Object>> compileRunnableClassSnippet( String codeSnippet ) {
        return compileRunnableClassSnippet( codeSnippet, "", "Runner" );
    }

    public static Optional<Class<Object>> compileRunnableClassSnippet( String codeSnippet, String pkg, String className ) {
        String qualifiedClassName = pkg.isEmpty() ? className : pkg + "." + className;
        String code = String.format( "%s import com.typecheckit.annotation.Linear;"
                        + "public class %s implements Runnable {"
                        + "  public void run() {\n"
                        + "    %s\n"
                        + "  }"
                        + "}",
                pkg.isEmpty() ? "" : "package " + pkg + ";",
                className,
                codeSnippet );

        return compiler.compile( qualifiedClassName, code, System.out );
    }

}
