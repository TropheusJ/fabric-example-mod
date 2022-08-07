package io.github.tropheusj.auto_maintainer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.google.gson.JsonParser;

import groovy.lang.Closure;

import io.github.tropheusj.auto_maintainer.minecraft.Minecraft;

import org.gradle.api.Project;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Properties;

public abstract class Util {
	private Util() {
	}

	public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(20))
			.version(Version.HTTP_2)
			.build();
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static Properties getGradleProperties(Project project) {
		File file = project.file("gradle.properties");
		Properties properties = new Properties();
		try (FileInputStream in = new FileInputStream(file)) {
			properties.load(in);
		} catch (IOException e) {
			throw new RuntimeException("Failed to load gradle.properties file!");
		}
		return properties;
	}

	public static String stringFromUrl(String url) {
		HttpRequest request = HttpRequest.newBuilder()
				.timeout(Duration.ofSeconds(20))
				.uri(URI.create(url))
				.build();
		try {
			HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString());
			return response.body();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Failed to fetch JSON from URL! URL: " + url, e);
		}
	}

	public static JsonElement jsonFromUrl(String url) {
		return JsonParser.parseString(stringFromUrl(url));
	}

	public static Element xmlFromUrl(String url) {
		String data = stringFromUrl(url);
		try (ByteArrayInputStream input = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))) {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(input);
			return doc.getDocumentElement();
		} catch (IOException | ParserConfigurationException | SAXException e) {
			throw new RuntimeException(e);
		}
	}

	public static Element subElement(Element parent, String key) {
		return (Element) parent.getElementsByTagName(key).item(0);
	}

	public static String snakeCase(String string) {
		return string.toLowerCase(Locale.ROOT).replace(" ", "_");
	}

	public static void checkNull(String property, String name, String expectedKey) {
		if (property == null) {
			throw new RuntimeException(String.format(
					"'%s' could not find it's current version; Expected a key in gradle.properties matching '%s'\n",
					name, expectedKey
			));
		}
	}

	public static String getMcVer() {
		return Minecraft.INSTANCE.versions.newVer();
	}

	public static String getVersionTarget(String version) {
		for (JsonElement element : Minecraft.INSTANCE.manifest.versions) {
			JsonObject entry = element.getAsJsonObject();
			String id = entry.get("id").getAsString();
			if (!version.equals(id))
				continue;
			String dataUrl = entry.get("url").getAsString();
			JsonObject data = Util.jsonFromUrl(dataUrl).getAsJsonObject();
			return data.get("assets").getAsString();
		}
		throw new RuntimeException("Could not find version in manifest:" + version);
	}

	public static String versionToSemver(String version) {
		String target = getVersionTarget(version);
		if (version.equals(target))
			return version;
		String year = version.substring(0, 2);
		String week = version.substring(3, 5);
		String end = version.substring(5);
		String format = "%s-alpha.%s.%s.%s";
		if (!Minecraft.SNAPSHOT.matcher(version).find())
			format = "%s-%s.w.%s.%s"; // special snapshots are different because yes
		return String.format(format, target, year, week, end);
	}

	/**
	 * Create runGametest task.
	 * Jank since loom can't be compiled against and used in the project at the same time.
	 */
	public static void createGameTestTask(Project project) {
		Object loomConfig = project.getExtensions().getByName("loom");
		try {
			Method runs = loomConfig.getClass().getDeclaredMethod("runs", Closure.class);
			Closure<?> closure = new Closure<>(new Object()) {
				private void doCall(Object container) {
					try {
						Method register = container.getClass().getDeclaredMethod("register", String.class, Closure.class);
						Closure<?> closure = new Closure<>(this) {
							private void doCall(Object runConfigSettings) {
								Class<?> c = runConfigSettings.getClass();
								try {
									c.getDeclaredMethod("server").invoke(runConfigSettings);
									c.getDeclaredMethod("name", String.class).invoke(runConfigSettings, "Minecraft Test");
									Method vmArg = c.getDeclaredMethod("vmArg", String.class);
									vmArg.invoke(runConfigSettings, "-Dfabric-api.gametest");
									vmArg.invoke(runConfigSettings, "-Dfabric-api.gametest.report-file=" + project.getBuildDir() + "/junit.xml");
									c.getDeclaredMethod("runDir", String.class).invoke(runConfigSettings, "build/gametest");
								} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
									throw new RuntimeException(e);
								}
							}
						};
						register.invoke(container, "gametest", closure);
					} catch (Throwable t) {
						throw new RuntimeException(t);
					}
				}
			};
			runs.invoke(loomConfig, closure);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}
}
