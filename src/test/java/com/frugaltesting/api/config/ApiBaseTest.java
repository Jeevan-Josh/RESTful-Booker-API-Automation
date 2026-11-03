package com.frugaltesting.api.config;

import io.restassured.RestAssured;
import org.testng.annotations.BeforeClass;

public abstract class ApiBaseTest {

    @BeforeClass
    protected void configureApiBaseUri() {
        RestAssured.baseURI = "https://restful-booker.herokuapp.com";
    }
}
