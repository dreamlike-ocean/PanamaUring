package top.dreamlike.panama.generator.helper;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.utility.JavaConstant;

import java.lang.invoke.VarHandle;

@HashCodeAndEqualsPlugin.Enhance
public class VarHandlerInvocation extends StackManipulation.AbstractBase {

    private static final String METHOD_HANDLE_NAME = "java/lang/invoke/VarHandle";


    private static final String INVOKE_GET = "get";

    private static final String INVOKE_SET = "set";

    private final JavaConstant.MethodType methodType;

    private final boolean invokeGet;

    public VarHandlerInvocation(JavaConstant.MethodType methodType, boolean invokeGet) {
        this.methodType = methodType;
        this.invokeGet = invokeGet;
    }

    /**
     * {@inheritDoc}
     */
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        VarHandle vh = null;
        if (invokeGet) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, METHOD_HANDLE_NAME, INVOKE_GET, methodType.getDescriptor(), false);
        } else {
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, METHOD_HANDLE_NAME, INVOKE_SET, methodType.getDescriptor(), false);
        }
        int size = methodType.getReturnType().getStackSize().getSize() - methodType.getParameterTypes().getStackSize();
        return new Size(size, Math.max(size, 0));
    }
}
