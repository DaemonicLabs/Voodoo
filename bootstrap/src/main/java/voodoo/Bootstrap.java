package voodoo;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

public class Bootstrap {
    private static final File binariesDir = new File("../.voodoo/");
    private static final File lastFile = new File(binariesDir, "newest.jar");
    private static final Properties props = new Properties();

    static {
        try {
            props.load(Bootstrap.class.getResourceAsStream("/maven.properties"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static final String mavenUrl = props.getProperty("url");
    private static final String group = props.getProperty("group");
    private static final String artifact = props.getProperty("name");
    private static final String classifier = props.getProperty("classifier");

    public static void main(String[] args) {
        try {
            cleanup();
            launch(args);
        } catch(Throwable t) {
            t.printStackTrace();
            System.err.println("Error: " + t.getLocalizedMessage());
            System.exit(-1);
        }
    }

    private static void cleanup() {
        binariesDir.mkdirs();
        File[] files = binariesDir.listFiles((pathname -> pathname.getName().endsWith(".tmp")));
        for (File file : files) {
            file.delete();
        }
    }


    private static void launch(String[] originalArgs) throws Throwable {
        System.out.printf("Downloading the %s binary...%n", artifact);

        File file;
        try {
            file = download();
            if(!file.exists()) {
                throw new IllegalStateException("downloaded file does not seem to exist");
            }
        } catch(IOException e) {
            e.printStackTrace();
            System.err.printf("cannot download %s from %s, trying to reuse last binary%n", artifact, mavenUrl);
            file = lastFile;
        }

        if(!file.exists()) {
            throw new IllegalStateException(String.format("binary %s does not exist", file.getPath()));
        }
        System.out.printf("Loaded %s%n", file.getPath());
        String java = Paths.get(System.getProperty("java.home"), "bin", "java").toFile().getPath();
        File workingDir = new File(System.getProperty("user.dir"));

        String[] debugArgs;
        if(System.getProperty("kotlinx.coroutines.debug") != null) {
            debugArgs = new String[]{"-Dkotlinx.coroutines.debug"};
        } else {
            debugArgs = new String[0];
        }

        ArrayList<String> argsList = new ArrayList<>();
        argsList.add(java);
        argsList.addAll(Arrays.asList(debugArgs));
        argsList.add("-jar");
        argsList.add(file.getPath());
        argsList.addAll(Arrays.asList(originalArgs));

        String[] args = argsList.toArray(new String[0]);

        System.out.printf("Executing %s", argsList.toString());
        int exitStatus = new ProcessBuilder(args)
            .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
                .waitFor();
        System.exit(exitStatus);
    }

    private static File download() throws Exception {
        String groupPath = group.replace( '.', '/');
        String mavenMetadataUrl = mavenUrl + '/' + groupPath + '/' + artifact + "/maven-metadata.xml";
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse((new URL(mavenMetadataUrl)).openStream());
        XPathFactory xpFactory = XPathFactory.newInstance();
        XPath xPath = xpFactory.newXPath();
        String xpath = "/metadata/versioning/release/text()";
        String releaseVersion = (String) xPath.evaluate(xpath, doc, XPathConstants.STRING);
        return downloadArtifact(mavenUrl, group, artifact, releaseVersion, classifier, "jar", binariesDir);
    }

    public static byte[] createMD5(File file) throws Exception {
        InputStream fis =  new FileInputStream(file);

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return complete.digest();
    }

    private static final String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static  File downloadArtifact(String mavenUrl,
                                         String group,
                                         String artifactId,
                                         String version,
                                         String classifier,
                                         String extension,
                                         File outputDir
    ) throws Exception {
        String groupPath;
        String classifierSuffix;
        groupPath = group.replace('.', '/');
        if (classifier != null) {
            classifierSuffix = '-' + classifier;
        } else {
            classifierSuffix = "";
        }

        String artifactUrl = mavenUrl + '/' + groupPath + '/' + artifactId + '/' + version + '/' + artifactId + '-' + version + classifierSuffix + '.' + extension;
        File tmpFile = new File(outputDir, artifactId + '-' + version + classifierSuffix + '.' + extension + ".tmp");
        File targetFile = new File(outputDir, artifactId + '-' + version + classifierSuffix + '.' + extension);

        String md5;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(artifactUrl + ".md5").openStream(), StandardCharsets.UTF_8))) {
            md5 = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }

        if (targetFile.exists()) {
            String fileMd5 = toHexString(createMD5(targetFile));

            if (fileMd5.equalsIgnoreCase(md5)) {
                System.out.println("cached file matched md5 hash");
                return targetFile;
            }
        }

        try (BufferedInputStream in = new BufferedInputStream(new URL(artifactUrl).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(tmpFile)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }

        {
            String fileMd5 = toHexString(createMD5(tmpFile));
            if(!fileMd5.equalsIgnoreCase(md5)) {
                throw new IllegalArgumentException(String.format("%s did not match md5 hash: '%s' file: %s", artifactUrl, md5, fileMd5));
            }
        }

        Files.copy(tmpFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        tmpFile.delete();

        return targetFile;
    }

}
