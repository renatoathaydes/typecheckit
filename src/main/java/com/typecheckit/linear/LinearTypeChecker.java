package com.typecheckit.linear;

import com.sun.source.tree.*;
import com.sun.tools.javac.tree.JCTree;
import com.typecheckit.BlockKind;
import com.typecheckit.TypeChecker;
import com.typecheckit.TypeCheckerUtils;
import com.typecheckit.VariableScope;
import com.typecheckit.annotation.Linear;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import static com.typecheckit.BlockKind.*;
import static javax.tools.Diagnostic.Kind.ERROR;

public final class LinearTypeChecker extends TypeChecker {

    private final VariableScope<LinearMark> allowedUsagesByVarName = new VariableScope<>();
    private final Stack<BlockKind> blockStack = new Stack<>();

    @Override
    public Void visitAssignment( AssignmentTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitAssignment " + node );
        return super.visitAssignment( node, typeCheckerUtils );
    }

    @Override
    public Void visitAnnotation( AnnotationTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitAnnotation " + node );
        return super.visitAnnotation( node, typeCheckerUtils );
    }

    @Override
    public Void visitCompilationUnit( CompilationUnitTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitCompilationUnit " + node );
        return super.visitCompilationUnit( node, typeCheckerUtils );
    }

    @Override
    public Void visitImport( ImportTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitImport " + node );
        return super.visitImport( node, typeCheckerUtils );
    }

    @Override
    public Void visitClass( ClassTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitClass " + node );
        allowedUsagesByVarName.enterScope();
        blockStack.push( BlockKind.OTHER );
        super.visitClass( node, typeCheckerUtils );
        blockStack.pop();
        allowedUsagesByVarName.exitScope();
        return null;
    }

    @Override
    public Void visitMethod( MethodTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitMethod " + node );
        allowedUsagesByVarName.enterScope();
        blockStack.push( BlockKind.OTHER );
        super.visitMethod( node, typeCheckerUtils );
        blockStack.pop();
        allowedUsagesByVarName.exitScope();
        return null;
    }

    @Override
    public Void visitVariable( VariableTree node, TypeCheckerUtils typeCheckerUtils ) {
        List<String> annotations = typeCheckerUtils.annotationNames( node );
        if ( annotations.contains( Linear.class.getName() ) ) {
            allowedUsagesByVarName.put( node.getName().toString(), new LinearMark() );
        }
        System.out.println( "visitVariable " + node );
        return super.visitVariable( node, typeCheckerUtils );
    }

    @Override
    public Void visitEmptyStatement( EmptyStatementTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitEmptyStatement " + node );
        return super.visitEmptyStatement( node, typeCheckerUtils );
    }

    @Override
    public Void visitBlock( BlockTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitBlock " + node );
        boolean correctScopeAfterElseBlock = false;
        if ( blockStack.peek() == IF_THEN_ELSE ) {
            // first block of if/else block, update block stack so we know when we get to the else block
            blockStack.pop();
            blockStack.push( ELSE );

            // duplicate the current scope before entering the 'then' branch...
            // this causes the 'then' branch to become "detached" from the scope below it
            allowedUsagesByVarName.duplicateScope();
        } else if ( blockStack.peek() == ELSE ) {
            // swap the 'then' branch with the scope that was active before it was entered...
            // we enter the 'else' scope still "connected" to the previously active scope, so that the
            // corrections from the 'then' branch can be done directly on the entered scope
            allowedUsagesByVarName.swapScopes();

            correctScopeAfterElseBlock = true;
        }

        allowedUsagesByVarName.enterScope();

        super.visitBlock( node, typeCheckerUtils );

        if ( correctScopeAfterElseBlock ) {
            Map<String, LinearMark> elseScope = allowedUsagesByVarName.exitScope();

            // put the active scope back in its place, so we can remove the detached 'then' branch
            allowedUsagesByVarName.swapScopes();

            Map<String, LinearMark> thenScope = allowedUsagesByVarName.exitScope();

            // apply corrections directly on the 'else' scope because it is "connected" to the active scope
            applyScopeCorrections( elseScope, thenScope );
        } else {
            // simple case, just exit
            allowedUsagesByVarName.exitScope();
        }

        return null;
    }

