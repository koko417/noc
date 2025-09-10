package tanabu.noc.view;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v139.network.Network;
import org.openqa.selenium.devtools.v139.network.Network.GetResponseBodyResponse;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tanabu.noc.model.*;
import tanabu.noc.Main;
import tanabu.noc.event.EventBus;

//TODO: Change to use JSONPATH
public class ClassroomOb {

	public ClassroomOb(String target, String userDataDir, EventBus eb) throws InterruptedException {
		ChromeOptions options = new ChromeOptions();
		// options.setBinary(BINARY);


		options.addArguments("--no-sandbox");
		options.addArguments("--disable-dev-shm-usage");
		options.addArguments("--disable-gpu");
		options.addArguments("--remote-allow-origins=*");
		options.addArguments("--remote-debugging-port=9222");
		options.addArguments("--headless=new");
		options.addArguments("--user-data-dir=" + userDataDir);
		ChromeDriver driver = new ChromeDriver(options);		
		
		DevTools devTools = driver.getDevTools();
		devTools.createSession();
		devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));

		devTools.addListener(Network.responseReceived(), res -> {
			try {
				GetResponseBodyResponse info = devTools.send(Network.getResponseBody(res.getRequestId()));
				ObjectMapper mapper = new ObjectMapper();
				String raw = info.getBody();
				String url = res.getResponse().getUrl();

				if (url.contains("rpcids=UG41I")) {
					String json = raw.split("\n")[raw.substring(0, raw.indexOf("[[")).split("\n").length];

					String tmp = mapper.readTree(json).get(0).get(2).asText();
					JsonNode node = mapper.readTree(tmp);
					for (JsonNode c : node.get(2)) {
						eb.callEvent(new User(c));
					}
				}
				if (url.contains("rpcids=tQShAc")) {
					String json = raw.split("\n")[raw.substring(0, raw.indexOf("[[")).split("\n").length];

					String tmp = mapper.readTree(json).get(0).get(2).asText();
					JsonNode node = mapper.readTree(tmp);
					for (JsonNode c : node.get(1)) {
						eb.callEvent(new FileInfo(c.get(2).get(2).get(0)));
					}
					clickShowButton(driver);
				}
				if (url.contains("rpcids=pONvgf")) {
					String json = raw.split("\n")[raw.substring(0, raw.indexOf("[[")).split("\n").length];
					String tmp = mapper.readTree(json).get(0).get(2).asText();
					JsonNode node = mapper.readTree(tmp);
					for (JsonNode c : node.get(2)) {
						if (c.size() == 3) {
							eb.callEvent(new Comment(c.get(2)));
						} else if (c.size() == 2) {
							eb.callEvent(new Task(c.get(1)));
						}
					}
					clickShowButton(driver);
				}
				
			} catch (Exception e) {
			}
		});

		driver.get(target);
		CountDownLatch latch = new CountDownLatch(1);
	    latch.await();  

	    //TODO: Threading manager
	    devTools.close();
	    driver.quit();
	}

	private static void clickShowButton(WebDriver driver) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
		WebElement button = wait
				.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[aria-label='表示']")));
		button.click();
	}

}
