# cloudtrust-parent

Cloudtrust parent is the parent POM for Cloudtrust components
It includes:
* cloudtrust-common: common tools for any Cloudtrust component
* cloudtrust-test-tools: common tools for unit tests of Cloudtrust components
* kc-cloudtrust-common: common tools for Keycloak-related Cloudtrust component
* kc-cloudtrust-test-tools: common tools for Keycloak-related unit tests of Cloudtrust components

To run Keycloak tests, you can use the predefined container KeycloakDeploy by requesting its instantiation when starting JUnit5 tests by using @ExtendWith(KeycloakDeploy.class).
KeycloakDeploy needs to be configured with a resource file keycloak.properties

```
# comment your file with a heading #

# Values are references to files which will be copied in $KC_HOME/providers.
# If a version is present in the filename and the file is not found,
# a file with another version will be choosen (if found).
[modules]
../keycloak-module/target/keycloak-module-0.0.0.jar

# following section allow to add libraries in the classpath
# Note that this classpath is there for a future use but has to be debugged
[classpath]
./target/org.apache.commons.lang3-5.1.2.jar

# following section will define Keycloak build arguments
# key=value will become --key=value in the build command line
[build-arguments]
dbpassword=my-db-p@ssword

# following section will define additional Keycloak execution arguments
# value will become --value in the start command line
[exec-arguments]
debug

# following section will be added to the $KC_HOME/conf/keycloak.conf
# Values are always of the form: key=value
[properties]
spi-required-action-my-identifier-my-first-property=3
spi-required-action-my-identifier-my-second-property=Code=%s

# following section will define environment variables
# you can invoke some predefined functions
# . func:base64(...) will encode a value with base64
# . func:randombytes(N) will create a random value of N bytes
# . func:file(filename.ext) will set the content of a file to a environment variable
[environment]
MY_SECRET_KEY=func:base64(func:randombytes(16))
MY_FILE_CONTENT=func:file(myfile.json)
```