package org.ewgf.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;


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
