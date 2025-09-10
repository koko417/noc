package tanabu.noc;

import java.io.FileWriter;


import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import tanabu.noc.configuration.ClassroomConfig;
import tanabu.noc.configuration.ManageConfig;
import tanabu.noc.configuration.RootConfig;
import tanabu.noc.database.Database;
import tanabu.noc.event.EventBus;
import tanabu.noc.model.*;
import tanabu.noc.view.ClassroomOb;
import tanabu.noc.controller.*;

public class Main {
	// private static final String BINARY = "/usr/bin/chromium-browser";
	private static String p = Paths.get("").toAbsolutePath().toString();
	private static final String userDataDir = p+"/CH1";
	private static Database db;
	private static RootConfig config;

	public static void main(String[] args) throws Exception {
		new Main();
	}

	public Main() throws Exception {
		// Configuration
		ManageConfig mc = new ManageConfig(p+"/config.yml");
		config = mc.load();
		// TEST---------------------
		getLoggedInDriver();
		// Database
		this.db = new Database();
		// EventBus
		EventBus eb = new EventBus();
		// Listener
		eb.register(User.class, new UserListener());
		eb.register(FileInfo.class, new FileListener());
		eb.register(Task.class, new TaskListener());
		eb.register(Comment.class, new CommentListener());
		// Viewer
		ExecutorService executor = Executors.newFixedThreadPool(config.classrooms.size());
		for (ClassroomConfig cc : config.classrooms) {
			executor.submit(() -> new ClassroomOb(cc.url, userDataDir, eb));
		}
		executor.shutdown();

	}

	public static Database getDb() {
		return db;
	}
	
	public static int COMMENT = 0x00;
	public static int TASK = 0x01;
	public synchronized static void notifier(int type, String commentId) {
		Socket socket = new Socket();
        try {
        	socket = new Socket("127.0.0.1", 50007);
        	socket.setSoTimeout(10000);
            OutputStream out = socket.getOutputStream();
            out.write(type);
            out.write(commentId.getBytes("UTF-8"));
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }

	private static void getLoggedInDriver() throws Exception {

		ChromeOptions options = new ChromeOptions();
		// options.setBinary(BINARY);
		//options.addArguments("--headless=new");
		options.addArguments("--user-data-dir=" + userDataDir);

		WebDriver driver = new ChromeDriver(options);

		driver.get("https://accounts.google.com/signin");

		try {
		driver.findElement(By.id("identifierId")).sendKeys(config.user);
		}catch (Exception e) { driver.quit(); return; }
		driver.findElement(By.id("identifierNext")).click();
		
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
		wait.until(
		ExpectedConditions.visibilityOfElementLocated(By.name("Passwd"))
		).sendKeys(config.pass);
		driver.findElement(By.id("passwordNext")).click();
		driver.quit();
		    
	}
}