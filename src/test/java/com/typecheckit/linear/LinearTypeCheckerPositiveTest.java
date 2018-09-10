package com.typecheckit.linear;

import com.typecheckit.TestUtils;
import com.typecheckit.annotation.Linear;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

public class LinearTypeCheckerPositiveTest extends TestUtils {

    @Before
    public void setup() {
        addImports( Linear.class.getName() );
    }

    @Test
    public void canAssignLiteralToLinearVariable() {
        Optional<Class<Object>> compiledClass = compileRunnableClassSnippet(
                "@Linear String s = \"hello @Linear\";\n"
                        + "System.out.println(s.toString());" );

        assertSuccessfulCompilationOfRunnableClass( compiledClass );
    }

    @Test
    public void canAssignLinearVariableAfterDeclaration() {
        Optional<Class<Object>> compiledClass = compileRunnableClassSnippet(
                "@Linear int x;\n"
                        + "x = 10;\n"
                        + "System.out.println(x);" );

        assertSuccessfulCompilationOfRunnableClass( compiledClass );
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

        assertSuccessfulCompilationOfRunnableClass( compiledClass );
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

        assertSuccessfulCompilationOfRunnableClass( compiledClass );
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

        assertSuccessfulCompilationOfRunnableClass( compiledClass );
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

        assertSuccessfulCompilationOfRunnableClass( compiledClass );
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

        assertSuccessfulCompilationOfRunnableClass( compiledClass );
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

        assertSuccessfulCompilationOfRunnableClass( compiledClass );
    }

    @Test
    public void canAssignLinearVariableWithinDisjointIfBranches() {
        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear int x;\n"
                                + "if (System.currentTimeMillis() > 20000000L) {\n"
                                + "    x = 10;\n"
                                + "} else if (System.currentTimeMillis() > 20000L) {\n"
                                + "    x = 20;\n"
                                + "} else {\n"
                                + "    x = 30;\n"
                                + "}\n"
                                + "System.out.println(x);" );

