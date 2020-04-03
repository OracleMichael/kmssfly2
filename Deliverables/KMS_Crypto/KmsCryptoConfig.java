import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.keymanagement.KmsCryptoClient;
import com.oracle.bmc.keymanagement.KmsManagementClient;
import com.oracle.bmc.keymanagement.KmsVaultClient;
import com.oracle.bmc.keymanagement.model.CreateKeyDetails;
import com.oracle.bmc.keymanagement.model.DecryptDataDetails;
import com.oracle.bmc.keymanagement.model.EncryptDataDetails;
import com.oracle.bmc.keymanagement.model.GenerateKeyDetails;
import com.oracle.bmc.keymanagement.model.KeyShape;
import com.oracle.bmc.keymanagement.model.Vault;
import com.oracle.bmc.keymanagement.requests.CreateKeyRequest;
import com.oracle.bmc.keymanagement.requests.DecryptRequest;
import com.oracle.bmc.keymanagement.requests.EncryptRequest;
import com.oracle.bmc.keymanagement.requests.GenerateDataEncryptionKeyRequest;
import com.oracle.bmc.keymanagement.requests.GetVaultRequest;
import com.oracle.bmc.keymanagement.responses.CreateKeyResponse;
import com.oracle.bmc.keymanagement.responses.DecryptResponse;
import com.oracle.bmc.keymanagement.responses.EncryptResponse;
import com.oracle.bmc.keymanagement.responses.GenerateDataEncryptionKeyResponse;
import com.oracle.bmc.keymanagement.responses.GetVaultResponse;
import org.apache.commons.codec.binary.Base64;

import java.util.HashMap;
import java.util.Map;

public class KmsCryptoConfig {

	private static final int DEFAULT_KEY_LENGTH = 32;
	private static final KeyShape TEST_KEY_SHAPE =
			KeyShape.builder().algorithm(KeyShape.Algorithm.Aes).length(DEFAULT_KEY_LENGTH).build();

	private static final long DELAY = 10L; // 10 second delay
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

		System.out.println("\nNOTE: Any 404 or 401 errors are most likely due to incorrect URLs/OCIDs provided for this file: please double check these.\n");

		// Configuring the AuthenticationDetailsProvider. It's assuming there is a default OCI config file
		// "config", and a profile in that config with the name "DEFAULT". Make changes to the following
		// lines if needed.
		final String configurationFilePath = "~/.oci/config";
		final String profile = "DEFAULT";
		final AuthenticationDetailsProvider provider =
			new ConfigFileAuthenticationDetailsProvider(configurationFilePath, profile);

		// Initialize the KMS Clients. We need all three clients.
		//     * kmsVaultClient: the client for Vault Management
		//     * kmsManagementClient: the client for Key Management
		//     * kmsCryptoClient: the client for Crypographic Management
		KmsVaultClient kmsVaultClient = new KmsVaultClient(provider);
		KmsManagementClient kmsManagementClient = new KmsManagementClient(provider);
		KmsCryptoClient kmsCryptoClient = new KmsCryptoClient(provider);

		// Set the region
		kmsVaultClient.setRegion(region);

		// Get vault test
		Vault vault = getVaultTest(kmsVaultClient, vaultId);

		// Set the cryptographic management endpoint
		kmsManagementClient.setEndpoint(vault.getManagementEndpoint());
		kmsCryptoClient.setEndpoint(vault.getCryptoEndpoint());

		// Get the keyID using createKey
		String keyId = createKeyTest(kmsManagementClient, compartmentId, "SFLY_DELIVERABLES_TEST_KMSCONFIG");

