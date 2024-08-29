package org.tekkenstats;

import org.springframework.beans.factory.annotation.Autowired;
import org.tekkenstats.interfaces.EnumRepository;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class FighterService
{
    private final EnumRepository enumRepository;

    //constructor injection
    @Autowired
    public FighterService(EnumRepository enumRepository)
    {
        this.enumRepository = enumRepository;
    }

    public Map<String, String> getAllFighters() {
        EnumDocument fightersDoc = enumRepository.findFightersDocument();

        return fightersDoc != null ? fightersDoc.getFighters() : Collections.emptyMap();
    }
}
