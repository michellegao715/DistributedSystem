import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/*Save salt and hashed password(by salt) as a secret to data store..*/
class Secret {
	byte[] salt;
	byte[] hashedPassword;

	public Secret(byte[] salt, byte[] hashedPassword) {
		this.salt = salt;
		this.hashedPassword = hashedPassword;
	}
}

public class UserStore {
	private static final Random RANDOM = new SecureRandom();
	private static final int ITERATIONS = 10000;
	private static final int KEY_LENGTH = 256; //hashed value has 256 bits.

	private final ConcurrentHashMap<String, Secret> store;

	public UserStore() {
		this.store = new ConcurrentHashMap<String, Secret>();
	}

	
	public synchronized void add(String username, String password) {
		byte[] salt = getNextSalt();
		Secret secret = new Secret(salt, hash(password.toCharArray(), salt));
		store.put(username, secret);
	}

	public synchronized boolean contains(String username) {
		return store.containsKey(username);
	}

	/*Check if the username matches the password in userstore.*/
	public synchronized boolean matches(String username, String password) {
		if(!store.containsKey(username))
			return false;
		else {
			Secret secret = store.get(username);
			return isExpectedPassword(password.toCharArray(), secret.salt, secret.hashedPassword);
		}
	}

	/*Returns a random salt to be used to hash a password*/
	private static byte[] getNextSalt() {
		byte[] salt = new byte[16];
		RANDOM.nextBytes(salt);
		return salt;
	}
	
	/*  Returns a salted and hashed password, and destroy the password by filling the array with zero.
	 * Algorithm: PBKDF2WithHmacSHA1
	 * http://stackoverflow.com/questions/18142745/how-do-i-generate-a-salt-in-java-for-salted-hash*/
	private static byte[] hash(char[] password, byte[] salt) {
		PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
		Arrays.fill(password, Character.MIN_VALUE);
		try {
			SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			return skf.generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new AssertionError("Error while hashing a password: " + e.getMessage(), e);
		} finally {
			spec.clearPassword();
		}
	}
	
	/*Hash the password with salt(saved in userstore) and check if the hashedpassword is the same to the expected hashed password(saved
	 * in userstore).*/
	private static boolean isExpectedPassword(char[] password, byte[] salt, byte[] expectedHash) {
		byte[] pwdHash = hash(password, salt);
		Arrays.fill(password, Character.MIN_VALUE);
		if (pwdHash.length != expectedHash.length) return false;
		for (int i = 0; i < pwdHash.length; i++) {
			if (pwdHash[i] != expectedHash[i]) return false;
		}
		return true;
	}
}
