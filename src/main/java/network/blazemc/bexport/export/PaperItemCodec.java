package network.blazemc.bexport.export;

import java.util.Base64;
import java.util.Objects;
import org.bukkit.inventory.ItemStack;

/**
 * Paper native ItemStack binary codec. Bytes come from {@link ItemStack#serializeAsBytes()}
 * and are stored as standard Java Base64. The round-trip uses {@link ItemStack#deserializeBytes(byte[])}.
 */
public final class PaperItemCodec {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    public String encode(ItemStack item) {
        Objects.requireNonNull(item, "item");
        byte[] bytes = item.serializeAsBytes();
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("Paper returned empty serializeAsBytes() data.");
        }
        return ENCODER.encodeToString(bytes);
    }

    public byte[] encodeBytes(ItemStack item) {
        Objects.requireNonNull(item, "item");
        return item.serializeAsBytes();
    }

    public ItemStack decode(String base64) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalArgumentException("Base64 data is empty.");
        }
        byte[] bytes;
        try {
            bytes = DECODER.decode(base64.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid Base64 data.", exception);
        }
        return decodeBytes(bytes);
    }

    public ItemStack decodeBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Serialized item bytes are empty.");
        }
        ItemStack restored = ItemStack.deserializeBytes(bytes);
        if (restored == null || restored.getType().isAir()) {
            throw new IllegalStateException("Deserialized ItemStack is empty.");
        }
        return restored;
    }

    public String encodeSet(ItemStack[] items) {
        byte[] bytes = ItemStack.serializeItemsAsBytes(items);
        return ENCODER.encodeToString(bytes);
    }

    public ItemStack[] decodeSet(String base64) {
        byte[] bytes = DECODER.decode(base64.trim());
        return ItemStack.deserializeItemsFromBytes(bytes);
    }

    public ValidationResult validate(ItemStack original, String base64) {
        try {
            ItemStack restored = decode(base64);
            if (restored.getType() != original.getType()) {
                return ValidationResult.failed("material mismatch: " + original.getType() + " vs " + restored.getType());
            }
            if (restored.getAmount() != original.getAmount()) {
                return ValidationResult.failed("amount mismatch: " + original.getAmount() + " vs " + restored.getAmount());
            }
            if (!restored.isSimilar(original) && !original.isSimilar(restored)) {
                return ValidationResult.failed("isSimilar() returned false after deserializeBytes()");
            }
            return ValidationResult.ok(restored);
        } catch (RuntimeException exception) {
            return ValidationResult.failed(exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    public record ValidationResult(boolean success, String reason, ItemStack restored) {
        static ValidationResult ok(ItemStack restored) {
            return new ValidationResult(true, "ok", restored);
        }

        static ValidationResult failed(String reason) {
            return new ValidationResult(false, reason, null);
        }
    }
}
