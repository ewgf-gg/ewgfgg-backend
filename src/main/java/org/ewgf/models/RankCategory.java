package org.ewgf.models;

public enum RankCategory {
    ALL_RANKS("All Ranks"),
    MASTER("Master"),
    ADVANCED("Advanced"),
    INTERMEDIATE("Intermediate"),
    BEGINNER("Beginner");

    String category;

    RankCategory(String category) {
        this.category = category;
    }
}
