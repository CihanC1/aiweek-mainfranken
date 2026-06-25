package de.mainfrankenit.identity.adapter.in.rest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
@QuarkusTest
class AuthResourceTest {
    @Test void registerCreatesSessionAndProtectsCurrentUser(){
        Map<String,String> cookies=given().contentType("application/json")
                .body("{\"displayName\":\"Ada Lovelace\",\"email\":\"ada@example.de\",\"password\":\"sicher123\"}")
                .post("/api/auth/register").then().statusCode(200)
                .body("displayName",is("Ada Lovelace")).body("email",is("ada@example.de"))
                .body("passwordHash",nullValue()).extract().cookies();
        given().cookies(cookies).get("/api/auth/me").then().statusCode(200).body("role",is("USER"));
        given().contentType("application/json").body("{\"displayName\":\"Other\",\"email\":\"ada@example.de\",\"password\":\"sicher123\"}")
                .post("/api/auth/register").then().statusCode(400).body("code",is("VALIDATION_ERROR"));
    }

    @Test void rejectsInvalidCredentials(){
        given().contentType("application/json").body("{\"email\":\"missing@example.de\",\"password\":\"wrongpass\"}")
                .post("/api/auth/login").then().statusCode(401).body("code",is("UNAUTHORIZED"));
    }
}