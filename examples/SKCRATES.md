# Adding exported ItemsAdder armor to SK Crates

This is the workflow that matches SK Crates’ public documentation. BExport does not invent crate YAML keys.

SK Crates (BuiltByBit: “SK Crates | Pick Or Roll, Per Crate”) is a Spigot-API plugin. Rewards are added with `/crates edit` and stored in one YAML file per crate. It does not document a `base64:` item field.

## Armor set

1. Get the four ItemsAdder pieces (any namespace; example only):
   ```
   /iagive Steve ruby:ruby_helmet 1
   /iagive Steve ruby:ruby_chestplate 1
   /iagive Steve ruby:ruby_leggings 1
   /iagive Steve ruby:ruby_boots 1
   ```
2. Put them on.
3. `/bexport exportarmor ruby_set`
4. Confirm chat printed:
   ```
   plugins/BExport/exports/ruby_set.yml
   plugins/BExport/exports/ruby_set.txt
   plugins/BExport/exports/ruby_set.skcrates.yml
   plugins/BExport/exports.yml
   ```
5. `/crates edit` → open the crate → add each worn piece as its own reward → save.

`ruby_set.yml` keeps each piece’s Paper Base64, Bukkit ItemStack YAML, and `namespace:item_id` so you can restore the exact stacks later. SK Crates itself is fed through the GUI, which copies the live ItemStack (ItemsAdder identity included).

## Single weapon / tool / food / block

```
/iagive Steve ruby:legendary_sword 1
/bexport export ruby_sword
/crates edit
```

Add the held item as a reward.

## If you edit crate YAML by hand

Only do this when the crate file **already** contains `==: org.bukkit.inventory.ItemStack` (the Spigot `YamlConfiguration.getItemStack` format). Copy the `bukkit-itemstack` node from the BExport YAML over that existing node, then `/crates reload`.

Do **not** paste `paper-bytes.base64` into SK Crates unless BExport’s SK Crates scan reported a live Base64 key name. That key name, if any, is written into `*.skcrates.yml`.
