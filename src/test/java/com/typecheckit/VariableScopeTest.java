package com.typecheckit;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class VariableScopeTest {

    private static class TestMark extends Mark<TestMark> {
        int index;
        boolean canAffectParentScope;

        TestMark( int index, boolean canAffectParentScope ) {
            this.index = index;
            this.canAffectParentScope = canAffectParentScope;
        }

        @Override
        protected TestMark enterNewScope() {
            return canAffectParentScope ? this : new TestMark( index + 1, canAffectParentScope );
        }

        @Override
        protected TestMark copy() {
            return new TestMark( index, canAffectParentScope );
        }

        @Override
        public String toString() {
            return "TestMark{" +
                    "index=" + index +
                    '}';
        }
    }

    private static class TestMarkMatcher extends BaseMatcher<TestMark> {

        private int expectedIndex;

        TestMarkMatcher( int expectedIndex ) {
            this.expectedIndex = expectedIndex;
        }

        @Override
        public boolean matches( Object item ) {
            if ( item instanceof TestMark ) {
                return ( ( TestMark ) item ).index == expectedIndex;
            }
            return false;
        }

        @Override
        public void describeTo( Description description ) {
            description.appendText( new TestMark( expectedIndex, false ).toString() );
        }
    }

    private static TestMarkMatcher hasIndex( int index ) {
        return new TestMarkMatcher( index );
    }

    @Test
    public void canEnterAndExitScopesAffectingParentScope() {
        VariableScope<TestMark> variableScope = new VariableScope<>();

        // there should always be a default scope we can add variables to
        variableScope.put( "test", new TestMark( 1, true ) );

        variableScope.enterScope();

        // child scope should inherit parent's variables
        assertThat( variableScope.get( "test" ), hasIndex( 1 ) );
        assertThat( variableScope.get( "other" ), nullValue() );

        variableScope.put( "child", new TestMark( 33, true ) );

        assertThat( variableScope.get( "test" ), hasIndex( 1 ) );
        assertThat( variableScope.get( "child" ), hasIndex( 33 ) );
        assertThat( variableScope.get( "other" ), nullValue() );

        variableScope.enterScope();

        // child scope should inherit parent's variables
        assertThat( variableScope.get( "test" ), hasIndex( 1 ) );
        assertThat( variableScope.get( "child" ), hasIndex( 33 ) );
        assertThat( variableScope.get( "other" ), nullValue() );

        variableScope.put( "grand-child", new TestMark( 42, true ) );

        assertThat( variableScope.get( "test" ), hasIndex( 1 ) );
        assertThat( variableScope.get( "child" ), hasIndex( 33 ) );
        assertThat( variableScope.get( "grand-child" ), hasIndex( 42 ) );
        assertThat( variableScope.get( "other" ), nullValue() );

        // modify variable that is present in parent scope
        variableScope.get( "child" ).index = 100;

        variableScope.exitScope();

        // we're back at the parent scope, with a modified child
        assertThat( variableScope.get( "test" ), hasIndex( 1 ) );
        assertThat( variableScope.get( "child" ), hasIndex( 100 ) );
        assertThat( variableScope.get( "grand-child" ), nullValue() );
        assertThat( variableScope.get( "other" ), nullValue() );

        variableScope.exitScope();

        // back to root scope
        assertThat( variableScope.get( "test" ), hasIndex( 1 ) );
        assertThat( variableScope.get( "child" ), nullValue() );
        assertThat( variableScope.get( "grand-child" ), nullValue() );
        assertThat( variableScope.get( "other" ), nullValue() );
    }

    @Test
    public void canEnterAndExitScopesWithIndependentParentScope() {
        VariableScope<TestMark> variableScope = new VariableScope<>();

        // there should always be a default scope we can add variables to
        variableScope.put( "test", new TestMark( 1, false ) );

        variableScope.enterScope();

        // child scope should inherit parent's variables
        assertThat( variableScope.get( "test" ), hasIndex( 2 ) );
        assertThat( variableScope.get( "other" ), nullValue() );

        variableScope.put( "child", new TestMark( 33, false ) );

        assertThat( variableScope.get( "test" ), hasIndex( 2 ) );
        assertThat( variableScope.get( "child" ), hasIndex( 33 ) );
        assertThat( variableScope.get( "other" ), nullValue() );

        variableScope.enterScope();

        // child scope should inherit parent's variables
        assertThat( variableScope.get( "test" ), hasIndex( 3 ) );
        assertThat( variableScope.get( "child" ), hasIndex( 34 ) );
        assertThat( variableScope.get( "other" ), nullValue() );

        variableScope.put( "grand-child", new TestMark( 42, false ) );

        assertThat( variableScope.get( "test" ), hasIndex( 3 ) );
        assertThat( variableScope.get( "child" ), hasIndex( 34 ) );
        assertThat( variableScope.get( "grand-child" ), hasIndex( 42 ) );
        assertThat( variableScope.get( "other" ), nullValue() );

        // modify variable that is present in parent scope
        variableScope.get( "child" ).index = 100;

        variableScope.exitScope();

        // we're back at the parent scope, no children are modified
        assertThat( variableScope.get( "test" ), hasIndex( 2 ) );
        assertThat( variableScope.get( "child" ), hasIndex( 33 ) );
        assertThat( variableScope.get( "grand-child" ), nullValue() );
        assertThat( variableScope.get( "other" ), nullValue() );

        variableScope.exitScope();

        // back to root scope
        assertThat( variableScope.get( "test" ), hasIndex( 1 ) );
        assertThat( variableScope.get( "child" ), nullValue() );
        assertThat( variableScope.get( "grand-child" ), nullValue() );
        assertThat( variableScope.get( "other" ), nullValue() );
    }

    @Test
    public void canSwapScopes() {
        VariableScope<TestMark> variableScope = new VariableScope<>();

        variableScope.put( "first", new TestMark( 1, true ) );

        variableScope.enterScope();

        variableScope.put( "second", new TestMark( 10, true ) );

        assertThat( variableScope.get( "first" ), hasIndex( 1 ) );
        assertThat( variableScope.get( "second" ), hasIndex( 10 ) );
        assertThat( variableScope.get( "other" ), nullValue() );

        // make the root scope be in the top
        variableScope.swapScopes();

        // after swapping the stack, the active scope becomes the root scope
        assertThat( variableScope.get( "first" ), hasIndex( 1 ) );
        assertThat( variableScope.get( "second" ), nullValue() );
        assertThat( variableScope.get( "other" ), nullValue() );

        // turn scopes back to normal
        variableScope.swapScopes();

        assertThat( variableScope.get( "first" ), hasIndex( 1 ) );
        assertThat( variableScope.get( "second" ), hasIndex( 10 ) );
        assertThat( variableScope.get( "other" ), nullValue() );
    }

    @Test
    public void canDuplicateScope() {
        VariableScope<TestMark> variableScope = new VariableScope<>();

        variableScope.put( "first", new TestMark( 1, true ) );

        variableScope.duplicateScope();

        assertThat( variableScope.get( "first" ), hasIndex( 1 ) );

        variableScope.get( "first" ).index = 36;
        assertThat( variableScope.get( "first" ), hasIndex( 36 ) );

        variableScope.exitScope();

        // we cannot affect a parent scope when we duplicate a scope, rather than merely "entering" it
        assertThat( variableScope.get( "first" ), hasIndex( 1 ) );
    }
}
