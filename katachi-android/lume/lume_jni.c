/* JNI shim between app.katachi.LumeCore and the compiled Lume core.
 *
 * In compiled Lume a tensor is a pointer to its f64 elements and nothing else
 * (the shape lives in the type), so a Java double[] is passed straight in.
 * Every call is bracketed by lume_arena_mark/reset: whatever Lume allocates,
 * including the result, is released once it has been copied out. */
#include <jni.h>

long long lume_arena_mark(void);
void lume_arena_reset(long long mark);
long long apply(long long op, long long a, long long b);
double mismatch(long long a, long long b);
double tangram_error(long long target, long long cover);

#define CELLS 36
#define RASTER (96 * 96)

static int sized(JNIEnv *env, jdoubleArray array, jsize want) {
    return array != NULL && (*env)->GetArrayLength(env, array) == want;
}

static void throw_shape(JNIEnv *env) {
    jclass error = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (error != NULL) (*env)->ThrowNew(env, error, "array has the wrong size for puzzles.lm");
}

#define PIN(name) jdouble *p_##name = (*env)->GetDoubleArrayElements(env, name, NULL)
#define UNPIN(name) (*env)->ReleaseDoubleArrayElements(env, name, p_##name, JNI_ABORT)

JNIEXPORT jdoubleArray JNICALL
Java_app_katachi_LumeCore_nativeApply(JNIEnv *env, jobject self, jint op, jdoubleArray a, jdoubleArray b) {
    (void)self;
    if (!sized(env, a, CELLS) || !sized(env, b, CELLS)) {
        throw_shape(env);
        return NULL;
    }
    PIN(a); PIN(b);
    long long mark = lume_arena_mark();
    long long out = apply((long long)op, (long long)p_a, (long long)p_b);
    jdoubleArray copied = (*env)->NewDoubleArray(env, CELLS);
    if (copied != NULL) (*env)->SetDoubleArrayRegion(env, copied, 0, CELLS, (const jdouble *)out);
    lume_arena_reset(mark);
    UNPIN(a); UNPIN(b);
    return copied;
}

JNIEXPORT jdouble JNICALL
Java_app_katachi_LumeCore_nativeMismatch(JNIEnv *env, jobject self, jdoubleArray a, jdoubleArray b) {
    (void)self;
    if (!sized(env, a, CELLS) || !sized(env, b, CELLS)) {
        throw_shape(env);
        return 0;
    }
    PIN(a); PIN(b);
    long long mark = lume_arena_mark();
    double out = mismatch((long long)p_a, (long long)p_b);
    lume_arena_reset(mark);
    UNPIN(a); UNPIN(b);
    return out;
}

JNIEXPORT jdouble JNICALL
Java_app_katachi_LumeCore_nativeTangramError(JNIEnv *env, jobject self, jdoubleArray target, jdoubleArray cover) {
    (void)self;
    if (!sized(env, target, RASTER) || !sized(env, cover, RASTER)) {
        throw_shape(env);
        return 0;
    }
    PIN(target); PIN(cover);
    long long mark = lume_arena_mark();
    double out = tangram_error((long long)p_target, (long long)p_cover);
    lume_arena_reset(mark);
    UNPIN(target); UNPIN(cover);
    return out;
}
