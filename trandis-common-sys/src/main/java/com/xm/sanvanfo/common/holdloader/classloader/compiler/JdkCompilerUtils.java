package com.xm.sanvanfo.common.holdloader.classloader.compiler;

import javax.tools.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"unused", "WeakerAccess"})
public class JdkCompilerUtils {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([$_a-zA-Z][$_a-zA-Z0-9.]*);");

    private static final Pattern CLASS_PATTERN = Pattern.compile("class\\s+([$_a-zA-Z][$_a-zA-Z0-9]*)\\s+");

    private static final ArrayList<String> defaultOption = new ArrayList<>();

    static {
        defaultOption.add("-source");
        defaultOption.add("1.8");
        defaultOption.add("-target");
        defaultOption.add("1.8");
    }

    public static String doCompile(ArrayList<String> options, Path path) throws Exception {
        ArrayList<String> option = options;
        if(null == options || options.size() == 0) {
            option = defaultOption;
        }
        byte[] bytes = Files.readAllBytes(path);
        String content = new String(bytes);
        String packageName = getPackageName(content);
        String className = getClassName(content);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        try(StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticCollector, null, null)) {
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnosticCollector, option, null, fileManager.getJavaFileObjects(path.toFile()));
            boolean ret = task.call();
            if (!ret) {
                StringBuilder error = new StringBuilder();
                for (Diagnostic diagnostic : diagnosticCollector.getDiagnostics()) {
                    error.append(String.format(
                            "Code: %s%n" +
                                    "Kind: %s%n" +
                                    "Position: %s%n" +
                                    "Start Position: %s%n" +
                                    "End Position: %s%n" +
                                    "Source: %s%n" +
                                    "Message: %s%n",
                            diagnostic.getCode(), diagnostic.getKind(),
                            diagnostic.getPosition(), diagnostic.getStartPosition(),
                            diagnostic.getEndPosition(), diagnostic.getSource(),
                            diagnostic.getMessage(null)));
                }
                throw new Exception(error.toString());
            }
            return String.format("%s/%s.class", packageName.replaceAll("\\.", "/"), className);
        }
    }

    public static String getPackageName(String code) {
        code = code.trim();
        Matcher matcher = PACKAGE_PATTERN.matcher(code);
        String pkg;
        if (matcher.find()) {
            pkg = matcher.group(1);
        } else {
            pkg = "";
        }
        return pkg;
    }

    public static String getClassName(String code) {
        code = code.trim();
        Matcher matcher = CLASS_PATTERN.matcher(code);
        String cls;
        if (matcher.find()) {
            cls = matcher.group(1);
        } else {
            throw new IllegalArgumentException("No such class name in " + code);
        }
        return cls;
    }
}
