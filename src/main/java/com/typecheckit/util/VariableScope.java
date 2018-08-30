package com.typecheckit.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Scope for variables.
 *
 * @param <M> type of marks that may be applied to variables
 */
public class VariableScope<M extends Mark<M>> {

    private final Stack<Map<String, M>> scopes = new Stack<>();

    public VariableScope() {
        scopes.push( new HashMap<>( 6 ) );
    }

    public void enterScope() {
        Map<String, M> scopeLayer = new HashMap<>( 6 );
        for ( Map.Entry<String, M> entry : currentScope().entrySet() ) {
            scopeLayer.put( entry.getKey(), entry.getValue().enterNewScope() );
        }
        scopes.push( scopeLayer );
    }

    public void duplicateScope() {
        Map<String, M> scopeLayer = new HashMap<>( 6 );
        for ( Map.Entry<String, M> entry : currentScope().entrySet() ) {
            scopeLayer.put( entry.getKey(), entry.getValue().copy() );
        }
        scopes.push( scopeLayer );
    }

    public Map<String, M> exitScope() {
        return scopes.pop();
    }

    public void swapScopes() {
        Map<String, M> first = scopes.pop();
        Map<String, M> second = scopes.pop();
        scopes.push( first );
        scopes.push( second );
    }

    public M get( String name ) {
        return currentScope().get( name );
    }

    public void put( String name, M variable ) {
        currentScope().put( name, variable );
    }

    private Map<String, M> currentScope() {
        return scopes.peek();
    }
}
