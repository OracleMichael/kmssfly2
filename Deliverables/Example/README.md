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


need to create a postgres table called "encrypteddataDemo".
assumptions:
- user called "postgres" is created on instance principal
	- enter as this user via `sudo su postgres`; should bring you to `bash-4.2$ `
- postgresql is installed on instance principal (need to get specifics from darshan)
then you can do this to create the postgres:
```
yourhomelaptop:~$ ssh opc@whatever
instance:~$ sudo su postgres
bash-4.2$ psql postgres
could not change directory to "/home/opc/Deliverables"
psql (9.2.24, server 12.1)
WARNING: psql version 0.2, server version 12.0.
         Some psql features might not work.
Type "help" for help.

postgres=# \c kms
kms=# CREATE TABLE encrypteddataDemo (
kms(# id VARCHAR(255),
kms(# payload VARCHAR(255)
kms(# );
kms=# SELECT * FROM public.encrypteddataDemo;
 id | payload
----+---------
(0 rows)

kms=# \q
bash-4.2$ exit
exit
instance:~$ ./Deliverables/run.sh postgres
[LOTS OF DATA OMITTED]
Done.

logout
yourhomelaptop:~$
```
