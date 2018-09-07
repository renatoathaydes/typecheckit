package com.typecheckit.linear;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.typecheckit.ScopeBasedTypeChecker;
import com.typecheckit.annotation.Linear;
import com.typecheckit.util.TypeCheckerUtils;
import com.typecheckit.util.VariableScope.Scope;

import javax.lang.model.element.Name;
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
        boolean scanInitializer = true;
        ExpressionTree initializer = node.getInitializer();
        if ( initializer instanceof IdentifierTree ) {
            copyMarkToAlias( node.getName(), ( IdentifierTree ) initializer );
            scanInitializer = false; // no need to visit the initializer, already done what was needed
        } else if ( isLinear( node, typeCheckerUtils ) ) {
            currentScope().getVariables().put( node.getName().toString(), new LinearMark( node ) );
        }

        scan( node.getModifiers(), typeCheckerUtils );
        scan( node.getType(), typeCheckerUtils );
        scan( node.getNameExpression(), typeCheckerUtils );
        if ( scanInitializer ) {
            scan( initializer, typeCheckerUtils );
        }
        return null;
    }

    @Override
    public Void visitAssignment( AssignmentTree node, TypeCheckerUtils typeCheckerUtils ) {
        ExpressionTree variable = node.getVariable();
        ExpressionTree expression = node.getExpression();
        if ( variable instanceof IdentifierTree ) {
            // variable does not need to be scanned as we now know it's not being used, but assigned to
            IdentifierTree idVar = ( IdentifierTree ) variable;
            if ( expression instanceof IdentifierTree ) {
                IdentifierTree idExpr = ( IdentifierTree ) expression;
                copyMarkToAlias( idVar.getName(), idExpr );
            } else {
                scan( expression, typeCheckerUtils );
            }
        } else {
            scan( variable, typeCheckerUtils );
            scan( expression, typeCheckerUtils );
        }
        return null;
    }

    @Override
    public Void visitIdentifier( IdentifierTree node, TypeCheckerUtils typeCheckerUtils ) {
        String nodeName = node.getName().toString();
        Scope<LinearMark> scope = currentScope();
        LinearMark mark = scope.getVariables().get( nodeName );

        if ( mark != null ) {
            if ( scope.getBlockKind().isLoop() ) {
                // @Linear variable cannot be safely used in loops
                mark.markAsUsed();
            }
            if ( mark.isUsedUp() ) {
                CompilationUnitTree cu = typeCheckerUtils.getCompilationUnit();
                long lineNumber = ( node instanceof DiagnosticPosition )
                        ? cu.getLineMap().getLineNumber( ( ( DiagnosticPosition ) node ).getStartPosition() )
                        : -1;
                String fileName = cu.getSourceFile().getName();
                System.out.println( "ERROR at " + node.getName() + ":" + lineNumber );
                typeCheckerUtils.getMessager().printMessage( ERROR,
                        fileName + ":" + lineNumber + " " + errorMessage( mark, node ) );
            } else {
                mark.markAsUsed();
            }
        }
        return super.visitIdentifier( node, typeCheckerUtils );
    }

    private void copyMarkToAlias( Name variable, IdentifierTree expression ) {
        LinearMark mark = currentScope().getVariables().get( expression.getName().toString() );
        if ( mark != null ) {
            currentScope().getVariables().put( variable.toString(), mark );
        }
    }

    private static String errorMessage( LinearMark mark, IdentifierTree node ) {
        String aliasInfo = "";
        if ( !mark.name().equals( node.getName().toString() ) ) {
            aliasInfo = " (aliased as " + node.getName() + ")";
        }

        return "Re-using @Linear variable " + mark.name() + aliasInfo;
    }

}
