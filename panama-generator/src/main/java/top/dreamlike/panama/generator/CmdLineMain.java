package top.dreamlike.panama.generator;

import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CmdLineMain {
    public static void main(String[] args) throws IOException {

        var jarPath = "/home/dreamlike/java-code/PanamaUring/graal-native-example/target/graal-native-example-fat.jar";
        File file = new File(jarPath);
        if (!file.exists()) {
            System.err.println(STR."\{args[0]} dont exist!");
            return;
        }

        JarFile jarFile = new JarFile(file);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            if (!jarEntry.isDirectory() && jarEntry.getName().endsWith(".class")) {
                try (var inputStream = jarFile.getInputStream(jarEntry)) {
                    byte[] classFile = inputStream.readAllBytes();
                    ClassReader classReader = new ClassReader(classFile);
                    classReader.accept(new ClassVisitor(Opcodes.ASM9) {

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, ClassReader.SKIP_DEBUG);
                }
            }
        }
    }
}
