package dev.ryanhcode.sable.util;

import net.minecraft.util.filefix.access.FileRelation;
import net.minecraft.util.filefix.operations.FileFixOperation;
import net.minecraft.util.filefix.operations.FileFixOperations;
import net.minecraft.util.filefix.operations.Move;

import java.util.List;
import java.util.Map;

/**
 * File moves that bring a 1.21.11 world's mod data into the 26.1 world layout.
 *
 * <p>26.1's {@code DimensionStorageFileFix} moves each dimension from the world root / {@code DIM-1} / {@code DIM1}
 * into {@code dimensions/minecraft/<name>}, and saved data from {@code data/<id>.dat} into
 * {@code data/<namespace>/<id>.dat}, but only for vanilla's own files. Mods add their moves to that fix (see the
 * {@code DimensionStorageFileFixMixin}s) so they run in the same copy-on-write upgrade: all or nothing, with the
 * vanilla progress screen. Missing files are skipped and existing targets are never overwritten.</p>
 */
public final class LegacyWorldFileFixes {
    /** 1.21.11 dimension folders, relative to the world root, and their 26.1 locations. */
    private static final Map<String, String> OLD_TO_NEW_DIMENSIONS = Map.of(
            "", "dimensions/minecraft/overworld",
            "DIM-1", "dimensions/minecraft/the_nether",
            "DIM1", "dimensions/minecraft/the_end"
    );

    /** The same, for each dimension's saved-data folder. */
    private static final Map<String, String> OLD_TO_NEW_DATA = Map.of(
            "data", "dimensions/minecraft/overworld/data",
            "DIM-1/data", "dimensions/minecraft/the_nether/data",
            "DIM1/data", "dimensions/minecraft/the_end/data"
    );

    private LegacyWorldFileFixes() {
    }

    /**
     * Moves per-dimension saved data {@code <id>.dat} to {@code <namespace>/<id>.dat}, matching a 26.1
     * {@code SavedDataType} id of {@code namespace:<id>}.
     */
    public static List<FileFixOperation> movePerDimensionSavedData(final String namespace, final List<String> ids) {
        final List<Move> moves = ids.stream()
                .map(id -> FileFixOperations.move(id + ".dat", namespace + "/" + id + ".dat"))
                .toList();
        return List.of(
                // Modded dimensions already lived in dimensions/<ns>/<path>; only the layout inside data/ changed.
                FileFixOperations.applyInFolders(FileRelation.DIMENSIONS_DATA, List.copyOf(moves)),
                FileFixOperations.groupMove(OLD_TO_NEW_DATA, moves)
        );
    }

    /** Moves a per-dimension folder (e.g. Sable's {@code sublevels}) along with its dimension. */
    public static FileFixOperation movePerDimensionFolder(final String folder) {
        return FileFixOperations.groupMove(OLD_TO_NEW_DIMENSIONS, List.of(FileFixOperations.moveSimple(folder)));
    }
}
