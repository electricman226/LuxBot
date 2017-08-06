package me.james.luxbot.commands;

import java.sql.*;
import me.james.basebot.command.*;
import me.james.luxbot.*;
import sx.blah.discord.handle.obj.*;

public class SetOutputChannelCommand extends Command
{

    @Override
    public String doCommand( String[] args, IUser user, IChannel chan )
    {
        if ( !user.getPermissionsForGuild( chan.getGuild() ).contains( Permissions.MANAGE_SERVER ) && user.getLongID() != chan.getGuild().getOwnerLongID() )
        {
            chan.sendMessage( "**Error:** You do not have permission to do this. (requires *Manage Server* permission)" );
            return "No permission.";
        }
        if ( !chan.getModifiedPermissions( LuxBot.BOT.getBot().getOurUser() ).contains( Permissions.SEND_MESSAGES ) && LuxBot.BOT.getBot().getOurUser().getRolesForGuild( chan.getGuild() ).stream().noneMatch( r -> r.getPermissions().contains( Permissions.SEND_MESSAGES ) ) )
        {
            // We don't have permission to send messages in this channel, and Discord deprecated general channel, so DM the issuer.
            user.getOrCreatePMChannel().sendMessage( "**Error:** I don't have permission to send messages into " + chan.mention() + "." );
            return "No self permission.";
        }
        try
        {
            LuxBot.DATABASE.prepareStatement( "INSERT INTO `guild_output_channels` (guild_id, channel_id) VALUES('" + chan.getGuild().getLongID() + "', '" + chan.getLongID() + "') ON DUPLICATE KEY UPDATE channel_id='" + chan.getLongID() + "'" ).executeUpdate();
            LuxBot.guildChannels.put( chan.getGuild(), chan );
        } catch ( SQLException e )
        {
            chan.sendMessage( "**Error:** There was an error attempting to set your guild's output channel. (SQL error)" );
            e.printStackTrace();
        }
        chan.sendMessage( "**Success:** This channel " + chan.mention() + " is now the LuxBot output channel." );
        return "Output channel of guild " + chan.getGuild().getLongID() + "/" + chan.getGuild().getName() + " is now " + chan.getLongID() + "/" + chan.getName();
    }
}
