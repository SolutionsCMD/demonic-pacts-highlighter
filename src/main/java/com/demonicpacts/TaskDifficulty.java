package com.demonicpacts;

import java.awt.Color;

public enum TaskDifficulty
{
    EASY(10, new Color(0, 180, 0)),
    MEDIUM(30, new Color(255, 176, 0)),
    HARD(80, new Color(220, 60, 60)),
    ELITE(200, new Color(160, 32, 240)),
    MASTER(400, new Color(0, 200, 200));

    private final int points;
    private final Color color;

    TaskDifficulty(int points, Color color)
    {
        this.points = points;
        this.color = color;
    }

    public int getPoints()
    {
        return points;
    }

    public Color getColor()
    {
        return color;
    }
}
