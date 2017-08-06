package me.james.luxbot;

import sx.blah.discord.handle.obj.*;

public class TrackGuild
{
    private IGuild guild;
    private IChannel chan;

    public TrackGuild( IGuild g, IChannel chan )
    {
        this.guild = g;
        this.chan = chan;
    }

    public IGuild getGuild()
    {
        return guild;
    }

    public IChannel getOutputChannel()
    {
        return chan;
    }
}
