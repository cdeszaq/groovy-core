/*
 * Copyright 2003-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovy.transform.stc

import org.codehaus.groovy.control.customizers.CompilationCustomizer
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.transform.stc.StaticTypesMarker
import org.codehaus.groovy.ast.ClassHelper

/**
 * Unit tests for static type checking : generics.
 *
 * @author Cedric Champeau
 */
class GenericsSTCTest extends StaticTypeCheckingTestCase {

    void testDeclaration() {
        assertScript '''
            List test = new LinkedList<String>()
        '''
    }

    void testDeclaration5() {
        assertScript '''
            Map<String,Integer> obj = new HashMap<String,Integer>()
        '''
    }

    void testDeclaration6() {
        shouldFailWithMessages '''
            Map<String,String> obj = new HashMap<String,Integer>()
        ''', 'Incompatible generic argument types. Cannot assign java.util.HashMap <String, Integer> to: java.util.Map <String, String>'
    }

    void testAddOnList() {
        shouldFailWithMessages '''
            List<String> list = []
            list.add(1)
        ''', "Cannot find matching method java.util.List#add(int)"
    }

    void testAddOnList2() {
        assertScript '''
            List<String> list = []
            list.add 'Hello'
        '''

        assertScript '''
            List<Integer> list = []
            list.add 1
        '''
    }

    void testAddOnListWithDiamond() {
        assertScript '''
            List<String> list = new LinkedList<>()
            list.add 'Hello'
        '''
    }
    
    void testAddOnListUsingLeftShift() {
        shouldFailWithMessages '''
            List<String> list = []
            list << 1
        ''', 'Cannot find matching method java.util.List#leftShift(int)'
    }

    void testAddOnList2UsingLeftShift() {
        assertScript '''
            List<String> list = []
            list << 'Hello'
        '''

        assertScript '''
            List<Integer> list = []
            list << 1
        '''
    }

    void testAddOnListWithDiamondUsingLeftShift() {
        assertScript '''
            List<String> list = new LinkedList<>()
            list << 'Hello'
        '''
    }

    void testListInferrenceWithNullElems() {
        assertScript '''
            List<String> strings = ['a', null]
            assert strings == ['a',null]
        '''
    }

    void testListInferrenceWithAllNullElems() {
        assertScript '''
            List<String> strings = [null, null]
            assert strings == [null,null]
        '''
    }

    void testAddOnListWithDiamondAndWrongType() {
        shouldFailWithMessages '''
            List<Integer> list = new LinkedList<>()
            list.add 'Hello'
        ''', 'Cannot find matching method java.util.LinkedList#add(java.lang.String)'
    }

    void testAddOnListWithDiamondAndWrongTypeUsingLeftShift() {
        shouldFailWithMessages '''
            List<Integer> list = new LinkedList<>()
            list << 'Hello'
        ''', 'Cannot find matching method java.util.LinkedList#leftShift(java.lang.String)'
    }

    void testAddOnListWithDiamondAndNullUsingLeftShift() {
        assertScript '''
            List<Integer> list = new LinkedList<>()
            list << null
        '''
    }

    void testReturnTypeInference() {
        assertScript '''
            class Foo<U> {
                U method() { }
            }
            Foo<Integer> foo = new Foo<Integer>()
            Integer result = foo.method()
        '''
    }

    void testReturnTypeInferenceWithDiamond() {
        assertScript '''
            class Foo<U> {
                U method() { }
            }
            Foo<Integer> foo = new Foo<>()
            Integer result = foo.method()
        '''
    }

    void testReturnTypeInferenceWithMethodGenerics() {
        assertScript '''
            List<Long> list = Arrays.asList([0L,0L] as Long[])
        '''
    }

    void testReturnTypeInferenceWithMethodGenericsAndVarArg() {
        assertScript '''
            List<Long> list = Arrays.asList(0L,0L)
        '''
    }

    void testDiamondInferrenceFromConstructor() {
        assertScript '''
            Set< Long > s2 = new HashSet<>()
        '''
    }

    void testDiamondInferrenceFromConstructorWithoutAssignment() {
        assertScript '''
            new HashSet<>(Arrays.asList(0L,0L));
        '''
    }

