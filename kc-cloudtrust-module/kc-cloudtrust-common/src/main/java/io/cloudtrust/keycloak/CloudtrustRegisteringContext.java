package io.cloudtrust.keycloak;

import org.keycloak.credential.CredentialModel;

import java.util.List;

public class CloudtrustRegisteringContext {
    private List<AlternativeAuthenticator> authenticationSelections;
    private CredentialModel selectedCredential;

    public List<AlternativeAuthenticator> getAlternatives() {
        return this.authenticationSelections;
    }

    public void setAlternatives(List<AlternativeAuthenticator> authenticationSelections) {
        this.authenticationSelections = authenticationSelections;
    }

    public CredentialModel getSelectedCredential() {
        return this.selectedCredential;
    }

    public void setSelectedCredential(CredentialModel credential) {
        this.selectedCredential = credential;
    }
}
