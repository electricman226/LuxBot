package me.james.luxbot.commands;

import java.sql.*;
import java.util.*;
import me.james.basebot.command.*;
import me.james.luxbot.*;
import net.rithms.riot.api.*;
import net.rithms.riot.api.endpoints.summoner.dto.*;
import net.rithms.riot.constant.*;
import sx.blah.discord.handle.obj.*;

public class RemoveSummonerCommand extends Command
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
            chan.sendMessage( "**Usage:** !removesummoner <platform> <summoner name...>" );
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
            int rowsMod = LuxBot.DATABASE.prepareStatement( String.format( "DELETE FROM `summoner_guilds` WHERE `guild_id`=%d AND `summoner_id`=%d AND `platform`='%s'", chan.getGuild().getLongID(), sum.getId(), plat.getName() ) ).executeUpdate();
            if ( rowsMod <= 0 )
            {
                chan.sendMessage( "*Error:** That Summoner is not added to your guild." );
                return "Summoner not in guild. (guild " + chan.getGuild().getLongID() + "/" + chan.getGuild().getName() + " summoner " + sum.toString() + " platform " + plat.getName() + ")";
            }
            if ( rowsMod > 1 )
            {
                chan.sendMessage( "**Success:** Summoner " + sum.getName() + " is no longer part of your guild; however, SQL returned more than 1 rows modified?" );
                return "Summoner " + sum.toString() + " (platform " + plat.getName() + ") is no longer part of guild " + chan.getGuild().getLongID() + "/" + chan.getGuild().getName() + " - SQL rows modded " + rowsMod + ", however.";
            }
            chan.sendMessage( "**Success:** Summoner " + sum.getName() + " is no longer part of your guild." );
            return "Summoner " + sum.toString() + " (platform " + plat.getName() + ") is no longer part of guild " + chan.getGuild().getLongID() + "/" + chan.getGuild().getName();
        } catch ( RiotApiException e )
        {
            chan.sendMessage( "**Error:** Invalid Summoner." );
            return "Invalid summoner/drunk API.";
        } catch ( SQLException e )
        {
            chan.sendMessage( "**Error:** There was an error whilst attempting to remove that Summoner. (SQL error)" );
            e.printStackTrace();
        }
        return null;
    }
}
