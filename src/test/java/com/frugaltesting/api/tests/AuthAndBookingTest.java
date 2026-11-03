package com.frugaltesting.api.tests;

import com.frugaltesting.api.config.ApiBaseTest;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

public class AuthAndBookingTest extends ApiBaseTest {

    private String token;
    private Integer bookingId;

    @Test
    public void authenticateAndStoreToken() {
        Response response = given()
                .contentType("application/json")
                .body("""
                        {
                          "username": "admin",
                          "password": "password123"
                        }
                        """)
                .when()
                .post("/auth");

        Assert.assertEquals(response.statusCode(), 200, "Authentication should return HTTP 200");

        token = response.jsonPath().getString("token");
        Assert.assertNotNull(token, "Authentication response should contain a token");
        Assert.assertFalse(token.isBlank(), "Authentication token should not be empty");
    }

    @Test(dependsOnMethods = "authenticateAndStoreToken")
    public void createBookingAndStoreId() {
        Assert.assertNotNull(token, "Authentication token must be available before booking creation");

        Response response = given()
                .contentType("application/json")
                .body("""
                        {
                          "firstname": "Test",
                          "lastname": "User",
                          "totalprice": 150,
                          "depositpaid": true,
                          "bookingdates": {
                            "checkin": "2026-12-01",
                            "checkout": "2026-12-05"
                          },
                          "additionalneeds": "Breakfast"
                        }
                        """)
                .when()
                .post("/booking");

        Assert.assertEquals(response.statusCode(), 200, "Booking creation should return HTTP 200");

        Integer responseBookingId = response.jsonPath().getInt("bookingid");
        Assert.assertNotNull(responseBookingId, "Booking response should contain a booking ID");
        Assert.assertTrue(responseBookingId > 0, "Booking ID should be positive");
        bookingId = responseBookingId;

        Assert.assertEquals(
                response.jsonPath().getString("booking.firstname"),
                "Test",
                "Booking first name should match the request"
        );
        Assert.assertEquals(
                response.jsonPath().getString("booking.lastname"),
                "User",
                "Booking last name should match the request"
        );
        Assert.assertEquals(
                response.jsonPath().getInt("booking.totalprice"),
                150,
                "Booking total price should match the request"
        );
        Assert.assertTrue(
                response.jsonPath().getBoolean("booking.depositpaid"),
                "Booking deposit status should match the request"
        );
        Assert.assertEquals(
                response.jsonPath().getString("booking.bookingdates.checkin"),
                "2026-12-01",
                "Booking check-in date should match the request"
        );
        Assert.assertEquals(
                response.jsonPath().getString("booking.bookingdates.checkout"),
                "2026-12-05",
                "Booking check-out date should match the request"
        );
        Assert.assertEquals(
                response.jsonPath().getString("booking.additionalneeds"),
                "Breakfast",
                "Booking additional needs should match the request"
        );
    }

    @Test(dependsOnMethods = "createBookingAndStoreId")
    public void retrieveBooking() {
        Assert.assertNotNull(bookingId, "Booking ID must be available before retrieving the booking");

        Response response = given()
                .pathParam("bookingId", bookingId)
                .when()
                .get("/booking/{bookingId}");

        Assert.assertEquals(response.statusCode(), 200, "Booking retrieval should return HTTP 200");
        Assert.assertEquals(response.jsonPath().getString("firstname"), "Test");
        Assert.assertEquals(response.jsonPath().getString("lastname"), "User");
        Assert.assertEquals(response.jsonPath().getInt("totalprice"), 150);
        Assert.assertTrue(response.jsonPath().getBoolean("depositpaid"));
        Assert.assertEquals(response.jsonPath().getString("additionalneeds"), "Breakfast");
    }

