package com.typecheckit;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.comp.CompileStates;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.typecheckit.linear.LinearTypeChecker;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

@SupportedSourceVersion( SourceVersion.RELEASE_8 )
@SupportedAnnotationTypes( "*" )
public class TypeCheckitProcessor extends AbstractProcessor {

    @Override
    public synchronized void init( ProcessingEnvironment env ) {
        super.init( env );

        Iterator<TypeChecker> loader = ServiceLoader.load( TypeChecker.class ).iterator();
        List<TypeChecker> typeCheckers = new ArrayList<>();

        // TODO make this a service
        typeCheckers.add( new LinearTypeChecker() );

        if ( loader.hasNext() ) {
            while ( loader.hasNext() ) {
                TypeChecker typeChecker = loader.next();
                typeCheckers.add( typeChecker );
            }
        } else {
            // no type checkers installed!
            //return;
        }

        final TypeCheckitTaskListener listener = new TypeCheckitTaskListener( ( JavacProcessingEnvironment ) env, typeCheckers );

        System.out.println( "Processor options: " + env.getOptions() );

        JavacTask.instance( env ).addTaskListener( listener );

        Context ctx = ( ( JavacProcessingEnvironment ) processingEnv ).getContext();
        JavaCompiler compiler = JavaCompiler.instance( ctx );
        compiler.shouldStopPolicyIfNoError =
                CompileStates.CompileState.max( compiler.shouldStopPolicyIfNoError, CompileStates.CompileState.FLOW );
        compiler.shouldStopPolicyIfError =
                CompileStates.CompileState.max( compiler.shouldStopPolicyIfError, CompileStates.CompileState.FLOW );
    }

    @Override
    public boolean process( Set<? extends TypeElement> annotations,
                            RoundEnvironment roundEnv ) {
        return false;
    }

}
