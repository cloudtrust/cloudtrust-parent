package io.cloudtrust.tests;

import org.junit.jupiter.api.Assertions;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class comes from https://gist.github.com/mjpitz/79ac7bfe7d5f6f064b38
 * <p>
 * Automates JUnit testing of simple getter / setter methods.
 *
 * <p>
 * This class was modeled after the {@link EqualsVerifier} approach where in a
 * few lines of code, you can test the entirety of a simple Java object. For
 * example:
 * </p>
 *
 * <pre>
 * GetterSetterVerifier.forClass(MyClass.class).verify();
 * </pre>
 *
 * <p>
 * You can also specify which properties you do no want to test in the event
 * that the associated getters and setters are non-trivial. For example:
 * </p>
 *
 * <pre>
 * GetterSetterVerifier.forClass(MyClass.class).exclude("someComplexProperty").exclude("anotherComplexProperty")
 * 		.verify();
 * </pre>
 *
 * <p>
 * On the other hand, if you'd rather be more verbose about what properties are
 * tested, you can specify them using the include syntax. When using the include
 * approach, only the properties that you specified will be tested. For example:
 * </p>
 *
 * <pre>
 * GetterSetterVerifier.forClass(MyClass.class).include("someSimpleProperty").include("anotherSimpleProperty").verify();
 * </pre>
 * <p>
 * References: {@link http://www.jqno.nl/equalsverifier/}
 * {@link https://www.altamiracorp.com/blog/employee-posts/do-you-unit-test-getters}
 * {@link http://vcs.patapouf.org/svn/glibersat/STANdsl/STAN/src-tester/com/midwinter/junit/GetterSetterTester.java}
 */
public class GetterSetterVerifier<T> {
    private final Class<T> type;
    private final Set<String> excludes = new HashSet<>();
    private final Set<String> includes = new HashSet<>();
    private final Map<Class<?>, InstanceConstructor> constructors = new HashMap<>();

    public interface InstanceConstructor {
        Object construct();
    }

    /**
     * Creates a getter / setter verifier to test properties for a particular class.
     *
     * @param type The class that we are testing
     */
    private GetterSetterVerifier(final Class<T> type) {
        this.type = type;
    }

    public GetterSetterVerifier<T> usesDefaultConstructors() {
        final short shortValue = 2;
        return this.usesConstructor(int.class, () -> 1).usesConstructor(long.class, () -> 1L)
                .usesConstructor(double.class, () -> 1.0).usesConstructor(short.class, () -> shortValue)
                .usesConstructor(boolean.class, () -> true).usesConstructor(List.class, ArrayList::new)
                .usesConstructor(String[].class, () -> new String[0]).usesConstructor(byte[].class, () -> new byte[0]);
    }

    public GetterSetterVerifier<T> usesConstructor(final Class<?> clazz, final InstanceConstructor constructor) {
        this.constructors.put(clazz, constructor);
        return this;
    }

    /**
     * Method used to identify the properties that we are going to test. If no
     * includes are specified, then all the properties are considered for testing.
     *
     * @param include The name of the property that we are going to test.
     * @return This object, for method chaining.
     */
    public GetterSetterVerifier<T> include(final String include) {
        this.includes.add(include);
        return this;
    }

    /**
     * Method used to identify the properties that will be ignored during testing.
     * If no excludes are specified, then no properties will be excluded.
     *
     * @param exclude The name of the property that we are going to ignore.
     * @return This object, for method chaining.
     */
    public GetterSetterVerifier<T> exclude(final String exclude) {
        this.excludes.add(exclude);
        return this;
    }

    /**
     * Verify the class's getters and setters
     */
    public void verify() {
        try {
            final BeanInfo beanInfo = Introspector.getBeanInfo(this.type);
            final PropertyDescriptor[] properties = beanInfo.getPropertyDescriptors();

            for (final PropertyDescriptor property : properties) {
                if (this.shouldTestProperty(property)) {
                    this.testProperty(property);
                }
            }
        } catch (final Exception e) {
            throw new AssertionError(e.getMessage());
        }
    }

    /**
     * Determine if we need to test the property based on a few conditions. 1. The
     * property has both a getter and a setter. 2. The property was not excluded. 3.
     * The property was considered for testing.
     *
     * @param property The property that we are determining if we going to test.
     * @return True if we should test the property. False if we shouldn't.
     */
    private boolean shouldTestProperty(final PropertyDescriptor property) {
        if (property.getWriteMethod() == null || property.getReadMethod() == null || this.excludes.contains(property.getDisplayName())) {
            return false;
        }

        return this.includes.isEmpty() || this.includes.contains(property.getDisplayName());
    }

    /**
     * Test an individual property by getting the read method and write method and
     * passing the default value for the type to the setter and asserting that the
     * same value was returned.
     *
     * @param property The property that we are testing.
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws InvocationTargetException
     */
    private void testProperty(final PropertyDescriptor property)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        final Object target = this.constructInstance(this.type);
        final Object setValue = this.constructInstance(property.getPropertyType());

        final Method getter = property.getReadMethod();
        final Method setter = property.getWriteMethod();

        setter.invoke(target, setValue);
        final Object getValue = getter.invoke(target);

        Assertions.assertEquals(setValue, getValue, property.getDisplayName() + " getter / setter do not produce the same result.");
    }

    private Object constructInstance(final Class<?> type)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        final InstanceConstructor constructor = this.constructors.get(type);
        return constructor == null ? type.getDeclaredConstructor().newInstance() : constructor.construct();
    }

    /**
     * Factory method for easily creating a test for the getters and setters.
     *
     * @param type The class that we are testing the getters and setters for.
     * @return An object that can be used for testing the getters and setters of a
     * class.
     */
    public static <T> GetterSetterVerifier<T> forClass(final Class<T> type) {
        return new GetterSetterVerifier<>(type);
    }
}