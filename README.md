# Bank Highlighter

Bank Highlighter is a RuneLite plugin that lets you apply colored highlights to bank items for quick visual grouping.

<p align="center">
  <img src="https://i.imgur.com/02lFj5r.png" alt="Bank Highlighter"/>
</p>

## Install
**Plugin Hub:** search for **Bank Highlighter** and enable it.

**From source (dev):**
```bash
./gradlew build
```

## How to use
1. Open your bank.
2. Right‑click an item (hold **Shift** if you enabled “Require Shift”).
3. Choose **Bank Highlight** → pick a color from existing tags or **Pick** for a custom color.
4. Use **Reset** to remove a highlight.

Highlights apply per **item ID**, so every copy of the item will be highlighted.

## Configuration
**Menu**
- **Require Shift**: Only show the Bank Highlight menu when holding Shift.

**Tag display mode**
- **Outline**: Draw an outline around the item icon.
- **Underline**: Draw an underline on the item slot.
- **Fill**: Fill the slot background with the tag color.
- **Fill opacity**: Opacity for the fill (0–255).

**Integration**
- **Share with Inventory Tags**: Use the Inventory Tags config group so item colors are shared between both plugins.

## Integrations & Compatibility
- **Inventory Tags**: When “Share with Inventory Tags” is enabled, highlights are read/written from the `inventorytags` config group.
- **Bank Tag Layouts / Custom layouts**: Highlights display on layout/duplicate items in the bank interface.

## Troubleshooting
- **Menu doesn’t appear**: Disable “Require Shift” or ensure you’re holding Shift. Also make sure the bank is open.
- **Highlights not showing**: Verify Outline/Fill/Underline are enabled in config and that items are tagged.

## Notes
- Highlight colors are stored by item ID, not by individual bank slot.
