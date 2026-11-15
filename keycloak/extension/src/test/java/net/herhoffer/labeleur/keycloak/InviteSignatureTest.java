package net.herhoffer.labeleur.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InviteSignatureTest
{
	/** The app's OrganisationTest checks the same value; computed independently with Python's hmac. */
	static final String KNOWN = InviteSignature.of("s3cret", "anna@example.org");

	@Test
	void matchesTheApp()
	{
		assertEquals("020EGxo1mJ-vZPsomHw40LdmE4RQxdAaTL_Q8-jzWcE", KNOWN);
	}

	@Test
	void invitesOnlyTheSignedAddress()
	{
		assertTrue(InviteSignature.invites("s3cret", " Anna@Example.org ", KNOWN));
		assertFalse(InviteSignature.invites("s3cret", "bob@example.org", KNOWN));
		assertFalse(InviteSignature.invites("other", "anna@example.org", KNOWN));
		assertFalse(InviteSignature.invites("s3cret", "anna@example.org", null));
		assertFalse(InviteSignature.invites(null, "anna@example.org", KNOWN));
		assertFalse(InviteSignature.invites("", "anna@example.org", KNOWN));
	}
}
