package com.adobe.cqunittest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.Scanner;

import org.apache.sling.testing.tools.osgi.WebconsoleClient;
import org.apache.sling.testing.tools.sling.BundlesInstaller;

public class CQUnitTestUtil {
	private final static String TEST_SERVER_URL = "/system/sling/junit/";
	private final static String JSON_SUFFEX = ".json";
	private final static String SEPARATOR_LINE = "--------------------------------------\n";
	private final static String DEFAULT_CQ_SERVER_URL = "http://localhost:4502";
	private static String DEFAULT_USERNAME = "admin";
	private static String DEFAULT_PASSWORD = "admin";
	private static int totalNum = 0;
	private static int failureNum = 0;
	private static int ignoreNum = 0;
	private static String testName = null;

	private static String username;
	private static String password;
	private static String cqServerURL;

	private static void init() {
		InputStream in;
		try {
			in = new BufferedInputStream(CQUnitTestUtil.class.getResourceAsStream("test-config.properties"));
			Properties prop = new Properties();
			prop.load(in);
			username = prop.getProperty("username", DEFAULT_USERNAME);
			password = prop.getProperty("password", DEFAULT_PASSWORD);
			cqServerURL = prop.getProperty("cqserverurl", DEFAULT_CQ_SERVER_URL);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String getBundlePath() {
		String path = "";
		File currentFolder = new File("target");

		if (currentFolder.exists()) {
			for (String filename : currentFolder.list()) {
				if (filename.endsWith("jar") && !filename.endsWith("sources.jar")) {
					path = currentFolder.getAbsolutePath() + File.separator + filename;
				}
			}
		}
		return path;
	}

	public static boolean installBundle() throws Exception {
		init();
		// install the bundle to CQ by sling testing tools
		WebconsoleClient client = new WebconsoleClient(cqServerURL, username, password);
		String pathname = getBundlePath();
		File bundleFile = new File(pathname);
		client.installBundle(bundleFile, true);
		BundlesInstaller installer = new BundlesInstaller(client);
		String symbolicName = installer.getBundleSymbolicName(bundleFile);
		client.checkBundleInstalled(symbolicName, 5);
		String state = client.getBundleState(symbolicName);
		if (state.toLowerCase().equals("active")) {
			System.out.println("Bundle has been Activated");
			return true;
		} else {
			System.err.println("Bundle has not been Activated");
			return false;
		}
	}

	public static boolean executeMvn() throws IOException {
		boolean succ = false;
		Runtime rt = Runtime.getRuntime();
		Process proc = rt.exec("cmd /c mvn clean install  -Dmaven.test.skip=true");
		// output the command lines to the console
		StringBuilder cmdLines = new StringBuilder();
		InputStream stdin = proc.getInputStream();
		BufferedInputStream buff = new BufferedInputStream(stdin);
		Scanner scanner = new Scanner(buff);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			System.out.println(line);
			cmdLines.append(line);
		}
		if (cmdLines.toString().contains("BUILD SUCCESS")) {
			succ = true;
		} else {
			succ = false;
		}
		return succ;
	}

	private static void sendPostRequest(URLConnection connection) {
		try {
			OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
			out.flush();
			out.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void getResponseContent(URLConnection connection) {
		BufferedReader in;
		StringBuilder content = new StringBuilder();
		StringBuilder rawJSON = new StringBuilder();
		System.err.println(testName);
		try {
			in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line = null;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				rawJSON.append(line).append("\n");
				if (line.startsWith("}") || line.contains("INFO_TYPE") || line.startsWith("[")
						|| line.startsWith("]")) {
					continue;
				} else {
					if (line.startsWith("\"description\"")) {
						content.append(SEPARATOR_LINE);
						totalNum++;
					} else if (line.startsWith("\"failure\"")) {
						failureNum++;
					} else if (line.startsWith("\"ignore\"")) {
						ignoreNum++;
					}
					if (line.startsWith("\"stack trace")) {
						char[] chars = line.toCharArray();
						for (int i = 0; i < chars.length; i++) {
							char c = chars[i];
							if (c =='\\') {
								if (i< chars.length -1 && chars[i+1] == 't') {
									content.append("\n");
								}
								i +=1;
							} else {
								content.append(c);
							}
						}
						content.append("\n");
					} else {
						content.append(line).append("\n");
					}
				}
			}

			System.out.println(content.toString());
			System.out.println("Total tests: " + totalNum + " Failures: " + failureNum + " Ignored: "
					+ ignoreNum);
//			System.out.println(rawJSON.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// private static String escapeHtml(String content) {
	// content = content.replaceAll("&amp;", "&").replaceAll("&lt;",
	// "<").replaceAll("&gt;", ">")
	// .replaceAll("&apos;", "\'").replaceAll("&quot;",
	// "\"").replaceAll("&nbsp;", " ")
	// .replaceAll("&copy;", "@").replaceAll("&reg;", "?");
	// return content;
	// }

	public static void runTest(Class<?> klass) {
		URL url;
		testName = klass.getName();
		try {
			url = new URL(cqServerURL + TEST_SERVER_URL + testName + JSON_SUFFEX);
			URLConnection connection = url.openConnection();
			connection.setDoOutput(true);
			sendPostRequest(connection);
			System.out.println("Response Code: " + ((HttpURLConnection) connection).getResponseCode());
			getResponseContent(connection);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
