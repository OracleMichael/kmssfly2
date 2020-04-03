# Key Management Service: Cryptographic Endpoints

This simple example demonstrates how to use the OCI Java SDK to call the cryptographic endpoints of a vault on OCI. The cryptographic endpoints are `encrypt`, `decrypt`, and `generateDataEncryptionKey`.

# Prerequisites

For additional information, see `README.md` in the parent folder Deliverables.

- Java SDK is downloaded
- Bash terminal is available to use
- Infrastructure is set up properly
- Authentication is set up properly:
	- Instance Principal: dynamic group and policies are created
	- Config file: config file is set up properly and API Key is added to your cloud account

It is assumed the file structure looks something like this:
<pre>
> Deliverables
	> KMS_Crypto
		KmsCrypto.java
		KmsCryptoConfig.java
		README.md [this file]
	> [other projects]
	[other files]
	> lib
		javax.activation-1.2.0.jar
		oci-java-sdk-full-1.12.0.jar
		postgresql-42.2.9.jar
		slf4j-jdk14-1.7.30.jar
		> third-party
			> lib
				[41 files]
</pre>

IMPORTANT: You will need to set the `vaultId`, `keyId`, and `region` variables on line 44-46 of both `KmsCrypto.java` and `KmsCryptoConfig.java`.

# How to run

- Open your Bash terminal.
- Navigate to the top directory (in this case, Deliverables). If the top directory is nested under "~/Desktop", then you would run:
```
cd ~/Desktop/Deliverables
```
- Run these commands **if using an instance principal**:
the first compiles the Java file, and the second runs the program. You will see some output that will verify that the program is working.
```
javac -cp lib/*.jar:lib/third-party/lib/* KMS_Crypto/KmsCrypto.java
java -cp KMS_Crypto:lib/*.jar:lib/third-party/lib/* KmsCrypto
```
- Run these commands **if using a config file**:
```
javac -cp lib/*.jar:lib/third-party/lib/* KMS_Crypto/KmsCryptoConfig.java
java -cp KMS_Crypto:lib/*.jar:lib/third-party/lib/* KmsCryptoConfig
```
