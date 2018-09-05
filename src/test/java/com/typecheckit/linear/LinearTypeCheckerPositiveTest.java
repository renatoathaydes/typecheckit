package com.typecheckit.linear;

import com.typecheckit.TestUtils;
import org.junit.Test;

import java.util.Optional;

public class LinearTypeCheckerPositiveTest extends TestUtils {

    @Test
    public void canAssignLiteralToLinearVariable() {
        Optional<Class<Object>> compiledClass = compileRunnableClassSnippet(
                "@Linear String s = \"hello @Linear\";\n" +
                        "System.out.println(s);" );

        assertSuccessfulCompilationOfClass( compiledClass );
    }

    @Test
    public void canAssignNewObjectToLinearVariable() {
        Optional<Class<Object>> runner = compileRunnableClassSnippet(
                "@Linear Object s = new Object();\n"
                        + "System.out.println(s.toString());",
                "pkg", "Runner2", System.out );

        assertSuccessfulCompilationOfClass( runner, "pkg.Runner2", Runnable.class );
    }

    @Test
    public void canUseLinearVariableWithinSimpleIfBranch() {
        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String hello = \"hello @Linear\";\n"
                                + "if (System.currentTimeMillis() > 20000000L) {\n"
                                + "    hello.toLowerCase();\n"
                                + "}" );

        assertSuccessfulCompilationOfClass( compiledClass );
    }

    @Test
    public void canUseLinearVariableWithinDisjointIfBranches() {
        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String hello = \"hello @Linear\";\n"
                                + "if (System.currentTimeMillis() > 20000000L) {\n"
                                + "    hello.toLowerCase();\n"
                                + "} else {\n"
                                + "    hello.toUpperCase();\n"
                                + "}" );

        assertSuccessfulCompilationOfClass( compiledClass );
    }

    @Test
    public void canUseLinearVariableWithinManyDisjointIfBranches() {
        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String hello = \"hello @Linear\";\n"
                                + "if (System.currentTimeMillis() > 20000000L) {\n"
                                + "    hello.toLowerCase();\n"
                                + "} else if (System.currentTimeMillis() > 20000L) {\n"
                                + "    hello.length();\n"
                                + "} else if (System.currentTimeMillis() > 20L) {\n"
                                + "    hello.toUpperCase();\n"
                                + "} else {\n"
                                + "    hello.toString();\n"
                                + "}" );

        assertSuccessfulCompilationOfClass( compiledClass );
    }

    @Test
    public void canUseLinearVariableWithinDisjointIfBranchesWithoutElse() {
        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String hello = \"hello @Linear\";\n"
                                + "if (System.currentTimeMillis() > 20000000L) {\n"
                                + "    hello.toLowerCase();\n"
                                + "} else if (System.currentTimeMillis() > 20000L) {\n"
                                + "    hello.length();\n"
                                + "} else if (System.currentTimeMillis() > 20L) {\n"
                                + "    hello.toUpperCase();\n"
                                + "}" );

        assertSuccessfulCompilationOfClass( compiledClass );
    }

    @Test
    public void canUseLinearVariableWithinDisjointNestedIfBranches() {
        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String hello = \"hello @Linear\";\n"
                                + "long t = System.currentTimeMillis();\n"
                                + "if (t > 0)\n"
                                + "  if (10 < t && t < 20) hello.toUpperCase();\n"
                                + "  else hello.toLowerCase();\n"
                                + "else if (t < -10L)\n"
                                + "  if (t < -20L) hello.length();\n"
                                + "  else if (t < -30L) hello.length();\n"
                                + "  else if (t < -100L)\n"
                                + "    if (t < 200L) hello.toUpperCase();\n"
                                + "" );

        assertSuccessfulCompilationOfClass( compiledClass );
    }

    @Test
    public void canUseLinearVariableWithinIfTryBranches() {
        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String hello = \"hello @Linear\";\n"
                                + "if (System.currentTimeMillis() > 20000000L) try {\n"
                                + "  hello.toUpperCase();\n"
                                + "} catch (RuntimeException e) {\n"
                                + "  e.printStackTrace();\n"
                                + "} else {\n"
                                + "  hello.toLowerCase();\n"
                                + "}" );

        assertSuccessfulCompilationOfClass( compiledClass );
    }

}
