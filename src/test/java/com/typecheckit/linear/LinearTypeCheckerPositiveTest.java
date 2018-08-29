package com.typecheckit.linear;

import com.typecheckit.TestUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class LinearTypeCheckerPositiveTest extends TestUtils {

    @Test
    public void canAssignLiteralToLinearVariable() {
        Optional<Class<Object>> runner = compileRunnableClassSnippet(
                "@Linear String s = \"hello @Linear\";\n" +
                        "System.out.println(s);" );

        assertThat( runner.orElseThrow( () -> new RuntimeException( "Error compiling" ) )
                .asSubclass( Runnable.class )
                .getName(), equalTo( "Runner" ) );
    }

    @Test
    public void canAssignNewObjectToLinearVariable() {
        Optional<Class<Object>> runner = compileRunnableClassSnippet(
                "@Linear Object s = new Object();\n"
                        + "System.out.println(s.toString());",
                "pkg", "Runner2", System.out );

        assertThat( runner.orElseThrow( () -> new RuntimeException( "Error compiling" ) )
                .asSubclass( Runnable.class )
                .getName(), equalTo( "pkg.Runner2" ) );
    }

    @Test
    public void canUseLinearVariableWithinDisjointIfBranches() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String hello = \"hello @Linear\";\n"
                                + "if (System.currentTimeMillis() > 20000000L) {\n"
                                + "    hello.toLowerCase();\n"
                                + "} else {\n"
                                + "    hello.toUpperCase();\n"
                                + "}",
                        new PrintStream( writer, true ) );

        assertThat( compiledClass.orElseThrow( () -> new RuntimeException( "Error compiling" ) )
                .asSubclass( Runnable.class )
                .getName(), equalTo( "Runner" ) );
    }

}
