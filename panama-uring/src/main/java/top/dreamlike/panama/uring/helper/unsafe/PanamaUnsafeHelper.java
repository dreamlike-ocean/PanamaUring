package top.dreamlike.panama.uring.helper.unsafe;


class PanamaUnsafeHelper {

    @FunctionalInterface
    public interface VoidThrowableFn {
        void get() throws Throwable;
    }

    @FunctionalInterface
    public interface ThrowableFn<T> {
        T get() throws Throwable;
    }


    public static <T> T throwable(ThrowableFn<T> supplier) {
        try {
            return supplier.get();
        }catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static void throwable(VoidThrowableFn r) {
        try {
           r.get();
        }catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static String classToSig(Class c) {
        if (c.isArray()) {
            return "[" + classToSig(c.getComponentType());
        }
        switch (c.getName()) {
            case "void" : return  "V";
            case "boolean" : return "Z";
            case "byte" : return "B";
            case "char" : return "C";
            case "short" : return "S";
            case "int" : return "I";
            case "long" : return "J";
            case "float" : return "F";
            case "double" : return "D";
            default : return "L" + c.getName().replace('.', '/') + ";";
        }
    }
}
