# Demonic Pacts League Task Highlighter

A RuneLite plugin that highlights NPCs, items, world objects, and ground items related to **Demonic Pacts League** tasks. Hover for rich tooltips showing task details, requirements, points, and area info — all color-coded by difficulty.

## Install

Search **"Demonic Pacts"** in the RuneLite Plugin Hub and click install.

## Features

**Highlighting** — Task-related entities get colored overlays in the game world:
- NPCs (convex hull + difficulty label overhead)
- Items in inventory, bank, and equipment (colored borders + difficulty dot)
- Ground items (tile highlights + labels)
- World objects like mining rocks, trees, and patches (convex hull + label)

**Rich Tooltips** — Hover any highlighted entity to see task name, description, difficulty tier, point value, area, skill/quest requirements, and quantity needed.

**Difficulty Color Coding:**
- Green — Easy (10 pts)
- Orange — Medium (30 pts)
- Red — Hard (80 pts)
- Purple — Elite (200 pts)
- Cyan — Master (400 pts)

**Task Completion Tracking:**
- Auto-detects task completions from game chat messages
- Right-click any highlighted entity to manually mark tasks complete/incomplete
- Completed tasks stop highlighting (configurable)
- Per-account tracking — each RS account has its own completion data

**Configurable** — Toggle each overlay type, tooltip fields, colors, border width, and completion behavior independently in the plugin settings.

## Configuration

Open RuneLite settings → search "Demonic Pacts":

| Setting | Default | Description |
|---------|---------|-------------|
| Highlight NPCs | On | Colored hulls on task NPCs |
| Highlight Items | On | Colored borders on task items |
| Highlight Ground Items | On | Tile highlights for ground items |
| Highlight World Objects | On | Highlights on rocks, trees, patches |
| Use Difficulty Colors | On | Color by tier vs single custom color |
| NPC Border Width | 2 | Hull outline thickness |
| Show Tooltips | On | Hover tooltips |
| Show Requirements | On | Skill/quest reqs in tooltips |
| Show Points | On | Point values in tooltips |
| Show Area | On | Task area in tooltips |
| Hide Completed Tasks | On | Stop highlighting completed tasks |
| Auto-Detect Completion | On | Mark tasks done from chat messages |
| Show Completed In Tooltip | Off | Show checkmark on done tasks |

## Contributing

To add or update tasks, edit `TaskDatabase.java`. Use the helper methods:

```java
defeatNpc("Task Name", "Description", "Area", TaskDifficulty.HARD, "Requirements", "NPC Name");
equipItem("Task Name", "Description", "Area", TaskDifficulty.ELITE, "Requirements", "Item 1", "Item 2");
mineItem("Task Name", "Description", "Area", TaskDifficulty.EASY, "Requirements", qty, "Ore name");
chopItem("Task Name", "Description", "Area", TaskDifficulty.MEDIUM, "Requirements", qty, "Log name");
```

World object mappings (rocks → ores, trees → logs) are in `OBJECT_TO_TASK_KEYWORDS` at the top of `TaskDatabase.java`.

Pull requests welcome — especially for adding missing tasks or improving entity matching.

## License

BSD 2-Clause — see [LICENSE](LICENSE).