        assertSuccessfulCompilationOfRunnableClass( compiledClass );
    }

    @Test
    public void canAssignLinearVariableWithinDisjointTernaryOperatorBranches() {
        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear int x =\n"
                                + "(System.currentTimeMillis() > 20000000L)\n"
                                + "?   10\n"
                                + ": (System.currentTimeMillis() > 20000L)\n"
                                + "?   20\n"
                                + ":   30;\n"
                                + "System.out.println(x);" );

        assertSuccessfulCompilationOfRunnableClass( compiledClass );
    }

    @Test
    public void canUseLinearVariableWithinMutuallyExclusiveSwitchBranches() {
        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String x = \"\"; @Linear int n = 1;\n"
                                + "switch (n) {\n"
                                + "  case 1: x.toString(); break;\n"
                                + "  case 2: x.toUpperCase(); break;\n"
                                + "  case 3: x.toLowerCase(); break;\n"
                                + "  default: x.hashCode();\n"
                                + "}" );

        assertSuccessfulCompilationOfRunnableClass( compiledClass );
    }

    @Test
    public void canReassignLinearVariableToAnotherVariable() {
        Optional<Class<Object>> compiledClass =
                compileRunnableClassSnippet(
                        "@Linear String x = \"a\";\n"
                                + "@Linear String y = \"b\";\n"
                                + "x = y; // creates an alias\n"
                                + "System.out.println(x.toUpperCase()); // uses up both x and y \n" );

        assertSuccessfulCompilationOfRunnableClass( compiledClass );

        compiledClass = compileRunnableClassSnippet(
                "@Linear String x = \"a\";\n"
                        + "@Linear String y = \"b\";\n"
                        + "x = y; // creates an alias\n"
                        + "System.out.println(y.toUpperCase()); // uses up both x and y \n" );

        assertSuccessfulCompilationOfRunnableClass( compiledClass );

        compiledClass = compileRunnableClassSnippet(
                "@Linear int a = 1;\n"
                        + "@Linear int b = a;\n"
                        + "@Linear int c = a;\n"
                        + "@Linear int d = a;\n"
                        + "@Linear int e = b;\n"
                        + "@Linear int f = c;\n"
                        + "@Linear int g = f;\n"
                        + "@Linear int notAlias = 2;\n"
                        + "System.out.println(g); // uses up all aliases \n"
                        + "System.out.println(notAlias);" );

        assertSuccessfulCompilationOfRunnableClass( compiledClass );
    }

    @Test
    public void canAssignLinearMethodReturnToLinearVariable() {
        Optional<Class<Object>> compiledClass = compileClass(
                "@Linear String getSomething() {\n"
                        + "  return new String(\"hello\");\n"
                        + "}\n"
                        + "void run() {\n"
                        + "  @Linear String s = getSomething();\n"
                        + "  System.out.println(s.toString());"
                        + "}\n" );

        assertSuccessfulCompilationOfClass( compiledClass );
    }

    @Test
    public void canAssignAnyPrimitiveMethodReturnTypeToLinearVariable() {
        Optional<Class<Object>> compiledClass = compileRunnableClassSnippet(
                "String s = \"hello\";\n"
                        + "@Linear int h = s.hashCode();\n"
                        + "@Linear int i = s.length();\n"
                        + "@Linear boolean b = s.isEmpty();\n"
                        + "System.out.println(\"\" + b + i + h);" );

        assertSuccessfulCompilationOfRunnableClass( compiledClass );
    }

    @Test
    public void canPassLinearVariableToStaticMethodTakingLinearVariable() {
        Optional<Class<Object>> compiledClass = compileClass(
                "\nstatic class Helper {\n"
                        + "  static void hello(String name, @Linear String surname) {\n"
                        + "   System.out.println(name + \" \" + surname);\n"
                        + "  }\n"
                        + "}\n"
                        + "void run() {\n"
                        + "  String name = \"Joe\";\n"
                        + "  @Linear String sur = \"Doe\";\n"
                        + "  Helper.hello(name, sur);\n"
                        + "}" );

        assertSuccessfulCompilationOfClass( compiledClass );
    }

    @Test
    public void canPassLinearVariableToStaticMethodTakingPrimitiveVariable() {
        Optional<Class<Object>> compiledClass = compileClass(
                "\nstatic class Helper {\n"
                        + "  static void hello(String name, int i) {\n"
                        + "   System.out.println(name + \" \" + i);\n"
                        + "  }\n"
                        + "}\n"
                        + "void run() {\n"
                        + "  String name = \"Joe\";\n"
                        + "  @Linear int i = 2;\n"
                        + "  Helper.hello(name, i);\n"
                        + "}" );

        assertSuccessfulCompilationOfClass( compiledClass );
    }

    @Test
    public void canPassLinearVariableToInstanceMethodTakingLinearVariable() {
        Optional<Class<Object>> compiledClass = compileClass(
                "\nvoid hello(String name, @Linear String surname) {\n"
                        + "  System.out.println(name + \" \" + surname);\n"
                        + "}\n"
                        + "void run() {\n"
                        + "  String name = \"Joe\";\n"
                        + "  @Linear String sur = \"Doe\";\n"
                        + "  hello(name, sur);\n"
                        + "}" );

        assertSuccessfulCompilationOfClass( compiledClass );
    }

    @Test
    public void canPassLinearVariableToInstanceMethodTakingPrimitiveVariable() {
        Optional<Class<Object>> compiledClass = compileClass(
                "\nvoid hello(String name, int i) {\n"
                        + "  System.out.println(name + \" \" + i);\n"
                        + "}\n"
                        + "void run() {\n"
                        + "  String name = \"Joe\";\n"
                        + "  @Linear int i = 1;\n"
                        + "  hello(name, i);\n"
                        + "}" );

        assertSuccessfulCompilationOfClass( compiledClass );
    }

    @Test
    public void methodCanReturnLinearVariable() {
        Optional<Class<Object>> compiledClass = compileClass(
                "\n@Linear String hello(@Linear String name) {\n"
                        + "  return \"Hello \" + name;\n"
                        + "}\n"
                        + "void run() {\n"
                        + "  @Linear String msg = hello(\"Joe\");\n"
                        + "  assert msg.equals(\"Hello Joe\");\n"
                        + "}" );

        assertSuccessfulCompilationOfClass( compiledClass );
    }

    @Test
    public void methodCanReturnNewLinearVariable() {
        Optional<Class<Object>> compiledClass = compileClass(
                "\n@Linear String hello(@Linear String name) {\n"
                        + "  return new String(\"Hello \" + name);\n"
                        + "}\n"
                        + "void run() {\n"
                        + "  @Linear String msg = hello(\"Joe\");\n"
                        + "  assert msg.equals(\"Hello Joe\");\n"
                        + "}" );

        assertSuccessfulCompilationOfClass( compiledClass );
    }

    @Test
    public void methodCanReturnPrimitiveLinearVariable() {
        Optional<Class<Object>> compiledClass = compileClass(
                "\n@Linear int two() {\n"
                        + "  return 2;\n"
                        + "}\n"
                        + "void run() {\n"
                        + "  @Linear int twoVar = two();\n"
                        + "  assert twoVar == 2;\n"
                        + "}" );

        assertSuccessfulCompilationOfClass( compiledClass );

        // method does not need to be linear for primitive variable to be assignable to return value
        compiledClass = compileClass(
                "\nint two() {\n"
                        + "  return 2;\n"
                        + "}\n"
                        + "void run() {\n"
                        + "  @Linear int twoVar = two();\n"
                        + "  assert twoVar == 2;\n"
                        + "}" );

        assertSuccessfulCompilationOfClass( compiledClass );
    }

    @Test
    public void methodCanReturnPrimitiveExpressionLinearVariable() {
        Optional<Class<Object>> compiledClass = compileClass(
                "\n@Linear int two() {\n"
                        + "  return 1 + 1 - 1 + 1;\n"
                        + "}\n"
                        + "void run() {\n"
                        + "  @Linear int twoVar = two();\n"
                        + "  assert twoVar == 2;\n"
                        + "}" );

        assertSuccessfulCompilationOfClass( compiledClass );
    }

    @Test
    public void methodCanReturnNewLinearVariableFromDifferentBranches() {
        Optional<Class<Object>> compiledClass = compileClass(
                "\n@Linear String hello(boolean a, boolean b) {\n"
                        + "  if (a && b) return \"both\";\n"
                        + "  if (a) return \"only a\";\n"
                        + "  else if (b) return \"only b\";\n"
                        + "  else return \"none\";\n"
                        + "}\n"
                        + "void run() {\n"
                        + "  @Linear String msg = hello(true, false);\n"
                        + "  assert msg.equals(\"only a\");\n"
                        + "}" );

        assertSuccessfulCompilationOfClass( compiledClass );
    }

}
