package io.cloudtrust.tests;

import io.cloudtrust.tests.GetterSetterVerifier.InstanceConstructor;
import org.junit.Test;

public class GetterSetterVerifierTest {
    public static class NoDefaultConstrutor {
        public NoDefaultConstrutor(int unused) {
        }
    }

    private static final InstanceConstructor ndcConstructor = () -> new NoDefaultConstrutor(0);

    public static class ValidBean {
        private int integer;
        private String string;
        private NoDefaultConstrutor noDefConstructor;

        public int getInteger() {
            return integer;
        }

        public void setInteger(int integer) {
            this.integer = integer;
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public NoDefaultConstrutor getNoDefaultConstrutor() {
            return noDefConstructor;
        }

        public void setNoDefaultConstrutor(NoDefaultConstrutor noDefConstrutor) {
            this.noDefConstructor = noDefConstrutor;
        }
    }

    public static class InvalidBean extends ValidBean {
        public void setInteger(int integer) {
        }
    }

    @Test(expected = AssertionError.class)
    public void validBeanFailureTest() {
        GetterSetterVerifier.forClass(ValidBean.class).usesDefaultConstructors().verify();
    }

    @Test
    public void validBeanSuccessTest() {
        GetterSetterVerifier.forClass(ValidBean.class).usesDefaultConstructors().usesConstructor(NoDefaultConstrutor.class, ndcConstructor).verify();
    }

    @Test(expected = AssertionError.class)
    public void invalidBeanFailureTest() {
        GetterSetterVerifier.forClass(InvalidBean.class).usesDefaultConstructors().verify();
    }

    @Test
    public void invalidBeanSuccessTest() {
        GetterSetterVerifier.forClass(InvalidBean.class).usesDefaultConstructors().include("string").verify();
        GetterSetterVerifier.forClass(InvalidBean.class).usesDefaultConstructors().exclude("integer").usesConstructor(NoDefaultConstrutor.class, ndcConstructor).verify();
        GetterSetterVerifier.forClass(InvalidBean.class).usesDefaultConstructors().include("string").include("character").usesConstructor(NoDefaultConstrutor.class, ndcConstructor).verify();
    }
}
