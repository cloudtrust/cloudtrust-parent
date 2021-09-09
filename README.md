# cloudtrust-parent

Cloudtrust parent is the parent POM for Cloudtrust components
It includes:
* cloudtrust-common: common tools for any Cloudtrust component
* cloudtrust-test-tools: common tools for unit tests of Cloudtrust components
* kc-cloudtrust-common: common tools for Keycloak-related Cloudtrust component
* kc-cloudtrust-testsuite: common tools for Keycloak-related unit tests of Cloudtrust components

This repo needs Keycloak Arquillian Testsuite to compile. To get it, you need to perform the following command in Keycloak Project:
'''mvn install -Pdistribution,console-ui-tests -DskipTests -Dbrowser=chrome'''
