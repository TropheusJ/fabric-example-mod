package io.github.tropheusj.auto_maintainer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

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

	/**
	 * Convert a Minecraft version into a loader-compatible semver version.
	 * For Releases, Release Candidates, Pre-releases, and special snapshots, it will be properly converted.
	 * For regular snapshots or other versions, an asterisk '*' will be returned,
	 * since it's not possible to find the data loader wants.
	 * | 		type		 | 	 	  version         | 		  semver		 	 | 		return	   |
	 * -------------------------------------------------------------------------------------------------
	 * | 	  release 		 |	       1.19.2    	  |      	  1.19.2		   	 |      1.19.2	   |
	 * |  release candidate  |       1.17.1-rc2 	  | 	    1.17.1-rc.2	 		 | 	 1.17.1-rc.2   |
	 * | 	pre release 	 |       1.19.1-pre2      | 	   1.19.1-beta.2 	 	 | 	1.19.1-beta.2  |
	 * | 	 snapshot 		 |    	  22w18a 	  	  |     1.19-alpha.22.18.a 		 |		  *		   |
	 * | 	  other		 	 |	22w13oneBlockAtATime  | 1.19-22.w.13.oneBlockAtATime |		  *		   |
	 */
	public static String versionToSemver(String version) {
		if (Minecraft.RELEASE.matcher(version).matches())
			return version;
		if (version.indexOf('-') != -1) {
			String[] split = version.split("-");
			String mcVer = split[0];
			if (Minecraft.RELEASE.matcher(mcVer).matches()) {
				String type = null;
				String suffix = split[1];
				int rcIndex = suffix.indexOf("rc");
				int preIndex = suffix.indexOf("pre");
				if (rcIndex != -1) {
					type = "rc";
					suffix = suffix.substring(rcIndex + "rc".length());
				} else if (preIndex != -1) {
					type = "beta";
					suffix = suffix.substring(preIndex + "pre".length());
				}
				if (type != null) {
					try {
						int number = Integer.parseInt(suffix);
						if (number > 0) {
							return "%s-%s.%s".formatted(mcVer,  type, number);
						}
					} catch (NumberFormatException ignored) {
					}
				}
			}
		}
		return "*";
	}
}
