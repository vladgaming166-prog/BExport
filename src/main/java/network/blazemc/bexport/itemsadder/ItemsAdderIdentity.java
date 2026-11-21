package network.blazemc.bexport.itemsadder;

public record ItemsAdderIdentity(
    String namespacedId,
    String namespace,
    String id,
    String displayName,
    boolean block,
    boolean customDurability,
    Integer durability,
    Integer maxDurability,
    String source
) {
    public static ItemsAdderIdentity of(String namespacedId, String source) {
        String namespace = "";
        String id = namespacedId;
        int colon = namespacedId.indexOf(':');
        if (colon > 0 && colon < namespacedId.length() - 1) {
            namespace = namespacedId.substring(0, colon);
            id = namespacedId.substring(colon + 1);
        }
        return new ItemsAdderIdentity(namespacedId, namespace, id, null, false, false, null, null, source);
    }
}
