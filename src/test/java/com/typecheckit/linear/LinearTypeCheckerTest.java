package com.typecheckit.linear;

import com.typecheckit.TestUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class LinearTypeCheckerTest extends TestUtils {

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
    public void cannotUseLinearVariableAfterUsedUp() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String s = \"hello @Linear\";\n"
                                + "String t = s.toUpperCase(); // used up\n"
                                + "System.out.println(s.toLowerCase()); // should fail here\n"
                                + "System.out.println(t);",
                        new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );

        String compilerOutput = writer.toString();
        System.out.println( "-----\n" + compilerOutput + "\n------" );
        List<String> outputLines = Arrays.asList( compilerOutput.split( "\n" ) );

        assertThat( outputLines,
                hasItem( "error: Runner.java:4 Re-using @Linear variable s" ) );
    }

    @Test
    public void cannotUseLinearVariableAfterUsedUpInIfStatementCondition() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String hi = \"hello @Linear\";\n"
                                + "if (hi.length() > 2) {\n"
                                + "  System.out.println(true);\n"
                                + "}\n"
                                + "System.out.println(hi.toLowerCase()); // should fail here",
                        new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );

        String compilerOutput = writer.toString();
        System.out.println( "-----\n" + compilerOutput + "\n------" );
        List<String> outputLines = Arrays.asList( compilerOutput.split( "\n" ) );

        assertThat( outputLines,
                hasItem( "error: Runner.java:6 Re-using @Linear variable hi" ) );
    }

    @Test
    public void cannotUseLinearVariableAfterUsedUpInIfStatementBlock() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String hi = \"hello @Linear\";\n"
                                + "if (System.currentTimeMillis() > 20000000L) {\n"
                                + "    System.out.println(hi);\n"
                                + "}\n"
                                + "System.out.println(hi.toLowerCase()); // should fail here",
                        new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );

        String compilerOutput = writer.toString();
        System.out.println( "-----\n" + compilerOutput + "\n------" );
        List<String> outputLines = Arrays.asList( compilerOutput.split( "\n" ) );

        assertThat( outputLines,
                hasItem( "error: Runner.java:6 Re-using @Linear variable hi" ) );
    }

    @Test
    public void cannotUseLinearVariableAfterUsedUpFromInsideIfStatementBlock() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String hello = \"hello @Linear\";\n"
                                + "System.out.println(hello); // used up\n"
                                + "if (System.currentTimeMillis() > 20000000L) {\n"
                                + "    hello = hello.toLowerCase(); // should fail here\n"
                                + "}",
                        new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );

        String compilerOutput = writer.toString();
        System.out.println( "-----\n" + compilerOutput + "\n------" );
        List<String> outputLines = Arrays.asList( compilerOutput.split( "\n" ) );

        assertThat( outputLines,
                hasItem( "error: Runner.java:5 Re-using @Linear variable hello" ) );
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


    @Test
    public void cannotUseLinearVariableInsideForLoop() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String hello = \"hello @Linear\";\n"
                                + "for (int i = 0; i < 1; i++) {\n"
                                + "    hello.toLowerCase(); // should fail here\n"
                                + "}",
                        new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );

        String compilerOutput = writer.toString();
        System.out.println( "-----\n" + compilerOutput + "\n------" );
        List<String> outputLines = Arrays.asList( compilerOutput.split( "\n" ) );

        assertThat( outputLines,
                hasItem( "error: Runner.java:4 Re-using @Linear variable hello" ) );
    }

}
