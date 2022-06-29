package io.cloudtrust.keycloak;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.cloudtrust.tests.GetterSetterVerifier;

class CloudtrustContextTest {
	@Test
	void pureCoverage() {
		GetterSetterVerifier.forClass(CloudtrustContext.class).usesDefaultConstructors().verify();
	}

	@Test
	void toStringTest() {
		String credentialId = "ze-cred-id";
		CloudtrustContext ctx = new CloudtrustContext();
		ctx.setCredentialId(credentialId);
		Assertions.assertTrue(ctx.toString().contains("\"credentialId\""));
		Assertions.assertTrue(ctx.toString().contains(credentialId));
	}
}
