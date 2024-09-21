package org.tekkenstats.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tekkenstats.Player;
import org.tekkenstats.interfaces.PlayerRepository;
//import org.tekkenstats.mdbDocuments.PlayerDocument;

import java.util.List;

@Service
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepository;


    public List<Player> getPlayersByName(String name) {
        return playerRepository.findByName(name);
    }

//    public PlayerDocument findPlayerByID(String ID)
//    {
//        return playerRepository.findPlayerByID(ID);
//    }
}
