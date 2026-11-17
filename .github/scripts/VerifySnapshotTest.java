import java.nio.file.Files;
import java.util.List;

/** Offline release-verification checks, runnable with the JDK used by CI. */
public class VerifySnapshotTest {
    static final String VERSION = "0.0.1-ci.123.1-SNAPSHOT";
    static final String PIN = "0.0.1-ci.123.1-20261116.120023-1";
    interface Check { void run() throws Exception; }

    static String metadata(String pin, String... extensions) {
        var entries = new StringBuilder();
        for (var extension : extensions) entries.append("<snapshotVersion><extension>").append(extension)
                .append("</extension><value>").append(pin).append("</value></snapshotVersion>");
        return "<metadata><version>" + VERSION + "</version><versioning><snapshotVersions>" + entries +
                "</snapshotVersions></versioning></metadata>";
    }

    static void equal(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }

    static void rejects(String message, Check check) throws Exception {
        try {
            check.run();
        } catch (IllegalArgumentException error) {
            if (!error.getMessage().contains(message)) throw error;
            return;
        }
        throw new AssertionError("Expected rejection: " + message);
    }

    public static void main(String[] args) throws Exception {
        var coordinates = List.of(new VerifySnapshot.Coordinate("com.eignex", "util"),
                new VerifySnapshot.Coordinate("com.eignex", "util-jvm"),
                new VerifySnapshot.Coordinate("com.eignex", "util-iosarm64"));
        equal(PIN, VerifySnapshot.verify(VERSION, coordinates, url -> metadata(PIN, "pom", "module", "klib")));
        rejects("Publication pins disagree", () -> VerifySnapshot.verify(VERSION, coordinates,
                url -> metadata(url.contains("/util-iosarm64/") ? PIN + "3" : PIN, "pom", "module")));
        rejects("incomplete", () -> VerifySnapshot.snapshotPin(metadata(PIN, "pom", "klib"), VERSION));
        rejects("inconsistent pins", () -> VerifySnapshot.snapshotPin(
                metadata(PIN, "pom", "module").replace("<extension>module</extension><value>" + PIN,
                        "<extension>module</extension><value>" + PIN + "3"), VERSION));
        rejects("wrong base version", () -> VerifySnapshot.snapshotPin(metadata(PIN, "pom", "module"), "other-SNAPSHOT"));
        rejects("Invalid immutable snapshot pin", () -> VerifySnapshot.snapshotPin(metadata(VERSION, "pom", "module"), VERSION));
        var directory = Files.createTempDirectory("verify-snapshot-");
        try {
            rejects("No generated publications", () -> VerifySnapshot.publications(directory, VERSION));
            var path = Files.createDirectory(directory.resolve("iosArm64")).resolve("pom-default.xml");
            Files.writeString(path, "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">" +
                    "<groupId>com.eignex</groupId><artifactId>util-iosarm64</artifactId><version>" + VERSION +
                    "</version></project>");
            equal(List.of(coordinates.get(2)), VerifySnapshot.publications(directory, VERSION));
            rejects("Unexpected publication version", () -> VerifySnapshot.publications(directory, "other-SNAPSHOT"));
        } finally {
            try (var paths = Files.walk(directory)) {
                for (var path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("Snapshot publication checks passed");
    }
}
