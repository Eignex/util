import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

/** Require every generated Maven publication to expose the same immutable snapshot pin. */
public class VerifySnapshot {
    static final String REPOSITORY = "https://central.sonatype.com/repository/maven-snapshots";
    record Coordinate(String group, String artifact) {}
    interface Fetcher { String fetch(String url) throws Exception; }

    static Element parse(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                .getDocumentElement();
    }

    static List<Element> children(Element parent, String name) {
        var result = new ArrayList<Element>();
        for (var node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && name.equals(element.getLocalName())) result.add(element);
        }
        return result;
    }

    static String text(Element parent, String name) {
        var matches = children(parent, name);
        return matches.isEmpty() ? "" : matches.get(0).getTextContent();
    }

    static List<Coordinate> publications(Path directory, String version) throws Exception {
        var result = new HashSet<Coordinate>();
        if (Files.isDirectory(directory)) {
            try (var paths = Files.newDirectoryStream(directory)) {
                for (var child : paths) {
                    var path = child.resolve("pom-default.xml");
                    if (!Files.isRegularFile(path)) continue;
                    var pom = parse(Files.readString(path));
                    require("http://maven.apache.org/POM/4.0.0".equals(pom.getNamespaceURI()) &&
                            version.equals(text(pom, "version")), "Unexpected publication version in " + path);
                    var group = text(pom, "groupId");
                    var artifact = text(pom, "artifactId");
                    require(!group.isEmpty() && !artifact.isEmpty(), "Missing publication coordinates in " + path);
                    result.add(new Coordinate(group, artifact));
                }
            }
        }
        require(!result.isEmpty(), "No generated publications found in " + directory);
        return result.stream().sorted(java.util.Comparator.comparing(Coordinate::group)
                .thenComparing(Coordinate::artifact)).toList();
    }

    static String snapshotPin(String metadata, String version) throws Exception {
        var root = parse(metadata);
        require(version.equals(text(root, "version")), "Snapshot metadata has the wrong base version");
        var pins = new HashSet<String>();
        var extensions = new HashSet<String>();
        for (var versioning : children(root, "versioning")) {
            for (var versions : children(versioning, "snapshotVersions")) {
                for (var entry : children(versions, "snapshotVersion")) {
                    pins.add(text(entry, "value"));
                    extensions.add(text(entry, "extension"));
                }
            }
        }
        require(pins.size() == 1 && extensions.containsAll(List.of("pom", "module")),
                "Snapshot metadata is incomplete or contains inconsistent pins");
        var pin = pins.iterator().next();
        var prefix = version.endsWith("SNAPSHOT") ? version.substring(0, version.length() - 8) : version;
        require(!pin.isEmpty() && pin.startsWith(prefix) && !pin.endsWith("SNAPSHOT"),
                "Invalid immutable snapshot pin: " + pin);
        return pin;
    }

    static String verify(String version, List<Coordinate> coordinates, Fetcher fetch) throws Exception {
        var pins = new HashMap<Coordinate, String>();
        for (var coordinate : coordinates) {
            var url = REPOSITORY + "/" + coordinate.group().replace('.', '/') + "/" +
                    coordinate.artifact() + "/" + version + "/maven-metadata.xml";
            pins.put(coordinate, snapshotPin(fetch.fetch(url), version));
        }
        require(new HashSet<>(pins.values()).size() == 1, "Publication pins disagree: " + pins);
        return pins.values().iterator().next();
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 2, "Usage: VerifySnapshot.java VERSION PUBLICATIONS_DIRECTORY");
        var coordinates = publications(Path.of(args[1]), args[0]);
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        Fetcher fetch = url -> {
            var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new java.io.IOException("HTTP " + response.statusCode() + ": " + url);
            return response.body();
        };
        // Repository metadata can take a moment to become visible after the final upload.
        for (int attempt = 0; attempt < 6; attempt++) {
            try {
                System.out.println(verify(args[0], coordinates, fetch));
                return;
            } catch (java.io.IOException | IllegalArgumentException | org.xml.sax.SAXException error) {
                if (attempt == 5) throw error;
                Thread.sleep(5000);
            }
        }
    }
}
