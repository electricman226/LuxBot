package me.james.luxbot.commands;

import java.sql.*;
import java.util.*;
import me.james.basebot.command.*;
import me.james.luxbot.*;
import net.rithms.riot.api.*;
import net.rithms.riot.api.endpoints.match.dto.*;
import net.rithms.riot.api.endpoints.summoner.dto.*;
import net.rithms.riot.constant.*;
import sx.blah.discord.handle.obj.*;

public class AddSummonerCommand extends Command
{
    @Override
    public String doCommand( String[] args, IUser user, IChannel chan )
    {
        if ( !user.getPermissionsForGuild( chan.getGuild() ).contains( Permissions.MANAGE_CHANNEL ) && !user.getPermissionsForGuild( chan.getGuild() ).contains( Permissions.MANAGE_CHANNELS ) && user.getLongID() != chan.getGuild().getOwnerLongID() )
        {
            chan.sendMessage( "**Error:** You do not have permission to do this. (requires *Manage Channels* permission, or permission to manage this channel)" );
            return "No permission.";
        }
        if ( !LuxBot.guildChannels.containsKey( chan.getGuild() ) )
        {
            chan.sendMessage( "**Error:** You haven't setup LuxBot properly yet. Use `!setchannel` to designate an output channel for all game info." );
            return "Not registered.";
        }
        if ( args.length < 3 )
        {
            chan.sendMessage( "**Usage:** !addsummoner <platform> <summoner name...>" );
            return "Not enough args.";
        }
        Platform plat;
        try
        {
            plat = Platform.getPlatformByName( args[1] );
        } catch ( NoSuchElementException e )
        {
            String str = "**Error:** Invalid platform. Valid platforms are:\n";
            for ( Platform p : Platform.values() )
                str += "- " + p.getName() + "\n";
            str = str.trim();
            chan.sendMessage( str );
            return "Invalid platform.";
        }
        String name = "";
        for ( int i = 2; i < args.length; i++ )
            name += args[i] + " ";
        name = name.trim();
        try
        {
            Summoner sum = LuxBot.RIOT_API.getSummonerByName( plat, name );
            if ( sum == null )
            {
                chan.sendMessage( "**Error:** Invalid Summoner." );
                return "Invalid summoner.";
            }
            ResultSet rs = LuxBot.DATABASE.prepareStatement( String.format( "SELECT `guild_id`, `summoner_id`, `platform` FROM `summoner_guilds` WHERE `guild_id`='%d' AND `summoner_id`='%d' AND platform='%s'", chan.getGuild().getLongID(), sum.getId(), plat.getName() ) ).executeQuery();
            if ( rs.next() )
            {
                chan.sendMessage( "**Error:** This Summoner is already added." );
                return "Already existing Summoner.";
            }
            LuxBot.DATABASE.prepareStatement( String.format( "INSERT INTO `summoner_guilds`(`guild_id`, `summoner_id`, `platform`) VALUES (%d,%d,'%s')", chan.getGuild().getLongID(), sum.getId(), plat.getName() ) ).executeUpdate();
            TrackSummoner ts = new TrackSummoner( sum, plat );
            ts.addGuild( new TrackGuild( chan.getGuild(), LuxBot.guildChannels.get( chan.getGuild() ) ) );
            Match lg = LuxBot.getLastGame( sum, plat );
            if ( lg == null )
            {
                chan.sendMessage( "**Error:** No recent games for this Summoner. This error usually occurs when a Summoner has no recent games." );
                return "No recent games?";
            }
            ts.setLastGame( lg );
            LuxBot.DATABASE.prepareStatement( String.format( "INSERT INTO `last_games`(`summoner_id`, `game_id`, `platform`) VALUES (%d,%d,'%s')", sum.getId(), lg.getGameId(), plat.getName() ) ).executeUpdate();
            LuxBot.summoners.add( ts );
            chan.sendMessage( "**Success:** Summoner " + sum.getName() + " is now part of your guild." );
            return "Summoner " + sum.toString() + " is now part of guild " + chan.getGuild().getLongID() + "/" + chan.getGuild().getName();
        } catch ( RiotApiException e )
        {
            chan.sendMessage( "**Error:** Invalid Summoner." );
            return "Invalid summoner/drunk API.";
        } catch ( SQLException e )
        {
            chan.sendMessage( "**Error:** There was an error whilst attempting to add that Summoner. (SQL error)" );
            e.printStackTrace();
        }
        return null;
    }
}
