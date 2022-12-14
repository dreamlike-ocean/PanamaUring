// Generated by jextract

package top.dreamlike.nativeLib.string;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public class string_h  {

    /* package-private */ string_h() {}
    public static OfByte C_CHAR = Constants$root.C_CHAR$LAYOUT;
    public static OfShort C_SHORT = Constants$root.C_SHORT$LAYOUT;
    public static OfInt C_INT = Constants$root.C_INT$LAYOUT;
    public static OfLong C_LONG = Constants$root.C_LONG_LONG$LAYOUT;
    public static OfLong C_LONG_LONG = Constants$root.C_LONG_LONG$LAYOUT;
    public static OfFloat C_FLOAT = Constants$root.C_FLOAT$LAYOUT;
    public static OfDouble C_DOUBLE = Constants$root.C_DOUBLE$LAYOUT;
    public static OfAddress C_POINTER = Constants$root.C_POINTER$LAYOUT;
    public static int _STRING_H() {
        return (int)1L;
    }
    public static int _FEATURES_H() {
        return (int)1L;
    }
    public static int _DEFAULT_SOURCE() {
        return (int)1L;
    }
    public static int __GLIBC_USE_ISOC2X() {
        return (int)0L;
    }
    public static int __USE_ISOC11() {
        return (int)1L;
    }
    public static int __USE_ISOC99() {
        return (int)1L;
    }
    public static int __USE_ISOC95() {
        return (int)1L;
    }
    public static int __USE_POSIX_IMPLICITLY() {
        return (int)1L;
    }
    public static int _POSIX_SOURCE() {
        return (int)1L;
    }
    public static int __USE_POSIX() {
        return (int)1L;
    }
    public static int __USE_POSIX2() {
        return (int)1L;
    }
    public static int __USE_POSIX199309() {
        return (int)1L;
    }
    public static int __USE_POSIX199506() {
        return (int)1L;
    }
    public static int __USE_XOPEN2K() {
        return (int)1L;
    }
    public static int __USE_XOPEN2K8() {
        return (int)1L;
    }
    public static int _ATFILE_SOURCE() {
        return (int)1L;
    }
    public static int __USE_MISC() {
        return (int)1L;
    }
    public static int __USE_ATFILE() {
        return (int)1L;
    }
    public static int __USE_FORTIFY_LEVEL() {
        return (int)0L;
    }
    public static int __GLIBC_USE_DEPRECATED_GETS() {
        return (int)0L;
    }
    public static int __GLIBC_USE_DEPRECATED_SCANF() {
        return (int)0L;
    }
    public static int _STDC_PREDEF_H() {
        return (int)1L;
    }
    public static int __STDC_IEC_559__() {
        return (int)1L;
    }
    public static int __STDC_IEC_559_COMPLEX__() {
        return (int)1L;
    }
    public static int __GNU_LIBRARY__() {
        return (int)6L;
    }
    public static int __GLIBC__() {
        return (int)2L;
    }
    public static int __GLIBC_MINOR__() {
        return (int)31L;
    }
    public static int _SYS_CDEFS_H() {
        return (int)1L;
    }
    public static int __glibc_c99_flexarr_available() {
        return (int)1L;
    }
    public static int __WORDSIZE() {
        return (int)64L;
    }
    public static int __WORDSIZE_TIME64_COMPAT32() {
        return (int)1L;
    }
    public static int __SYSCALL_WORDSIZE() {
        return (int)64L;
    }
    public static int __LONG_DOUBLE_USES_FLOAT128() {
        return (int)0L;
    }
    public static int __HAVE_GENERIC_SELECTION() {
        return (int)1L;
    }
    public static int __GLIBC_USE_LIB_EXT2() {
        return (int)0L;
    }
    public static int __GLIBC_USE_IEC_60559_BFP_EXT() {
        return (int)0L;
    }
    public static int __GLIBC_USE_IEC_60559_BFP_EXT_C2X() {
        return (int)0L;
    }
    public static int __GLIBC_USE_IEC_60559_FUNCS_EXT() {
        return (int)0L;
    }
    public static int __GLIBC_USE_IEC_60559_FUNCS_EXT_C2X() {
        return (int)0L;
    }
    public static int __GLIBC_USE_IEC_60559_TYPES_EXT() {
        return (int)0L;
    }
    public static int _BITS_TYPES_LOCALE_T_H() {
        return (int)1L;
    }
    public static int _BITS_TYPES___LOCALE_T_H() {
        return (int)1L;
    }
    public static int _STRINGS_H() {
        return (int)1L;
    }
    public static MethodHandle memcpy$MH() {
        return RuntimeHelper.requireNonNull(constants$0.memcpy$MH,"memcpy");
    }
    public static MemoryAddress memcpy ( Addressable __dest,  Addressable __src,  long __n) {
        var mh$ = memcpy$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__dest, __src, __n);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle memmove$MH() {
        return RuntimeHelper.requireNonNull(constants$0.memmove$MH,"memmove");
    }
    public static MemoryAddress memmove ( Addressable __dest,  Addressable __src,  long __n) {
        var mh$ = memmove$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__dest, __src, __n);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle memccpy$MH() {
        return RuntimeHelper.requireNonNull(constants$0.memccpy$MH,"memccpy");
    }
    public static MemoryAddress memccpy ( Addressable __dest,  Addressable __src,  int __c,  long __n) {
        var mh$ = memccpy$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__dest, __src, __c, __n);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle memset$MH() {
        return RuntimeHelper.requireNonNull(constants$0.memset$MH,"memset");
    }
    public static MemoryAddress memset ( Addressable __s,  int __c,  long __n) {
        var mh$ = memset$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__s, __c, __n);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle memcmp$MH() {
        return RuntimeHelper.requireNonNull(constants$0.memcmp$MH,"memcmp");
    }
    public static int memcmp ( Addressable __s1,  Addressable __s2,  long __n) {
        var mh$ = memcmp$MH();
        try {
            return (int)mh$.invokeExact(__s1, __s2, __n);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle memchr$MH() {
        return RuntimeHelper.requireNonNull(constants$0.memchr$MH,"memchr");
    }
    public static MemoryAddress memchr ( Addressable __s,  int __c,  long __n) {
        var mh$ = memchr$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__s, __c, __n);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strcpy$MH() {
        return RuntimeHelper.requireNonNull(constants$1.strcpy$MH,"strcpy");
    }
    public static MemoryAddress strcpy ( Addressable __dest,  Addressable __src) {
        var mh$ = strcpy$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__dest, __src);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strncpy$MH() {
        return RuntimeHelper.requireNonNull(constants$1.strncpy$MH,"strncpy");
    }
    public static MemoryAddress strncpy ( Addressable __dest,  Addressable __src,  long __n) {
        var mh$ = strncpy$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__dest, __src, __n);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strcat$MH() {
        return RuntimeHelper.requireNonNull(constants$1.strcat$MH,"strcat");
    }
    public static MemoryAddress strcat ( Addressable __dest,  Addressable __src) {
        var mh$ = strcat$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__dest, __src);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strncat$MH() {
        return RuntimeHelper.requireNonNull(constants$1.strncat$MH,"strncat");
    }
    public static MemoryAddress strncat ( Addressable __dest,  Addressable __src,  long __n) {
        var mh$ = strncat$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__dest, __src, __n);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strcmp$MH() {
        return RuntimeHelper.requireNonNull(constants$1.strcmp$MH,"strcmp");
    }
    public static int strcmp ( Addressable __s1,  Addressable __s2) {
        var mh$ = strcmp$MH();
        try {
            return (int)mh$.invokeExact(__s1, __s2);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strncmp$MH() {
        return RuntimeHelper.requireNonNull(constants$1.strncmp$MH,"strncmp");
    }
    public static int strncmp ( Addressable __s1,  Addressable __s2,  long __n) {
        var mh$ = strncmp$MH();
        try {
            return (int)mh$.invokeExact(__s1, __s2, __n);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strcoll$MH() {
        return RuntimeHelper.requireNonNull(constants$2.strcoll$MH,"strcoll");
    }
    public static int strcoll ( Addressable __s1,  Addressable __s2) {
        var mh$ = strcoll$MH();
        try {
            return (int)mh$.invokeExact(__s1, __s2);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strxfrm$MH() {
        return RuntimeHelper.requireNonNull(constants$2.strxfrm$MH,"strxfrm");
    }
    public static long strxfrm ( Addressable __dest,  Addressable __src,  long __n) {
        var mh$ = strxfrm$MH();
        try {
            return (long)mh$.invokeExact(__dest, __src, __n);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static OfAddress __locale_t = Constants$root.C_POINTER$LAYOUT;
    public static OfAddress locale_t = Constants$root.C_POINTER$LAYOUT;
    public static MethodHandle strcoll_l$MH() {
        return RuntimeHelper.requireNonNull(constants$2.strcoll_l$MH,"strcoll_l");
    }
    public static int strcoll_l ( Addressable __s1,  Addressable __s2,  Addressable __l) {
        var mh$ = strcoll_l$MH();
        try {
            return (int)mh$.invokeExact(__s1, __s2, __l);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strxfrm_l$MH() {
        return RuntimeHelper.requireNonNull(constants$2.strxfrm_l$MH,"strxfrm_l");
    }
    public static long strxfrm_l ( Addressable __dest,  Addressable __src,  long __n,  Addressable __l) {
        var mh$ = strxfrm_l$MH();
        try {
            return (long)mh$.invokeExact(__dest, __src, __n, __l);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strdup$MH() {
        return RuntimeHelper.requireNonNull(constants$2.strdup$MH,"strdup");
    }
    public static MemoryAddress strdup ( Addressable __s) {
        var mh$ = strdup$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__s);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strndup$MH() {
        return RuntimeHelper.requireNonNull(constants$2.strndup$MH,"strndup");
    }
    public static MemoryAddress strndup ( Addressable __string,  long __n) {
        var mh$ = strndup$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__string, __n);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strchr$MH() {
        return RuntimeHelper.requireNonNull(constants$3.strchr$MH,"strchr");
    }
    public static MemoryAddress strchr ( Addressable __s,  int __c) {
        var mh$ = strchr$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__s, __c);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strrchr$MH() {
        return RuntimeHelper.requireNonNull(constants$3.strrchr$MH,"strrchr");
    }
    public static MemoryAddress strrchr ( Addressable __s,  int __c) {
        var mh$ = strrchr$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__s, __c);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strcspn$MH() {
        return RuntimeHelper.requireNonNull(constants$3.strcspn$MH,"strcspn");
    }
    public static long strcspn ( Addressable __s,  Addressable __reject) {
        var mh$ = strcspn$MH();
        try {
            return (long)mh$.invokeExact(__s, __reject);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strspn$MH() {
        return RuntimeHelper.requireNonNull(constants$3.strspn$MH,"strspn");
    }
    public static long strspn ( Addressable __s,  Addressable __accept) {
        var mh$ = strspn$MH();
        try {
            return (long)mh$.invokeExact(__s, __accept);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strpbrk$MH() {
        return RuntimeHelper.requireNonNull(constants$3.strpbrk$MH,"strpbrk");
    }
    public static MemoryAddress strpbrk ( Addressable __s,  Addressable __accept) {
        var mh$ = strpbrk$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__s, __accept);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strstr$MH() {
        return RuntimeHelper.requireNonNull(constants$3.strstr$MH,"strstr");
    }
    public static MemoryAddress strstr ( Addressable __haystack,  Addressable __needle) {
        var mh$ = strstr$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__haystack, __needle);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strtok$MH() {
        return RuntimeHelper.requireNonNull(constants$4.strtok$MH,"strtok");
    }
    public static MemoryAddress strtok ( Addressable __s,  Addressable __delim) {
        var mh$ = strtok$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__s, __delim);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle __strtok_r$MH() {
        return RuntimeHelper.requireNonNull(constants$4.__strtok_r$MH,"__strtok_r");
    }
    public static MemoryAddress __strtok_r ( Addressable __s,  Addressable __delim,  Addressable __save_ptr) {
        var mh$ = __strtok_r$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__s, __delim, __save_ptr);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strtok_r$MH() {
        return RuntimeHelper.requireNonNull(constants$4.strtok_r$MH,"strtok_r");
    }
    public static MemoryAddress strtok_r ( Addressable __s,  Addressable __delim,  Addressable __save_ptr) {
        var mh$ = strtok_r$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__s, __delim, __save_ptr);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strlen$MH() {
        return RuntimeHelper.requireNonNull(constants$4.strlen$MH,"strlen");
    }
    public static long strlen ( Addressable __s) {
        var mh$ = strlen$MH();
        try {
            return (long)mh$.invokeExact(__s);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strnlen$MH() {
        return RuntimeHelper.requireNonNull(constants$4.strnlen$MH,"strnlen");
    }
    public static long strnlen ( Addressable __string,  long __maxlen) {
        var mh$ = strnlen$MH();
        try {
            return (long)mh$.invokeExact(__string, __maxlen);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strerror$MH() {
        return RuntimeHelper.requireNonNull(constants$4.strerror$MH,"strerror");
    }
    public static MemoryAddress strerror ( int __errnum) {
        var mh$ = strerror$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__errnum);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strerror_r$MH() {
        return RuntimeHelper.requireNonNull(constants$5.strerror_r$MH,"strerror_r");
    }
    public static int strerror_r ( int __errnum,  Addressable __buf,  long __buflen) {
        var mh$ = strerror_r$MH();
        try {
            return (int)mh$.invokeExact(__errnum, __buf, __buflen);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strerror_l$MH() {
        return RuntimeHelper.requireNonNull(constants$5.strerror_l$MH,"strerror_l");
    }
    public static MemoryAddress strerror_l ( int __errnum,  Addressable __l) {
        var mh$ = strerror_l$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__errnum, __l);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle bcmp$MH() {
        return RuntimeHelper.requireNonNull(constants$5.bcmp$MH,"bcmp");
    }
    public static int bcmp ( Addressable __s1,  Addressable __s2,  long __n) {
        var mh$ = bcmp$MH();
        try {
            return (int)mh$.invokeExact(__s1, __s2, __n);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle bcopy$MH() {
        return RuntimeHelper.requireNonNull(constants$5.bcopy$MH,"bcopy");
    }
    public static void bcopy ( Addressable __src,  Addressable __dest,  long __n) {
        var mh$ = bcopy$MH();
        try {
            mh$.invokeExact(__src, __dest, __n);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle bzero$MH() {
        return RuntimeHelper.requireNonNull(constants$5.bzero$MH,"bzero");
    }
    public static void bzero ( Addressable __s,  long __n) {
        var mh$ = bzero$MH();
        try {
            mh$.invokeExact(__s, __n);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle index$MH() {
        return RuntimeHelper.requireNonNull(constants$5.index$MH,"index");
    }
    public static MemoryAddress index ( Addressable __s,  int __c) {
        var mh$ = index$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__s, __c);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle rindex$MH() {
        return RuntimeHelper.requireNonNull(constants$6.rindex$MH,"rindex");
    }
    public static MemoryAddress rindex ( Addressable __s,  int __c) {
        var mh$ = rindex$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__s, __c);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle ffs$MH() {
        return RuntimeHelper.requireNonNull(constants$6.ffs$MH,"ffs");
    }
    public static int ffs ( int __i) {
        var mh$ = ffs$MH();
        try {
            return (int)mh$.invokeExact(__i);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle ffsl$MH() {
        return RuntimeHelper.requireNonNull(constants$6.ffsl$MH,"ffsl");
    }
    public static int ffsl ( long __l) {
        var mh$ = ffsl$MH();
        try {
            return (int)mh$.invokeExact(__l);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle ffsll$MH() {
        return RuntimeHelper.requireNonNull(constants$6.ffsll$MH,"ffsll");
    }
    public static int ffsll ( long __ll) {
        var mh$ = ffsll$MH();
        try {
            return (int)mh$.invokeExact(__ll);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strcasecmp$MH() {
        return RuntimeHelper.requireNonNull(constants$6.strcasecmp$MH,"strcasecmp");
    }
    public static int strcasecmp ( Addressable __s1,  Addressable __s2) {
        var mh$ = strcasecmp$MH();
        try {
            return (int)mh$.invokeExact(__s1, __s2);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strncasecmp$MH() {
        return RuntimeHelper.requireNonNull(constants$6.strncasecmp$MH,"strncasecmp");
    }
    public static int strncasecmp ( Addressable __s1,  Addressable __s2,  long __n) {
        var mh$ = strncasecmp$MH();
        try {
            return (int)mh$.invokeExact(__s1, __s2, __n);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strcasecmp_l$MH() {
        return RuntimeHelper.requireNonNull(constants$7.strcasecmp_l$MH,"strcasecmp_l");
    }
    public static int strcasecmp_l ( Addressable __s1,  Addressable __s2,  Addressable __loc) {
        var mh$ = strcasecmp_l$MH();
        try {
            return (int)mh$.invokeExact(__s1, __s2, __loc);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strncasecmp_l$MH() {
        return RuntimeHelper.requireNonNull(constants$7.strncasecmp_l$MH,"strncasecmp_l");
    }
    public static int strncasecmp_l ( Addressable __s1,  Addressable __s2,  long __n,  Addressable __loc) {
        var mh$ = strncasecmp_l$MH();
        try {
            return (int)mh$.invokeExact(__s1, __s2, __n, __loc);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle explicit_bzero$MH() {
        return RuntimeHelper.requireNonNull(constants$7.explicit_bzero$MH,"explicit_bzero");
    }
    public static void explicit_bzero ( Addressable __s,  long __n) {
        var mh$ = explicit_bzero$MH();
        try {
            mh$.invokeExact(__s, __n);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strsep$MH() {
        return RuntimeHelper.requireNonNull(constants$7.strsep$MH,"strsep");
    }
    public static MemoryAddress strsep ( Addressable __stringp,  Addressable __delim) {
        var mh$ = strsep$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__stringp, __delim);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle strsignal$MH() {
        return RuntimeHelper.requireNonNull(constants$7.strsignal$MH,"strsignal");
    }
    public static MemoryAddress strsignal ( int __sig) {
        var mh$ = strsignal$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__sig);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle __stpcpy$MH() {
        return RuntimeHelper.requireNonNull(constants$7.__stpcpy$MH,"__stpcpy");
    }
    public static MemoryAddress __stpcpy ( Addressable __dest,  Addressable __src) {
        var mh$ = __stpcpy$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__dest, __src);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle stpcpy$MH() {
        return RuntimeHelper.requireNonNull(constants$8.stpcpy$MH,"stpcpy");
    }
    public static MemoryAddress stpcpy ( Addressable __dest,  Addressable __src) {
        var mh$ = stpcpy$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__dest, __src);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle __stpncpy$MH() {
        return RuntimeHelper.requireNonNull(constants$8.__stpncpy$MH,"__stpncpy");
    }
    public static MemoryAddress __stpncpy ( Addressable __dest,  Addressable __src,  long __n) {
        var mh$ = __stpncpy$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__dest, __src, __n);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle stpncpy$MH() {
        return RuntimeHelper.requireNonNull(constants$8.stpncpy$MH,"stpncpy");
    }
    public static MemoryAddress stpncpy ( Addressable __dest,  Addressable __src,  long __n) {
        var mh$ = stpncpy$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(__dest, __src, __n);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static long _POSIX_C_SOURCE() {
        return 200809L;
    }
    public static long __STDC_ISO_10646__() {
        return 201706L;
    }
    public static MemoryAddress NULL() {
        return constants$8.NULL$ADDR;
    }
}


