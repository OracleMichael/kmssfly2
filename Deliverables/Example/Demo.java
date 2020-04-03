import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.keymanagement.*;
import com.oracle.bmc.keymanagement.model.*;
import com.oracle.bmc.keymanagement.requests.*;
import com.oracle.bmc.keymanagement.responses.*;

import java.sql.*;

import org.apache.commons.codec.binary.Base64;

public class Demo {

	// DEFAULT_KEY_LENGTH describes the number of characters to use in the key.
	private static final int DEFAULT_KEY_LENGTH = 32;
	// TEST_KEY_SHAPE is the KeyShape used for testing.
	private static final KeyShape TEST_KEY_SHAPE = KeyShape.builder().algorithm(KeyShape.Algorithm.Aes).length(DEFAULT_KEY_LENGTH).build();
	// TEXT_TO_ENCRYPT is the plaintext to encrypt for this demo.
	private static final String TEXT_TO_ENCRYPT = "John Doe,600600600,01-01-2000,Bank of America,1234567890000000";

	// PSQL_ADDRESS is the location of the postgres server. This has been tested for a postgres server on the instance principal.
	private static final String PSQL_ADDRESS = "jdbc:postgresql://localhost:5432/kms";
	// PSQL_TABLE_NAME is the name of the table on the PSQL server. This should be created beforehand.
	private static final String PSQL_TABLE_NAME = "encrypteddatademo";

	// DELAY controls the delay for waiting for a key to become available (you cannot use the management API to modify a key that is not available).
	private static final long DELAY = 10L; // 10 second delay
	// TIMEOUT is the maximum total time to wait for the key to become available.
	private static final long TIMEOUT = 120L; // 2 minute timeout

