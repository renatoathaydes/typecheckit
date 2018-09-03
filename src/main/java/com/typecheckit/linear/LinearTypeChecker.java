package com.typecheckit.linear;

import com.sun.source.tree.*;
import com.sun.tools.javac.tree.JCTree;
import com.typecheckit.BlockKind;
import com.typecheckit.TypeChecker;
import com.typecheckit.annotation.Linear;
import com.typecheckit.util.TypeCheckerUtils;
import com.typecheckit.util.VariableScope;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import static com.typecheckit.BlockKind.ELSE;
import static com.typecheckit.BlockKind.IF_THEN;
import static com.typecheckit.BlockKind.IF_THEN_ELSE;
import static javax.tools.Diagnostic.Kind.ERROR;

public final class LinearTypeChecker extends TypeChecker {

    private final VariableScope<LinearMark> allowedUsagesByVarName = new VariableScope<>();
    private final Stack<BlockKind> blockStack = new Stack<>();

    @Override
    public void stop() {
        if ( !blockStack.isEmpty() ) {
            throw new IllegalStateException( "Finished visit to LinearTypeChecker without clearing the blockStack" );
        }
        if ( allowedUsagesByVarName.size() != 1 ) {
            System.out.println( allowedUsagesByVarName );
            throw new IllegalStateException( "Finished visit to LinearTypeChecker with unexpected number of scopes " +
                    "for variables: expected 1, but was " + allowedUsagesByVarName.size() );
        }
    }

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
        allowedUsagesByVarName.enterScope( "class-" + node.getSimpleName() );
        blockStack.push( BlockKind.OTHER );
        super.visitClass( node, typeCheckerUtils );
        blockStack.pop();
        allowedUsagesByVarName.exitScope();
        return null;
    }

    @Override
    public Void visitMethod( MethodTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitMethod " + node );
        allowedUsagesByVarName.enterScope( "method-" + node.getName() );
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
        BlockKind blockKind = blockStack.peek();
        if ( blockKind == IF_THEN_ELSE ) {
            // duplicate the current scope before entering the 'then' branch...
            // this causes the 'then' branch to become "detached" from the scope below it.
            // that's necessary because each disjoint if/else branch must have independent scopes which are
            // merged only after the else branches are visited.
            allowedUsagesByVarName.duplicateScope();

            super.visitBlock( node, typeCheckerUtils );

            // swap the detached 'then' scope with the active scope instead of simply exiting it...
            // this puts the active scope on the top of the stack, so that the 'else' branch can use it,
            // then merge its scope with the 'then' scope.
            allowedUsagesByVarName.swapScopes();
        } else {
            // simple block
            allowedUsagesByVarName.enterScope( node.getKind().name() );
            super.visitBlock( node, typeCheckerUtils );
            allowedUsagesByVarName.exitScope();
        }

        return null;
    }

    private void applyElseBlocksCorrections( Map<String, LinearMark> elseScope ) {
        do {
            // swap the top scopes so we can reach the 'then' branch
            allowedUsagesByVarName.swapScopes();

            Map<String, LinearMark> thenScope = allowedUsagesByVarName.exitScope().getVariables();

            // apply corrections directly on the 'else' scope because it is "connected" to the active scope
            applyScopeCorrections( elseScope, thenScope );
        } while ( popBlockKindIfIsElse() );
    }

    private boolean popBlockKindIfIsElse() {
        boolean isElse = blockStack.peek() == ELSE;
        if ( isElse ) {
            blockStack.pop();
        }
        return isElse;
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
        blockStack.push( BlockKind.WHILE_LOOP );
        super.visitDoWhileLoop( node, typeCheckerUtils );
        blockStack.pop();
        return null;
    }

    @Override
    public Void visitWhileLoop( WhileLoopTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitWhileLoop " + node );
        blockStack.push( BlockKind.WHILE_LOOP );
        super.visitWhileLoop( node, typeCheckerUtils );
        blockStack.pop();
        return null;
    }

    @Override
    public Void visitForLoop( ForLoopTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitForLoop " + node );
        blockStack.push( BlockKind.FOR_LOOP );
        super.visitForLoop( node, typeCheckerUtils );
        blockStack.pop();
        return null;
    }

    @Override
    public Void visitEnhancedForLoop( EnhancedForLoopTree node, TypeCheckerUtils typeCheckerUtils ) {
        System.out.println( "visitEnhancedForLoop " + node );
        blockStack.push( BlockKind.FOR_LOOP );
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
        StatementTree elseStatement = node.getElseStatement();
        boolean hasElse = elseStatement != null;
        System.out.println( "visitIf " + node );
        scan( node.getCondition(), typeCheckerUtils );
        blockStack.push( hasElse ? IF_THEN_ELSE : IF_THEN );
        scan( node.getThenStatement(), typeCheckerUtils );
        blockStack.pop();
        if ( hasElse ) {
            blockStack.push( ELSE );
            // if the else statement is another "if", we will re-enter this method from here, piling up ELSEs
            scan( elseStatement, typeCheckerUtils );
        }

        // cleanup any ELSE blocks that may have been "piled up" as we visited chains of if-else branches
        if ( blockStack.peek() == ELSE ) {
            blockStack.pop();
            allowedUsagesByVarName.enterScope( "ELSE" );
            Map<String, LinearMark> elseScope = allowedUsagesByVarName.exitScope().getVariables();
            applyElseBlocksCorrections( elseScope );
        }
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
            if ( blockStack.peek().isLoop() ) {
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
