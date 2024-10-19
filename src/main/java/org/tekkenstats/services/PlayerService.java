package org.tekkenstats.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tekkenstats.models.Player;
import org.tekkenstats.repositories.PlayerRepository;

import java.util.Optional;

@Service
public class PlayerService
{
    @Autowired
    private PlayerRepository playerRepository;


    public Optional<Player> getPlayerWithStats(String playerId)
    {
        return playerRepository.findById(playerId);
    }

}
