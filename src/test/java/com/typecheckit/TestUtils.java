package com.typecheckit;

import com.athaydes.osgiaas.javac.internal.DefaultClassLoaderContext;
import com.athaydes.osgiaas.javac.internal.compiler.OsgiaasJavaCompiler;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;

public class TestUtils {

    private final OsgiaasJavaCompiler compiler =
            new OsgiaasJavaCompiler(
                    DefaultClassLoaderContext.INSTANCE,
                    asList( "-processor",
                            "com.typecheckit.TypeCheckitProcessor" ) );

    private final Set<String> imports = new HashSet<>( 5 );

    protected void addImports( String... imports ) {
        this.imports.addAll( Arrays.asList( imports ) );
    }

    protected void clearImports() {
        imports.clear();
    }

    public Optional<Class<Object>> compileRunnableClassSnippet( String codeSnippet ) {
        return compileRunnableClassSnippet( codeSnippet, "", "Runner", System.out );
    }

    public Optional<Class<Object>> compileRunnableClassSnippet( String codeSnippet, PrintStream writer ) {
        return compileRunnableClassSnippet( codeSnippet, "", "Runner", writer );
    }

    public Optional<Class<Object>> compileRunnableClassSnippet( String codeSnippet,
                                                                String pkg,
                                                                String className,
                                                                PrintStream writer ) {
        String qualifiedClassName = pkg.isEmpty() ? className : pkg + "." + className;
        String code = String.format( "%s %s"
                        + "public class %s implements Runnable {"
                        + "  public void run() {\n"
                        + "    %s\n"
                        + "  }"
                        + "}",
                pkg.isEmpty() ? "" : "package " + pkg + ";",
                imports.stream().map( i -> String.format( "import %s;", i ) ).collect( joining() ),
                className,
                codeSnippet );

        return compiler.compile( qualifiedClassName, code, writer );
    }

    public void assertCompilationErrorContains( ByteArrayOutputStream writer, String error ) {
        String compilerOutput = writer.toString();
        System.out.println( "-----\n" + compilerOutput + "\n------" );
        List<String> outputLines = Arrays.asList( compilerOutput.split( "\n" ) );

        assertThat( outputLines, hasItem( error ) );
    }

    public void assertSuccessfulCompilationOfClass(
            @SuppressWarnings( "OptionalUsedAsFieldOrParameterType" ) Optional<Class<Object>> compiledClass ) {
        assertSuccessfulCompilationOfClass( compiledClass, "Runner", Runnable.class );
    }

    public void assertSuccessfulCompilationOfClass(
            @SuppressWarnings( "OptionalUsedAsFieldOrParameterType" ) Optional<Class<Object>> compiledClass,
            String expectedClassName,
            Class<?> expectedSuperType ) {
        assertThat( compiledClass.orElseThrow( () -> new RuntimeException( "Error compiling, see compiler output" ) )
                .asSubclass( expectedSuperType )
                .getName(), equalTo( expectedClassName ) );
    }

}
