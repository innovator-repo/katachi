/* Calls the compiled Lume core exactly as the JNI shim will: grids are
 * plain double arrays, each call bracketed by an arena mark and reset. */
#include <math.h>
#include <stdio.h>
#include <string.h>

long long lume_arena_mark(void);
void lume_arena_reset(long long mark);
long long apply(long long op, long long a, long long b);
double mismatch(long long a, long long b);
double tangram_error(long long target, long long cover);

#define N 6
#define R 96
static double l[N * N], none[N * N], out[N * N], target[R * R], cover[R * R];
static int failures;

static void expect(const char *what, double got, double want) {
    int ok = fabs(got - want) < 1e-9;
    printf("%s  %-34s got %.9f want %.9f\n", ok ? "PASS" : "FAIL", what, got, want);
    if (!ok) failures++;
}

static void run(int op, const double *a) {
    long long mark = lume_arena_mark();
    memcpy(out, (double *)apply(op, (long long)a, (long long)none), sizeof out);
    lume_arena_reset(mark);
}

int main(void) {
    /* An L in the top-left corner: (0,0) (1,0) (2,0) (2,1). */
    l[0] = l[6] = l[12] = l[13] = 1;
    long long start = lume_arena_mark();
    for (int round = 0; round < 1000; round++) run(0, l);
    expect("quarter turn: top row", out[3] + out[4] + out[5], 3);
    expect("quarter turn: (1,3)", out[1 * N + 3], 1);
    run(1, l);
    expect("mirror: (2,4)", out[2 * N + 4], 1);
    run(3, l);
    expect("shift right: (0,1)", out[1], 1);
    run(5, l);
    double inverted = 0;
    for (int i = 0; i < N * N; i++) inverted += out[i];
    expect("invert: cell count", inverted, 32);
    run(9, l);
    double filled = 0;
    for (int i = 0; i < N * N; i++) filled += out[i];
    expect("quarter fill: cell count", filled, 16);
    expect("quarter fill: (5,5)", out[35], 1);
    long long mark = lume_arena_mark();
    expect("mismatch with itself", mismatch((long long)l, (long long)l), 0);
    expect("mismatch with nothing", mismatch((long long)l, (long long)none), 4);
    for (int i = 0; i < R * R; i++) { target[i] = i < R * R / 2; cover[i] = target[i]; }
    expect("tangram solved", tangram_error((long long)target, (long long)cover), 0);
    cover[0] = 0; cover[R * R - 1] = 2;
    expect("tangram one hole, one spill", tangram_error((long long)target, (long long)cover), 3.0 / (R * R / 2));
    lume_arena_reset(mark);
    expect("arena back to start", (double)(lume_arena_mark() - start), 0);
    printf(failures ? "%d FAILED\n" : "all passed\n", failures);
    return failures != 0;
}
