package org.tekkenstats.events;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class ReplayProcessingCompletedEvent
{
    private Set<Integer> gameVersions;
}
