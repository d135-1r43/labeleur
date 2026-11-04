package net.herhoffer.labeleur;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class TapePresetResourceTest
{

	private static void add(int lengthMm, boolean readAlong)
	{
		given().contentType(ContentType.JSON).body("{\"lengthMm\": " + lengthMm + ", \"readAlong\": " + readAlong + "}")
			.post("/api/settings/tape-presets").then()
			.statusCode(200)
			.body("id", equalTo(lengthMm + (readAlong ? "-along" : "-across")));
	}

	@Test
	@TestSecurity(user = "presets-a")
	void keepsPresetsShortestFirstAndOnlyOnce()
	{
		add(120, true);
		add(80, false);
		add(80, true);
		add(80, true);
		given().get("/api/settings/tape-presets").then()
			.statusCode(200)
			.body("id", contains("80-across", "80-along", "120-along"))
			.body("[1].lengthMm", equalTo(80))
			.body("[1].readAlong", equalTo(true));

		given().delete("/api/settings/tape-presets/80-across").then().statusCode(204);
		given().get("/api/settings/tape-presets").then().body("id", contains("80-along", "120-along"));
		given().delete("/api/settings/tape-presets/80-across").then().statusCode(404);
	}

	@Test
	@TestSecurity(user = "presets-b")
	void presetsBelongToTheirUser()
	{
		add(50, false);
		given().get("/api/settings/tape-presets").then().body("id", contains("50-across"));
	}

	@Test
	@TestSecurity(user = "presets-c")
	void rejectsLengthsThePrinterCannotCut()
	{
		given().contentType(ContentType.JSON).body("{\"lengthMm\": 10, \"readAlong\": true}")
			.post("/api/settings/tape-presets").then()
			.statusCode(400);
		given().contentType(ContentType.JSON).body("{\"lengthMm\": 400, \"readAlong\": false}")
			.post("/api/settings/tape-presets").then()
			.statusCode(400);
		given().get("/api/settings/tape-presets").then().body("$", hasSize(0));
	}
}
