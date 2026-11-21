# BExport

Paper plugin by **BlazeMC Network**. It exports the ItemStack an administrator is holding (or wearing) to Base64 using Paper’s native binary serializer, together with the ItemsAdder namespaced id when ItemsAdder is present.

The files are written under `plugins/BExport/exports/` so they can be downloaded from a Minecraft host’s File Manager.

## Requirements

| Piece | Version |
| --- | --- |
| Server | Paper 1.21.11 **or** Paper 26.2 |
| Java | 21 or 25 (the JAR is Java 21 bytecode, so both runtimes load it) |
| ItemsAdder | 4.0.18 (soft depend — exports still run without it) |
| SK Crates | Optional. Inspected at runtime if installed |

BExport does **not** need NBTAPI, ProtocolLib, or any other library plugin. It does not replace ItemsAdder or SK Crates.

## Install

1. Build or download `BExport.jar`.
2. Put it in the server `plugins/` folder.
3. Restart, or run `/reload` only if you already accept `/reload` risks.
4. Grant `bexport.admin` (OPs have it by default).
5. Hold an ItemsAdder item and run `/bexport export <name>`.

After the first start you will have:

```
plugins/BExport/config.yml
plugins/BExport/exports.yml
plugins/BExport/exports/
```

## Commands

Permission: `bexport.admin`

| Command | Action |
| --- | --- |
| `/bexport` | Help |
| `/bexport export <name>` | Export the item in the main hand |
| `/bexport exportarmor <name>` | Export helmet, chestplate, leggings and boots |
| `/bexport reload` | Reload `config.yml` and re-scan SK Crates |

Tab completion is included. Names may only contain letters, digits, `_`, `-` and `.` (max 64). Path characters are rejected.

## What an export contains

Every successful export writes:

1. `plugins/BExport/exports/<name>.yml` — Base64, ItemsAdder id, material, readable metadata, Bukkit YAML ItemStack
2. `plugins/BExport/exports/<name>.txt` — the Base64 string alone, ready to copy
3. `plugins/BExport/exports/<name>.skcrates.yml` — SK Crates notes plus a snippet that matches **whatever SK Crates actually stores on this server**
4. An entry in `plugins/BExport/exports.yml`

The chat message after a success prints those exact paths.

Serialization:

- Bytes: `ItemStack.serializeAsBytes()` (Paper NBT, including components and persistent data)
- Encoding: `java.util.Base64`
- Check: the plugin immediately calls `ItemStack.deserializeBytes(...)` and aborts if the restored item does not match
- ItemsAdder id: official `CustomStack.byItemStack(...)` / `getNamespacedID()` when ItemsAdder is loaded
- Nothing is converted to a vanilla-only material. The original ItemStack is cloned and stored as-is.

Armor exports store all four slots in one YAML file, each with its own Base64 string, plus a set-level `serializeItemsAsBytes()` value.

## SK Crates — verified usage

No SK Crates JAR was present in this repository, so BExport does **not** invent reward keys and does **not** claim that SK Crates reads Paper Base64 automatically.

Public SK Crates documentation ([BuiltByBit resource 123298](https://builtbybit.com/resources/sk-crates-pick-or-roll-per-crate.123298/)) states:

- The plugin is **built against the Spigot API** (not Paper), so it cannot call `ItemStack.serializeAsBytes()`.
- Each crate is its own YAML file (`mode`, rewards, permission, hologram).
- Rewards are added **in-game** with `/crates edit`.

### Add a custom ItemsAdder item to a crate

1. Give yourself the custom item:
   ```
   /iagive YourName namespace:item_id 1
   ```
2. Hold it.
3. Optionally archive the exact stack:
   ```
   /bexport export ruby_sword
   ```
   Download `plugins/BExport/exports/ruby_sword.yml` from the File Manager if you want a backup.
4. Run:
   ```
   /crates edit
   ```
5. Open the crate and add the held ItemsAdder item as a reward in the SK Crates GUI.
6. Save. SK Crates stores that ItemStack itself.

That GUI path is the only reward-loading method documented by SK Crates. It preserves ItemsAdder identity, model data, enchantments, attributes, lore and amount because the plugin copies the live ItemStack.

### Add a custom ItemsAdder armor set

SK Crates has no documented “armor set” reward type. Add **four rewards** (or four command rewards that give each piece):

```
/iagive YourName ruby:ruby_helmet 1
/iagive YourName ruby:ruby_chestplate 1
/iagive YourName ruby:ruby_leggings 1
/iagive YourName ruby:ruby_boots 1
```

Equip them, then:

```
/bexport exportarmor ruby_set
```

Then `/crates edit` and add each piece as its own reward. The export file `plugins/BExport/exports/ruby_set.yml` keeps the four Base64 strings and namespaced ids together so you can re-give the exact stacks later.

Worked example (replace the namespace with yours — BExport never hardcodes item ids):

```
/iagive Steve ruby:ruby_helmet 1
/iagive Steve ruby:ruby_chestplate 1
/iagive Steve ruby:ruby_leggings 1
/iagive Steve ruby:ruby_boots 1
/bexport exportarmor ruby_set
/crates edit
```

If you later want SK Crates to *give* the pieces via ItemsAdder instead of a captured stack, the official command (not an SK Crates config key) is:

```
iagive %player% ruby:ruby_chestplate 1
```

Only use that inside an SK Crates reward if your crate file already has a console-command reward field. BExport will not invent that field.

### If SK Crates is installed when you export

BExport scans `plugins/<SKCrates>/` YAML:

- If it finds Bukkit `ItemStack` nodes, the `.skcrates.yml` file uses **that exact node name** and dumps `YamlConfiguration`-compatible ItemStack YAML.
- If it finds an existing Base64 item key, the snippet copies Paper Base64 into **that exact key name**.
- If it finds neither, the snippet only documents `/crates edit` and includes a Bukkit ItemStack dump for servers whose crate YAML already uses `==: org.bukkit.inventory.ItemStack`.

See `examples/ruby_sword.yml` and `examples/SKCRATES.md`.

## Build

Java 21+.

```bash
./gradlew build
```

Output:

- `build/libs/BExport.jar`
- `dist/BExport.jar`

`./gradlew test` runs MockBukkit round-trips for weapons, armor, food and blocks (ItemsAdder-like persistent data included). A live ItemsAdder + Paper server is still required to exercise `CustomStack` against real custom items.

## Project layout

```
src/main/java/network/blazemc/bexport/
src/main/resources/plugin.yml
src/main/resources/config.yml
examples/
```

Compile target: `io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT`  
ItemsAdder API: `beer.devs:itemsadder-api:4.0.18-beta-10` (provided, not shaded)
