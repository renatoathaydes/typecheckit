package com.typecheckit.util;

import com.sun.source.tree.MethodTree;
import com.typecheckit.BlockKind;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

/**
 * Scope stack that keeps track of variables that may be "marked" for improved type checking.
 *
 * @param <M> type of marks that may be applied to variables within a scope
 */
public final class ScopeStack<M extends Mark<M>> {

    private final Stack<Scope<M>> scopes = new Stack<>();

    public ScopeStack() {
        scopes.push( new Scope<>( BlockKind.ROOT, "<root>", new HashMap<>( 6 ), null ) );
    }

    public int size() {
        return scopes.size();
    }

    public void enterScope( BlockKind blockKind ) {
        enterScope( blockKind, "<>" );
    }

    public void enterScope( MethodTree methodTree ) {
        enterScope( BlockKind.METHOD, methodTree.getName(), methodTree );
    }

    public void enterScope( BlockKind blockKind, CharSequence name ) {
        enterScope( blockKind, name, currentMethod() );
    }

    private void enterScope( BlockKind blockKind, CharSequence name, MethodTree methodTree ) {
        Map<CharSequence, M> variables = new HashMap<>( 6 );
        for ( Map.Entry<CharSequence, M> entry : currentScope().getVariables().entrySet() ) {
            variables.put( entry.getKey(), entry.getValue().enterNewScope() );
        }
        scopes.push( new Scope<>( blockKind, name, variables, methodTree ) );
    }

    public void duplicateScope() {
        Map<CharSequence, M> variables = new HashMap<>( 6 );
        Scope<M> scope = currentScope();
        for ( Map.Entry<CharSequence, M> entry : scope.getVariables().entrySet() ) {
            variables.put( entry.getKey(), entry.getValue().copy() );
        }
        scopes.push( new Scope<M>( scope.getBlockKind(),
                scope.getName() + "(duplicate)",
                variables, scope.getMethodTree().orElse( null ) ) );
    }

    public Scope<M> exitScope() {
        if ( scopes.size() < 2 ) {
            throw new IllegalStateException( "Cannot exit the root scope" );
        }
        return scopes.pop();
    }

    public void swapScopes() {
        Scope<M> first = scopes.pop();
        Scope<M> second = scopes.pop();
        scopes.push( first );
        scopes.push( second );
    }

    public M get( CharSequence name ) {
        return currentScope().getVariables().get( name );
    }

    public void put( CharSequence name, M variable ) {
        currentScope().getVariables().put( name, variable );
    }

    public Scope<M> currentScope() {
        return scopes.peek();
    }

    private MethodTree currentMethod() {
        MethodTree result = null;
        for ( Scope<M> scope : scopes ) {
            result = scope.methodTree;
        }
        return result;
    }

    public boolean isWithinLoop() {
        for ( Scope<M> scope : scopes ) {
            if ( scope.getBlockKind().isLoop() ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "ScopeStack{" +
                "scopesStack=" + scopes +
                '}';
    }

    public static final class Scope<M> {
        private final BlockKind blockKind;
        private final CharSequence name;
        private final Map<CharSequence, M> variables;
        private final MethodTree methodTree;

        public Scope( BlockKind blockKind, CharSequence name, Map<CharSequence, M> variables, MethodTree methodTree ) {
            this.blockKind = blockKind;
            this.name = name;
            this.variables = variables;
            this.methodTree = methodTree;
        }

        public BlockKind getBlockKind() {
            return blockKind;
        }

        public CharSequence getName() {
            return name;
        }

        public Map<CharSequence, M> getVariables() {
            return variables;
        }

        public Optional<MethodTree> getMethodTree() {
            return Optional.ofNullable( methodTree );
        }

        @Override
        public String toString() {
            return "Scope{" +
                    "blockKind='" + blockKind.name() + '\'' +
                    ", name='" + name + '\'' +
                    ", variables=" + variables +
                    ", methodTree=" + methodTree +
                    '}';
        }
    }

}