    void testDiamondInferrenceFromConstructor2() {
        shouldFailWithMessages '''
            Set< Number > s3 = new HashSet<>(Arrays.asList(0L,0L));
        ''', 'Cannot assign java.util.HashSet <java.lang.Long> to: java.util.Set <Number>'
    }

    void testDiamondInferrenceFromConstructor3() {
        assertScript '''
            Set<Number> s4 = new HashSet<Number>(Arrays.asList(0L,0L))
        '''
    }


    void testLinkedListWithListArgument() {
        assertScript '''
            List<String> list = new LinkedList<String>(['1','2','3'])
        '''
    }

    void testLinkedListWithListArgumentAndWrongElementTypes() {
        shouldFailWithMessages '''
            List<String> list = new LinkedList<String>([1,2,3])
        ''', 'Cannot find matching method java.util.LinkedList#<init>(java.util.List <java.lang.Integer>)'
    }

    void testCompatibleGenericAssignmentWithInferrence() {
        shouldFailWithMessages '''
            List<String> elements = ['a','b', 1]
        ''', 'Incompatible generic argument types. Cannot assign java.util.List <java.io.Serializable> to: java.util.List <String>'
    }

    void testGenericAssignmentWithSubClass() {
        assertScript '''
            List<String> list = new groovy.transform.stc.GenericsSTCTest.MyList()
        '''
    }

    void testGenericAssignmentWithSubClassAndWrongGenericType() {
        shouldFailWithMessages '''
            List<Integer> list = new groovy.transform.stc.GenericsSTCTest.MyList()
        ''', 'Incompatible generic argument types'
    }

    void testAddShouldBeAllowedOnUncheckedGenerics() {
        assertScript '''
            List list = []
            list.add 'Hello'
            list.add 2
            list.add 'the'
            list.add 'world'
        '''
    }

    void testAssignmentShouldFailBecauseOfLowerBound() {
        shouldFailWithMessages '''
            List<? super Number> list = ['string']
        ''', 'Number'
    }

    void testGroovy5154() {
        assertScript '''
            class Foo {
                def say() {
                    FooWithGenerics f
                    FooBound fb
                    f.say(fb)
                }
            }

            class FooWithGenerics {
                def <T extends FooBound> void say(T p) {
                }
            }
            class FooBound {
            }
            new Foo()
        '''
    }

    void testGroovy5154WithSubclass() {
        assertScript '''
            class Foo {
                def say() {
                    FooWithGenerics f
                    FooBound2 fb
                    f.say(fb)
                }
            }

            class FooWithGenerics {
                def <T extends FooBound> void say(T p) {
                }
            }
            class FooBound {
            }
            class FooBound2 extends FooBound {}
            new Foo()
        '''
    }

    void testGroovy5154WithIncorrectType() {
        shouldFailWithMessages '''
            class Foo {
                def say() {
                    FooWithGenerics f
                    Object fb
                    f.say(fb)
                }
            }

            class FooWithGenerics {
                def <T extends FooBound> void say(T p) {
                }
            }
            class FooBound {
            }
            new Foo()
        ''', 'Cannot find matching method FooWithGenerics#say(java.lang.Object)'
    }

    void testVoidReturnTypeInferrence() {
      assertScript '''
        Object m() {
          def s = '1234'
          println 'Hello'
        }
      '''
    }

    // GROOVY-5237
    void testGenericTypeArgumentAsField() {
        assertScript '''
            class Container<T> {
                private T initialValue
                Container(T initialValue) { this.initialValue = initialValue }
                T get() { initialValue }
            }
            Container<Date> c = new Container<Date>(new Date())
            long time = c.get().time
        '''
    }

    void testReturnAnntationClass() {
        assertScript '''
            import java.lang.annotation.Documented
            Documented annotation = Deprecated.getAnnotation(Documented)
        '''
    }

    void testReturnListOfParameterizedType() {
        assertScript '''
            class A {}
            class B extends A { void bar() {} }
            public <T extends A> List<T> foo() { [] }

            List<B> list = foo()
            list.add(new B())
        '''
    }

    void testMethodCallWithClassParameterUsingClassLiteralArg() {
        assertScript '''
            class A {}
            class B extends A {}
            class Foo {
                void m(Class<? extends A> clazz) {}
            }
            new Foo().m(B)
        '''
    }
  
