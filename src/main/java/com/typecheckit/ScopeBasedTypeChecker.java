package com.typecheckit;

import com.sun.source.tree.*;
import com.typecheckit.util.Mark;
import com.typecheckit.util.ScopeStack;
import com.typecheckit.util.TypeCheckerUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.typecheckit.BlockKind.IF;
import static com.typecheckit.BlockKind.SWITCH;
import static com.typecheckit.BlockKind.SWITCH_CASE;
import static java.util.Collections.singleton;

public abstract class ScopeBasedTypeChecker<M extends Mark<M>> extends DebugTypeChecker {

    private final ScopeStack<M> scopes = new ScopeStack<>();

    protected ScopeStack.Scope<M> currentScope() {
        return scopes.currentScope();
    }

    @Override
    public void stop() {
        if ( scopes.size() != 1 ) {
            System.err.println( scopes );
            throw new IllegalStateException( "Finished visit to LinearTypeChecker with unexpected number of scopes " +
                    "for variables: expected 1, but was " + scopes.size() );
        }
    }


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
        scopes.enterScope( BlockKind.CLASS, node.getSimpleName() );
        super.visitClass( node, typeCheckerUtils );
        scopes.exitScope();
        return null;
    }

    @Override
    public Void visitMethod( MethodTree node, TypeCheckerUtils typeCheckerUtils ) {
        scopes.enterScope( node );
        super.visitMethod( node, typeCheckerUtils );
        scopes.exitScope();
        return null;
    }

    @Override
    public Void visitVariable( VariableTree node, TypeCheckerUtils typeCheckerUtils ) {
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
        scopes.enterScope( BlockKind.WHILE_LOOP );
        super.visitDoWhileLoop( node, typeCheckerUtils );
        scopes.exitScope();
        return null;
    }

    @Override
    public Void visitWhileLoop( WhileLoopTree node, TypeCheckerUtils typeCheckerUtils ) {
        scopes.enterScope( BlockKind.WHILE_LOOP );
        super.visitWhileLoop( node, typeCheckerUtils );
        scopes.exitScope();
        return null;
    }

    @Override
    public Void visitForLoop( ForLoopTree node, TypeCheckerUtils typeCheckerUtils ) {
        scopes.enterScope( BlockKind.FOR_LOOP );
        super.visitForLoop( node, typeCheckerUtils );
        scopes.exitScope();
        return null;
    }

    @Override
    public Void visitEnhancedForLoop( EnhancedForLoopTree node, TypeCheckerUtils typeCheckerUtils ) {
        scopes.enterScope( BlockKind.FOR_LOOP );
        super.visitEnhancedForLoop( node, typeCheckerUtils );
        scopes.exitScope();
        return null;
    }

    @Override
    public Void visitLabeledStatement( LabeledStatementTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitLabeledStatement( node, typeCheckerUtils );
    }

    @Override
    public Void visitSwitch( SwitchTree node, TypeCheckerUtils typeCheckerUtils ) {
        scopes.enterScope( SWITCH );
        scan( node.getExpression(), typeCheckerUtils );
        Iterator<? extends CaseTree> cases = node.getCases().iterator();
        if ( cases.hasNext() ) {
            visitSwitchElseCase( new SwitchElseCase( cases ), typeCheckerUtils );
        }
        scopes.exitScope();
        return null;
    }

    private void visitSwitchElseCase( SwitchElseCase node, TypeCheckerUtils typeCheckerUtils ) {
        SwitchElseCase elseStatement = node.getElseStatement();
        boolean hasElse = elseStatement != null;
        scopes.enterScope( SWITCH_CASE );

        if ( hasElse ) {
            scanMutuallyExclusiveTrees( node.primaryCase, elseStatement, typeCheckerUtils );
        } else {
            scan( node.primaryCase, typeCheckerUtils );
            scopes.exitScope();
        }
    }

    @Override
    public Void visitCase( CaseTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitCase( node, typeCheckerUtils );
    }

    @Override
    public Void visitSynchronized( SynchronizedTree node, TypeCheckerUtils typeCheckerUtils ) {
        scopes.enterScope( BlockKind.SYNCHRONIZED );
        super.visitSynchronized( node, typeCheckerUtils );
        scopes.exitScope();
        return null;
    }

    @Override
    public Void visitTry( TryTree node, TypeCheckerUtils typeCheckerUtils ) {
        scopes.enterScope( BlockKind.OTHER );
        super.visitTry( node, typeCheckerUtils );
        scopes.exitScope();
        return null;
    }

    @Override
    public Void visitCatch( CatchTree node, TypeCheckerUtils typeCheckerUtils ) {
        return super.visitCatch( node, typeCheckerUtils );
    }

    @Override
    public Void visitConditionalExpression( ConditionalExpressionTree node, TypeCheckerUtils typeCheckerUtils ) {
        scan( node.getCondition(), typeCheckerUtils );
        scopes.enterScope( IF );
        scanMutuallyExclusiveTrees( singleton( node.getTrueExpression() ), node.getFalseExpression(), typeCheckerUtils );
        return null;
    }

    @Override
    public Void visitIf( IfTree node, TypeCheckerUtils typeCheckerUtils ) {
        StatementTree elseStatement = node.getElseStatement();
        boolean hasElse = elseStatement != null;
        scan( node.getCondition(), typeCheckerUtils );
        scopes.enterScope( IF );

        if ( hasElse ) {
            scanMutuallyExclusiveTrees( singleton( node.getThenStatement() ), elseStatement, typeCheckerUtils );
        } else {
            scan( node.getThenStatement(), typeCheckerUtils );
            scopes.exitScope();
        }
        return null;
    }

    private boolean isCaseBreaking( CaseTree caseTree ) {
        List<? extends StatementTree> statements = caseTree.getStatements();
        if ( !statements.isEmpty() ) {
            StatementTree lastStatement = statements.get( statements.size() - 1 );
            return lastStatement instanceof BreakTree;
        }
        return false;
    }

    private void scanMutuallyExclusiveTrees( Iterable<? extends Tree> tree1,
                                             Tree tree2,
                                             TypeCheckerUtils typeCheckerUtils ) {
        // duplicate the current scope before scanning the first branch...
        // this causes the branch to become "detached" from the scope below it.
        // that's necessary because each disjoint branch must have independent scopes which are
        // merged only after the second branch is visited.
        scopes.duplicateScope();
        scan( tree1, typeCheckerUtils );

        // swap the detached first scope with the active scope instead of simply exiting it...
        // this puts the active scope on the top of the stack, so that the second branch can use it,
        // then merge its scope with the first scope.
        scopes.swapScopes();

        scan( tree2, typeCheckerUtils );

        Map<String, M> activeScope = scopes.exitScope().getVariables();
        Map<String, M> tempScope = scopes.exitScope().getVariables();
        applyScopeCorrections( activeScope, tempScope );
    }

    private void applyScopeCorrections( Map<String, M> activeScope, Map<String, M> temporaryScope ) {
        activeScope.forEach( ( key, mark ) -> {
            M tempMark = temporaryScope.get( key );
            if ( tempMark != null ) {
                mark.merge( tempMark );
            }
        } );
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

    private class SwitchElseCase implements Tree {

        private final List<CaseTree> primaryCase;
        private final SwitchElseCase elseCase;

        SwitchElseCase( Iterator<? extends CaseTree> cases ) {
            primaryCase = new ArrayList<>( 4 );
            CaseTree nextCase = cases.next();
            primaryCase.add( nextCase );
            while ( !isCaseBreaking( nextCase ) ) {
                if ( cases.hasNext() ) {
                    nextCase = cases.next();
                    primaryCase.add( nextCase );
                } else {
                    break;
                }
            }
            elseCase = cases.hasNext() ? new SwitchElseCase( cases ) : null;
        }

        @Override
        public Kind getKind() {
            return Kind.CASE;
        }

        @Override
        public <R, D> R accept( TreeVisitor<R, D> visitor, D data ) {
            visitSwitchElseCase( this, ( TypeCheckerUtils ) data );
            return null;
        }

        SwitchElseCase getElseStatement() {
            return elseCase;
        }
    }

}
