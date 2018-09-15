package com.typecheckit.linear;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;
import com.typecheckit.ScopeBasedTypeChecker;
import com.typecheckit.annotation.Linear;
import com.typecheckit.util.ScopeStack.Scope;
import com.typecheckit.util.TypeCheckerUtils;

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

    private boolean isLinear( IdentifierTree identifierTree ) {
        return currentScope().getVariables().containsKey( identifierTree.getName().toString() );
    }

    private boolean isLinear( ModifiersTree modifiers, TypeCheckerUtils typeCheckerUtils ) {
        return typeCheckerUtils.annotationNames( modifiers ).stream()
                .anyMatch( linearAnnotationNames::contains );
    }

    private boolean isLinear( Type type ) {
        return type.getKind().isPrimitive() || type.getAnnotationMirrors().stream()
                .anyMatch( it -> it.getAnnotationType().toString().equals( LINEAR_CLASS_NAME ) );
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
        if ( initializer != null ) {
            if ( initializer.getKind() == Tree.Kind.IDENTIFIER ) {
                IdentifierTree idInit = ( IdentifierTree ) initializer;
                boolean copied = copyMarkToAlias( node.getName(), idInit );
                if ( !copied ) { // then the initializer did not have a mark
                    if ( isLinear( node.getModifiers(), typeCheckerUtils ) ) { // then the assignment cannot be allowed
                        reportError( typeCheckerUtils, idInit, assignmentError( node, idInit ) );
                    }
                }
                scanInitializer = false; // no need to visit the initializer, already done what was needed
            } else if ( isLinear( node.getModifiers(), typeCheckerUtils ) ) {
                if ( initializer.getKind() == Tree.Kind.METHOD_INVOCATION ) {
                    boolean isLinearReturnType = hasLinearReturnType( typeCheckerUtils, ( MethodInvocationTree ) initializer );
                    if ( !isLinearReturnType ) {
                        reportError( typeCheckerUtils, node, assignmentError( node, ( MethodInvocationTree ) initializer ) );
                    }
                }
                currentScope().getVariables().put( node.getName().toString(), new LinearMark( node ) );
            }
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
        if ( variable.getKind() == Tree.Kind.IDENTIFIER ) {
            // variable does not need to be scanned as we now know it's not being used, but assigned to
            IdentifierTree idVar = ( IdentifierTree ) variable;
            if ( expression.getKind() == Tree.Kind.IDENTIFIER ) {
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
            if ( getScopes().isWithinLoop() ) {
                // @Linear variable cannot be safely used in loops
                mark.markAsUsed();
            }
            if ( mark.isUsedUp() ) {
                reportError( typeCheckerUtils, node, reusingError( mark, node ) );
            } else {
                mark.markAsUsed();
            }
        }
        return super.visitIdentifier( node, typeCheckerUtils );
    }

    @Override
    public Void visitMethodInvocation( MethodInvocationTree node, TypeCheckerUtils typeCheckerUtils ) {
        List<Type> parameterTypes = typeCheckerUtils.getMethodParameters( node );
        java.util.List<? extends ExpressionTree> arguments = node.getArguments();
        for ( int i = 0, max = Math.min( parameterTypes.size(), arguments.size() ); i < max; i++ ) {
            ExpressionTree arg = arguments.get( i );
            if ( arg.getKind() == Tree.Kind.IDENTIFIER && isLinear( ( IdentifierTree ) arg ) ) {
                // the argument is a @Linear variable, check if the method accepts @Linear variables
                if ( !isLinear( parameterTypes.get( i ) ) ) {
                    reportError( typeCheckerUtils, node, methodCallError( node, arg, i ) );
                }
            }
        }
        return super.visitMethodInvocation( node, typeCheckerUtils );
    }

    @Override
    public Void visitReturn( ReturnTree node, TypeCheckerUtils typeCheckerUtils ) {
        Scope<LinearMark> scope = currentScope();

        scope.getMethodTree().ifPresent( methodTree -> {
            if ( isLinear( methodTree.getModifiers(), typeCheckerUtils ) ) {
                ExpressionTree expression = node.getExpression();
                verifyExpressionIsLinear( methodTree, typeCheckerUtils, expression );
            }
        } );

        return super.visitReturn( node, typeCheckerUtils );
    }

    private void verifyExpressionIsLinear( MethodTree methodTree,
                                           TypeCheckerUtils typeCheckerUtils,
                                           ExpressionTree expression ) {
        Tree erroneousTree = null;
        Tree.Kind kind = expression.getKind();

        if ( kind == Tree.Kind.ASSIGNMENT ) {
            verifyExpressionIsLinear( methodTree, typeCheckerUtils, ( ( AssignmentTree ) expression ).getExpression() );
        } else if ( kind.asInterface().equals( CompoundAssignmentTree.class ) ) {
            verifyExpressionIsLinear( methodTree, typeCheckerUtils, ( ( CompoundAssignmentTree ) expression ).getExpression() );
        } else if ( kind.asInterface().equals( ConditionalExpressionTree.class ) ) {
            verifyExpressionIsLinear( methodTree, typeCheckerUtils, ( ( ConditionalExpressionTree ) expression ).getTrueExpression() );
            verifyExpressionIsLinear( methodTree, typeCheckerUtils, ( ( ConditionalExpressionTree ) expression ).getFalseExpression() );
        } else if ( kind == Tree.Kind.TYPE_CAST ) {
            verifyExpressionIsLinear( methodTree, typeCheckerUtils, ( ( TypeCastTree ) expression ).getExpression() );
        } else if ( kind == Tree.Kind.PARENTHESIZED ) {
            verifyExpressionIsLinear( methodTree, typeCheckerUtils, ( ( ParenthesizedTree ) expression ).getExpression() );
        } else if ( kind == Tree.Kind.METHOD_INVOCATION ) {
            if ( !hasLinearReturnType( typeCheckerUtils, ( MethodInvocationTree ) expression ) ) {
                erroneousTree = expression;
            }
        } else if ( kind == Tree.Kind.IDENTIFIER ) {
            if ( !isLinear( ( IdentifierTree ) expression ) ) {
                erroneousTree = expression;
            }
        } else if ( kind == Tree.Kind.ARRAY_ACCESS
                || kind == Tree.Kind.MEMBER_REFERENCE
                || kind == Tree.Kind.MEMBER_SELECT ) {
            // none of these types of expression can be linear
            erroneousTree = expression;
        }

        if ( erroneousTree != null ) {
            reportError( typeCheckerUtils, erroneousTree,
                    returnValueError( methodTree, erroneousTree ) );
        }
    }

    @SuppressWarnings( "ConstantConditions" )
    private boolean hasLinearReturnType( TypeCheckerUtils typeCheckerUtils, MethodInvocationTree initializer ) {
        // do not report error if we simply can't find the element because
        // that probably means there's some other error in the source code already
        boolean defaultReturnValue = true;

        return typeCheckerUtils.getTreeElement( initializer )
                .map( element -> isLinear( ( ( Symbol.MethodSymbol ) element ).getReturnType() ) )
                .orElse( defaultReturnValue );
    }

    private boolean copyMarkToAlias( Name variable, IdentifierTree expression ) {
        LinearMark mark = currentScope().getVariables().get( expression.getName().toString() );
        if ( mark != null ) {
            currentScope().getVariables().put( variable.toString(), mark );
        }
        return mark != null;
    }

    private static void reportError( TypeCheckerUtils typeCheckerUtils, Tree node, String error ) {
        CompilationUnitTree cu = typeCheckerUtils.getCompilationUnit();
        long lineNumber = ( node instanceof DiagnosticPosition )
                ? cu.getLineMap().getLineNumber( ( ( DiagnosticPosition ) node ).getStartPosition() )
                : -1;
        String fileName = cu.getSourceFile().getName();
        typeCheckerUtils.getMessager().printMessage( ERROR,
                fileName + ":" + lineNumber + " " + error );
    }

    private static String reusingError( LinearMark mark, IdentifierTree node ) {
        String aliasInfo = "";
        if ( !mark.name().equals( node.getName().toString() ) ) {
            aliasInfo = " (aliased as " + node.getName() + ")";
        }

        return "Re-using @Linear variable " + mark.name() + aliasInfo;
    }

    private static String assignmentError( VariableTree node, IdentifierTree initializer ) {
        return "Cannot assign non-linear variable " + initializer.getName() + " to linear variable " + node.getName();
    }

    private static String assignmentError( VariableTree node, MethodInvocationTree initializer ) {
        return "Cannot assign non-linear return type of " + initializer + " to linear variable " + node.getName();
    }

    private static String methodCallError( MethodInvocationTree node, ExpressionTree arg, int argIndex ) {
        return "Cannot use linear variable " + arg + " as argument of method " +
                node.getMethodSelect() + "() at index " + argIndex + " (parameter is not linear)";
    }

    private static String returnValueError( MethodTree methodTree, Tree tree ) {
        return "Cannot return non-linear value " + tree + " in linear method " + methodTree.getName() + "()";
    }
}
