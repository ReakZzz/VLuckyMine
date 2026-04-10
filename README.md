# ✦ V-LuckyMine

> **Paper 1.21.1 Plugin** — by [@reakkz](https://github.com/reakkz)

When a player mines a configured ore or block, a lucky container spawns in its place filled with randomised loot. Every aspect — which blocks trigger it, what drops, how rare, what container type — is fully configurable via two clean YAML files and an in-game GUI.

---

## 📦 Building

Requires **Java 21** and **Maven**.

```bash
mvn clean package
```

Drop `target/V-LuckyMine-1.0.0.jar` into your server's `plugins/` folder and restart.

---

## 🗂 File Structure

```
V-LuckyMine/
├── pom.xml
├── src/main/
│   ├── resources/
│   │   ├── plugin.yml
│   │   ├── config.yml          ← Settings, messages, particles, blocked worlds
│   │   └── loot.yml            ← NBT loot tables + block chances
│   └── java/com/luckymine/
│       ├── LuckyMine.java
│       ├── commands/
│       │   └── LuckyMineCommand.java
│       ├── gui/
│       │   ├── GuiUtil.java
│       │   ├── GuiManager.java
│       │   ├── MainMenuGui.java
│       │   ├── LootListGui.java
│       │   ├── EditLootGui.java
│       │   ├── EditItemGui.java
│       │   ├── BlockListGui.java
│       │   ├── EditBlockGui.java
│       │   ├── StructureSelectorGui.java
│       │   └── NbtSelectorGui.java
│       ├── listeners/
│       │   ├── BlockBreakListener.java
│       │   ├── ChestInteractListener.java
│       │   └── GuiListener.java
│       ├── managers/
│       │   ├── ConfigManager.java
│       │   └── NBTManager.java
│       └── utils/
│           ├── LootEntry.java
│           └── NbtTable.java
```

---

## ⚙️ config.yml — Settings

```yaml
chest-despawn-ticks: 200   # How long the container stays (20 ticks = 1 second)
silk-touch: false          # Allow Silk Touch tools to trigger lucky containers?
prefix: '&6[&eV-LuckyMine&6] &r'

messages:
  chest-found: '{prefix}&aYou found a &6Lucky Chest&a! Open it fast!'
  # ... all plugin messages are customisable here

blocked-worlds:
  - world_nether            # Plugin is fully disabled in these worlds

particles:
  enabled: true
  type: PORTAL              # Any valid Bukkit Particle name
  sound: ENTITY_ENDERMAN_TELEPORT
  count: 20
  offset-x: 0.3
  offset-y: 0.5
  offset-z: 0.3
  speed: 0.05
```

---

## 🎲 loot.yml — Loot Tables & Blocks

### NBT Loot Tables

```yaml
nbt:
  diamond_loot:
    structure: chest          # Container type (see list below)
    items:
      - DIAMOND:2 50.0        # MATERIAL:AMOUNT CHANCE%
      - IRON_INGOT:10 80.0    # Amount and chance are optional (default: 1, 100%)
      - GOLD_INGOT:5 60.0
```

Each item in the table has its **own independent chance** to appear. Every time the chest spawns, each item is rolled separately.

**Supported structures:**

| Category | Values |
|---|---|
| Vanilla | `chest` `trapped_chest` `hopper` `barrel` `dropper` `dispenser` |
| Shulker | `shulker_box` `white_shulker_box` `orange_shulker_box` `magenta_shulker_box` `light_blue_shulker_box` `yellow_shulker_box` `lime_shulker_box` `pink_shulker_box` `gray_shulker_box` `light_gray_shulker_box` `cyan_shulker_box` `purple_shulker_box` `blue_shulker_box` `brown_shulker_box` `green_shulker_box` `red_shulker_box` `black_shulker_box` |
| Copper* | `copper_chest` `exposed_copper_chest` `weathered_copper_chest` `oxidized_copper_chest` |

*Copper chests require a mod/datapack — falls back to `chest` on vanilla servers.*

The container always **faces toward the player** who mined the block.

### Block Chances

```yaml
blocks:
  DIAMOND_ORE:
    chance: 50.0         # 0.0–100.0% probability to spawn a container
    nbt: diamond_loot    # Which loot table to use (or "random" for any)
  STONE:
    chance: 1.0
    nbt: random
```

---

## 🎮 In-Game GUI

Open with `/lm`, `/luckymine`, `/lm menu`, or `/luckymine menu`.

> **Commands and the GUI are OP-only** and completely hidden from non-ops (no tab-complete, no `/help` entry, no permission message).

### Main Menu
The root screen with three options: **Loot Tables**, **Block Chances**, and **Reload**.

### ✦ Loot Tables
- Paginated list of all NBT tables
- Click any table to edit it
- **Right-click** the green `✚` button while **holding an item** to add it to the table (default 0% chance)

### Edit Loot Table
| Slot | Action |
|---|---|
| 0 | Change the container structure |
| 4 | Table info (name, item count, current structure) |
| 8 | Delete the table permanently |
| 9–44 | Existing items — click to edit chance/amount |
| 46 | Previous page |
| 52 | Next page |
| 53 | Add held item (default 0% chance) |

### Edit Item
Full editor for a single loot entry:
- **Row 1** — Amount adjuster: `-10 / -5 / -1` · display (click for exact via chat) · `+1 / +5 / +10`
- **Row 2** — Chance adjuster: `-10% / -5% / -1%` · display (click for exact via chat) · `+1% / +5% / +10%`
- **Row 4** — Remove item
- **Row 5** — Back

### ⛏ Block Chances
- Paginated list of all configured blocks
- Click any block to edit its chance and assigned loot table
- **Right-click** the green `✚` button while **holding a block** to register it (default 0% chance, `random` table)

### Edit Block
- `±` buttons to adjust spawn chance
- Click the paper display to type an exact value in chat
- **Change Loot Table** → opens the loot table selector
- **Delete** — removes the block from config

### Select Structure (paginated)
All supported container types displayed with their icons. Currently selected one is highlighted in green.

### Select Loot Table
All registered tables plus a `random` option. Used when assigning a table to a block.

---

## 🔑 Permissions

| Permission | Default | Description |
|---|---|---|
| `luckymine.admin` | OP | Full access to all commands and the GUI |
| `luckymine.lucky` | Everyone | Can trigger lucky container spawns when mining |

---

## 🔧 Commands

> All commands hidden from non-ops.

| Command | Description |
|---|---|
| `/lm` or `/luckymine` | Open the GUI menu |
| `/lm menu` | Open the GUI menu |
| `/lm reload` | Reload `config.yml` and `loot.yml` |
| `/lm createnbt <key>` | Create/overwrite an NBT table by right-clicking a chest |
| `/lm cancel` | Cancel an active `createnbt` session |

**Aliases:** `/lm` = `/luckymine`

---

## 🧠 How It Works

1. Player mines a block listed in `loot.yml > blocks`
2. The plugin rolls the block's `chance` percentage
3. If successful, it resolves the assigned loot table and rolls **each item's individual chance**
4. One tick after the break (so the block is fully removed), the container spawns exactly where the ore was, facing the player
5. The container despawns automatically after `chest-despawn-ticks`
6. If the player **closes the container** before the timer, it despawns immediately
7. If the player **breaks the container**, the block drop is suppressed but all items scatter naturally on the ground

---

## 📝 Notes

- **Silk Touch** behaviour is controlled by `silk-touch` in `config.yml` (default `false` — silk touch will NOT trigger lucky spawns)
- All changes made in the GUI are **saved to `loot.yml` immediately** — no manual editing required
- New items added via GUI default to **0% chance** so you can set the correct value before enabling them
- New blocks added via GUI default to **0% chance** with the `random` table
- `createnbt` items also default to **0% chance** after saving
