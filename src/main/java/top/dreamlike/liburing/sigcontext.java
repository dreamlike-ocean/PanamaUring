// Generated by jextract

package top.dreamlike.liburing;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public class sigcontext {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_LONG_LONG$LAYOUT.withName("r8"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("r9"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("r10"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("r11"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("r12"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("r13"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("r14"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("r15"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("rdi"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("rsi"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("rbp"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("rbx"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("rdx"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("rax"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("rcx"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("rsp"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("rip"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("eflags"),
        Constants$root.C_SHORT$LAYOUT.withName("cs"),
        Constants$root.C_SHORT$LAYOUT.withName("gs"),
        Constants$root.C_SHORT$LAYOUT.withName("fs"),
        Constants$root.C_SHORT$LAYOUT.withName("__pad0"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("err"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("trapno"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("oldmask"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("cr2"),
        MemoryLayout.unionLayout(
            Constants$root.C_POINTER$LAYOUT.withName("fpstate"),
            Constants$root.C_LONG_LONG$LAYOUT.withName("__fpstate_word")
        ).withName("$anon$0"),
        MemoryLayout.sequenceLayout(8, Constants$root.C_LONG_LONG$LAYOUT).withName("__reserved1")
    ).withName("sigcontext");
    public static MemoryLayout $LAYOUT() {
        return sigcontext.$struct$LAYOUT;
    }
    static final VarHandle r8$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("r8"));
    public static VarHandle r8$VH() {
        return sigcontext.r8$VH;
    }
    public static long r8$get(MemorySegment seg) {
        return (long)sigcontext.r8$VH.get(seg);
    }
    public static void r8$set( MemorySegment seg, long x) {
        sigcontext.r8$VH.set(seg, x);
    }
    public static long r8$get(MemorySegment seg, long index) {
        return (long)sigcontext.r8$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void r8$set(MemorySegment seg, long index, long x) {
        sigcontext.r8$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle r9$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("r9"));
    public static VarHandle r9$VH() {
        return sigcontext.r9$VH;
    }
    public static long r9$get(MemorySegment seg) {
        return (long)sigcontext.r9$VH.get(seg);
    }
    public static void r9$set( MemorySegment seg, long x) {
        sigcontext.r9$VH.set(seg, x);
    }
    public static long r9$get(MemorySegment seg, long index) {
        return (long)sigcontext.r9$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void r9$set(MemorySegment seg, long index, long x) {
        sigcontext.r9$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle r10$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("r10"));
    public static VarHandle r10$VH() {
        return sigcontext.r10$VH;
    }
    public static long r10$get(MemorySegment seg) {
        return (long)sigcontext.r10$VH.get(seg);
    }
    public static void r10$set( MemorySegment seg, long x) {
        sigcontext.r10$VH.set(seg, x);
    }
    public static long r10$get(MemorySegment seg, long index) {
        return (long)sigcontext.r10$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void r10$set(MemorySegment seg, long index, long x) {
        sigcontext.r10$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle r11$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("r11"));
    public static VarHandle r11$VH() {
        return sigcontext.r11$VH;
    }
    public static long r11$get(MemorySegment seg) {
        return (long)sigcontext.r11$VH.get(seg);
    }
    public static void r11$set( MemorySegment seg, long x) {
        sigcontext.r11$VH.set(seg, x);
    }
    public static long r11$get(MemorySegment seg, long index) {
        return (long)sigcontext.r11$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void r11$set(MemorySegment seg, long index, long x) {
        sigcontext.r11$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle r12$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("r12"));
    public static VarHandle r12$VH() {
        return sigcontext.r12$VH;
    }
    public static long r12$get(MemorySegment seg) {
        return (long)sigcontext.r12$VH.get(seg);
    }
    public static void r12$set( MemorySegment seg, long x) {
        sigcontext.r12$VH.set(seg, x);
    }
    public static long r12$get(MemorySegment seg, long index) {
        return (long)sigcontext.r12$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void r12$set(MemorySegment seg, long index, long x) {
        sigcontext.r12$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle r13$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("r13"));
    public static VarHandle r13$VH() {
        return sigcontext.r13$VH;
    }
    public static long r13$get(MemorySegment seg) {
        return (long)sigcontext.r13$VH.get(seg);
    }
    public static void r13$set( MemorySegment seg, long x) {
        sigcontext.r13$VH.set(seg, x);
    }
    public static long r13$get(MemorySegment seg, long index) {
        return (long)sigcontext.r13$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void r13$set(MemorySegment seg, long index, long x) {
        sigcontext.r13$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle r14$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("r14"));
    public static VarHandle r14$VH() {
        return sigcontext.r14$VH;
    }
    public static long r14$get(MemorySegment seg) {
        return (long)sigcontext.r14$VH.get(seg);
    }
    public static void r14$set( MemorySegment seg, long x) {
        sigcontext.r14$VH.set(seg, x);
    }
    public static long r14$get(MemorySegment seg, long index) {
        return (long)sigcontext.r14$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void r14$set(MemorySegment seg, long index, long x) {
        sigcontext.r14$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle r15$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("r15"));
    public static VarHandle r15$VH() {
        return sigcontext.r15$VH;
    }
    public static long r15$get(MemorySegment seg) {
        return (long)sigcontext.r15$VH.get(seg);
    }
    public static void r15$set( MemorySegment seg, long x) {
        sigcontext.r15$VH.set(seg, x);
    }
    public static long r15$get(MemorySegment seg, long index) {
        return (long)sigcontext.r15$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void r15$set(MemorySegment seg, long index, long x) {
        sigcontext.r15$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle rdi$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("rdi"));
    public static VarHandle rdi$VH() {
        return sigcontext.rdi$VH;
    }
    public static long rdi$get(MemorySegment seg) {
        return (long)sigcontext.rdi$VH.get(seg);
    }
    public static void rdi$set( MemorySegment seg, long x) {
        sigcontext.rdi$VH.set(seg, x);
    }
    public static long rdi$get(MemorySegment seg, long index) {
        return (long)sigcontext.rdi$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void rdi$set(MemorySegment seg, long index, long x) {
        sigcontext.rdi$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle rsi$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("rsi"));
    public static VarHandle rsi$VH() {
        return sigcontext.rsi$VH;
    }
    public static long rsi$get(MemorySegment seg) {
        return (long)sigcontext.rsi$VH.get(seg);
    }
    public static void rsi$set( MemorySegment seg, long x) {
        sigcontext.rsi$VH.set(seg, x);
    }
    public static long rsi$get(MemorySegment seg, long index) {
        return (long)sigcontext.rsi$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void rsi$set(MemorySegment seg, long index, long x) {
        sigcontext.rsi$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle rbp$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("rbp"));
    public static VarHandle rbp$VH() {
        return sigcontext.rbp$VH;
    }
    public static long rbp$get(MemorySegment seg) {
        return (long)sigcontext.rbp$VH.get(seg);
    }
    public static void rbp$set( MemorySegment seg, long x) {
        sigcontext.rbp$VH.set(seg, x);
    }
    public static long rbp$get(MemorySegment seg, long index) {
        return (long)sigcontext.rbp$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void rbp$set(MemorySegment seg, long index, long x) {
        sigcontext.rbp$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle rbx$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("rbx"));
    public static VarHandle rbx$VH() {
        return sigcontext.rbx$VH;
    }
    public static long rbx$get(MemorySegment seg) {
        return (long)sigcontext.rbx$VH.get(seg);
    }
    public static void rbx$set( MemorySegment seg, long x) {
        sigcontext.rbx$VH.set(seg, x);
    }
    public static long rbx$get(MemorySegment seg, long index) {
        return (long)sigcontext.rbx$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void rbx$set(MemorySegment seg, long index, long x) {
        sigcontext.rbx$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle rdx$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("rdx"));
    public static VarHandle rdx$VH() {
        return sigcontext.rdx$VH;
    }
    public static long rdx$get(MemorySegment seg) {
        return (long)sigcontext.rdx$VH.get(seg);
    }
    public static void rdx$set( MemorySegment seg, long x) {
        sigcontext.rdx$VH.set(seg, x);
    }
    public static long rdx$get(MemorySegment seg, long index) {
        return (long)sigcontext.rdx$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void rdx$set(MemorySegment seg, long index, long x) {
        sigcontext.rdx$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle rax$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("rax"));
    public static VarHandle rax$VH() {
        return sigcontext.rax$VH;
    }
    public static long rax$get(MemorySegment seg) {
        return (long)sigcontext.rax$VH.get(seg);
    }
    public static void rax$set( MemorySegment seg, long x) {
        sigcontext.rax$VH.set(seg, x);
    }
    public static long rax$get(MemorySegment seg, long index) {
        return (long)sigcontext.rax$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void rax$set(MemorySegment seg, long index, long x) {
        sigcontext.rax$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle rcx$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("rcx"));
    public static VarHandle rcx$VH() {
        return sigcontext.rcx$VH;
    }
    public static long rcx$get(MemorySegment seg) {
        return (long)sigcontext.rcx$VH.get(seg);
    }
    public static void rcx$set( MemorySegment seg, long x) {
        sigcontext.rcx$VH.set(seg, x);
    }
    public static long rcx$get(MemorySegment seg, long index) {
        return (long)sigcontext.rcx$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void rcx$set(MemorySegment seg, long index, long x) {
        sigcontext.rcx$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle rsp$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("rsp"));
    public static VarHandle rsp$VH() {
        return sigcontext.rsp$VH;
    }
    public static long rsp$get(MemorySegment seg) {
        return (long)sigcontext.rsp$VH.get(seg);
    }
    public static void rsp$set( MemorySegment seg, long x) {
        sigcontext.rsp$VH.set(seg, x);
    }
    public static long rsp$get(MemorySegment seg, long index) {
        return (long)sigcontext.rsp$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void rsp$set(MemorySegment seg, long index, long x) {
        sigcontext.rsp$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle rip$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("rip"));
    public static VarHandle rip$VH() {
        return sigcontext.rip$VH;
    }
    public static long rip$get(MemorySegment seg) {
        return (long)sigcontext.rip$VH.get(seg);
    }
    public static void rip$set( MemorySegment seg, long x) {
        sigcontext.rip$VH.set(seg, x);
    }
    public static long rip$get(MemorySegment seg, long index) {
        return (long)sigcontext.rip$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void rip$set(MemorySegment seg, long index, long x) {
        sigcontext.rip$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle eflags$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("eflags"));
    public static VarHandle eflags$VH() {
        return sigcontext.eflags$VH;
    }
    public static long eflags$get(MemorySegment seg) {
        return (long)sigcontext.eflags$VH.get(seg);
    }
    public static void eflags$set( MemorySegment seg, long x) {
        sigcontext.eflags$VH.set(seg, x);
    }
    public static long eflags$get(MemorySegment seg, long index) {
        return (long)sigcontext.eflags$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void eflags$set(MemorySegment seg, long index, long x) {
        sigcontext.eflags$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle cs$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("cs"));
    public static VarHandle cs$VH() {
        return sigcontext.cs$VH;
    }
    public static short cs$get(MemorySegment seg) {
        return (short)sigcontext.cs$VH.get(seg);
    }
    public static void cs$set( MemorySegment seg, short x) {
        sigcontext.cs$VH.set(seg, x);
    }
    public static short cs$get(MemorySegment seg, long index) {
        return (short)sigcontext.cs$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void cs$set(MemorySegment seg, long index, short x) {
        sigcontext.cs$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle gs$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("gs"));
    public static VarHandle gs$VH() {
        return sigcontext.gs$VH;
    }
    public static short gs$get(MemorySegment seg) {
        return (short)sigcontext.gs$VH.get(seg);
    }
    public static void gs$set( MemorySegment seg, short x) {
        sigcontext.gs$VH.set(seg, x);
    }
    public static short gs$get(MemorySegment seg, long index) {
        return (short)sigcontext.gs$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void gs$set(MemorySegment seg, long index, short x) {
        sigcontext.gs$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle fs$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("fs"));
    public static VarHandle fs$VH() {
        return sigcontext.fs$VH;
    }
    public static short fs$get(MemorySegment seg) {
        return (short)sigcontext.fs$VH.get(seg);
    }
    public static void fs$set( MemorySegment seg, short x) {
        sigcontext.fs$VH.set(seg, x);
    }
    public static short fs$get(MemorySegment seg, long index) {
        return (short)sigcontext.fs$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void fs$set(MemorySegment seg, long index, short x) {
        sigcontext.fs$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle __pad0$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("__pad0"));
    public static VarHandle __pad0$VH() {
        return sigcontext.__pad0$VH;
    }
    public static short __pad0$get(MemorySegment seg) {
        return (short)sigcontext.__pad0$VH.get(seg);
    }
    public static void __pad0$set( MemorySegment seg, short x) {
        sigcontext.__pad0$VH.set(seg, x);
    }
    public static short __pad0$get(MemorySegment seg, long index) {
        return (short)sigcontext.__pad0$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void __pad0$set(MemorySegment seg, long index, short x) {
        sigcontext.__pad0$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle err$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("err"));
    public static VarHandle err$VH() {
        return sigcontext.err$VH;
    }
    public static long err$get(MemorySegment seg) {
        return (long)sigcontext.err$VH.get(seg);
    }
    public static void err$set( MemorySegment seg, long x) {
        sigcontext.err$VH.set(seg, x);
    }
    public static long err$get(MemorySegment seg, long index) {
        return (long)sigcontext.err$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void err$set(MemorySegment seg, long index, long x) {
        sigcontext.err$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle trapno$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("trapno"));
    public static VarHandle trapno$VH() {
        return sigcontext.trapno$VH;
    }
    public static long trapno$get(MemorySegment seg) {
        return (long)sigcontext.trapno$VH.get(seg);
    }
    public static void trapno$set( MemorySegment seg, long x) {
        sigcontext.trapno$VH.set(seg, x);
    }
    public static long trapno$get(MemorySegment seg, long index) {
        return (long)sigcontext.trapno$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void trapno$set(MemorySegment seg, long index, long x) {
        sigcontext.trapno$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle oldmask$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("oldmask"));
    public static VarHandle oldmask$VH() {
        return sigcontext.oldmask$VH;
    }
    public static long oldmask$get(MemorySegment seg) {
        return (long)sigcontext.oldmask$VH.get(seg);
    }
    public static void oldmask$set( MemorySegment seg, long x) {
        sigcontext.oldmask$VH.set(seg, x);
    }
    public static long oldmask$get(MemorySegment seg, long index) {
        return (long)sigcontext.oldmask$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void oldmask$set(MemorySegment seg, long index, long x) {
        sigcontext.oldmask$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle cr2$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("cr2"));
    public static VarHandle cr2$VH() {
        return sigcontext.cr2$VH;
    }
    public static long cr2$get(MemorySegment seg) {
        return (long)sigcontext.cr2$VH.get(seg);
    }
    public static void cr2$set( MemorySegment seg, long x) {
        sigcontext.cr2$VH.set(seg, x);
    }
    public static long cr2$get(MemorySegment seg, long index) {
        return (long)sigcontext.cr2$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void cr2$set(MemorySegment seg, long index, long x) {
        sigcontext.cr2$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle fpstate$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("$anon$0"), MemoryLayout.PathElement.groupElement("fpstate"));
    public static VarHandle fpstate$VH() {
        return sigcontext.fpstate$VH;
    }
    public static MemoryAddress fpstate$get(MemorySegment seg) {
        return (java.lang.foreign.MemoryAddress)sigcontext.fpstate$VH.get(seg);
    }
    public static void fpstate$set( MemorySegment seg, MemoryAddress x) {
        sigcontext.fpstate$VH.set(seg, x);
    }
    public static MemoryAddress fpstate$get(MemorySegment seg, long index) {
        return (java.lang.foreign.MemoryAddress)sigcontext.fpstate$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void fpstate$set(MemorySegment seg, long index, MemoryAddress x) {
        sigcontext.fpstate$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle __fpstate_word$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("$anon$0"), MemoryLayout.PathElement.groupElement("__fpstate_word"));
    public static VarHandle __fpstate_word$VH() {
        return sigcontext.__fpstate_word$VH;
    }
    public static long __fpstate_word$get(MemorySegment seg) {
        return (long)sigcontext.__fpstate_word$VH.get(seg);
    }
    public static void __fpstate_word$set( MemorySegment seg, long x) {
        sigcontext.__fpstate_word$VH.set(seg, x);
    }
    public static long __fpstate_word$get(MemorySegment seg, long index) {
        return (long)sigcontext.__fpstate_word$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void __fpstate_word$set(MemorySegment seg, long index, long x) {
        sigcontext.__fpstate_word$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static MemorySegment __reserved1$slice(MemorySegment seg) {
        return seg.asSlice(192, 64);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}


