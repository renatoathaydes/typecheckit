package com.typecheckit.linear;

import com.sun.source.tree.*;
import com.sun.tools.javac.tree.JCTree;
import com.typecheckit.TypeChecker;
import com.typecheckit.TypeCheckerUtils;
import com.typecheckit.annotation.Linear;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.tools.Diagnostic.Kind.ERROR;

public class LinearTypeChecker extends TypeChecker {

    private static class LinearMark {
        boolean isUsedUp;
    }

    private final Map<String, LinearMark> allowedUsagesByVarName = new HashMap<>();

    @Override
    public Void visitAssignment( AssignmentTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitAssignment( node, typeCheckerUtils );
    }

    @Override
    public Void visitAnnotation( AnnotationTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitAnnotation( node, typeCheckerUtils );
    }

    @Override
    public Void visitCompilationUnit( CompilationUnitTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitCompilationUnit( node, typeCheckerUtils );
    }

    @Override
    public Void visitImport( ImportTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitImport( node, typeCheckerUtils );
    }

    @Override
    public Void visitClass( ClassTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitClass( node, typeCheckerUtils );
    }

    @Override
    public Void visitMethod( MethodTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitMethod( node, typeCheckerUtils );
    }

    @Override
    public Void visitVariable( VariableTree node, TypeCheckerUtils typeCheckerUtils ) {
        List<String> annotations = typeCheckerUtils.annotationNames( node );
        if ( annotations.contains( Linear.class.getName() ) ) {
            allowedUsagesByVarName.put( node.getName().toString(), new LinearMark() );
        }
        return super.visitVariable( node, typeCheckerUtils );
    }

    @Override
    public Void visitEmptyStatement( EmptyStatementTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitEmptyStatement( node, typeCheckerUtils );
    }

    @Override
    public Void visitBlock( BlockTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitBlock( node, typeCheckerUtils );
    }

    @Override
    public Void visitDoWhileLoop( DoWhileLoopTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitDoWhileLoop( node, typeCheckerUtils );
    }

    @Override
    public Void visitWhileLoop( WhileLoopTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitWhileLoop( node, typeCheckerUtils );
    }

    @Override
    public Void visitForLoop( ForLoopTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitForLoop( node, typeCheckerUtils );
    }

    @Override
    public Void visitEnhancedForLoop( EnhancedForLoopTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitEnhancedForLoop( node, typeCheckerUtils );
    }

    @Override
    public Void visitLabeledStatement( LabeledStatementTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitLabeledStatement( node, typeCheckerUtils );
    }

    @Override
    public Void visitSwitch( SwitchTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitSwitch( node, typeCheckerUtils );
    }

    @Override
    public Void visitCase( CaseTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitCase( node, typeCheckerUtils );
    }

    @Override
    public Void visitSynchronized( SynchronizedTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitSynchronized( node, typeCheckerUtils );
    }

    @Override
    public Void visitTry( TryTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitTry( node, typeCheckerUtils );
    }

    @Override
    public Void visitCatch( CatchTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitCatch( node, typeCheckerUtils );
    }

    @Override
    public Void visitConditionalExpression( ConditionalExpressionTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitConditionalExpression( node, typeCheckerUtils );
    }

    @Override
    public Void visitIf( IfTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitIf( node, typeCheckerUtils );
    }

    @Override
    public Void visitExpressionStatement( ExpressionStatementTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitExpressionStatement( node, typeCheckerUtils );
    }

    @Override
    public Void visitBreak( BreakTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitBreak( node, typeCheckerUtils );
    }

    @Override
    public Void visitContinue( ContinueTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitContinue( node, typeCheckerUtils );
    }

    @Override
    public Void visitReturn( ReturnTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitReturn( node, typeCheckerUtils );
    }

    @Override
    public Void visitThrow( ThrowTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitThrow( node, typeCheckerUtils );
    }

    @Override
    public Void visitAssert( AssertTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitAssert( node, typeCheckerUtils );
    }

