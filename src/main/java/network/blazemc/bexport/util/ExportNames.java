package network.blazemc.bexport.util;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class ExportNames {

    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$");

    private ExportNames() {
    }

    public static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.contains("..") || trimmed.contains("/") || trimmed.contains("\\")) {
            return Optional.empty();
        }
        if (!VALID.matcher(trimmed).matches()) {
            return Optional.empty();
        }
        return Optional.of(trimmed);
    }

    public static String indexKey(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
