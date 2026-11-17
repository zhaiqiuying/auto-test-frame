package com.example.qa.btest.engine.monitor.api;

import com.example.qa.btest.model.TestContext;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReceiverManager {

    public static List<Receiver> receiverList;

    static {
        try {
            receiverList = initReceiverList();
        } catch (IOException e) {
            log.error("Failed to init receiver list", e);
        }
    }

    private static List<Receiver> initReceiverList() throws IOException {
        return ReciverLoader.loadRecivers();
    }

    public static void notice(String msg) {
        TestContext testContext = new TestContext();

        for (Receiver receiver : receiverList) {
            receiver.notice(testContext);
        }
    }

    public static class ReciverLoader {

        private static final String RECEIVER_URL_PATH = "test-monitor/";
        public static List<Receiver> loadRecivers() throws IOException {
            List<Receiver> receiverList = load(Receiver.class, getReviverJarFiles(RECEIVER_URL_PATH, ReciverLoader.class));
            return receiverList;
        }

        private static <T> List<T> load(Class<T> receiverClass,
                                           Map<String, URL> reviverJarFiles) throws IOException {

            URL[] urls = new URL[reviverJarFiles.size()];
            int i = 0;
            for (Map.Entry<String, URL> entry : reviverJarFiles.entrySet()) {
                File tempFile = createTempFile(entry.getKey(), entry.getValue());
                urls[i++] = tempFile.toURI().toURL();
            }
            URLClassLoader urlClassLoader =  new URLClassLoader(urls, receiverClass.getClassLoader());
            return load(receiverClass, urlClassLoader);
        }

        public static File createTempFile(String jarName, URL url)
                throws IOException {
            InputStream jarStream = url.openStream();
            if (jarStream == null) {
                throw new FileNotFoundException(jarName + ".jar");
            }
            File file = File.createTempFile(jarName, ".jar", null);
            file.deleteOnExit();

            OutputStream out = new FileOutputStream(file);
            try {
                copy(jarStream, out, 8096, true);
                return file;
            } finally {
                out.close();
            }
        }

        private static int copy(InputStream input, OutputStream output, int bufferSize, boolean closeStreams)
                throws IOException {
            try {
                byte[] buffer = new byte[bufferSize];
                int count = 0;
                int n = 0;
                while (-1 != (n = input.read(buffer))) {
                    output.write(buffer, 0, n);
                    count += n;
                }
                return count;
            } finally {
                if (closeStreams) {
                    input.close();
                    output.close();
                }
            }
        }

        private static <T> List<T> load(Class<T> receiverClass, URLClassLoader urlClassLoader) {
            ServiceLoader<T> serviceLoader =  ServiceLoader.load(receiverClass, urlClassLoader);
            List<T> result = new ArrayList<>();
            for (T t : serviceLoader) {
                result.add(receiverClass.cast(t));
            }
            return result;
        }

        private static Map<String, URL> getReviverJarFiles(String path, Class clazz) {
            List<String> jarNames = findJarNames(path);
            Map<String, URL> result = new HashMap<String, URL>(jarNames.size());
            for (String jarName : jarNames) {
                String name = jarName.substring(jarName.lastIndexOf("/") + 1, jarName.lastIndexOf("."));

                URL url = clazz.getResource("/" + jarName);
                result.put(name, url);
            }
            return result;
        }

        private static List<String> findJarNames(String path) {
            Pattern pattern = Pattern.compile(path + "(.*).jar");
            return findAgentJarFileNames(pattern);
        }

        public static List<String> findAgentJarFileNames(Pattern pattern) {
            URL agentJarUrl = getAgentJarUrl();
            log.info("agent jar path: {}", agentJarUrl.getPath());
            return findJarFileNames(agentJarUrl, pattern);
        }

        public static List<String> findJarFileNames(URL agentJarUrl, Pattern pattern) {
            JarFile jarFile = null;
            try {
                jarFile = getAgentJarFile(agentJarUrl);

                List<String> names = new ArrayList();
                for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
                    JarEntry jarEntry = entries.nextElement();
                    if (pattern.matcher(jarEntry.getName()).matches()) {
                        names.add(jarEntry.getName());
                    }
                }
                return names;
            } catch (Exception e) {
            } finally {
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (IOException localIOException2) {}
                }
            }
            return Collections.emptyList();
        }

        public static URL getAgentJarUrl() {
            return ReciverLoader.class.getProtectionDomain().getCodeSource().getLocation();
        }

        private static JarFile getAgentJarFile(URL agentJarUrl) {
            if (agentJarUrl == null) {
                return null;
            }
            try {
                return new JarFile(getAgentJarFileName(agentJarUrl));
            } catch (IOException e) {}
            return null;
        }

        private static String getAgentJarFileName(URL agentJarUrl) {
            if (agentJarUrl == null) {
                return null;
            }
            try {
                return URLDecoder.decode(agentJarUrl.getFile().replace("+", "%2B"), "UTF-8");
            } catch (IOException e) {}
            return null;
        }
    }
}
