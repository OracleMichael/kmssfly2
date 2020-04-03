import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.keymanagement.KmsManagementClient;
import com.oracle.bmc.keymanagement.KmsVaultClient;
import com.oracle.bmc.keymanagement.model.CreateKeyDetails;
import com.oracle.bmc.keymanagement.model.KeyShape;
import com.oracle.bmc.keymanagement.model.KeySummary;
import com.oracle.bmc.keymanagement.model.KeyVersionSummary;
import com.oracle.bmc.keymanagement.model.ScheduleKeyDeletionDetails;
import com.oracle.bmc.keymanagement.model.UpdateKeyDetails;
import com.oracle.bmc.keymanagement.model.Vault;
import com.oracle.bmc.keymanagement.requests.CancelKeyDeletionRequest;
import com.oracle.bmc.keymanagement.requests.CreateKeyRequest;
import com.oracle.bmc.keymanagement.requests.CreateKeyVersionRequest;
import com.oracle.bmc.keymanagement.requests.DisableKeyRequest;
import com.oracle.bmc.keymanagement.requests.EnableKeyRequest;
import com.oracle.bmc.keymanagement.requests.GetKeyRequest;
import com.oracle.bmc.keymanagement.requests.GetVaultRequest;
import com.oracle.bmc.keymanagement.requests.ListKeyVersionsRequest;
import com.oracle.bmc.keymanagement.requests.ListKeysRequest;
import com.oracle.bmc.keymanagement.requests.ScheduleKeyDeletionRequest;
import com.oracle.bmc.keymanagement.requests.UpdateKeyRequest;
import com.oracle.bmc.keymanagement.responses.CancelKeyDeletionResponse;
import com.oracle.bmc.keymanagement.responses.CreateKeyResponse;
import com.oracle.bmc.keymanagement.responses.CreateKeyVersionResponse;
import com.oracle.bmc.keymanagement.responses.DisableKeyResponse;
import com.oracle.bmc.keymanagement.responses.EnableKeyResponse;
import com.oracle.bmc.keymanagement.responses.GetKeyResponse;
import com.oracle.bmc.keymanagement.responses.GetVaultResponse;
import com.oracle.bmc.keymanagement.responses.ListKeyVersionsResponse;
import com.oracle.bmc.keymanagement.responses.ListKeysResponse;
import com.oracle.bmc.keymanagement.responses.ScheduleKeyDeletionResponse;
import com.oracle.bmc.keymanagement.responses.UpdateKeyResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class KeysConfig {

	private static final int DEFAULT_KEY_LENGTH = 32;
	// DELAY controls the delay for waiting for a key to become available (you cannot use the management API to modify a key that is not available).
	// Please note that the delay is inversely proportional to the number of API calls made.
	private static final long DELAY = 10L; // 10 second delay
	// TIMEOUT is the maximum total time to wait for the key to become available. Honestly it's just increments of DELAY rather than total time waited,
	// especially for small DELAYs (for instance, if you somehow want a delay of 0.01 seconds...do not do this)
	private static final long TIMEOUT = 120L; // 2 minute timeout

	// The KeyShape used for testing
	private static final KeyShape TEST_KEY_SHAPE =
			KeyShape.builder().algorithm(KeyShape.Algorithm.Aes).length(DEFAULT_KEY_LENGTH).build();

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

		System.out.println("Any 404 or 401 errors are most likely due to incorrect URLs/OCIDs provided in either the config file or this file: please double check these.\n");

		// Configuring the AuthenticationDetailsProvider. It's assuming there is a default OCI config file
		// "config", and a profile in that config with the name "DEFAULT". Make changes to the following
		// line if needed.
		final String configurationFilePath = "~/.oci/config";
		final String profile = "DEFAULT";
		final AuthenticationDetailsProvider provider =
			new ConfigFileAuthenticationDetailsProvider(configurationFilePath, profile);

		// Initialize the KMS Clients. KMS has three management clients, two of which are below:
		//	  * KmsVaultClient: The client for Vault Management
		//	  * KmsManagementClient: The client for Key Management
		KmsVaultClient kmsVaultClient = new KmsVaultClient(provider);
		KmsManagementClient kmsManagementClient = new KmsManagementClient(provider);

		// Set the region
		kmsVaultClient.setRegion(region);

		// Get vault test
		Vault vault = getVaultTest(kmsVaultClient, vaultId);

		// Set the key management endpoint
		kmsManagementClient.setEndpoint(vault.getManagementEndpoint());

		// TESTS: Management / Key Operations
		// CREATE KEY
		String keyId = createKeyTest(kmsManagementClient, compartmentId, "SFLY_DELIVERABLES_TEST_KEYSCONFIG");

		// ignore these operations, but they are here in case sfly wants to use them
		if (false) {
			// GET KEY
			getKeyTest(kmsManagementClient, keyId);

			// UPDATE KEY RESET TAGS
			updateKeyResetTagsTest(kmsManagementClient, keyId);

			// UPDATE KEY
			updateKeyTest(kmsManagementClient, keyId);

			// LIST KEYS
			listKeysTest(kmsManagementClient, compartmentId);

			// DISABLE KEY
			disableKeyTest(kmsManagementClient, keyId);

			// ENABLE KEY
			enableKeyTest(kmsManagementClient, keyId);

			// SCHEDULE KEY DELETION
			scheduleKeyDeletionTest(kmsManagementClient, keyId);

			// CANCEL KEY DELETION
			cancelKeyDeletionTest(kmsManagementClient, keyId);
		}

		// ROTATE KEY
		createKeyVersionTest(kmsManagementClient, keyId);

		// LIST KEY VERSION
		listKeyVersionsTest(kmsManagementClient, keyId);

		System.out.println("\nDone.\n");
	}

	public static Vault getVaultTest(KmsVaultClient kmsVaultClient, String vaultId) {
		System.out.println("======== GetVault Test ========");
		GetVaultRequest getVaultRequest = GetVaultRequest.builder().vaultId(vaultId).build();
		GetVaultResponse response = kmsVaultClient.getVault(getVaultRequest);
		System.out.println("Vault Retrieved:\n" + response.getVault() + "\n");
		return response.getVault();
	}

	public static String createKeyTest(KmsManagementClient kmsManagementClient, String compartmentId, String keyName) {
		System.out.println("======== CreateKey Test ========");
		CreateKeyDetails createKeyDetails = CreateKeyDetails.builder()
			.keyShape(TEST_KEY_SHAPE)
			.compartmentId(compartmentId)
			.displayName(keyName)
			.freeformTags(getSampleFreeformTagData())
			.build();
		CreateKeyRequest createKeyRequest = CreateKeyRequest.builder().createKeyDetails(createKeyDetails).build();
		CreateKeyResponse response = kmsManagementClient.createKey(createKeyRequest);
		System.out.println("Newly Created Key:\n" + response.getKey() + "\n");
		return response.getKey().getId();
	}

	public static void getKeyTest(KmsManagementClient kmsManagementClient, String keyId) {
		System.out.println("======== GetKey Test ========");
		GetKeyRequest getKeyRequest = GetKeyRequest.builder().keyId(keyId).build();
		GetKeyResponse response = kmsManagementClient.getKey(getKeyRequest);
		System.out.println("Key Retrieved:\n" + response.getKey());
	}

	public static void listKeysTest(KmsManagementClient kmsManagementClient, String compartmentId) {
		System.out.println("======== ListKeys Test ========");
		ListKeysRequest listKeysRequest = ListKeysRequest.builder().compartmentId(compartmentId).build();
		ListKeysResponse response = kmsManagementClient.listKeys(listKeysRequest);
		System.out.println("ListKeys Response: ");
		for (KeySummary key : response.getItems()) {
			System.out.println(key);
		}
		System.out.println();
	}

	// NEEDS TO WAIT
	public static void updateKeyResetTagsTest(KmsManagementClient kmsManagementClient, String keyId) throws Exception {
		System.out.println("======== UpdateKey Test ========");
		Map<String, String> newEmptyFreeformTag = Collections.emptyMap();
		UpdateKeyDetails updateKeyDetails = UpdateKeyDetails.builder()
			.displayName("Test_Key_V2")
			.freeformTags(newEmptyFreeformTag)
			.build();
		UpdateKeyRequest updateKeyRequest = updateKeyRequest = UpdateKeyRequest.builder().updateKeyDetails(updateKeyDetails).keyId(keyId).build();
		UpdateKeyResponse response = null;
		long time_waited = 0L;
		boolean keyIsAvailable = false;
		while (time_waited < TIMEOUT && !keyIsAvailable) {
			try {
				response = kmsManagementClient.updateKey(updateKeyRequest);
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
		System.out.println("Updated Key:\n" + response.getKey() + "\n");
	}

	// NEEDS TO WAIT
	public static void updateKeyTest(KmsManagementClient kmsManagementClient, String keyId) throws Exception {
		System.out.println("======== UpdateKey Test ========");
		Map<String, String> newFreeformTag = getSampleFreeformTagData();
		newFreeformTag.put("dummyfreeformkey3", "dummyfreeformvalue3");
		UpdateKeyDetails updateKeyDetails = UpdateKeyDetails.builder()
			.displayName("Test_Key_V2")
			.freeformTags(newFreeformTag)
			.build();
		UpdateKeyRequest updateKeyRequest = UpdateKeyRequest.builder().updateKeyDetails(updateKeyDetails).keyId(keyId).build();
		UpdateKeyResponse response = null;
		long time_waited = 0L;
		boolean keyIsAvailable = false;
		while (time_waited < TIMEOUT && !keyIsAvailable) {
			try {
				response = kmsManagementClient.updateKey(updateKeyRequest);
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
		System.out.println("Updated Key:\n" + response.getKey() + "\n");
	}

	// NEEDS TO WAIT
	public static void disableKeyTest(KmsManagementClient kmsManagementClient, String keyId) throws Exception {
		System.out.println("======== DisableKey Test ========");
		DisableKeyRequest disableKeyRequest = DisableKeyRequest.builder().keyId(keyId).build();
		DisableKeyResponse response = null;
		long time_waited = 0L;
		boolean keyIsAvailable = false;
		while (time_waited < TIMEOUT && !keyIsAvailable) {
			try {
				response = kmsManagementClient.disableKey(disableKeyRequest);
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
		System.out.println("Key Disabled Successfully, Updated Key:\n" + response.getKey() + "\n");
	}

	// NEEDS TO WAIT
	public static void enableKeyTest(KmsManagementClient kmsManagementClient, String keyId) throws Exception {
		System.out.println("======== EnableKey Test ========");
		EnableKeyRequest enableKeyRequest = EnableKeyRequest.builder().keyId(keyId).build();
		EnableKeyResponse response = null;
		long time_waited = 0L;
		boolean keyIsAvailable = false;
		while (time_waited < TIMEOUT && !keyIsAvailable) {
			try {
				response = kmsManagementClient.enableKey(enableKeyRequest);
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
		System.out.println("Key Enabled Successfully, Updated Key:\n" + response.getKey() + "\n");
	}

	// NEEDS TO WAIT
	public static void cancelKeyDeletionTest(KmsManagementClient kmsManagementClient, String keyId) throws Exception{
		System.out.println("======== CancelKeyDeletion Test ========");
		CancelKeyDeletionRequest cancelKeyDeletionRequest = CancelKeyDeletionRequest.builder().keyId(keyId).build();
		CancelKeyDeletionResponse response = null;
		long time_waited = 0L;
		boolean keyIsAvailable = false;
		while (time_waited < TIMEOUT && !keyIsAvailable) {
			try {
				response = kmsManagementClient.cancelKeyDeletion(cancelKeyDeletionRequest);
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
		System.out.println("Key Cancelled deletion Successfully, Updated Key:\n" + response.getKey() + "\n");
	}

	// NEEDS TO WAIT
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
				Thread.sleep(DELAY * 1000L);
				time_waited += DELAY;
			}
		}
		if (!keyIsAvailable) {
			throw new Exception("ERROR: timeout of " + TIMEOUT + " seconds reached. Please check your vault for the state of the key with KeyId <" + keyId + ">.");
		}
		System.out.println("Key Scheduled deletion Successfully, Updated Key:\n" + response.getKey() + "\n");
	}

	// NEEDS TO WAIT
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
				Thread.sleep(DELAY * 1000L);
				time_waited += DELAY;
			}
		}
		if (!keyIsAvailable) {
			throw new Exception("ERROR: timeout of " + TIMEOUT + " seconds reached. Please check your vault for the state of the key with KeyId <" + keyId + ">.");
		}
		System.out.println("Newly Created KeyVersion:\n" + response.getKeyVersion() + "\n");
	}

	public static void listKeyVersionsTest(KmsManagementClient kmsManagementClient, String keyId) {
		System.out.println("======== ListKeyVersions Test ========");
		ListKeyVersionsRequest listKeyVersionsRequest = ListKeyVersionsRequest.builder().keyId(keyId).build();
		ListKeyVersionsResponse response = kmsManagementClient.listKeyVersions(listKeyVersionsRequest);
		System.out.println("ListKeyVersions Response: ");
		for (KeyVersionSummary keyVersion : response.getItems()) {
			System.out.println(keyVersion);
		}
		System.out.println();
	}

	private static Map<String, String> getSampleFreeformTagData() {
		Map<String, String> freeformTags = new HashMap<String, String>();
		freeformTags.put("dummyfreeformkey1", "dummyfreeformvalue1");
		freeformTags.put("dummyfreeformkey2", "dummyfreeformvalue2");
		return freeformTags;
	}

	// helper methods that apply bold and {{color}} to a string (doesn't work for all output terminals)
	public static String black(String in) { return "\033[1;30m" + in + "\033[0m"; }
	public static String blue(String in) { return "\033[1;33m" + in + "\033[0m"; }
}
