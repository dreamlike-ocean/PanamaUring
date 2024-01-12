package top.dreamlike.panama.generator.helper;

import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;

public record MethodVariableAccessLoader(int targetOffset, StackManipulation loadOp) {


    public static MethodVariableAccessLoader calLoader(Class type, int offset) {
        int targetAddOffset = 0;
        StackManipulation wait = null;
        if (type == long.class && type.isPrimitive()) {
            wait = MethodVariableAccess.LONG.loadFrom(offset);
            targetAddOffset = 2;
        } else if (type == double.class && type.isPrimitive()) {
            wait = MethodVariableAccess.DOUBLE.loadFrom(offset);
            targetAddOffset = 2;
        } else if (type == float.class && type.isPrimitive()) {
            wait = MethodVariableAccess.FLOAT.loadFrom(offset);
            targetAddOffset = 1;
        } else if (type == int.class && type.isPrimitive()) {
            wait = MethodVariableAccess.INTEGER.loadFrom(offset);
            targetAddOffset = 1;
        } else {
            wait = MethodVariableAccess.REFERENCE.loadFrom(offset);
            targetAddOffset = 1;
        }
        return new MethodVariableAccessLoader(targetAddOffset, wait);
    }
}
