package voodoo;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class Wrapper {

    public static void main(String[] args) throws URISyntaxException {
        Properties props = new Properties();
        File propertiesFile = new File(new File(Wrapper.class.getProtectionDomain().getCodeSource().getLocation().toURI()), "../wrapper.properties");
        if(!propertiesFile.exists()) {
            System.err.println("cannot open file " + propertiesFile.getPath());
            System.exit(-1);
        }
        try {
            try(FileReader reader = new FileReader(propertiesFile)) {
                props.load(reader);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        String distributionUrl = props.getProperty("distributionUrl", null);

        if(distributionUrl == null) {
            System.err.println("missing key 'distributionUrl' in file " + propertiesFile.getPath());
            System.exit(-1);
        }

        String distributionPath = props.getProperty("distributionPath", null);;

        if(distributionPath == null) {
            System.err.println("missing key 'distributionPath' in file " + propertiesFile.getPath());
            System.exit(-1);
        }

        File binariesDir = new File(distributionPath);

        try {
            cleanup(binariesDir);
            launch(distributionUrl, binariesDir, args);
        } catch(Throwable t) {
            t.printStackTrace();
            System.err.println("Error: " + t.getLocalizedMessage());
            System.exit(-1);
        }
    }

    private static void cleanup(File binariesDir) {
        binariesDir.mkdirs();
        File[] files = binariesDir.listFiles((pathname -> pathname.getName().endsWith(".tmp")));
        for (File file : files) {
            file.delete();
        }
    }


    private static void launch(String distributionUrl, File binariesDir, String[] originalArgs) throws Throwable {
        String artifact = distributionUrl.substring(distributionUrl.lastIndexOf('/'));
        artifact = artifact.substring(0, artifact.lastIndexOf(".jar"));

        System.out.printf("Downloading the %s binary...%n", artifact);

//        File lastFile = new File(binariesDir, artifact + ".last.jar");

        File file;
        try {
            file = download(distributionUrl, binariesDir, artifact);
            if(!file.exists()) {
                throw new IllegalStateException("downloaded file does not seem to exist");
            }
        } catch(IOException e) {
            e.printStackTrace();
            System.err.printf("cannot download %s from %s%n", artifact, distributionUrl);
//            file = lastFile;
            System.exit(-1);
            return;
        }

        if(!file.exists()) {
            throw new IllegalStateException(String.format("binary %s does not exist", file.getPath()));
        }
//        Files.copy(file.toPath(), lastFile.toPath());
        System.out.printf("Loaded %s%n", file.getPath());
        String java = Paths.get(System.getProperty("java.home"), "bin", "java").toFile().getPath();
        File workingDir = new File(System.getProperty("user.dir"));

        String[] systemPropertyArgs;
//        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
//            Object key = entry.getKey();
//            Object value = entry.getValue();
//            System.out.println(key + ": " + value);
//            //TODO: systemPropertyArgs += "-D${key}=${value}"
//        }
        if(System.getProperty("kotlinx.coroutines.debug") != null) {
            systemPropertyArgs = new String[]{"-Dkotlinx.coroutines.debug"};
        } else {
            systemPropertyArgs = new String[0];
        }

        ArrayList<String> argsList = new ArrayList<>();
        argsList.add(java);
        argsList.addAll(Arrays.asList(systemPropertyArgs));
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

    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static File download(
            String distributionUrl,
            File outputDir,
            String artifact
    ) throws Exception {
        File targetFile = new File(outputDir, artifact + ".jar");
        File tmpFile = new File(targetFile.getParent(), targetFile.getName() + ".tmp");

        String md5;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(distributionUrl + ".md5").openStream(), StandardCharsets.UTF_8))) {
            md5 = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }

        if (targetFile.exists()) {
            String fileMd5 = toHexString(createMD5(targetFile));

            if (fileMd5.equalsIgnoreCase(md5)) {
                System.out.println("cached file matched md5 hash");
                return targetFile;
            }
        }

        try (BufferedInputStream in = new BufferedInputStream(new URL(distributionUrl).openStream());
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
                throw new IllegalArgumentException(String.format("%s did not match md5 hash: '%s' fileHash: %s", distributionUrl, md5, fileMd5));
            }
        }

        Files.copy(tmpFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        tmpFile.delete();

        return targetFile;
    }

}