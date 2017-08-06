package me.james.luxbot.commands;

import me.james.basebot.command.*;
import me.james.luxbot.*;
import sx.blah.discord.handle.obj.*;

public class SendGlobalMessageCommand extends Command
{
    @Override
    public String doCommand( String[] args, IUser user, IChannel chan )
    {
        if ( args.length < 2 )
        {
            chan.sendMessage( "**Usage:** !sendmessage <msg...>" );
            return "Not enough args.";
        }
        String msg = "";
        for ( int i = 1; i < args.length; i++ )
            msg += args[i] + " ";
        msg = msg.trim();
        for ( IChannel msgChan : LuxBot.guildChannels.values() )
            msgChan.sendMessage( msg );

        return null;
    }

    @Override
    public boolean isPrivateMessageRequired()
    {
        return true;
    }

    @Override
    public boolean isBotOwnerSenderRequired()
    {
        return true;
    }
}
