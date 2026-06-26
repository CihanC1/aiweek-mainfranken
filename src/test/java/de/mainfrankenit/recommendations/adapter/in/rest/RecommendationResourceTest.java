package de.mainfrankenit.recommendations.adapter.in.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class RecommendationResourceTest {

    @Test
    void meRecommendationsReturnsDataForAuthenticatedUser() {
        String userId = given()
                .contentType("application/json")
                .body("{\"displayName\":\"Rec User\",\"email\":\"rec.user@example.de\",\"password\":\"sicher123\"}")
                .post("/api/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        Map<String, String> cookies = given()
                .contentType("application/json")
                .body("{\"email\":\"rec.user@example.de\",\"password\":\"sicher123\"}")
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .cookies();

        given()
                .cookies(cookies)
                .get("/api/auth/me/recommendations")
                .then()
                .statusCode(200)
                .body(notNullValue());

        given()
                .cookies(cookies)
                .get("/api/users/" + userId + "/recommendations")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }
}
