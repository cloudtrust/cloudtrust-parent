package io.cloudtrust.tests;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;

/**
 * This class is used to check default behavior for Exception classes. It is supposed to execute some basic checks to avoid writing tests just for coverage
 *
 * @author fpe
 */
public class ExceptionVerifier {
    interface ReflectConstructor<T extends Exception> {
        T construct(Constructor<T> constructor) throws InvocationTargetException, IllegalAccessException, InstantiationException;
    }

    public static void verify(Class<? extends Exception> clazz) {
        String message = "the message";
        Throwable cause = new Throwable("cause");

        forConstructor(clazz, cl -> cl.newInstance(message), e -> Assertions.assertEquals(message, e.getMessage()), String.class);
        forConstructor(clazz, cl -> cl.newInstance(cause), e -> Assertions.assertEquals(cause, e.getCause()), Throwable.class);
        forConstructor(clazz, cl -> cl.newInstance(message, cause), e -> {
            Assertions.assertEquals(message, e.getMessage());
            Assertions.assertEquals(cause, e.getCause());
        }, String.class, Throwable.class);
    }

    private static <T extends Exception> void forConstructor(Class<T> clazz, ReflectConstructor<T> constructor, Consumer<T> validator, Class<?>... parameterTypes) {
        try {
            Constructor<T> constructorClazz = clazz.getConstructor(parameterTypes);
            T exception = constructor.construct(constructorClazz);
            validator.accept(exception);
        } catch (NoSuchMethodException | SecurityException e) {
            // Ignore non-existing constructor
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
            Assertions.fail("Could not construct exception: " + e.getMessage());
        }
    }
}