    @Override
    public Void visitMethodInvocation( MethodInvocationTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitMethodInvocation( node, typeCheckerUtils );
    }

    @Override
    public Void visitNewClass( NewClassTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitNewClass( node, typeCheckerUtils );
    }

    @Override
    public Void visitNewArray( NewArrayTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitNewArray( node, typeCheckerUtils );
    }

    @Override
    public Void visitLambdaExpression( LambdaExpressionTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitLambdaExpression( node, typeCheckerUtils );
    }

    @Override
    public Void visitParenthesized( ParenthesizedTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitParenthesized( node, typeCheckerUtils );
    }

    @Override
    public Void visitCompoundAssignment( CompoundAssignmentTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitCompoundAssignment( node, typeCheckerUtils );
    }

    @Override
    public Void visitUnary( UnaryTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitUnary( node, typeCheckerUtils );
    }

    @Override
    public Void visitBinary( BinaryTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitBinary( node, typeCheckerUtils );
    }

    @Override
    public Void visitTypeCast( TypeCastTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitTypeCast( node, typeCheckerUtils );
    }

    @Override
    public Void visitInstanceOf( InstanceOfTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitInstanceOf( node, typeCheckerUtils );
    }

    @Override
    public Void visitArrayAccess( ArrayAccessTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitArrayAccess( node, typeCheckerUtils );
    }

    @Override
    public Void visitMemberSelect( MemberSelectTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitMemberSelect( node, typeCheckerUtils );
    }

    @Override
    public Void visitMemberReference( MemberReferenceTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitMemberReference( node, typeCheckerUtils );
    }

    @Override
    public Void visitIdentifier( IdentifierTree node, TypeCheckerUtils typeCheckerUtils ) {
        String nodeName = node.getName().toString();
        LinearMark mark = allowedUsagesByVarName.get( nodeName );
        if ( mark != null ) {
            if ( mark.isUsedUp ) {
                CompilationUnitTree cu = typeCheckerUtils.getCompilationUnit();
                long lineNumber = cu.getLineMap().getLineNumber(
                        ( ( JCTree.JCIdent ) node ).getStartPosition() );
                String fileName = cu.getSourceFile().getName();
                typeCheckerUtils.getMessager().printMessage( ERROR,
                        fileName + ":" + lineNumber + " Re-using @Linear variable " + node.getName() );
            } else {
                mark.isUsedUp = true;
            }
        }
        return super.visitIdentifier( node, typeCheckerUtils );
    }

    @Override
    public Void visitLiteral( LiteralTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitLiteral( node, typeCheckerUtils );
    }

    @Override
    public Void visitPrimitiveType( PrimitiveTypeTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitPrimitiveType( node, typeCheckerUtils );
    }

    @Override
    public Void visitArrayType( ArrayTypeTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitArrayType( node, typeCheckerUtils );
    }

    @Override
    public Void visitParameterizedType( ParameterizedTypeTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitParameterizedType( node, typeCheckerUtils );
    }

    @Override
    public Void visitUnionType( UnionTypeTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitUnionType( node, typeCheckerUtils );
    }

    @Override
    public Void visitIntersectionType( IntersectionTypeTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitIntersectionType( node, typeCheckerUtils );
    }

    @Override
    public Void visitTypeParameter( TypeParameterTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitTypeParameter( node, typeCheckerUtils );
    }

    @Override
    public Void visitWildcard( WildcardTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitWildcard( node, typeCheckerUtils );
    }

    @Override
    public Void visitModifiers( ModifiersTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitModifiers( node, typeCheckerUtils );
    }

    @Override
    public Void visitAnnotatedType( AnnotatedTypeTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitAnnotatedType( node, typeCheckerUtils );
    }

    @Override
    public Void visitOther( Tree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitOther( node, typeCheckerUtils );
    }

    @Override
    public Void visitErroneous( ErroneousTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitErroneous( node, typeCheckerUtils );
    }
}