    void testMethodCallWithClassParameterUsingClassLiteralArgWithoutWrappingClass() {
        assertScript '''
            class A {}
            class B extends A {}
            void m(Class<? extends A> clazz) {}
            m(B)
        '''
    }

    void testConstructorCallWithClassParameterUsingClassLiteralArg() {
        assertScript '''
            class A {}
            class B extends A {}
            class C extends B {}
            class Foo {
                Foo(Class<? extends A> clazz) {}
            }
            new Foo(B)
            new Foo(C)
        '''
    }

    void testConstructorCallWithClassParameterUsingClassLiteralArgAndInterface() {
        assertScript '''
            interface A {}
            class B implements A {}
            class C extends B {}
            class Foo {
                Foo(Class<? extends A> clazz) {}
            }
            new Foo(B)
            new Foo(C)
        '''
    }

    void testPutMethodWithPrimitiveValue() {
        assertScript '''
            Map<String, Integer> map = new HashMap<String,Integer>()
            map.put('hello', 1)
        '''
    }

    void testPutMethodWithWrongValueType() {
        shouldFailWithMessages '''
            Map<String, Integer> map = new HashMap<String,Integer>()
            map.put('hello', new Object())
        ''', 'Cannot find matching method java.util.HashMap#put(java.lang.String, java.lang.Object)'
    }

    void testPutMethodWithPrimitiveValueAndArrayPut() {
        assertScript '''
            Map<String, Integer> map = new HashMap<String,Integer>()
            map['hello'] = 1
        '''
    }

    void testShouldComplainAboutToInteger() {
        shouldFailWithMessages '''
            class Test {
                static test2() {
                    if (new Random().nextBoolean()) {
                        def a = new ArrayList<String>()
                        a << "a" << "b" << "c"
                        return a
                    } else {
                        def b = new LinkedList<Number>()
                        b << 1 << 2 << 3
                        return b
                    }
                }

                static test() {
                    def result = test2()
                    result[0].toInteger()
                    //result[0].toString()
                }
            }
            new Test()
        ''', 'Cannot find matching method java.io.Serializable#toInteger()'
    }

    void testAssignmentOfNewInstance() {
        assertScript '''
            class Foo {
                static Class clazz = Date
                public static void main(String... args) {
                    @ASTTest(phase=INSTRUCTION_SELECTION, value={
                        assert node.getNodeMetaData(INFERRED_TYPE) == OBJECT_TYPE
                    })
                    def obj = clazz.newInstance()
                }
            }
        '''
    }

    // GROOVY-5415
    void testShouldUseMethodGenericType1() {
        assertScript '''import groovy.transform.stc.GenericsSTCTest.ClassA
        class ClassB {
            void bar() {
                def ClassA<Long> a = new ClassA<Long>();
                a.foo(this.getClass());
            }
        }
        new ClassB()
        '''
    }
    // GROOVY-5415
    void testShouldUseMethodGenericType2() {
        shouldFailWithMessages '''import groovy.transform.stc.GenericsSTCTest.ClassA
        class ClassB {
            void bar() {
                def ClassA<Long> a = new ClassA<Long>();
                a.bar(this.getClass());
            }
        }
        new ClassB()
        ''', 'Cannot find matching method groovy.transform.stc.GenericsSTCTest$ClassA#bar'
    }

    // GROOVY-5516
    void testAddAllWithCollectionShouldBeAllowed() {
        assertScript '''
            List<String> list = ['a','b','c']
            Collection<String> e = list.findAll { it }
            list.addAll(e)
        '''
    }
    void testAddAllWithCollectionShouldNotBeAllowed() {
        shouldFailWithMessages '''
            List<String> list = ['a','b','c']
            Collection<Integer> e = (Collection<Integer>) [1,2,3]
            list.addAll(e)
        ''', 'Cannot call java.util.List#addAll(java.lang.String) with arguments [java.util.Collection <Integer>]'
    }

    // GROOVY-5528
    void testAssignmentToInterfaceFromUserClassWithGenerics() {
        assertScript '''class UserList<T> extends LinkedList<T> {}
        List<String> list = new UserList<String>()
        '''
    }

