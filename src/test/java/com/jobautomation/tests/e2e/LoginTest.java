package com.jobautomation.tests.e2e;

import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginTest extends BaseE2ETest {

    @Test(groups = "e2e", description = "Login with env credentials lands on the logged-in home page")
    public void loginSucceeds() {
        Assert.assertTrue(home.isLoggedIn(), "profile menu should be visible after login");
    }
}
