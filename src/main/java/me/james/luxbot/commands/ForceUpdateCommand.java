package me.james.luxbot.commands;

import me.james.basebot.command.*;
import me.james.luxbot.*;
import sx.blah.discord.handle.obj.*;

public class ForceUpdateCommand extends Command
{
    @Override
    public String doCommand( String[] args, IUser user, IChannel chan )
    {
        LuxBot.BOT.getLogger().info( "Force updating by command..." );
        long startTime = System.currentTimeMillis();
        chan.sendMessage( "Beginning update..." );
        LuxBot.update();
        chan.sendMessage( "Finished! Took " + ( System.currentTimeMillis() - startTime ) + "ms for **" + LuxBot.summoners.size() + "** summoners and **" + LuxBot.guildChannels.size() + "** guilds." );
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