	public static void main(final String[] args) throws Exception {
		/* These three variables will be passed in via command line. Refer to the README for more details.
			* compartmentId: the OCID of the compartment in which your key will be created in the createKeyTest.
			* vaultId: the OCID of the virtual vault you are using
			* region: the region in which the vault was created
		By default, this should be the same compartment your vault is located in.*/

		if (args.length != 3) {
			throw new IllegalArgumentException("This program requires three arguments: compartmentId, vaultId, and region.");
		}
		final String compartmentId = args[0];
		final String vaultId = args[1];
		final String region = args[2];
		System.out.println(black("Received arguments:\n    compartment Id: " + compartmentId + "\n    vaultId: " + vaultId + "\n    region: " + region));/**/

		System.out.println("\nAny 404 or 401 errors are most likely due to incorrect URLs/OCIDs provided for this file: please double check these.\n");

		// Configuring the AuthenticationDetailsProvider. Here, we use an Instance Principal. It is
		// assumed that the compute instance this file resides in has associated policies that make it
		// an instance principal. See the below link for more details:
		// https://docs.cloud.oracle.com/iaas/Content/Identity/Tasks/callingservicesfrominstances.htm?Highlight=instance%20principal
		final InstancePrincipalsAuthenticationDetailsProvider provider =
			InstancePrincipalsAuthenticationDetailsProvider.builder().build();

		// Intialize the KMS clients. KMS has three management clients as follows:
		// 	* kmsVaultClient: the client for Vault Management
		// 	* kmsManagementClient: the client for Key Management
		// 	* kmsCryptoClient: the client for Cryptographic Management
		KmsVaultClient kmsVaultClient = new KmsVaultClient(provider);
		KmsManagementClient kmsManagementClient = new KmsManagementClient(provider);
		KmsCryptoClient kmsCryptoClient = new KmsCryptoClient(provider);

		// Set the region
		kmsVaultClient.setRegion(region);

		// Get vault test
		Vault vault = getVaultTest(kmsVaultClient, vaultId);

		// Set the management endpoints
		kmsManagementClient.setEndpoint(vault.getManagementEndpoint());
		kmsCryptoClient.setEndpoint(vault.getCryptoEndpoint());

		// 1: A key is created in the vault.
		System.out.println(black("1: Creating key 1..."));
		final String keyId1 = createKeyTest(kmsManagementClient, compartmentId, "SFLY_DELIVERABLES_TEST_DEMO_KEY1");
		System.out.println(black("Created key 1.\n"));

		// 2: A string payload is encrypted using this vault key.
		System.out.println(black("2: Encrypting string payload using key 1..."));
		final String ciphertext1 = encryptTest(kmsCryptoClient, keyId1, TEXT_TO_ENCRYPT);
		System.out.println(black("Encrypted payload \"" + TEXT_TO_ENCRYPT + "\".\n"));

		// 3: This information is sent to a Postgres table.
		System.out.println(black("3: Sending encrypted payload 1 to PostgreSQL table..."));
		try (Connection connection = DriverManager.getConnection(PSQL_ADDRESS, "postgres", "")) {
			System.out.println("Connected to PostgreSQL database.\nExecuting insert...");
			final PreparedStatement ps = connection.prepareStatement("INSERT INTO " + PSQL_TABLE_NAME + " (keyid, payload) VALUES ('" + keyId1 + "','" + ciphertext1 + "')");
			ps.executeUpdate();

			ps.close();
			System.out.println(black("Sent encrypted payload \"" + ciphertext1 + "\" to postgres database."));
		} catch (SQLException e) {
			System.out.println("Connection failure.\n" + e);
		} catch (Exception e) {
			System.out.println("Something went wrong.\n" + e);
		}
		System.out.println();

		// 4: Another key is created in the vault.
		System.out.println(black("4: Creating key 2..."));
		final String keyId2 = createKeyTest(kmsManagementClient, compartmentId, "SFLY_DELIVERABLES_TEST_DEMO_KEY2");
		System.out.println(black("Created key 2.\n"));//, disabled key 1.\n"));

		// 5: The same String payload is encrypted using the second vault key. This information is sent to a Postgres table.
		System.out.println(black("5: Encrypting string payload using key 2..."));
		final String ciphertext2 = encryptTest(kmsCryptoClient, keyId2, TEXT_TO_ENCRYPT);
		try (Connection connection = DriverManager.getConnection(PSQL_ADDRESS, "postgres", "")) {
			System.out.println("Connected to PostgreSQL database.\nExecuting insert...");
			final PreparedStatement ps = connection.prepareStatement("INSERT INTO " + PSQL_TABLE_NAME + " (keyid, payload) VALUES ('" + keyId2 + "','" + ciphertext2 + "')");
			ps.executeUpdate();

			ps.close();
			System.out.println(black("Sent encrypted payload \"" + ciphertext2 + "\" to postgres database."));
		} catch (SQLException e) {
			System.out.println("Connection failure.\n" + e);
		} catch (Exception e) {
			System.out.println("Something went wrong.\n" + e);
		}
		System.out.println();

		// 6: The encrypted data is queried from the PSQL table.
		System.out.println(black("6: Grabbing encrypted strings from PSQL table..."));
		String ciphertextFromPSQL1 = ""; String ciphertextFromPSQL2 = "";
		try (Connection connection = DriverManager.getConnection(PSQL_ADDRESS, "postgres", "")) {
			System.out.println("Connected to PostgreSQL database.");
			final Statement statement = connection.createStatement();

			System.out.println("Executing first select...");
			ResultSet resultSetSelect1 = statement.executeQuery("SELECT * FROM " + PSQL_TABLE_NAME + " WHERE keyid = '" + keyId1 + "'");
			if (resultSetSelect1.next()) { // gets the first row in the ResultSet
				ciphertextFromPSQL1 = resultSetSelect1.getString("payload");
				System.out.println("...Grabbed result 1: payload is \"" + ciphertextFromPSQL1 + "\".");
			} else {
				System.out.println("Failed to grab result 1.");
			}
			System.out.println("Executing second select...");
			ResultSet resultSetSelect2 = statement.executeQuery("SELECT * FROM " + PSQL_TABLE_NAME + " WHERE keyid = '" + keyId2 + "'");
			if (resultSetSelect2.next()) { // gets the first row in the ResultSet
				ciphertextFromPSQL2 = resultSetSelect2.getString("payload");
				System.out.println("...Grabbed result 2: payload is \"" + ciphertextFromPSQL2 + "\".");
			} else {
				System.out.println("Failed to grab result 2.");
			}

			resultSetSelect1.close();
			resultSetSelect2.close();
			statement.close();
			System.out.println(black("Successfully queried and processed both ciphertexts from postgres database."));
		} catch (SQLException e) {
			System.out.println("Connection failure.\n" + e);
		} catch (Exception e) {
			System.out.println("Something went wrong.\n" + e);
		}
		System.out.println();

		// 7: Both payloads are decrypted using both vault keys.
		System.out.println(black("7: Decrypting payloads using the vault keys..."));
		final String decryptedCtext1 = decryptTest(kmsCryptoClient, keyId1, ciphertextFromPSQL1);
		final String decryptedCtext2 = decryptTest(kmsCryptoClient, keyId2, ciphertextFromPSQL2);
		System.out.println("    Key: " + keyId1 + "; ciphertext: " + ciphertextFromPSQL1 + "; plaintext: " + decryptedCtext1);
		System.out.println("    Key: " + keyId2 + "; ciphertext: " + ciphertextFromPSQL2 + "; plaintext: " + decryptedCtext2);
		System.out.println(black("Successfully decrypted all payloads using their relevant vault keys.\n"));

		// 8: The first vault key is rotated.
		System.out.println(black("8: Rotating key 1..."));
		createKeyVersionTest(kmsManagementClient, keyId1);
		System.out.println(black("Rotated key 1.\n"));

		// 9: The string payload is encrypted, then decrypted using the rotated vault key.
		System.out.println(black("9: Encrypting and decrypting payload using rotated vault key..."));
		final String ciphertext3 = encryptTest(kmsCryptoClient, keyId1, TEXT_TO_ENCRYPT);
		final String decryptedCtext3 = decryptTest(kmsCryptoClient, keyId1, ciphertext3);
		System.out.println(blue("Note the different ciphertext from the previous encryption using the newly rotated key."));
		System.out.println(black("Encrypted and decrypted payload \n    " + TEXT_TO_ENCRYPT + "\nas ciphertext\n    " + ciphertext3 + "\nand decrypted as\n    " + decryptedCtext3));

		// 10: Both keys are scheduled for deletion.
		System.out.println(black("10: Scheduling keys for deletion..."));
		scheduleKeyDeletionTest(kmsManagementClient, keyId1);
		scheduleKeyDeletionTest(kmsManagementClient, keyId2);
		System.out.println(black("Both keys scheduled for deletion.\n"));

		// 11: The contents of the PSQL table are shown.
		System.out.println(black("11: Showing contents of PSQL table..."));
		try (Connection connection = DriverManager.getConnection(PSQL_ADDRESS, "postgres", "")) {
			System.out.println("Connected to PostgreSQL database.");
			final Statement statement = connection.createStatement();

			ResultSet resultSetSelect = statement.executeQuery("SELECT * FROM " + PSQL_TABLE_NAME);
			System.out.println("Reading table from " + PSQL_TABLE_NAME);
			System.out.printf("%-30.30s  %-30.30s%n", "ID", "payload");
			while (resultSetSelect.next()) {
				System.out.printf("%-30.30s  %-30.30s%n", resultSetSelect.getString("id"), resultSetSelect.getString("payload"));
			}

			resultSetSelect.close();
			statement.close();
			System.out.println(black("Table printed."));
		} catch (SQLException e) {
			System.out.println("Connection failure.\n" + e);
		} catch (Exception e) {
			System.out.println("Something went wrong.\n" + e);
		}
		System.out.println();

		System.out.println("\n\033[1;32mDone. Exiting program...\033[0m\n");
	}

