package io.cloudtrust.keycloak;

import org.junit.jupiter.api.Test;
import org.keycloak.credential.CredentialModel;

import io.cloudtrust.tests.GetterSetterVerifier;

class CloudtrustRegisteringContextTest {
	@Test
	void pureCoverage() {
		GetterSetterVerifier
			.forClass(CloudtrustRegisteringContext.class)
			.usesDefaultConstructors()
			.usesConstructor(CredentialModel.class, CredentialModel::new)
			.verify();
	}
}
