package net.herhoffer.labeleur;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import net.herhoffer.labeleur.organisation.InviteSignature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Anna invites Bea, which turns Anna's account into an organisation; Bea
 * registers from the link and from then on shares Anna's data. Steps run in
 * order, each as a different user.
 */
@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrganisationTest
{
	private static final Pattern LINK = Pattern.compile("http://localhost:8080/invite/([A-Za-z0-9_-]+)\\?k=([A-Za-z0-9_-]+)");

	static String beaToken;
	static String carlToken;
	static String revokedToken;

	@Inject
	MockMailbox mailbox;

	@Inject
	EntityManager em;

	/**
	 * The test database outlives a run: start these users from scratch, in
	 * every table keyed by user.
	 */
	@BeforeAll
	void forgetEarlierRuns()
	{
		QuarkusTransaction.requiringNew().run(() -> em.createNativeQuery("""
			do $$
			declare t text;
			begin
			  for t in select table_name from information_schema.columns where column_name = 'user_id' and table_schema = current_schema() loop
			    execute format('delete from %I where user_id like ''org-%%''', t);
			  end loop;
			  delete from invitation where owner_id like 'org-%';
			end $$""").executeUpdate());
	}

	@Test
	void signsInvitationsLikeKeycloak()
	{
		// keycloak/extension InviteSignatureTest checks the same value.
		assertThat(InviteSignature.of("s3cret", " Anna@Example.org")).isEqualTo("020EGxo1mJ-vZPsomHw40LdmE4RQxdAaTL_Q8-jzWcE");
	}

	@Test
	@Order(1)
	@TestSecurity(user = "org-anna")
	@OidcSecurity(claims = { @Claim(key = "email", value = "anna@example.org"), @Claim(key = "name", value = "Anna Alt") })
	void invitingTurnsTheAccountIntoAnOrganisation()
	{
		given().get("/api/me").then().body("organisation", equalTo(false));
		given().get("/api/organisation/members").then()
			.statusCode(200)
			.body("members", hasSize(1))
			.body("members[0].you", equalTo(true))
			.body("invitations", hasSize(0));
		given().contentType(ContentType.JSON).body("{\"title\": \"Shared shelf\", \"subtitle\": \"\"}").put("/api/records/signs/sig-shared").then().statusCode(200);

		mailbox.clear();
		beaToken = invite("Bea@Example.org");
		carlToken = invite("carl@example.org");
		revokedToken = invite("dora@example.org");

		List<Mail> mails = mailbox.getMailsSentTo("bea@example.org");
		assertThat(mails).hasSize(1);
		assertThat(mails.get(0).getSubject()).isEqualTo("Anna Alt invited you to Nordlicht Manufaktur GmbH on Labeleur");
		Matcher link = LINK.matcher(mails.get(0).getText());
		assertThat(link.find()).isTrue();
		assertThat(link.group(1)).isEqualTo(beaToken);
		assertThat(link.group(2)).isEqualTo(InviteSignature.of("labeleur-dev-secret", "bea@example.org"));

		given().get("/api/me").then().body("organisation", equalTo(true));
		given().get("/api/organisation/members").then()
			.body("members", hasSize(1))
			.body("members[0].founder", equalTo(true))
			.body("invitations.email", contains("bea@example.org", "carl@example.org", "dora@example.org"));

		// Inviting again sends a fresh link; the old one stops working.
		String again = invite("dora@example.org");
		given().get("/api/invitations/" + revokedToken).then().statusCode(404);
		revokedToken = again;
		given().delete("/api/organisation/invitations/" + revokedToken).then().statusCode(204);

		given().contentType(ContentType.JSON).body("{\"email\": \"not an address\"}").post("/api/organisation/invitations").then().statusCode(400);
		given().contentType(ContentType.JSON).body("{\"email\": \"anna@example.org\"}").post("/api/organisation/invitations").then().statusCode(409);
	}

	@Test
	@Order(2)
	@TestSecurity(user = "org-bea")
	@OidcSecurity(claims = @Claim(key = "email", value = "bea@example.org"))
	void aNewUserJoinsFromTheLink()
	{
		given().get("/api/invitations/" + beaToken).then()
			.statusCode(200)
			.body("organisation", equalTo("Nordlicht Manufaktur GmbH"))
			.body("invitedBy", equalTo("Anna Alt"))
			.body("problem", nullValue());
		given().get("/api/invitations/" + revokedToken).then().statusCode(404);

		given().post("/api/invitations/" + beaToken + "/accept").then().statusCode(204);
		given().post("/api/invitations/" + beaToken + "/accept").then().statusCode(404);

		given().get("/api/me").then().body("id", equalTo("org-bea")).body("organisation", equalTo(true));
		given().get("/api/records/signs").then().body("title", hasItem("Shared shelf"));
		given().contentType(ContentType.JSON).body("{\"title\": \"From Bea\", \"subtitle\": \"\"}").put("/api/records/signs/sig-bea").then().statusCode(200);
		given().contentType(ContentType.JSON).body("{\"name\": \"Alt & Partner\", \"address\": {\"country\": \"DE\"}}").put("/api/organisation").then().statusCode(200);
	}

	@Test
	@Order(3)
	@TestSecurity(user = "org-anna")
	@OidcSecurity(claims = @Claim(key = "email", value = "anna@example.org"))
	void everyoneWorksOnTheSameData()
	{
		given().get("/api/records/signs").then().body("title", hasItem("From Bea"));
		given().get("/api/organisation").then().body("name", equalTo("Alt & Partner"));
		given().get("/api/organisation/members").then()
			.body("members.email", contains("anna@example.org", "bea@example.org"))
			.body("invitations.email", contains("carl@example.org"));
		given().contentType(ContentType.JSON).body("{\"email\": \"bea@example.org\"}").post("/api/organisation/invitations").then().statusCode(409);
	}

	@Test
	@Order(4)
	@TestSecurity(user = "org-mallory")
	@OidcSecurity(claims = @Claim(key = "email", value = "mallory@example.org"))
	void onlyTheInvitedAddressCanAccept()
	{
		given().get("/api/invitations/" + carlToken).then().statusCode(200).body("problem", containsString("carl@example.org"));
		given().post("/api/invitations/" + carlToken + "/accept").then().statusCode(409).body("message", containsString("carl@example.org"));
	}

	@Test
	@Order(5)
	@TestSecurity(user = "org-carl")
	@OidcSecurity(claims = @Claim(key = "email", value = "carl@example.org"))
	void someoneWithAnAccountOfTheirOwnCannotJoin()
	{
		given().get("/api/me").then().statusCode(200);
		given().post("/api/invitations/" + carlToken + "/accept").then().statusCode(409).body("message", containsString("account of your own"));
		given().get("/api/records/signs").then().body("title", not(hasItem("Shared shelf")));
	}

	@Test
	@Order(6)
	@TestSecurity(user = "org-bea")
	@OidcSecurity(claims = @Claim(key = "email", value = "bea@example.org"))
	void theFounderStays()
	{
		given().delete("/api/organisation/members/org-anna").then().statusCode(409);
		given().delete("/api/organisation/members/someone-else").then().statusCode(404);
	}

	@Test
	@Order(7)
	@TestSecurity(user = "org-anna")
	@OidcSecurity(claims = @Claim(key = "email", value = "anna@example.org"))
	void removingAMember()
	{
		given().delete("/api/organisation/members/org-bea").then().statusCode(204);
		given().get("/api/organisation/members").then().body("members.email", contains("anna@example.org"));
	}

	@Test
	@Order(8)
	@TestSecurity(user = "org-bea")
	@OidcSecurity(claims = @Claim(key = "email", value = "bea@example.org"))
	void aRemovedMemberIsBackOnTheirOwn()
	{
		given().get("/api/me").then().body("organisation", equalTo(false));
		given().get("/api/records/signs").then().body("title", not(hasItem("Shared shelf")));
	}

	private static String invite(String email)
	{
		return given().contentType(ContentType.JSON)
			.body("{\"email\": \"" + email + "\"}")
			.post("/api/organisation/invitations")
			.then()
			.statusCode(200)
			.body("emailSent", equalTo(true))
			.body("link", containsString("/invite/"))
			.extract()
			.path("token");
	}
}
