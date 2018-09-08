package com.typecheckit.linear;

import com.typecheckit.TestUtils;
import com.typecheckit.annotation.Linear;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;

import static org.junit.Assert.assertFalse;

public class LinearTypeCheckerNegativeTest extends TestUtils {

    @Before
    public void setup() {
        addImports( Linear.class.getName() );
    }

    @Test
    public void linearAnnotationCanBeRecognizedWithoutImport() {
        clearImports();

        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@" + Linear.class.getName() + " String s = \"hello @Linear\";\n"
                                + "String t = s.toUpperCase(); // used up\n"
                                + "System.out.println(s.toLowerCase()); // should fail here\n"
                                + "System.out.println(t);",
                        new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );
        assertCompilationErrorContains( writer, "error: Runner.java:4 Re-using @Linear variable s" );
    }

    @Test
    public void linearAnnotationCanBeRecognizedWithStarImport() {
        clearImports();
        addImports( Linear.class.getPackage().getName() + ".*" );

        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String s = \"hello @Linear\";\n"
                                + "String t = s.toUpperCase(); // used up\n"
                                + "System.out.println(s.toLowerCase()); // should fail here\n"
                                + "System.out.println(t);",
                        new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );
        assertCompilationErrorContains( writer, "error: Runner.java:4 Re-using @Linear variable s" );
    }

    @Test
    public void linearAnnotationCanBeRecognizedWithAndWithoutQualifiedName() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@" + Linear.class.getName() + " int a = 1;\n"
                                + "@Linear int b = 2;\n"
                                + "@Linear int c = a + b; // both a and b used up\n"
                                + "System.out.println(a);\n"
                                + "System.out.println(b);\n"
                                + "System.out.println(c);",
                        new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );
        assertCompilationErrorContains( writer, "error: Runner.java:5 Re-using @Linear variable a" );
        assertCompilationErrorContains( writer, "error: Runner.java:6 Re-using @Linear variable b" );
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
        assertCompilationErrorContains( writer, "error: Runner.java:4 Re-using @Linear variable s" );
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
        assertCompilationErrorContains( writer, "error: Runner.java:6 Re-using @Linear variable hi" );
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
        assertCompilationErrorContains( writer, "error: Runner.java:6 Re-using @Linear variable hi" );
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
        assertCompilationErrorContains( writer, "error: Runner.java:5 Re-using @Linear variable hello" );
    }

    @Test
    public void cannotUseLinearVariableUsedUpFromInsideIfStatementBlockInAnotherBlock() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String hello = \"hello @Linear\";\n"
                                + "if (System.currentTimeMillis() > 20000000L) {\n"
                                + "    System.out.println(hello); // used up\n"
                                + "}\n"
                                + "if (System.currentTimeMillis() > 20000L) {\n"
                                + "    hello = hello.toLowerCase(); // should fail here\n"
                                + "}",
                        new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );
        assertCompilationErrorContains( writer, "error: Runner.java:7 Re-using @Linear variable hello" );
    }

    @Test
    public void cannotUseLinearVariableInsideMoreThanOneNestedIfBlock() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String hello = \"hello @Linear\";\n"
                                + "long t = System.currentTimeMillis();\n"
                                + "if (t > 20000000L) {\n"
                                + "    if (t > 20000L) {\n"
                                + "        System.out.println(hello); // used up\n"
                                + "    } else {}\n"
                                + "}\n"
                                + "if (t < 20000L) {\n"
                                + "    hello = hello.toLowerCase(); // should fail here\n"
                                + "}",
                        new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );
        assertCompilationErrorContains( writer, "error: Runner.java:10 Re-using @Linear variable hello" );
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
        assertCompilationErrorContains( writer, "error: Runner.java:4 Re-using @Linear variable hello" );
    }

    @Test
    public void cannotUseLinearVariableInsideEnhancedForLoop() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String hello = \"hello @Linear\";\n"
                                + "for (char c : \"word\".chars()) {\n"
                                + "    hello.toLowerCase(); // should fail here\n"
                                + "}",
                        new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );
        assertCompilationErrorContains( writer, "error: Runner.java:4 Re-using @Linear variable hello" );
    }

    @Test
    public void cannotUseLinearVariableInsideWhileLoop() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String hello = \"hello @Linear\";\n"
                                + "int i = 1;\n"
                                + "while (i++ < 10) {\n"
                                + "    hello.toLowerCase(); // should fail here\n"
                                + "}",
                        new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );
        assertCompilationErrorContains( writer, "error: Runner.java:5 Re-using @Linear variable hello" );
    }

    @Test
    public void cannotUseLinearVariableInsideDoWhileLoop() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String hello = \"hello @Linear\";\n"
                                + "int i = 1;\n"
                                + "do {\n"
                                + "    hello.toLowerCase(); // should fail here\n"
                                + "} while (i++ < 10);",
                        new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );
        assertCompilationErrorContains( writer, "error: Runner.java:5 Re-using @Linear variable hello" );
    }

    @Test
    public void cannotUseLinearVariableInTernaryOperatorAssignmentAfterUsedUp() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear int x = 10;\n"
                                + "int y = x > 5 ? 0 : 20; // used up x\n"
                                + "int z = y > 1 ? x : -1; // should fail\n"
                                + "System.out.println(z);",
                        new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );
        assertCompilationErrorContains( writer, "error: Runner.java:4 Re-using @Linear variable x" );
    }

    @Test
    public void cannotUseLinearVariableWithinFallThroughSwitchBranches() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String x = \"\"; @Linear int n = 1;\n"
                                + "switch (n) {\n"
                                + "  case 1: x.toString(); break;\n"
                                + "  case 2: x.toUpperCase(); // fall-through\n"
                                + "  case 3: x.toLowerCase(); break;\n"
                                + "  default: x.hashCode();\n"
                                + "}",
                        new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );
        assertCompilationErrorContains( writer, "error: Runner.java:6 Re-using @Linear variable x" );
    }

    @Test
    public void cannotUseAliasToUsedLinearVariable() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear int x = 10;\n"
                                + "@Linear int y = 20;\n"
                                + "x = y; // alias\n"
                                + "System.out.println(y); // used up x and y\n"
                                + "System.out.println(x); // fail here",
                        new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );
        assertCompilationErrorContains( writer, "error: Runner.java:6 Re-using @Linear variable y (aliased as x)" );
    }

    @Test
    public void cannotUseAliasedAndUsedLinearVariable() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear int x = 10;\n"
                                + "@Linear int y = 20;\n"
                                + "x = y; // alias\n"
                                + "System.out.println(y); // used up x and y\n"
                                + "System.out.println(y); // fail here",
                        new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );
        assertCompilationErrorContains( writer, "error: Runner.java:6 Re-using @Linear variable y" );
    }

    @Test
    public void cannotReuseAliasedLinearVariable() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass = compileRunnableClassSnippet(
                "@Linear int a = 1;\n"
                        + "int b = a;\n"
                        + "int c = a;\n"
                        + "int d = a;\n"
                        + "int e = b;\n"
                        + "int f = c;\n"
                        + "int g = f;\n"
                        + "System.out.println(g); // uses up all aliases \n"
                        + "System.out.println(f); // fail here",
                new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );
        assertCompilationErrorContains( writer, "error: Runner.java:10 Re-using @Linear variable a (aliased as f)" );
    }

    @Test
    public void cannotAssignNonLinearToLinearVariable() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "int x = 10;\n"
                                + "@Linear int y = x;\n",
                        new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );
        assertCompilationErrorContains( writer, "error: Runner.java:3 Cannot assign non-linear variable x to linear variable y" );
    }

    @Test
    public void cannotAssignNonLinearMethodReturnToLinearVariable() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass = compileRunnableClassSnippet(
                "@Linear String s = \"abc\";\n"
                        + "@Linear String t = s.toUpperCase();",
                new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );
        assertCompilationErrorContains( writer, "error: Runner.java:3 Cannot assign non-linear return type of " +
                "s.toUpperCase() to linear variable t" );

        compiledClass = compileRunnableClassSnippet(
                "@Linear String s = \"abc\";\n"
                        + "@Linear String t = s.toUpperCase().toLowerCase().toString();",
                new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );
        assertCompilationErrorContains( writer, "error: Runner.java:3 Cannot assign non-linear return type of " +
                "s.toUpperCase().toLowerCase().toString() to linear variable t" );
    }

    @Test
    public void cannotAssignArrayMethodReturnToLinearVariable() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass = compileRunnableClassSnippet(
                "@Linear String s = \"abc\";\n"
                        + "@Linear char[] c = s.toCharArray();",
                new PrintStream( writer, true ) );

        assertFalse( "Should not compile successfully", compiledClass.isPresent() );
        assertCompilationErrorContains( writer, "error: Runner.java:3 Cannot assign non-linear return type of " +
                "s.toCharArray() to linear variable c" );
    }
}
