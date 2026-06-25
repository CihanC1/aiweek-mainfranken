package de.mainfrankenit.identity.adapter.in.rest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
@QuarkusTest class UserResourceTest {
 @Test void createsAndValidatesOptIn(){String id=given().contentType("application/json").body("{\"displayName\":\"Ada\"}").post("/api/users").then().statusCode(201).body("displayName",is("Ada")).extract().path("id");given().contentType("application/json").body("{\"phoneNumber\":\"123\",\"explicitConsent\":false}").post("/api/users/"+id+"/whatsapp-opt-in").then().statusCode(400);}
}