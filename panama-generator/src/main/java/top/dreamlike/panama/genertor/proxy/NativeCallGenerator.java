package top.dreamlike.panama.genertor.proxy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;

public class NativeCallGenerator {
    private final ByteBuddy byteBuddy;

    private final NativeLibLookup nativeLibLookup;

    public NativeCallGenerator() {
        this.byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V21);
        this.nativeLibLookup = new NativeLibLookup();
    }


}