	// vault method to test the getVault endpoint
	public static Vault getVaultTest(KmsVaultClient kmsVaultClient, String vaultId) {
		System.out.println("======== GetVault Test ========");
		GetVaultRequest getVaultRequest = GetVaultRequest.builder().vaultId(vaultId).build();
		GetVaultResponse response = kmsVaultClient.getVault(getVaultRequest);
		System.out.println("GetVault response:\n" + response.getVault());
		return response.getVault();
	}

	// keyManagement method to test the createKey endpoint
	public static String createKeyTest(KmsManagementClient kmsManagementClient, String compartmentId, String nameOfKey) {
		System.out.println("======== CreateKey Test ========");
		CreateKeyDetails createKeyDetails = CreateKeyDetails.builder()
			.keyShape(TEST_KEY_SHAPE)
			.compartmentId(compartmentId)
			.displayName(nameOfKey)
			.build();
		CreateKeyRequest createKeyRequest = CreateKeyRequest.builder().createKeyDetails(createKeyDetails).build();
		CreateKeyResponse response = kmsManagementClient.createKey(createKeyRequest);
		System.out.println("CreateKey response:\n" + response.getKey());
		return response.getKey().getId();
	}

	// keyManagement method to test the scheduleKeyDeletion endpoint
	public static void scheduleKeyDeletionTest(KmsManagementClient kmsManagementClient, String keyId) throws Exception {
		System.out.println("======== ScheduleKeyDeletion Test ========");
		ScheduleKeyDeletionDetails scheduleKeyDeletionDetails = ScheduleKeyDeletionDetails.builder().timeOfDeletion(null).build();
		ScheduleKeyDeletionRequest scheduleKeyDeletionRequest = ScheduleKeyDeletionRequest.builder()
			.keyId(keyId)
			.scheduleKeyDeletionDetails(scheduleKeyDeletionDetails)
			.build();
		ScheduleKeyDeletionResponse response = null;
		long time_waited = 0L; 
		boolean keyIsAvailable = false;
		while (time_waited < TIMEOUT && !keyIsAvailable) {
			try {
				response = kmsManagementClient.scheduleKeyDeletion(scheduleKeyDeletionRequest);
				keyIsAvailable = true;
			} catch (Exception e) {
				System.out.println("    Waiting for key state to become available (" + time_waited + " seconds elapsed).");
				//System.out.println("        " + e); // DEBUG
				Thread.sleep(DELAY * 1000L);
				time_waited += DELAY;
			}
		}
		if (!keyIsAvailable) {
			throw new Exception("ERROR: timeout of " + TIMEOUT + " seconds reached. Please check your vault for the state of the key with KeyId <" + keyId + ">.");
		}
		System.out.println("Key Scheduled deletion Successfully, Updated Key:\n" + response.getKey());
	}

