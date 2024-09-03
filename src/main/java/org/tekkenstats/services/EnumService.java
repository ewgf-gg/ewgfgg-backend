package org.tekkenstats.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.tekkenstats.mdbDocuments.EnumDocument;
import org.tekkenstats.interfaces.EnumRepository;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class EnumService
{
    private final EnumRepository enumRepository;

    //constructor injection
    @Autowired
    public EnumService(EnumRepository enumRepository)
    {
        this.enumRepository = enumRepository;
    }

    public Map<String, String> getAllFighters() {
        EnumDocument fightersDoc = enumRepository.findFightersDocument();

        return fightersDoc != null ? fightersDoc.getFighters() : Collections.emptyMap();
    }
}