    // GROOVY-5559
    void testGStringInListShouldBeConsideredAsAString() {
        assertScript '''
        def bar = 1
        @ASTTest(phase=INSTRUCTION_SELECTION, value={
            assert node.getNodeMetaData(INFERRED_TYPE) == LIST_TYPE
            assert node.getNodeMetaData(INFERRED_TYPE).genericsTypes[0].type == STRING_TYPE
        })
        def list = ["foo", "$bar"]
        '''

        assertScript '''
        def bar = 1
        @ASTTest(phase=INSTRUCTION_SELECTION, value={
            assert node.getNodeMetaData(INFERRED_TYPE) == LIST_TYPE
            assert node.getNodeMetaData(INFERRED_TYPE).genericsTypes[0].type == STRING_TYPE
        })
        List<String> list = ["foo", "$bar"]
        '''

        assertScript '''
        def bar = 1
        @ASTTest(phase=INSTRUCTION_SELECTION, value={
            assert node.getNodeMetaData(INFERRED_TYPE) == LIST_TYPE
            assert node.getNodeMetaData(INFERRED_TYPE).genericsTypes[0].type == GSTRING_TYPE
        })
        List<String> list = ["$bar"] // single element means no LUB
        '''
    }

    // GROOVY-5559: related behaviour
    void testGStringString() {
        assertScript '''
            int i = 1
            @ASTTest(phase=INSTRUCTION_SELECTION, value={
                assert node.getNodeMetaData(INFERRED_TYPE) == GSTRING_TYPE
            })
            def str = "foo$i"
            assert str == 'foo1'
        '''
    }

    // GROOVY-5594
    void testMapEntryUsingPropertyNotation() {
        assertScript '''
        Map.Entry<Date, Integer> entry

        @ASTTest(phase=INSTRUCTION_SELECTION, value={
            assert node.getNodeMetaData(INFERRED_TYPE) == make(Date)
        })
        def k = entry?.key

        @ASTTest(phase=INSTRUCTION_SELECTION, value={
            assert node.getNodeMetaData(INFERRED_TYPE) == Integer_TYPE
        })
        def v = entry?.value
        '''
    }

    void testInferenceFromMap() {
        assertScript '''
        Map<Date, Integer> map

        @ASTTest(phase=INSTRUCTION_SELECTION, value={
            def infType = node.getNodeMetaData(INFERRED_TYPE)
            assert infType == make(Set)
            def entryInfType = infType.genericsTypes[0].type
            assert entryInfType == make(Map.Entry)
            assert entryInfType.genericsTypes[0].type == make(Date)
            assert entryInfType.genericsTypes[1].type == Integer_TYPE
        })
        def entries = map?.entrySet()
        '''
    }

    void testInferenceFromListOfMaps() {
        assertScript '''
        List<Map<Date, Integer>> maps

        @ASTTest(phase=INSTRUCTION_SELECTION, value={
            def listType = node.getNodeMetaData(INFERRED_TYPE)
            assert listType == Iterator_TYPE
            def infType = listType.genericsTypes[0].type
            assert infType == make(Map)
            assert infType.genericsTypes[0].type == make(Date)
            assert infType.genericsTypes[1].type == Integer_TYPE
        })
        def iter = maps?.iterator()
        '''
    }

    void testAssignNullMapWithGenerics() {
        assertScript '''
            Map<String, Integer> foo = null
            Integer result = foo?.get('a')
        '''
    }

    void testAssignNullListWithGenerics() {
        assertScript '''
            List<Integer> foo = null
            Integer result = foo?.get(0)
        '''
    }

    void testAssignNullListWithGenericsWithSequence() {
        assertScript '''
            List<Integer> foo = [1]
            foo = null
            Integer result = foo?.get(0)
        '''

    }

    void testMethodCallWithArgumentUsingNestedGenerics() {
        assertScript '''
           ThreadLocal<Map<Integer, String>> cachedConfigs = new ThreadLocal<Map<Integer, String>>()
           def configs = new HashMap<Integer, String>()
           cachedConfigs.set configs
        '''
    }

    void testInferDiamondUsingAIC() {
        shouldFailWithMessages '''
            Map<String,Date> map = new HashMap<>() {}
        ''', 'Cannot use diamond <> with anonymous inner classes'
    }

    static class MyList extends LinkedList<String> {}

    public static class ClassA<T> {
        public <X> Class<X> foo(Class<X> classType){
            return classType;
        }

        public <X> Class<X> bar(Class<T> classType){
            return null;
        }
    }

}

