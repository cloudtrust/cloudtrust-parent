package io.cloudtrust.keycloak;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.mockito.Mockito;

class AlternativeAuthenticatorTest {
	@Test
	void basicTest() {
		AuthenticationExecutionModel authExec = Mockito.mock(AuthenticationExecutionModel.class);
	    CredentialModel credential = Mockito.mock(CredentialModel.class);
	    AlternativeAuthenticator altAuthenticator = new AlternativeAuthenticator(authExec, credential);

	    Assertions.assertFalse(altAuthenticator.showCredentialType());

	    Mockito.when(credential.getId()).thenReturn(null);
	    Mockito.when(authExec.getId()).thenReturn("auth-id");
		Assertions.assertEquals("auth-id|", altAuthenticator.getId());

	    Mockito.when(credential.getId()).thenReturn("cred-id");
	    Mockito.when(authExec.getId()).thenReturn("auth-id");
		Assertions.assertEquals("auth-id|cred-id", altAuthenticator.getId());

		Mockito.when(authExec.getAuthenticator()).thenReturn("auth-name");
		Assertions.assertEquals("auth-name", altAuthenticator.getAuthExecName());

		Mockito.when(credential.getUserLabel()).thenReturn("user-label");
		Assertions.assertEquals("user-label", altAuthenticator.getCredentialName());
	}
}
