// Generated by jextract

package top.dreamlike.nativeLib.liburing;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.lang.foreign.*;

public class sigevent {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        MemoryLayout.unionLayout(
            Constants$root.C_INT$LAYOUT.withName("sival_int"),
            Constants$root.C_POINTER$LAYOUT.withName("sival_ptr")
        ).withName("sigev_value"),
        Constants$root.C_INT$LAYOUT.withName("sigev_signo"),
        Constants$root.C_INT$LAYOUT.withName("sigev_notify"),
        MemoryLayout.unionLayout(
            MemoryLayout.sequenceLayout(12, Constants$root.C_INT$LAYOUT).withName("_pad"),
            Constants$root.C_INT$LAYOUT.withName("_tid"),
            MemoryLayout.structLayout(
                Constants$root.C_POINTER$LAYOUT.withName("_function"),
                Constants$root.C_POINTER$LAYOUT.withName("_attribute")
            ).withName("_sigev_thread")
        ).withName("_sigev_un")
    ).withName("sigevent");
    public static MemoryLayout $LAYOUT() {
        return sigevent.$struct$LAYOUT;
    }
    public static MemorySegment sigev_value$slice(MemorySegment seg) {
        return seg.asSlice(0, 8);
    }
    static final VarHandle sigev_signo$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("sigev_signo"));
    public static VarHandle sigev_signo$VH() {
        return sigevent.sigev_signo$VH;
    }
    public static int sigev_signo$get(MemorySegment seg) {
        return (int)sigevent.sigev_signo$VH.get(seg);
    }
    public static void sigev_signo$set( MemorySegment seg, int x) {
        sigevent.sigev_signo$VH.set(seg, x);
    }
    public static int sigev_signo$get(MemorySegment seg, long index) {
        return (int)sigevent.sigev_signo$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void sigev_signo$set(MemorySegment seg, long index, int x) {
        sigevent.sigev_signo$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle sigev_notify$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("sigev_notify"));
    public static VarHandle sigev_notify$VH() {
        return sigevent.sigev_notify$VH;
    }
    public static int sigev_notify$get(MemorySegment seg) {
        return (int)sigevent.sigev_notify$VH.get(seg);
    }
    public static void sigev_notify$set( MemorySegment seg, int x) {
        sigevent.sigev_notify$VH.set(seg, x);
    }
    public static int sigev_notify$get(MemorySegment seg, long index) {
        return (int)sigevent.sigev_notify$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void sigev_notify$set(MemorySegment seg, long index, int x) {
        sigevent.sigev_notify$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static class _sigev_un {

        static final  GroupLayout _sigev_un$union$LAYOUT = MemoryLayout.unionLayout(
            MemoryLayout.sequenceLayout(12, Constants$root.C_INT$LAYOUT).withName("_pad"),
            Constants$root.C_INT$LAYOUT.withName("_tid"),
            MemoryLayout.structLayout(
                Constants$root.C_POINTER$LAYOUT.withName("_function"),
                Constants$root.C_POINTER$LAYOUT.withName("_attribute")
            ).withName("_sigev_thread")
        );
        public static MemoryLayout $LAYOUT() {
            return _sigev_un._sigev_un$union$LAYOUT;
        }
        public static MemorySegment _pad$slice(MemorySegment seg) {
            return seg.asSlice(0, 48);
        }
        static final VarHandle _tid$VH = _sigev_un$union$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("_tid"));
        public static VarHandle _tid$VH() {
            return _sigev_un._tid$VH;
        }
        public static int _tid$get(MemorySegment seg) {
            return (int)_sigev_un._tid$VH.get(seg);
        }
        public static void _tid$set( MemorySegment seg, int x) {
            _sigev_un._tid$VH.set(seg, x);
        }
        public static int _tid$get(MemorySegment seg, long index) {
            return (int)_sigev_un._tid$VH.get(seg.asSlice(index*sizeof()));
        }
        public static void _tid$set(MemorySegment seg, long index, int x) {
            _sigev_un._tid$VH.set(seg.asSlice(index*sizeof()), x);
        }
        public static class _sigev_thread {

            static final  GroupLayout _sigev_un$_sigev_thread$struct$LAYOUT = MemoryLayout.structLayout(
                Constants$root.C_POINTER$LAYOUT.withName("_function"),
                Constants$root.C_POINTER$LAYOUT.withName("_attribute")
            );
            public static MemoryLayout $LAYOUT() {
                return _sigev_thread._sigev_un$_sigev_thread$struct$LAYOUT;
            }
            static final FunctionDescriptor _function$FUNC = FunctionDescriptor.ofVoid(
                MemoryLayout.unionLayout(
                    Constants$root.C_INT$LAYOUT.withName("sival_int"),
                    Constants$root.C_POINTER$LAYOUT.withName("sival_ptr")
                ).withName("sigval")
            );
            static final MethodHandle _function$MH = RuntimeHelper.downcallHandle(
                _sigev_thread._function$FUNC
            );
            public interface _function {

                void apply(java.lang.foreign.MemorySegment _x0);
                static MemorySegment allocate(_function fi, MemorySession session) {
                    return RuntimeHelper.upcallStub(_function.class, fi, _sigev_thread._function$FUNC, session);
                }
                static _function ofAddress(MemoryAddress addr, MemorySession session) {
                    MemorySegment symbol = MemorySegment.ofAddress(addr, 0, session);
                    return (java.lang.foreign.MemorySegment __x0) -> {
                        try {
                            _sigev_thread._function$MH.invokeExact((Addressable)symbol, __x0);
                        } catch (Throwable ex$) {
                            throw new AssertionError("should not reach here", ex$);
                        }
                    };
                }
            }

            static final VarHandle _function$VH = _sigev_un$_sigev_thread$struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("_function"));
            public static VarHandle _function$VH() {
                return _sigev_thread._function$VH;
            }
            public static MemoryAddress _function$get(MemorySegment seg) {
                return (java.lang.foreign.MemoryAddress)_sigev_thread._function$VH.get(seg);
            }
            public static void _function$set( MemorySegment seg, MemoryAddress x) {
                _sigev_thread._function$VH.set(seg, x);
            }
            public static MemoryAddress _function$get(MemorySegment seg, long index) {
                return (java.lang.foreign.MemoryAddress)_sigev_thread._function$VH.get(seg.asSlice(index*sizeof()));
            }
            public static void _function$set(MemorySegment seg, long index, MemoryAddress x) {
                _sigev_thread._function$VH.set(seg.asSlice(index*sizeof()), x);
            }
            public static _function _function (MemorySegment segment, MemorySession session) {
                return _function.ofAddress(_function$get(segment), session);
            }
            static final VarHandle _attribute$VH = _sigev_un$_sigev_thread$struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("_attribute"));
            public static VarHandle _attribute$VH() {
                return _sigev_thread._attribute$VH;
            }
            public static MemoryAddress _attribute$get(MemorySegment seg) {
                return (java.lang.foreign.MemoryAddress)_sigev_thread._attribute$VH.get(seg);
            }
            public static void _attribute$set( MemorySegment seg, MemoryAddress x) {
                _sigev_thread._attribute$VH.set(seg, x);
            }
            public static MemoryAddress _attribute$get(MemorySegment seg, long index) {
                return (java.lang.foreign.MemoryAddress)_sigev_thread._attribute$VH.get(seg.asSlice(index*sizeof()));
            }
            public static void _attribute$set(MemorySegment seg, long index, MemoryAddress x) {
                _sigev_thread._attribute$VH.set(seg.asSlice(index*sizeof()), x);
            }
            public static long sizeof() { return $LAYOUT().byteSize(); }
            public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
            public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
                return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
            }
            public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
        }

        public static MemorySegment _sigev_thread$slice(MemorySegment seg) {
            return seg.asSlice(0, 16);
        }
        public static long sizeof() { return $LAYOUT().byteSize(); }
        public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
        public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
            return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
        }
        public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
    }

    public static MemorySegment _sigev_un$slice(MemorySegment seg) {
        return seg.asSlice(16, 48);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}


