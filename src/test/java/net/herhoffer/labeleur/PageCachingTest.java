package net.herhoffer.labeleur;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class PageCachingTest
{
	@Test
	void pagesAreCheckedWithTheServerEachTime()
	{
		// index.html also answers /login: kept for a day, signing in never
		// reached Keycloak.
		given().get("/page.html").then().statusCode(200).header("Cache-Control", equalTo("no-cache"));
	}

	@Test
	void hashedBundlesStayCached()
	{
		given().get("/assets/bundle-abc123.js").then().statusCode(200)
			.header("Cache-Control", containsString("immutable"));
	}
}
