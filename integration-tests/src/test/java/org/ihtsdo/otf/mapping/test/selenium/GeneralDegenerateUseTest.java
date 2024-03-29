package org.ihtsdo.otf.mapping.test.selenium;

import static org.junit.Assert.assertTrue;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Selenium login test class.
 */
public class GeneralDegenerateUseTest {

  /** The web driver. */
  static WebDriver webDriver;

  /** The config. */
  private static Properties config;

  /**
   * Sets up the class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {

    // get the config properties
    config = ConfigUtility.getConfigProperties();

    // construct a new webdriver
    switch (config.getProperty("selenium.browser")) {
      case "firefox":
        webDriver = new FirefoxDriver();
        break;
      case "chrome":
        webDriver = new ChromeDriver();
        break;
      case "ie":
        webDriver = new InternetExplorerDriver();
        break;
      default:
        throw new Exception(
            "Invalid browser specified in config file.  Valid options are: firefox, chrome, ie");
    }

    webDriver
        .manage()
        .timeouts()
        .implicitlyWait(new Long(config.getProperty("selenium.timeout")),
            TimeUnit.SECONDS);
  }

  /**
   * Teardown for class
   * @throws Exception the exception
   */
  @AfterClass
  public static void teardownClass() throws Exception {

    if (webDriver != null) {
      webDriver.quit();
    }
  }


  /**
   * User invalid login test.
   */
  @SuppressWarnings("static-method")
  @Test
  public void testDegenerateUseGuiGeneral001() {

    Logger.getLogger(GeneralDegenerateUseTest.class).info("Testing invalid user login...");

    // Open website
    webDriver.get(config.getProperty("selenium.url"));

    // fill in the user name from created valid user
    webDriver.findElement(By.id("userField")).sendKeys(
        config.getProperty("selenium.user.invalid.name"));

    // fill in the password from config file
    webDriver.findElement(By.id("passwordField")).sendKeys("invalid_password");

    // login
    webDriver.findElement(By.id("userLoginButton")).click();

    // Find the header content and test once injection is complete
    (new WebDriverWait(webDriver, new Long(
        config.getProperty("selenium.timeout")) / 1000))
        .until(new ExpectedCondition<Boolean>() {
          @Override
          public Boolean apply(WebDriver d) {
            return webDriver.findElement(By.id("globalError")).getText()
                .length() > 0;
          }
        });

    // verify that string matches pattern Text (Text)
    assertTrue(webDriver.findElement(By.id("globalError")).getText().length() > 0);

    Logger.getLogger(GeneralDegenerateUseTest.class).info("  PASS");
  }
}
