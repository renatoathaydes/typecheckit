package com.typecheckit;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.typecheckit.util.Mark;
import com.typecheckit.util.ScopeStack;
import com.typecheckit.util.TypeCheckerUtils;

import javax.lang.model.element.Name;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.typecheckit.BlockKind.IF;
import static com.typecheckit.BlockKind.SWITCH;
import static com.typecheckit.BlockKind.SWITCH_CASE;
import static java.util.Collections.singleton;

public abstract class ScopeBasedTypeChecker<M extends Mark<M>> extends TypeChecker {

    private final ScopeStack<M> scopes = new ScopeStack<>();

    protected ScopeStack.Scope<M> currentScope() {
        return scopes.currentScope();
    }

    public ScopeStack<M> getScopes() {
        return scopes;
    }

    @Override
    public void stop() {
        if ( scopes.size() != 1 ) {
            throw new IllegalStateException( "Finished visit to LinearTypeChecker with unexpected number of scopes " +
                    "for variables: expected 1, but was " + scopes.size() );
        }
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
        ExpressionTree initializer = node.getInitializer();
        if ( initializer != null ) {
            if ( initializer.getKind() == Tree.Kind.IDENTIFIER ) {
                IdentifierTree idInit = ( IdentifierTree ) initializer;
                copyMarkToAlias( node.getName(), idInit );
            }
        }
        return super.visitVariable( node, typeCheckerUtils );
    }


    @Override
    public Void visitAssignment( AssignmentTree node, TypeCheckerUtils typeCheckerUtils ) {
        ExpressionTree variable = node.getVariable();
        ExpressionTree expression = node.getExpression();
        if ( variable.getKind() == Tree.Kind.IDENTIFIER ) {
            IdentifierTree idVar = ( IdentifierTree ) variable;
            if ( expression.getKind() == Tree.Kind.IDENTIFIER ) {
                IdentifierTree idExpr = ( IdentifierTree ) expression;
                copyMarkToAlias( idVar.getName(), idExpr );
            }
        }
        return super.visitAssignment( node, typeCheckerUtils );
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

        Map<CharSequence, M> activeScope = scopes.exitScope().getVariables();
        Map<CharSequence, M> tempScope = scopes.exitScope().getVariables();
        applyScopeCorrections( activeScope, tempScope );
    }

    private void applyScopeCorrections( Map<CharSequence, M> activeScope, Map<CharSequence, M> temporaryScope ) {
        activeScope.forEach( ( key, mark ) -> {
            M tempMark = temporaryScope.get( key );
            if ( tempMark != null ) {
                mark.merge( tempMark );
            }
        } );
    }


    private void copyMarkToAlias( Name variable, IdentifierTree expression ) {
        M mark = currentScope().getVariables().get( expression.getName() );
        if ( mark != null ) {
            currentScope().getVariables().put( variable, mark.alias() );
        }
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
