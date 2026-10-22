package io.github.fabricators_of_create.porting_lib;

/**
 * Compile-time stand-ins for the Porting Lib datagen API, which has no 26.1 release.
 *
 * <p>Everything under {@code src/datagenStubs} keeps registration code (Registrate's
 * {@code .blockstate(...)}, {@code .model(...)} callbacks and friends) compiling. The callbacks
 * only run during a datagen run, so nothing here is reached in game. Running datagen fails fast
 * through {@link #unsupported()}. See PORTING-26.1.md, "Datagen stubs", for how to remove them.</p>
 */
public final class DatagenStubs {
    private DatagenStubs() {
    }

    public static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException(
                "Data generation is stubbed on the 26.1 branch; see PORTING-26.1.md (Datagen stubs)");
    }
}
