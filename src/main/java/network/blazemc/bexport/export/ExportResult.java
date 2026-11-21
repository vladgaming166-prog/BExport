package network.blazemc.bexport.export;

import java.nio.file.Path;
import java.util.List;
import network.blazemc.bexport.itemsadder.ItemsAdderIdentity;

public record ExportResult(
    boolean success,
    String name,
    String type,
    List<Path> files,
    ItemsAdderIdentity itemsAdder,
    String error
) {
    public static ExportResult failed(String error) {
        return new ExportResult(false, null, null, List.of(), null, error);
    }
}
