# Switchfly Deliverables

## Contents

Here is a brief overview of the contents of this folder:
<pre>
> Deliverables
	> Create_Rotate_Keys
		Keys.java
		KeysConfig.java
		README.md
	> Example
		Demo.java
		DemoConfig.java
		README.md
	> KMS_Crypto
		KmsCrypto.java
		KmsCryptoConfig.java
		README.md
	> lib
		javax.activation-1.2.0.jar
		oci-java-sdk-full-1.12.0.jar
		postgresql-42.2.9.jar
		slf4j-jdk14-1.7.30.jar
		> third-party
			> lib
				[41 files]
	> Postgres_Storage
		PostgresStore.java
		README.md
	README.md [this file]
	run.sh
</pre>

All projects contain a `README.md` file that shows how to compile the code with the OCI Java SDK.

- Create\_Rotate\_Keys

	This project shows how to create and rotate keys to use in your virtual vault, among other key management tasks.

- KMS\_Crypto

	This project shows how to call the cryptographic endpoints of your vault: encrypt, decrypt, and generateDataEncryptionKey.

- Postgres\_Storage

	This project shows how to store your payload into a Postgres table.

- Example

	This project bundles all three individual projects to illustrate this workflow:

	- A key is created in the vault.
	- A String payload is encrypted using this vault key.
	- This information is sent to a Postgres table.
	- Another key is created in the vault.
	- The same String payload is encrypted using the second vault key. This informatio nis sent to a Postgres table.
	- The encrypted data is queried from the PSQL table.
	- Both payloads are decrypted using both vault keys, with their decrypted base-64 strings displayed.
	- The first vault key is rotated.
	- The String payload is encrypted, then decrypted using the rotated vault key.
	- Both keys are scheduled for deletion.
	- The contents of the PSQL table are shown.

# Required setup

Please read all of the following to understand what infrastructure you require before moving forward.

## Java

Please install the latest version of Java. You can check to see if you have java installed by running `which java` and `java --version`.

## The Java SDK

