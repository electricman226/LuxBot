package me.james.luxbot;

import java.util.*;
import net.rithms.riot.api.endpoints.match.dto.*;
import net.rithms.riot.api.endpoints.summoner.dto.*;
import net.rithms.riot.constant.*;

public class TrackSummoner
{
    private final Summoner summoner;
    private final Platform plat;
    private Match lastGame;
    private ArrayList< TrackGuild > guilds = new ArrayList<>();

    public TrackSummoner( Summoner s, Platform plat )
    {
        this.summoner = s;
        this.plat = plat;
    }

    public void addGuild( TrackGuild g )
    {
        guilds.add( g );
    }

    public TrackGuild[] getGuilds()
    {
        return guilds.toArray( new TrackGuild[guilds.size()] );
    }

    public Summoner getSummoner()
    {
        return summoner;
    }

    public Match getLastGame()
    {
        return lastGame;
    }

    public void setLastGame( Match lastGame )
    {
        this.lastGame = lastGame;
    }

    public Platform getPlatform()
    {
        return plat;
    }
}
