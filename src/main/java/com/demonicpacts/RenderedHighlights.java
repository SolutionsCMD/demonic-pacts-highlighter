package com.demonicpacts;

import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

/**
 * Shared per-frame record of which entities each overlay actually drew a
 * highlight for. Lets the tooltip overlay suppress tooltips on entities that
 * the category / onePerType filters kept off-screen, so hovered tooltips stay
 * in sync with the visible highlights.
 *
 * Each overlay clears its own slice at the start of its render() and marks
 * what it actually drew. Overlays run on the EDT so locking isn't needed.
 */
@Singleton
public class RenderedHighlights
{
    // NPCs: tracked by the stable per-scene index.
    private final Set<Integer> renderedNpcIndices = new HashSet<>();
    // NPCs also tracked by lowercase name for fallback lookups in the tooltip.
    private final Set<String> renderedNpcNames = new HashSet<>();

    // World objects: "world_x:world_y:lowercase_object_name"
    private final Set<String> renderedObjectKeys = new HashSet<>();

    // Ground items: "world_x:world_y:lowercase_item_name"
    private final Set<String> renderedGroundKeys = new HashSet<>();

    // Inventory / bank / equipment items: "parent_widget_id:lowercase_item_name"
    private final Set<String> renderedItemKeys = new HashSet<>();

    public void clearNpcs()
    {
        renderedNpcIndices.clear();
        renderedNpcNames.clear();
    }

    public void markNpc(int npcIndex)
    {
        renderedNpcIndices.add(npcIndex);
    }

    public void markNpcName(String name)
    {
        if (name != null)
        {
            renderedNpcNames.add(name.toLowerCase());
        }
    }

    public boolean isNpcRendered(int npcIndex)
    {
        return renderedNpcIndices.contains(npcIndex);
    }

    public boolean isNpcRenderedByName(String name)
    {
        return name != null && renderedNpcNames.contains(name.toLowerCase());
    }

    public void clearObjects()
    {
        renderedObjectKeys.clear();
    }

    public void markObject(int worldX, int worldY, String objectName)
    {
        renderedObjectKeys.add(buildWorldKey(worldX, worldY, objectName));
    }

    public boolean isObjectRendered(int worldX, int worldY, String objectName)
    {
        return renderedObjectKeys.contains(buildWorldKey(worldX, worldY, objectName));
    }

    /**
     * Lenient check: is there a rendered object with this name within a small
     * tile radius of (worldX, worldY)? Covers cases where the menu entry's
     * scene coords and the gameObject's scene coords differ by a tile due to
     * multi-tile objects or orientation.
     */
    public boolean isObjectRenderedNear(int worldX, int worldY, String objectName, int radius)
    {
        if (objectName == null) return false;
        String lowerName = objectName.toLowerCase();
        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dy = -radius; dy <= radius; dy++)
            {
                if (renderedObjectKeys.contains(buildWorldKey(worldX + dx, worldY + dy, lowerName)))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Softer check for tooltip usage: is any object with this name currently
     * rendered anywhere? Useful when the tooltip can't easily pinpoint the
     * hovered tile but the overlay decided to keep the whole category off.
     */
    public boolean isAnyObjectRendered(String objectName)
    {
        if (objectName == null) return false;
        String suffix = ":" + objectName.toLowerCase();
        for (String key : renderedObjectKeys)
        {
            if (key.endsWith(suffix))
            {
                return true;
            }
        }
        return false;
    }

    public void clearGround()
    {
        renderedGroundKeys.clear();
    }

    public void markGround(int worldX, int worldY, String itemName)
    {
        renderedGroundKeys.add(buildWorldKey(worldX, worldY, itemName));
    }

    public boolean isGroundRendered(int worldX, int worldY, String itemName)
    {
        return renderedGroundKeys.contains(buildWorldKey(worldX, worldY, itemName));
    }

    /** Lenient check within a tile radius — same rationale as isObjectRenderedNear. */
    public boolean isGroundRenderedNear(int worldX, int worldY, String itemName, int radius)
    {
        if (itemName == null) return false;
        String lowerName = itemName.toLowerCase();
        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dy = -radius; dy <= radius; dy++)
            {
                if (renderedGroundKeys.contains(buildWorldKey(worldX + dx, worldY + dy, lowerName)))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isAnyGroundRendered(String itemName)
    {
        if (itemName == null) return false;
        String suffix = ":" + itemName.toLowerCase();
        for (String key : renderedGroundKeys)
        {
            if (key.endsWith(suffix))
            {
                return true;
            }
        }
        return false;
    }

    public void clearItems()
    {
        renderedItemKeys.clear();
    }

    public void markItem(int parentWidgetId, String itemName)
    {
        renderedItemKeys.add(parentWidgetId + ":" + itemName.toLowerCase());
    }

    public boolean isItemRendered(int parentWidgetId, String itemName)
    {
        return renderedItemKeys.contains(parentWidgetId + ":" + itemName.toLowerCase());
    }

    public boolean isAnyItemRendered(String itemName)
    {
        if (itemName == null) return false;
        String suffix = ":" + itemName.toLowerCase();
        for (String key : renderedItemKeys)
        {
            if (key.endsWith(suffix))
            {
                return true;
            }
        }
        return false;
    }

    private static String buildWorldKey(int worldX, int worldY, String name)
    {
        return worldX + ":" + worldY + ":" + (name == null ? "" : name.toLowerCase());
    }
}
