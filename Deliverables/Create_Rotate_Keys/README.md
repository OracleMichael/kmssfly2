# Key Management Service: Creating and Rotating Keys

This simple example demonstrates how to use the OCI Java SDK to create and rotate a key in a vault on OCI, among other important key management tasks.

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
	> Create_Rotate_Keys
		Keys.java
		KeysConfig.java
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

# How to run

- Open your Bash terminal.
- Navigate to the top directory (in this case, Deliverables). If the top directory is nested under "~/Desktop", then you would run:
```
cd ~/Desktop/Deliverables
```
- Run these commands **if using an instance principal**:
the first compiles the Java file, and the second runs the program. You will see some output that will verify that the program is working.
```
javac -cp lib/*.jar:lib/third-party/lib/* Create_Rotate_Keys/Keys.java
java -cp Create_Rotate_Keys:lib/*.jar:lib/third-party/lib/* Keys
```
- Run these commands **if using a config file**:
```
javac -cp lib/*.jar:lib/third-party/lib/* Create_Rotate_Keys/KeysConfig.java
java -cp Create_Rotate_Keys:lib/*.jar:lib/third-party/lib/* KeysConfig
```
