package org.tekkenstats.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Map;


@Data
@Entity
@Table(name = "tekken_stats_summary")
public class TekkenStatsSummary
{
    @Id
    private Integer id = 1;
    private long totalReplays;
    private long totalPlayers;
}
