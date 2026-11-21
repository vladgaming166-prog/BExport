package network.blazemc.bexport.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExportNamesTest {

    @Test
    void acceptsSimpleNames() {
        assertEquals("ruby_sword", ExportNames.normalize("ruby_sword").orElseThrow());
        assertEquals("Armor.Set-1", ExportNames.normalize("Armor.Set-1").orElseThrow());
    }

    @Test
    void rejectsPathTraversalAndEmpty() {
        assertTrue(ExportNames.normalize("../secret").isEmpty());
        assertTrue(ExportNames.normalize("foo/bar").isEmpty());
        assertTrue(ExportNames.normalize("foo\\bar").isEmpty());
        assertTrue(ExportNames.normalize("").isEmpty());
        assertTrue(ExportNames.normalize(" ").isEmpty());
        assertTrue(ExportNames.normalize("bad name").isEmpty());
        assertTrue(ExportNames.normalize(null).isEmpty());
    }
}