    private void applyScopeCorrections( Map<String, LinearMark> activeScope, Map<String, LinearMark> temporaryScope ) {
        activeScope.forEach( ( key, mark ) -> {
            LinearMark tempMark = temporaryScope.get( key );
            if ( tempMark != null ) {
                // variable is used up if the current state is usedUp OR it was usedUp in the temporary scope
                mark.isUsedUp |= tempMark.isUsedUp;
            }
        } );
    }

    @Override
    public Void visitDoWhileLoop( DoWhileLoopTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitDoWhileLoop " + node );
        blockStack.push( BlockKind.OTHER );
        super.visitDoWhileLoop( node, typeCheckerUtils );
        blockStack.pop();
        return null;
    }

    @Override
    public Void visitWhileLoop( WhileLoopTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitWhileLoop " + node );
        blockStack.push( BlockKind.OTHER );
        super.visitWhileLoop( node, typeCheckerUtils );
        blockStack.pop();
        return null;
    }

    @Override
    public Void visitForLoop( ForLoopTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitForLoop " + node );
        blockStack.push( BlockKind.OTHER );
        super.visitForLoop( node, typeCheckerUtils );
        blockStack.pop();
        return null;
    }

    @Override
    public Void visitEnhancedForLoop( EnhancedForLoopTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitEnhancedForLoop " + node );
        blockStack.push( BlockKind.OTHER );
        super.visitEnhancedForLoop( node, typeCheckerUtils );
        blockStack.pop();
        return null;
    }

    @Override
    public Void visitLabeledStatement( LabeledStatementTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitLabeledStatement " + node );
        return super.visitLabeledStatement( node, typeCheckerUtils );
    }

    @Override
    public Void visitSwitch( SwitchTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitSwitch " + node );
        blockStack.push( BlockKind.OTHER );
        super.visitSwitch( node, typeCheckerUtils );
        blockStack.pop();
        return null;
    }

    @Override
    public Void visitCase( CaseTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitCase " + node );
        return super.visitCase( node, typeCheckerUtils );
    }

    @Override
    public Void visitSynchronized( SynchronizedTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitSynchronized " + node );
        blockStack.push( BlockKind.OTHER );
        super.visitSynchronized( node, typeCheckerUtils );
        blockStack.pop();
        return null;
    }

    @Override
    public Void visitTry( TryTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitTry " + node );
        blockStack.push( BlockKind.OTHER );
        super.visitTry( node, typeCheckerUtils );
        blockStack.pop();
        return null;
    }

    @Override
    public Void visitCatch( CatchTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitCatch " + node );
        blockStack.push( BlockKind.OTHER );
        super.visitCatch( node, typeCheckerUtils );
        blockStack.pop();
        return null;
    }

    @Override
    public Void visitConditionalExpression( ConditionalExpressionTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitConditionalExpression " + node );
        return super.visitConditionalExpression( node, typeCheckerUtils );
    }

    @Override
    public Void visitIf( IfTree node, TypeCheckerUtils typeCheckerUtils ) {
        boolean hasElse = node.getElseStatement() != null;
        System.out.println( "visitIf " + node );
        blockStack.push( hasElse ? IF_THEN_ELSE : IF_THEN );
        super.visitIf( node, typeCheckerUtils );
        blockStack.pop();
        return null;
    }

    @Override
    public Void visitExpressionStatement( ExpressionStatementTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitExpressionStatement " + node );
        return super.visitExpressionStatement( node, typeCheckerUtils );
    }

    @Override
    public Void visitBreak( BreakTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitBreak " + node );
        return super.visitBreak( node, typeCheckerUtils );
    }

    @Override
    public Void visitContinue( ContinueTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitContinue " + node );
        return super.visitContinue( node, typeCheckerUtils );
    }

    @Override
    public Void visitReturn( ReturnTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitReturn " + node );
        return super.visitReturn( node, typeCheckerUtils );
    }

    @Override
    public Void visitThrow( ThrowTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitThrow " + node );
        return super.visitThrow( node, typeCheckerUtils );
    }

    @Override
    public Void visitAssert( AssertTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitAssert " + node );
        return super.visitAssert( node, typeCheckerUtils );
    }