This step is optional, as the required .jar file is included in this folder. However, the full SDK also contains example .java test programs that you can test your infrastructure/implementations against.
[Download the Java SDK here.](https://github.com/oracle/oci-java-sdk/releases) This project was tested using version 1.12.0, but feel free to download the most updated version. Download the zip file (this should be about 170-190 MB). The zip file contains the required JAR files, third-party libraries, shaded libraries, and example test files that demonstrate all functionality for a particular API.

## Bash terminal

If you are using Mac/Linux OS, please use the existing [bash] terminal for commands down the line. If you are using Windows OS, please download [Git Bash via Git](https://git-scm.com/downloads); you may instead use your Bash terminal of choice.

## Infrastructure

You will need the following infrastructure in order to run these demos:

- Cloud Tenancy
- Virtual Vault (WARNING: the virtual private vault is expensive)
	- Associated policies for managing the vault
- Virtual Cloud Network + Compute Instance (only if you want to use an Instance Principal instead of a Configuration File to authenticate your API requests)

Please refer to Oracle Docs or your contact(s) in Oracle Cloud on how to create the Virtual Vault and the policies required to manage the keys in the vault.

## Authentication

OCI requires a form of authentication in order for an app to make API calls. Here, the authentication can take the form of an instance principal or a config file.

### Instance Principal (recommended)

An Instance Principal is a name given to a specific Compute Instance that indicates it is able to call services, include make API calls. See [this link](https://docs.cloud.oracle.com/iaas/Content/Identity/Tasks/callingservicesfrominstances.htm?Highlight=instance%20principal). As such, you will need to create a compute instance if you have not done so already (image/shape specifications not important). Make sure you have at least one public subnet with a public IP address.

Navigate to your OCI console. In the left hamburger menu, scroll all the way down to "Identity", then select "Dynamic Group". If you do not see "Identity" but rather "Users, Account, Monitoring, Notifications", then scroll up and select "Compute" to bring you from the platform service console to the infrastructure service console. **NOTE**: the OCI console might be updated in the future such that this quick-fix no longer works, if so please contact your Oracle tech point of contact.

Create a dynamic group. You will need to build a single matching rule of the following form:
```
ALL {instance.id = 'YOUR_OCID'[, additional rules if you want]}
```

Finally, you will need to deploy all code to your compute instance. You can do this using the `scp` command in your bash terminal as shown below:
```
scp Deliverables.zip opc@YOUR_IP_ADDRESS:~
```

### Config File

**WARNING**: This method requires you to locally manage additional keys and store sensitive information, such as your API key fingerprint and various OCIDs.

The Config File contains all necessary information for your Java app to make authenticated API calls. Generally these files are stored in the folder `~/.oci`. Refer to [this link](https://docs.cloud.oracle.com/iaas/Content/API/Concepts/sdkconfig.htm) on how to build a config file for use with OCI tools.

Here is an example of a valid config file for all deliverables, with confidential information replaced with a regexp or example values. Note that it does not require a passphrase or an admin user. The file name is simply called `config`, i.e. with full file path `~/.oci/config`.

```
[DEFAULT]
user=ocid1.user.oc1..[a-z0-9]{60}
fingerprint=12:34:56:78:90:ab:cd:ef:12:34:56:78:90:ab:cd:ef
key_file=~/.oci/oci_api_key.pem
tenancy=ocid1.tenancy.oc1..[a-z0-9]{60}
region=us-phoenix-1
```

## API Key

If you choose to authenticate your application using a Config File, you will need to create PEM keys so that your application can verify your API calls; otherwise, skip this step. [Here is a guide](https://docs.cloud.oracle.com/iaas/Content/Functions/Tasks/functionssetupapikey.htm) on how to create the keys, but very briefly this is what you need to do:

- Create the `~/.oci` directory in which to store your keys:
```
cd
mkdir .oci
cd .oci
```
The `.oci` folder is the default folder that all OCI-related local applications reference. It is possible the second command may fail, indicating the `~/.oci` folder already exists.
- Once you have navigated to the `~/.oci` folder as in above, run these commands to generate the keys:
```
openssl genrsa -out private.pem 2048
openssl rsa -pubout -in private.pem -out public.pem
cat public.pem
```
The third command prints the public key to the terminal. You can then highlight the key and copy it. Do not use **Ctrl+C** as this is the interrupt command in Bash; right-click and copy or use **Command+C** for Mac users only.
**Note**: the linked guide above suggests that you add the option `-aes128` to the first command after the filename. This will prompt you for a password every time the private key is opened or used in a command, for instance in the second command above.
- Navigate to [cloud.oracle.com](cloud.oracle.com), login to your tenancy, and navigate to the **User Settings** in your profile (upper right hand corner)
- Click **Add Public Key** and paste the public key you copied earlier.
**Note**: you can have at most 3 API keys.
- You will get a corresponding MD5 fingerprint for use in your config file (see above).

# Important information to keep track of

Please make sure you have the following information at hand:

- Cloud tenancy name
- Cloud tenancy OCID
	- has regexp `ocid1.tenancy.oc1..[a-z0-9]{60}`
- Cloud user name
- Cloud user OCID
	- has regexp `ocid1.user.oc1..[a-z0-9]{60}`
- Cloud user password
- Active public API key MD5 fingerprint
	- looks something like `12:34:56:78:90:ab:cd:ef:12:34:56:78:90:ab:cd:ef`
- Virtual vault name
- Virtual vault OCID
	- has form `ocid1.vault.oc1.[REGION_CODE].[VAULT_IDENTIFIER].[a-z0-9]{60}`
- Cloud compartment name
- Cloud compartment OCID
	- has regexp `ocid1.compartment.oc1..[a-z0-9]{60}`
- Vault key name
- Vault key OCID
	- has form `ocid1.key.oc1.[REGION_CODE].[VAULT_IDENTIFIER].[a-z0-9]{60}`

# How to run

The script `run.sh` allows you to run any project in this compendium of deliverables. Run these commands:
```
./run.sh PROGRAM_ID
```
where `PROGRAM_ID` is one of these seven keywords: `key key\_config kms kms\_config postgres example example\_config`. View the code in the script for all valid arguments. For more information, please view the relevant README file for whichever project you would like to run.

# Helpful Links

- Downloads:
	- [OCI Java SDK download](https://github.com/oracle/oci-java-sdk/releases) (download the Zip file)
	- [SLF4J jar download](http://repo2.maven.org/maven2/org/slf4j/slf4j-jdk14/1.7.30/slf4j-jdk14-1.7.30.jar) (direct download from http://repo2.maven.org/maven2/org/slf4j/slf4j-jdk14/1.7.30/)
	- [javax jar download](https://jar-download.com/artifacts/com.sun.activation/javax.activation/1.2.0/source-code)
	- [Postgresql jar download](https://jdbc.postgresql.org/download/postgresql-42.2.9.jar) (direct download from https://jdbc.postgresql.org/download.html)
- [Oradocs on OCI Java SDK](https://docs.cloud.oracle.com/iaas/Content/API/SDKDocs/javasdk.htm)
- [Overview of Key Management](https://docs.cloud.oracle.com/iaas/Content/KeyManagement/Concepts/keyoverview.htm)
- [Oradocs on KMS Cryptographic Endpoints](https://docs.cloud.oracle.com/iaas/Content/KeyManagement/Tasks/usingkeys.htm#cli)
- [Creating an API Key](https://docs.cloud.oracle.com/iaas/Content/Functions/Tasks/functionssetupapikey.htm)
- [Calling Services from Instances and Setting up Instance Principals](https://docs.cloud.oracle.com/iaas/Content/Identity/Tasks/callingservicesfrominstances.htm?Highlight=instance%20principal)

