package top.dreamlike;

import org.graalvm.nativeimage.hosted.Feature;
import top.dreamlike.panama.example.StdLib;
import top.dreamlike.panama.generator.proxy.NativeImageHelper;

public class PanamaGeneratorFeature implements Feature {
    @Override
    public void duringSetup(DuringSetupAccess access) {
        //getpid
        NativeImageHelper.initPanamaFeature(StdLib.class);
    }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
    }


}
