package com.typecheckit;

import com.athaydes.osgiaas.javac.internal.DefaultClassLoaderContext;
import com.athaydes.osgiaas.javac.internal.compiler.OsgiaasJavaCompiler;
import com.typecheckit.linear.LinearTypeChecker;
import junit.framework.AssertionFailedError;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

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
                            "com.typecheckit.TypeCheckitProcessor", "-Atypechecker=" + typeCheckerClass().getName() ) );

    private final Set<String> imports = new HashSet<>( 5 );

    protected Class<? extends TypeChecker> typeCheckerClass() {
        return LinearTypeChecker.class;
    }

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
        return compileClass( "public void run() {\n"
                + codeSnippet
                + "\n}", pkg, className, writer, Runnable.class );
    }

    public Optional<Class<Object>> compileClass( String classBody ) {
        return compileClass( classBody, "", "MyClass", System.out );
    }

    public Optional<Class<Object>> compileClass( String classBody,
                                                 String pkg,
                                                 String className,
                                                 PrintStream writer,
                                                 Class<?>... interfaces ) {
        String qualifiedClassName = pkg.isEmpty() ? className : pkg + "." + className;
        String code = String.format( "%s %s "
                        + "public class %s%s {"
                        + "%s\n"
                        + "}",
                pkg.isEmpty() ? "" : "package " + pkg + ";",
                imports.stream().map( i -> String.format( "import %s;", i ) ).collect( joining() ),
                className,
                interfaces.length == 0 ? "" : " implements " + Stream.of( interfaces )
                        .map( Class::getName )
                        .collect( joining( ", " ) ),
                classBody );
        System.out.println( "-----\n" + code + "\n------" );
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
        assertSuccessfulCompilationOfClass( compiledClass, "MyClass", Object.class );
    }

    public void assertSuccessfulCompilationOfRunnableClass(
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

    public static <E extends Throwable> E shouldThrow( Class<E> errorType, Runnable action ) {
        try {
            action.run();
            throw new AssertionFailedError( "No exception was thrown" );
        } catch ( Exception e ) {
            if ( errorType.isAssignableFrom( e.getClass() ) ) {
                return errorType.cast( e );
            }
            throw new AssertionFailedError( "Expected Exception of type " + errorType.getName() +
                    " but " + e.getClass().getName() + " was thrown" );
        }
    }

}