    @Test(dependsOnMethods = "retrieveBooking")
    public void updateBooking() {
        Assert.assertNotNull(token, "Authentication token must be available before updating the booking");
        Assert.assertNotNull(bookingId, "Booking ID must be available before updating the booking");

        Response response = given()
                .contentType("application/json")
                .accept("application/json")
                .cookie("token", token)
                .pathParam("bookingId", bookingId)
                .body("""
                        {
                          "firstname": "Updated",
                          "lastname": "Customer",
                          "totalprice": 200,
                          "depositpaid": false,
                          "bookingdates": {
                            "checkin": "2026-12-02",
                            "checkout": "2026-12-06"
                          },
                          "additionalneeds": "Lunch"
                        }
                        """)
                .when()
                .put("/booking/{bookingId}");

        Assert.assertEquals(response.statusCode(), 200, "Booking update should return HTTP 200");
        Assert.assertEquals(response.jsonPath().getString("firstname"), "Updated");
        Assert.assertEquals(response.jsonPath().getString("lastname"), "Customer");
        Assert.assertEquals(response.jsonPath().getInt("totalprice"), 200);
        Assert.assertFalse(response.jsonPath().getBoolean("depositpaid"));
        Assert.assertEquals(response.jsonPath().getString("bookingdates.checkin"), "2026-12-02");
        Assert.assertEquals(response.jsonPath().getString("bookingdates.checkout"), "2026-12-06");
        Assert.assertEquals(response.jsonPath().getString("additionalneeds"), "Lunch");
    }

    @Test(dependsOnMethods = "updateBooking")
    public void deleteBookingAndVerifyItIsRemoved() {
        Assert.assertNotNull(token, "Authentication token must be available before deleting the booking");
        Assert.assertNotNull(bookingId, "Booking ID must be available before deleting the booking");

        Response deleteResponse = given()
                .cookie("token", token)
                .pathParam("bookingId", bookingId)
                .when()
                .delete("/booking/{bookingId}");

        Assert.assertEquals(deleteResponse.statusCode(), 201, "Booking deletion should return HTTP 201");

        Response getResponse = given()
                .pathParam("bookingId", bookingId)
                .when()
                .get("/booking/{bookingId}");

        Assert.assertEquals(getResponse.statusCode(), 404, "Deleted booking should return HTTP 404");
    }

    @Test
    public void updateSeparateBookingWithoutAuthentication() {
        Response createResponse = given()
                .contentType("application/json")
                .body("""
                        {
                          "firstname": "Unauthenticated",
                          "lastname": "Update",
                          "totalprice": 100,
                          "depositpaid": true,
                          "bookingdates": {
                            "checkin": "2026-12-01",
                            "checkout": "2026-12-05"
                          },
                          "additionalneeds": "Test fixture"
                        }
                        """)
                .when()
                .post("/booking");

        Assert.assertEquals(createResponse.statusCode(), 200, "Fixture booking creation should return HTTP 200");
        Integer separateBookingId = createResponse.jsonPath().getInt("bookingid");
        Assert.assertNotNull(separateBookingId, "Fixture booking response should include a booking ID");
        Assert.assertTrue(separateBookingId > 0, "Fixture booking ID should be positive");
        Assert.assertNotEquals(
                separateBookingId,
                bookingId,
                "Unauthenticated PUT must use a separate booking, not the original booking"
        );

        Response updateResponse = given()
                .contentType("application/json")
                .accept("application/json")
                .pathParam("bookingId", separateBookingId)
                .body("""
                        {
                          "firstname": "Should",
                          "lastname": "NotUpdate",
                          "totalprice": 200,
                          "depositpaid": false,
                          "bookingdates": {
                            "checkin": "2026-12-02",
                            "checkout": "2026-12-06"
                          },
                          "additionalneeds": "Unauthorized"
                        }
                        """)
                .when()
                .put("/booking/{bookingId}");

        Assert.assertEquals(
                updateResponse.statusCode(),
                403,
                "PUT without the authentication cookie should return HTTP 403"
        );
    }

    @Test
    public void getNonexistentBookingReturnsNotFound() {
        Response response = given()
                .when()
                .get("/booking/99999999");

        Assert.assertEquals(response.statusCode(), 404, "A nonexistent booking should return HTTP 404");
    }
}
