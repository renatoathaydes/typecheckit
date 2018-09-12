package com.typecheckit;

import com.typecheckit.util.Mark;
import com.typecheckit.util.ScopeStack;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;

import static com.typecheckit.TestUtils.shouldThrow;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class ScopeStackTest {

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
        public void merge( TestMark mark ) {
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
        ScopeStack<TestMark> scopeStack = new ScopeStack<>();

        // there should always be a default scope we can add variables to
        scopeStack.put( "test", new TestMark( 1, true ) );

        scopeStack.enterScope( BlockKind.OTHER );

        // child scope should inherit parent's variables
        assertThat( scopeStack.get( "test" ), hasIndex( 1 ) );
        assertThat( scopeStack.get( "other" ), nullValue() );

        scopeStack.put( "child", new TestMark( 33, true ) );

        assertThat( scopeStack.get( "test" ), hasIndex( 1 ) );
        assertThat( scopeStack.get( "child" ), hasIndex( 33 ) );
        assertThat( scopeStack.get( "other" ), nullValue() );

        scopeStack.enterScope( BlockKind.OTHER );

        // child scope should inherit parent's variables
        assertThat( scopeStack.get( "test" ), hasIndex( 1 ) );
        assertThat( scopeStack.get( "child" ), hasIndex( 33 ) );
        assertThat( scopeStack.get( "other" ), nullValue() );

        scopeStack.put( "grand-child", new TestMark( 42, true ) );

        assertThat( scopeStack.get( "test" ), hasIndex( 1 ) );
        assertThat( scopeStack.get( "child" ), hasIndex( 33 ) );
        assertThat( scopeStack.get( "grand-child" ), hasIndex( 42 ) );
        assertThat( scopeStack.get( "other" ), nullValue() );

        // modify variable that is present in parent scope
        scopeStack.get( "child" ).index = 100;

        scopeStack.exitScope();

        // we're back at the parent scope, with a modified child
        assertThat( scopeStack.get( "test" ), hasIndex( 1 ) );
        assertThat( scopeStack.get( "child" ), hasIndex( 100 ) );
        assertThat( scopeStack.get( "grand-child" ), nullValue() );
        assertThat( scopeStack.get( "other" ), nullValue() );

        scopeStack.exitScope();

        // back to root scope
        assertThat( scopeStack.get( "test" ), hasIndex( 1 ) );
        assertThat( scopeStack.get( "child" ), nullValue() );
        assertThat( scopeStack.get( "grand-child" ), nullValue() );
        assertThat( scopeStack.get( "other" ), nullValue() );
    }

    @Test
    public void canEnterAndExitScopesWithIndependentParentScope() {
        ScopeStack<TestMark> scopeStack = new ScopeStack<>();

        // there should always be a default scope we can add variables to
        scopeStack.put( "test", new TestMark( 1, false ) );

        scopeStack.enterScope( BlockKind.OTHER );

        // child scope should inherit parent's variables
        assertThat( scopeStack.get( "test" ), hasIndex( 2 ) );
        assertThat( scopeStack.get( "other" ), nullValue() );

        scopeStack.put( "child", new TestMark( 33, false ) );

        assertThat( scopeStack.get( "test" ), hasIndex( 2 ) );
        assertThat( scopeStack.get( "child" ), hasIndex( 33 ) );
        assertThat( scopeStack.get( "other" ), nullValue() );

        scopeStack.enterScope( BlockKind.OTHER );

        // child scope should inherit parent's variables
        assertThat( scopeStack.get( "test" ), hasIndex( 3 ) );
        assertThat( scopeStack.get( "child" ), hasIndex( 34 ) );
        assertThat( scopeStack.get( "other" ), nullValue() );

        scopeStack.put( "grand-child", new TestMark( 42, false ) );

        assertThat( scopeStack.get( "test" ), hasIndex( 3 ) );
        assertThat( scopeStack.get( "child" ), hasIndex( 34 ) );
        assertThat( scopeStack.get( "grand-child" ), hasIndex( 42 ) );
        assertThat( scopeStack.get( "other" ), nullValue() );

        // modify variable that is present in parent scope
        scopeStack.get( "child" ).index = 100;

        scopeStack.exitScope();

        // we're back at the parent scope, no children are modified
        assertThat( scopeStack.get( "test" ), hasIndex( 2 ) );
        assertThat( scopeStack.get( "child" ), hasIndex( 33 ) );
        assertThat( scopeStack.get( "grand-child" ), nullValue() );
        assertThat( scopeStack.get( "other" ), nullValue() );

        scopeStack.exitScope();

        // back to root scope
        assertThat( scopeStack.get( "test" ), hasIndex( 1 ) );
        assertThat( scopeStack.get( "child" ), nullValue() );
        assertThat( scopeStack.get( "grand-child" ), nullValue() );
        assertThat( scopeStack.get( "other" ), nullValue() );
    }

    @Test
    public void canSwapScopes() {
        ScopeStack<TestMark> scopeStack = new ScopeStack<>();

        scopeStack.put( "first", new TestMark( 1, true ) );

        scopeStack.enterScope( BlockKind.OTHER );

        scopeStack.put( "second", new TestMark( 10, true ) );

        assertThat( scopeStack.get( "first" ), hasIndex( 1 ) );
        assertThat( scopeStack.get( "second" ), hasIndex( 10 ) );
        assertThat( scopeStack.get( "other" ), nullValue() );

        // make the root scope be in the top
        scopeStack.swapScopes();

        // after swapping the stack, the active scope becomes the root scope
        assertThat( scopeStack.get( "first" ), hasIndex( 1 ) );
        assertThat( scopeStack.get( "second" ), nullValue() );
        assertThat( scopeStack.get( "other" ), nullValue() );

        // turn scopes back to normal
        scopeStack.swapScopes();

        assertThat( scopeStack.get( "first" ), hasIndex( 1 ) );
        assertThat( scopeStack.get( "second" ), hasIndex( 10 ) );
        assertThat( scopeStack.get( "other" ), nullValue() );
    }

    @Test
    public void canDuplicateScope() {
        ScopeStack<TestMark> scopeStack = new ScopeStack<>();

        scopeStack.put( "first", new TestMark( 1, true ) );

        scopeStack.duplicateScope();

        assertThat( scopeStack.get( "first" ), hasIndex( 1 ) );

        scopeStack.get( "first" ).index = 36;
        assertThat( scopeStack.get( "first" ), hasIndex( 36 ) );

        scopeStack.exitScope();

        // we cannot affect a parent scope when we duplicate a scope, rather than merely "entering" it
        assertThat( scopeStack.get( "first" ), hasIndex( 1 ) );
    }

    @Test
    public void cannotExitRootScope() {
        ScopeStack<TestMark> scopeStack = new ScopeStack<>();
        IllegalStateException error = shouldThrow( IllegalStateException.class, scopeStack::exitScope );
        assertThat( error.getMessage(), equalTo( "Cannot exit the root scope" ) );

        scopeStack.enterScope( BlockKind.OTHER );
        scopeStack.exitScope();

        error = shouldThrow( IllegalStateException.class, scopeStack::exitScope );
        assertThat( error.getMessage(), equalTo( "Cannot exit the root scope" ) );
    }

}
