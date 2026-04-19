package com.demonicpacts;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DemonicPactsTask
{
    String name;
    String description;
    String area;
    TaskDifficulty difficulty;
    TaskType type;
    String requirements;
    /**
     * Keywords to match against in-game entity names (NPC names, item names, etc.)
     * Matching is case-insensitive.
     */
    String[] matchKeywords;
    int quantity;
}
