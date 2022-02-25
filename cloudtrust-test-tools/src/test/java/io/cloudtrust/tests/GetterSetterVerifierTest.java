package io.cloudtrust.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.cloudtrust.tests.GetterSetterVerifier.InstanceConstructor;

class GetterSetterVerifierTest {
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

    @Test
    void validBeanFailureTest() {
    	GetterSetterVerifier<ValidBean> verifier = GetterSetterVerifier.forClass(ValidBean.class).usesDefaultConstructors();
        Assertions.assertThrows(AssertionError.class, () -> verifier.verify());
    }

    @Test
    void validBeanSuccessTest() {
        GetterSetterVerifier.forClass(ValidBean.class).usesDefaultConstructors().usesConstructor(NoDefaultConstrutor.class, ndcConstructor).verify();
    }

    @Test
    void invalidBeanFailureTest() {
    	GetterSetterVerifier<InvalidBean> verifier = GetterSetterVerifier.forClass(InvalidBean.class).usesDefaultConstructors();
    	Assertions.assertThrows(AssertionError.class, () -> verifier.verify());
    }

    @Test
    void invalidBeanSuccessTest() {
        GetterSetterVerifier.forClass(InvalidBean.class).usesDefaultConstructors().include("string").verify();
        GetterSetterVerifier.forClass(InvalidBean.class).usesDefaultConstructors().exclude("integer").usesConstructor(NoDefaultConstrutor.class, ndcConstructor).verify();
        GetterSetterVerifier.forClass(InvalidBean.class).usesDefaultConstructors().include("string").include("character").usesConstructor(NoDefaultConstrutor.class, ndcConstructor).verify();
    }
}
