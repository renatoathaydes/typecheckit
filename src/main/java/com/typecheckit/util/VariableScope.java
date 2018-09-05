package com.typecheckit.util;

import com.typecheckit.BlockKind;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Scope for variables.
 *
 * @param <M> type of marks that may be applied to variables
 */
public final class VariableScope<M extends Mark<M>> {

    private final Stack<Scope<M>> scopes = new Stack<>();

    public VariableScope() {
        scopes.push( new Scope<>( BlockKind.ROOT, "<root>", new HashMap<>( 6 ) ) );
    }

    public int size() {
        return scopes.size();
    }

    public void enterScope( BlockKind blockKind ) {
        enterScope( blockKind, "<>" );
    }

    public void enterScope( BlockKind blockKind, CharSequence name ) {
        Map<String, M> variables = new HashMap<>( 6 );
        for ( Map.Entry<String, M> entry : currentScope().getVariables().entrySet() ) {
            variables.put( entry.getKey(), entry.getValue().enterNewScope() );
        }
        scopes.push( new Scope<>( blockKind, name, variables ) );
    }

    public void duplicateScope() {
        Map<String, M> variables = new HashMap<>( 6 );
        Scope<M> scope = currentScope();
        for ( Map.Entry<String, M> entry : scope.getVariables().entrySet() ) {
            variables.put( entry.getKey(), entry.getValue().copy() );
        }
        scopes.push( new Scope<>( scope.getBlockKind(), scope.getName() + "(duplicate)", variables ) );
    }

    public Scope<M> exitScope() {
        return scopes.pop();
    }

    public void swapScopes() {
        Scope<M> first = scopes.pop();
        Scope<M> second = scopes.pop();
        scopes.push( first );
        scopes.push( second );
    }

    public M get( String name ) {
        return currentScope().getVariables().get( name );
    }

    public void put( String name, M variable ) {
        currentScope().getVariables().put( name, variable );
    }

    public Scope<M> currentScope() {
        return scopes.peek();
    }

    @Override
    public String toString() {
        return "VariableScope{" +
                "scopesStack=" + scopes +
                '}';
    }

    public static final class Scope<M> {
        private final BlockKind blockKind;
        private final CharSequence name;
        private final Map<String, M> variables;

        public Scope( BlockKind blockKind, CharSequence name, Map<String, M> variables ) {
            this.blockKind = blockKind;
            this.name = name;
            this.variables = variables;
        }

        public BlockKind getBlockKind() {
            return blockKind;
        }

        public CharSequence getName() {
            return name;
        }

        public Map<String, M> getVariables() {
            return variables;
        }

        @Override
        public String toString() {
            return "Scope{" +
                    "blockKind='" + blockKind.name() + '\'' +
                    ", name='" + name + '\'' +
                    ", variables=" + variables +
                    '}';
        }
    }

}
