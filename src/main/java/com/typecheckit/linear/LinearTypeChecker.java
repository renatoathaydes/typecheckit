package com.typecheckit.linear;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree;
import com.typecheckit.ScopeBasedTypeChecker;
import com.typecheckit.annotation.Linear;
import com.typecheckit.util.TypeCheckerUtils;
import com.typecheckit.util.VariableScope.Scope;

import java.util.HashSet;
import java.util.Set;

import static javax.tools.Diagnostic.Kind.ERROR;

public final class LinearTypeChecker extends ScopeBasedTypeChecker<LinearMark> {

    private static final String LINEAR_CLASS_NAME = Linear.class.getName();
    private static final String LINEAR_PKG_STAR = Linear.class.getPackage().getName() + ".*";

    private final Set<String> linearAnnotationNames = new HashSet<>( 2 );

    public LinearTypeChecker() {
        linearAnnotationNames.add( LINEAR_CLASS_NAME );
    }

    private boolean isLinear( VariableTree node, TypeCheckerUtils typeCheckerUtils ) {
        return typeCheckerUtils.annotationNames( node ).stream()
                .anyMatch( linearAnnotationNames::contains );
    }

    @Override
    public Void visitImport( ImportTree node, TypeCheckerUtils typeCheckerUtils ) {
        String importId = node.getQualifiedIdentifier().toString();
        boolean linearTypeImported = importId.equals( LINEAR_CLASS_NAME ) ||
                importId.equals( LINEAR_PKG_STAR );
        if ( linearTypeImported ) {
            linearAnnotationNames.add( "Linear" );
        }
        return super.visitImport( node, typeCheckerUtils );
    }

    @Override
    public Void visitVariable( VariableTree node, TypeCheckerUtils typeCheckerUtils ) {
        if ( isLinear( node, typeCheckerUtils ) ) {
            currentScope().getVariables().put( node.getName().toString(), new LinearMark() );
        }
        return super.visitVariable( node, typeCheckerUtils );
    }

    @Override
    public Void visitIdentifier( IdentifierTree node, TypeCheckerUtils typeCheckerUtils ) {
        String nodeName = node.getName().toString();
        Scope<LinearMark> scope = currentScope();
        LinearMark mark = scope.getVariables().get( nodeName );

        if ( mark != null ) {
            System.out.println( "Identifier being visited in block " + scope.getBlockKind() );
            if ( scope.getBlockKind().isLoop() ) {
                // @Linear variable cannot be safely used in loops
                mark.isUsedUp = true;
            }
            if ( mark.isUsedUp ) {
                CompilationUnitTree cu = typeCheckerUtils.getCompilationUnit();
                long lineNumber = cu.getLineMap().getLineNumber(
                        ( ( JCTree.JCIdent ) node ).getStartPosition() );
                String fileName = cu.getSourceFile().getName();
                System.out.println( "ERROR at " + node.getName() + ":" + lineNumber );
                typeCheckerUtils.getMessager().printMessage( ERROR,
                        fileName + ":" + lineNumber + " Re-using @Linear variable " + node.getName() );
            } else {
                mark.isUsedUp = true;
            }
        }
        return super.visitIdentifier( node, typeCheckerUtils );
    }

}