		// Testing the encryption endpoint
		String plaintext = "1234567890000000";
		String ciphertext = encryptTest(kmsCryptoClient, keyId, plaintext);
		// Testing the decryption endpoint
		decryptTest(kmsCryptoClient, keyId, ciphertext);
		// Testing the generateDataEncryptionKey endpoint
		generateDataEncryptionKeyTest(kmsCryptoClient, keyId);
	}

	public static Vault getVaultTest(KmsVaultClient kmsVaultClient, String vaultId) {
		System.out.println("======== GetVault Test ========");
		GetVaultRequest getVaultRequest = GetVaultRequest.builder().vaultId(vaultId).build();
		GetVaultResponse response = kmsVaultClient.getVault(getVaultRequest);
		System.out.println("Vault Retrieved:\n" + response.getVault() + "\n");
		return response.getVault();
	}

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

	public static String encryptTest(KmsCryptoClient kmsCryptoClient, String keyId, String plaintext) throws Exception {
		System.out.println("======== Encrypt Test ========");
		EncryptDataDetails encryptDataDetails = EncryptDataDetails.builder()
			.keyId(keyId)
			.plaintext(Base64.encodeBase64String(plaintext.getBytes()))
			.loggingContext(getSampleLoggingContext()) // this is optional
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
		System.out.println("Ciphertext:\n" + ciphertextResponse + "\n");
		return ciphertextResponse;
	}

	public static String decryptTest(KmsCryptoClient kmsCryptoClient, String keyId, String cipherText) throws Exception {
		System.out.println("======== Decrypt Test ========");
		DecryptDataDetails decryptDataDetails = DecryptDataDetails.builder()
			.ciphertext(cipherText)
			.keyId(keyId)
			.loggingContext(getSampleLoggingContext()) // optional
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
		System.out.println("Plaintext:\n" + plaintextResponse + "\n");
		return plaintextResponse;
	}

	public static void generateDataEncryptionKeyTest(KmsCryptoClient kmsCryptoClient, String keyId) throws Exception {
		System.out.println("======== GenerateDataEncryptionKey Test ========");
		GenerateKeyDetails generateKeyDetails = GenerateKeyDetails.builder()
			.keyId(keyId)
			.keyShape(TEST_KEY_SHAPE)
			.includePlaintextKey(true)
			.loggingContext(getSampleLoggingContext()) // optional
			.build();
		GenerateDataEncryptionKeyRequest generateDataEncryptionKeyRequest = GenerateDataEncryptionKeyRequest.builder()
			.generateKeyDetails(generateKeyDetails)
			.build();
		GenerateDataEncryptionKeyResponse response = null;
		long time_waited = 0L;
		boolean keyIsAvailable = false;
		while (time_waited < TIMEOUT && !keyIsAvailable) {
			try {
				response = kmsCryptoClient.generateDataEncryptionKey(generateDataEncryptionKeyRequest);
				keyIsAvailable = true;
			} catch (Exception e) {
				System.out.println("    Waiting for key state to become available (" + time_waited + " seconds elapsed).");
				Thread.sleep(DELAY * 1000L);
				time_waited += DELAY;
			}
		}
		if (!keyIsAvailable) {
			throw new Exception("ERROR: timeout of " + TIMEOUT + " seconds reached. Please check your vault for the state of the key with KeyId <" + keyId + ">.");
		}
		System.out.println("GenerateDataEncryptionKey Response:\n" + response.getGeneratedKey() + "\n");
	}

	// This map is used in the generateKeyDetails. It is not required, but it provides context for
	// audit logging. More information can be found at the following link:
	// https://docs.cloud.oracle.com/iaas/api/#/en/key/release/datatypes/GenerateKeyDetails
	private static Map<String, String> getSampleLoggingContext() {
		Map<String, String> loggingContext = new HashMap<String, String>();
		loggingContext.put("loggingContextKey1", "loggingContextValue1");
		loggingContext.put("loggingContextKey2", "loggingContextValue2");
		return loggingContext;
	}

	// helper methods that apply bold and {{color}} to a string (doesn't work for all output terminals)
	public static String black(String in) { return "\033[1;30m" + in + "\033[0m"; }
	public static String blue(String in) { return "\033[1;33m" + in + "\033[0m"; }
}
