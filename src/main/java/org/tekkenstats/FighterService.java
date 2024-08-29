package org.tekkenstats;

import org.springframework.beans.factory.annotation.Autowired;
import org.tekkenstats.interfaces.FighterRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FighterService
{
    private final FighterRepository fighterRepository;

    //constructor injection
    @Autowired
    public FighterService(FighterRepository fighterRepository)
    {
        this.fighterRepository = fighterRepository;
    }

    public List<Fighter> getAllFighters()
    {
        return fighterRepository.findAll();
    }
}
