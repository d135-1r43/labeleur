package net.herhoffer.labeleur.keycloak;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * An invitation to a Labeleur organisation, as Keycloak sees it: the app signs
 * the invited address with the labeleur client secret and sends the signature
 * along as {@code labeleur_invite}. A matching signature lets that address
 * register even when the allowlist doesn't name it. Must stay in step with
 * {@code net.herhoffer.labeleur.organisation.InviteSignature} in the app.
 */
public final class InviteSignature
{
	/** Where Keycloak keeps the authorization request's extra parameter. */
	public static final String CLIENT_NOTE = "client_request_param_labeleur_invite";

	private InviteSignature()
	{
	}

	public static String of(String secret, String email)
	{
		try
		{
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] sig = mac.doFinal(("labeleur-invite:" + email.strip().toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
		}
		catch (GeneralSecurityException e)
		{
			throw new IllegalStateException(e);
		}
	}

	/** Whether {@code signature} invites {@code email}; false without a secret. */
	public static boolean invites(String secret, String email, String signature)
	{
		if (secret == null || secret.isBlank() || email == null || email.isBlank() || signature == null) return false;
		return MessageDigest.isEqual(of(secret, email).getBytes(StandardCharsets.US_ASCII), signature.getBytes(StandardCharsets.US_ASCII));
	}
}
