package top.dreamlike;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeForeignAccess;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.ValueLayout;

public class PanamaFeature implements Feature {
    @Override
    public void duringSetup(DuringSetupAccess access) {
        //getpid
        RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.isTrivial());
    }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
//        RuntimeClassInitialization.initializeAtBuildTime(Main.class);
    }
}
