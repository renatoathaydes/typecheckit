package com.typecheckit.util;

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
        scopes.push( new Scope<>( "<root>", new HashMap<>( 6 ) ) );
    }

    public int size() {
        return scopes.size();
    }

    public void enterScope( String name ) {
        Map<String, M> variables = new HashMap<>( 6 );
        for ( Map.Entry<String, M> entry : currentScope().getVariables().entrySet() ) {
            variables.put( entry.getKey(), entry.getValue().enterNewScope() );
        }
        scopes.push( new Scope<>( name, variables ) );
    }

    public void duplicateScope() {
        Map<String, M> variables = new HashMap<>( 6 );
        Scope<M> scope = currentScope();
        for ( Map.Entry<String, M> entry : scope.getVariables().entrySet() ) {
            variables.put( entry.getKey(), entry.getValue().copy() );
        }
        scopes.push( new Scope<>( scope.getName() + "(duplicate)", variables ) );
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

    private Scope<M> currentScope() {
        return scopes.peek();
    }

    @Override
    public String toString() {
        return "VariableScope{" +
                "scopesStack=" + scopes +
                '}';
    }

    public static final class Scope<M> {
        private final String name;
        private final Map<String, M> variables;

        public Scope( String name, Map<String, M> variables ) {
            this.name = name;
            this.variables = variables;
        }

        public String getName() {
            return name;
        }

        public Map<String, M> getVariables() {
            return variables;
        }

        @Override
        public String toString() {
            return "Scope{" +
                    "name='" + name + '\'' +
                    ", variables=" + variables +
                    '}';
        }
    }

}
