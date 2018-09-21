package com.typecheckit.util;

import com.sun.source.tree.ImportTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.tools.javac.code.Type;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

public class AnnotationDetector {

    private final String annotationQualifiedName;
    private final String annotationSimpleName;
    private final String starImport;
    private final Set<String> annotationReferenceNames = new HashSet<>( 2 );

    public AnnotationDetector( Class<? extends Annotation> annotationType ) {
        annotationQualifiedName = annotationType.getName();
        starImport = annotationType.getPackage().getName() + ".*";

        annotationSimpleName = annotationQualifiedName.contains( "." ) ?
                annotationQualifiedName.substring( annotationQualifiedName.lastIndexOf( '.' ) + 1 ) : null;

        annotationReferenceNames.add( annotationQualifiedName );
    }

    public void addImport( ImportTree node ) {
        if ( annotationSimpleName == null ) {
            return;
        }

        String importId = node.getQualifiedIdentifier().toString();

        if ( importId.equals( annotationQualifiedName ) ||
                importId.equals( starImport ) ) {
            annotationReferenceNames.add( annotationSimpleName );
        }
    }

    public boolean isAnnotated( ModifiersTree modifiers, TypeCheckerUtils typeCheckerUtils ) {
        return typeCheckerUtils.annotationNames( modifiers ).stream()
                .anyMatch( annotationReferenceNames::contains );
    }

    public boolean isAnnotated( Type type ) {
        return type.getAnnotationMirrors().stream()
                .anyMatch( it -> it.getAnnotationType().toString().equals( annotationQualifiedName ) );
    }

}
