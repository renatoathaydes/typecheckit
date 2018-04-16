package com.typecheck.linear;

import com.athaydes.osgiaas.javac.internal.DefaultClassLoaderContext;
import com.athaydes.osgiaas.javac.internal.compiler.OsgiaasJavaCompiler;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class TypeCheckerProcessorTest {

    private OsgiaasJavaCompiler compiler =
            new OsgiaasJavaCompiler(
                    DefaultClassLoaderContext.INSTANCE,
                    asList(
                            "-processor",
                            "com.typecheck.linear.TypeCheckerProcessor",
                            "-AprintErrorStack"));

    @Test
    public void canAssignLiteralToLinearVariable()
            throws IllegalAccessException, InstantiationException {
        Optional<Class<Object>> runner =
                compiler.compile(
                        "Runner",
                        "import com.typecheck.linear.annotation.Linear;\n"
                                + "public class Runner implements Runnable {\n"
                                + "  public void run() {\n"
                                + "    @Linear String s = \"hello @Linear\";\n"
                                + "    System.out.println(s);\n"
                                + "  }\n"
                                + "}\n",
                        System.out);

        runner.orElseThrow(() -> new RuntimeException("Error compiling"))
                .asSubclass(Runnable.class)
                .newInstance()
                .run();
    }

    @Test
    public void canAssignNewObjectToLinearVariable()
            throws IllegalAccessException, InstantiationException {
        Optional<Class<Object>> runner =
                compiler.compile(
                        "Runner",
                        "import com.typecheck.linear.annotation.Linear;\n"
                                + "public class Runner implements Runnable {\n"
                                + "  public void run() {\n"
                                + "    @Linear Object s = new Object();\n"
                                + "    System.out.println(s.toString());\n"
                                + "  }\n"
                                + "}\n",
                        System.out);

        runner.orElseThrow(() -> new RuntimeException("Error compiling"))
                .asSubclass(Runnable.class)
                .newInstance()
                .run();
    }

    @Test
    public void cannotUseLinearVariableAfterUsedUp() {
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        Optional<Class<Object>> compiledClass =
                compiler.compile(
                        "Runner",
                        "import com.typecheck.linear.annotation.Linear;\n"
                                + "public class Runner implements Runnable {\n"
                                + "  public void run() {\n"
                                + "    @Linear String s = \"hello @Linear\";\n"
                                + "    String t = s.toUpperCase(); // used up\n"
                                + "    System.out.println(s.toLowerCase()); // should fail here\n"
                                + "    System.out.println(t);\n"
                                + "  }\n"
                                + "}\n",
                        new PrintStream(writer, true));

        assertFalse("Should not compile successfully", compiledClass.isPresent());
        assertThat(writer.toString(), containsString("Runner.java:7: error: [use.unsafe]"));
    }
}