    @Override
    public Void visitMethodInvocation( MethodInvocationTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitMethodInvocation " + node );
        return super.visitMethodInvocation( node, typeCheckerUtils );
    }

    @Override
    public Void visitNewClass( NewClassTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitNewClass " + node );
        return super.visitNewClass( node, typeCheckerUtils );
    }

    @Override
    public Void visitNewArray( NewArrayTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitNewArray " + node );
        return super.visitNewArray( node, typeCheckerUtils );
    }

    @Override
    public Void visitLambdaExpression( LambdaExpressionTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitLambdaExpression " + node );
        return super.visitLambdaExpression( node, typeCheckerUtils );
    }

    @Override
    public Void visitParenthesized( ParenthesizedTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitParenthesized " + node );
        return super.visitParenthesized( node, typeCheckerUtils );
    }

    @Override
    public Void visitCompoundAssignment( CompoundAssignmentTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitCompoundAssignment " + node );
        return super.visitCompoundAssignment( node, typeCheckerUtils );
    }

    @Override
    public Void visitUnary( UnaryTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitUnary " + node );
        return super.visitUnary( node, typeCheckerUtils );
    }

    @Override
    public Void visitBinary( BinaryTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitBinary " + node );
        return super.visitBinary( node, typeCheckerUtils );
    }

    @Override
    public Void visitTypeCast( TypeCastTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitTypeCast " + node );
        return super.visitTypeCast( node, typeCheckerUtils );
    }

    @Override
    public Void visitInstanceOf( InstanceOfTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitInstanceOf " + node );
        return super.visitInstanceOf( node, typeCheckerUtils );
    }

    @Override
    public Void visitArrayAccess( ArrayAccessTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitArrayAccess " + node );
        return super.visitArrayAccess( node, typeCheckerUtils );
    }

    @Override
    public Void visitMemberSelect( MemberSelectTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitMemberSelect " + node );
        return super.visitMemberSelect( node, typeCheckerUtils );
    }

    @Override
    public Void visitMemberReference( MemberReferenceTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitMemberReference " + node );
        return super.visitMemberReference( node, typeCheckerUtils );
    }

    @Override
    public Void visitIdentifier( IdentifierTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitIdentifier " + node );

        String nodeName = node.getName().toString();
        LinearMark mark = allowedUsagesByVarName.get( nodeName );
        if ( mark != null ) {
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

    @Override
    public Void visitLiteral( LiteralTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitLiteral " + node );
        return super.visitLiteral( node, typeCheckerUtils );
    }

    @Override
    public Void visitPrimitiveType( PrimitiveTypeTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitPrimitiveType " + node );
        return super.visitPrimitiveType( node, typeCheckerUtils );
    }

    @Override
    public Void visitArrayType( ArrayTypeTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitArrayType " + node );
        return super.visitArrayType( node, typeCheckerUtils );
    }

    @Override
    public Void visitParameterizedType( ParameterizedTypeTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitParameterizedType " + node );
        return super.visitParameterizedType( node, typeCheckerUtils );
    }

    @Override
    public Void visitUnionType( UnionTypeTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitUnionType " + node );
        return super.visitUnionType( node, typeCheckerUtils );
    }

    @Override
    public Void visitIntersectionType( IntersectionTypeTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitIntersectionType " + node );
        return super.visitIntersectionType( node, typeCheckerUtils );
    }

    @Override
    public Void visitTypeParameter( TypeParameterTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitTypeParameter " + node );
        return super.visitTypeParameter( node, typeCheckerUtils );
    }

    @Override
    public Void visitWildcard( WildcardTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitWildcard " + node );
        return super.visitWildcard( node, typeCheckerUtils );
    }

    @Override
    public Void visitModifiers( ModifiersTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitModifiers " + node );
        return super.visitModifiers( node, typeCheckerUtils );
    }

    @Override
    public Void visitAnnotatedType( AnnotatedTypeTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitAnnotatedType " + node );
        return super.visitAnnotatedType( node, typeCheckerUtils );
    }

    @Override
    public Void visitOther( Tree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitOther " + node );
        return super.visitOther( node, typeCheckerUtils );
    }

    @Override
    public Void visitErroneous( ErroneousTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitErroneous " + node );
        return super.visitErroneous( node, typeCheckerUtils );
    }
}
