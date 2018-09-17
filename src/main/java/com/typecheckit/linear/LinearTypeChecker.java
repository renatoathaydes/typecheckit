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
        return currentScope().getVariables().containsKey( identifierTree.getName() );
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
        boolean isLinearVariable = isLinear( node.getModifiers(), typeCheckerUtils );

        if ( isLinearVariable ) {
            ExpressionTree initializer = node.getInitializer();
            if ( initializer != null ) {
                checkLinearVariableValue( node, node.getName(), initializer, typeCheckerUtils );
            }
            System.out.println( "Variable " + node.getName() + " is linear!" );
            currentScope().getVariables().put( node.getName(), new LinearMark( node ) );
        }

        return super.visitVariable( node, typeCheckerUtils );
    }

    @Override
    public Void visitAssignment( AssignmentTree node, TypeCheckerUtils typeCheckerUtils ) {
        ExpressionTree variable = node.getVariable();
        if ( variable.getKind() == Tree.Kind.IDENTIFIER ) {
            IdentifierTree idVar = ( IdentifierTree ) variable;
            if ( isLinear( idVar ) ) {
                LinearMark mark = currentScope().getVariables().get( idVar.getName() );
                mark.ignoreNextUse(); // next use will be an assignment, which is not considered as a real use
                checkLinearVariableValue( node, idVar.getName(), node.getExpression(), typeCheckerUtils );
            }
        }
        ExpressionTree expression = node.getExpression();
        if ( expression.getKind() == Tree.Kind.IDENTIFIER ) {
            IdentifierTree idExpr = ( IdentifierTree ) variable;
            if ( isLinear( idExpr ) ) {
                LinearMark mark = currentScope().getVariables().get( idExpr.getName() );
                mark.ignoreNextUse(); // next use will be aliasing, which is not considered as a real use
            }
        }
        return super.visitAssignment( node, typeCheckerUtils );
    }

    private void checkLinearVariableValue( Tree node, Name nodeName,
                                           ExpressionTree value, TypeCheckerUtils typeCheckerUtils ) {
        if ( value.getKind() == Tree.Kind.IDENTIFIER ) {
            IdentifierTree idInit = ( IdentifierTree ) value;
            if ( !isLinear( idInit ) ) {
                reportError( typeCheckerUtils, idInit, assignmentError( nodeName, idInit ) );
            }
        } else if ( value.getKind() == Tree.Kind.METHOD_INVOCATION ) {
            MethodInvocationTree methodInitializer = ( MethodInvocationTree ) value;
            if ( !hasLinearReturnType( typeCheckerUtils, methodInitializer ) ) {
                reportError( typeCheckerUtils, node, assignmentError( nodeName, methodInitializer ) );
            }
        }
    }

    @Override
    public Void visitIdentifier( IdentifierTree node, TypeCheckerUtils typeCheckerUtils ) {
        Name nodeName = node.getName();
        Scope<LinearMark> scope = currentScope();
        LinearMark mark = scope.getVariables().get( nodeName );
        System.out.println( "Visiting ID " + nodeName + ", is Linear? " + mark );

        if ( mark != null ) {
            if ( getScopes().isWithinLoop() ) {
                // @Linear variable cannot be safely used in loops
                // (mark at least twice to ensure initialization within loops is disallowed)
                mark.markAsUsed();
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

    @SuppressWarnings( { "ConstantConditions", "BooleanMethodIsAlwaysInverted" } )
    private boolean hasLinearReturnType( TypeCheckerUtils typeCheckerUtils, MethodInvocationTree initializer ) {
        // do not report error if we simply can't find the element because
        // that probably means there's some other error in the source code already
        boolean defaultReturnValue = true;

        return typeCheckerUtils.getTreeElement( initializer )
                .map( element -> isLinear( ( ( Symbol.MethodSymbol ) element ).getReturnType() ) )
                .orElse( defaultReturnValue );
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
        if ( !mark.name().equals( node.getName() ) ) {
            aliasInfo = " (aliased as " + node.getName() + ")";
        }

        return "Re-using @Linear variable " + mark.name() + aliasInfo;
    }

    private static String assignmentError( Name node, IdentifierTree initializer ) {
        return "Cannot assign non-linear variable " + initializer.getName() + " to linear variable " + node;
    }

    private static String assignmentError( Name node, MethodInvocationTree initializer ) {
        return "Cannot assign non-linear return type of " + initializer + " to linear variable " + node;
    }

    private static String methodCallError( MethodInvocationTree node, ExpressionTree arg, int argIndex ) {
        return "Cannot use linear variable " + arg + " as argument of method " +
                node.getMethodSelect() + "() at index " + argIndex + " (parameter is not linear)";
    }

    private static String returnValueError( MethodTree methodTree, Tree tree ) {
        return "Cannot return non-linear value " + tree + " in linear method " + methodTree.getName() + "()";
    }
}