	// keyManagement method to test the createKeyVersion endpoint
	public static void createKeyVersionTest(KmsManagementClient kmsManagementClient, String keyId) throws Exception {
		System.out.println("======== CreateKeyVersion Test ========");
		CreateKeyVersionRequest createKeyVersionRequest = CreateKeyVersionRequest.builder().keyId(keyId).build();
		CreateKeyVersionResponse response = null;
		long time_waited = 0L; 
		boolean keyIsAvailable = false;
		while (time_waited < TIMEOUT && !keyIsAvailable) {
			try {
				response = kmsManagementClient.createKeyVersion(createKeyVersionRequest);
				keyIsAvailable = true;
			} catch (Exception e) {
				System.out.println("    Waiting for key state to become available (" + time_waited + " seconds elapsed).");
				//System.out.println("        " + e); // DEBUG
				Thread.sleep(DELAY * 1000L);
				time_waited += DELAY;
			}
		}
		if (!keyIsAvailable) {
			throw new Exception("ERROR: timeout of " + TIMEOUT + " seconds reached. Please check your vault for the state of the key with KeyId <" + keyId + ">.");
		}
		System.out.println("CreateKeyVersion response:\n" + response.getKeyVersion());
	}

	// cryptoManagement method to test the encrypt endpoint
	public static String encryptTest(KmsCryptoClient kmsCryptoClient, String keyId, String plaintext) throws Exception {
		System.out.println("======== Encrypt Test ========");
		EncryptDataDetails encryptDataDetails = EncryptDataDetails.builder()
			.keyId(keyId)
			.plaintext(Base64.encodeBase64String(plaintext.getBytes()))
			.build();
		EncryptRequest encryptRequest = EncryptRequest.builder().encryptDataDetails(encryptDataDetails).build();
		EncryptResponse response = null;
		long time_waited = 0L;
		boolean keyIsAvailable = false;
		while (time_waited < TIMEOUT && !keyIsAvailable) {
			try {
				response = kmsCryptoClient.encrypt(encryptRequest);
				keyIsAvailable = true;
			} catch (Exception e) {
				System.out.println("    Waiting for key state to become available (" + time_waited + " seconds elapsed).");
				//System.out.println("        " + e); // DEBUG
				Thread.sleep(DELAY * 1000L);
				time_waited += DELAY;
			}
		}
		if (!keyIsAvailable) {
			throw new Exception("ERROR: timeout of " + TIMEOUT + " seconds reached. Please check your vault for the state of the key with KeyId <" + keyId + ">.");
		}
		String ciphertextResponse = response.getEncryptedData().getCiphertext();
		System.out.println("Plaintext:\n" + plaintext);
		System.out.println("Ciphertext:\n" + ciphertextResponse);
		return ciphertextResponse;
	}

	// cryptoManagement method to test the decrypt endpoint
	public static String decryptTest(KmsCryptoClient kmsCryptoClient, String keyId, String cipherText) throws Exception {
		System.out.println("Decrypt Test: ");
		DecryptDataDetails decryptDataDetails = DecryptDataDetails.builder()
			.ciphertext(cipherText)
			.keyId(keyId)
			.build();
		DecryptRequest decryptRequest = DecryptRequest.builder().decryptDataDetails(decryptDataDetails).build();
		DecryptResponse response = null;
		long time_waited = 0L;
		boolean keyIsAvailable = false;
		while (time_waited < TIMEOUT && !keyIsAvailable) {
			try {
				response = kmsCryptoClient.decrypt(decryptRequest);
				keyIsAvailable = true;
			} catch (Exception e) {
				System.out.println("    Waiting for key state to become available (" + time_waited + " seconds elapsed).");
				//System.out.println("        " + e); // DEBUG
				Thread.sleep(DELAY * 1000L);
				time_waited += DELAY;
			}
		}
		if (!keyIsAvailable) {
			throw new Exception("ERROR: timeout of " + TIMEOUT + " seconds reached. Please check your vault for the state of the key with KeyId <" + keyId + ">.");
		}
		String plaintextResponseBase64 = response.getDecryptedData().getPlaintext();
		String plaintextResponse = new String(Base64.decodeBase64(plaintextResponseBase64));
		System.out.println("Plaintext (encoded as base 64):\n" + plaintextResponseBase64);
		System.out.println("Plaintext:\n" + plaintextResponse);
		return plaintextResponse;
	}

	// helper methods that apply bold and {{color}} to a string (doesn't work for all output terminals)
	public static String black(String in) { return "\033[1;30m" + in + "\033[0m"; }
	public static String blue(String in) { return "\033[1;33m" + in + "\033[0m"; }
}
