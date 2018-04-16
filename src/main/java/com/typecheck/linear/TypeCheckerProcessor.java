package com.typecheck.linear;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.comp.CompileStates;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
//@SupportedOptions("printErrorStack")
@SupportedAnnotationTypes("com.typecheck.linear.annotation.Linear")
public class TypeCheckerProcessor extends AbstractProcessor {

    private final AtomicReference<LinearTaskListener> taskListenerRef = new AtomicReference<>();

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        Iterator<TypeChecker> loader = ServiceLoader.load(TypeChecker.class).iterator();
        List<TypeChecker> typeCheckers = new ArrayList<>();

        // TODO make this a service
        typeCheckers.add(new LinearTypeChecker());

        if (loader.hasNext()) {
            while (loader.hasNext()) {
                TypeChecker typeChecker = loader.next();
                typeCheckers.add(typeChecker);
            }
        } else {
            // no type checkers installed!
            //return;
        }

        final LinearTaskListener listener = new LinearTaskListener((JavacProcessingEnvironment) env, typeCheckers);
        taskListenerRef.set(listener);

        System.out.println("Processor options: " + env.getOptions());

        JavacTask.instance(env).addTaskListener(listener);

        Context ctx = ((JavacProcessingEnvironment) processingEnv).getContext();
        JavaCompiler compiler = JavaCompiler.instance(ctx);
        compiler.shouldStopPolicyIfNoError =
                CompileStates.CompileState.max(compiler.shouldStopPolicyIfNoError, CompileStates.CompileState.FLOW);
        compiler.shouldStopPolicyIfError =
                CompileStates.CompileState.max(compiler.shouldStopPolicyIfError, CompileStates.CompileState.FLOW);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        return false;
    }


}
