package me.james.luxbot;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;
import me.james.basebot.*;
import me.james.basebot.command.*;
import me.james.luxbot.commands.*;
import net.rithms.riot.api.*;
import net.rithms.riot.api.endpoints.match.dto.*;
import net.rithms.riot.api.endpoints.static_data.constant.Locale;
import net.rithms.riot.api.endpoints.static_data.dto.*;
import net.rithms.riot.api.endpoints.summoner.dto.*;
import net.rithms.riot.constant.*;
import sx.blah.discord.handle.obj.*;

public class LuxBot extends BaseBot
{
    public static final File OUTPUT_TEXT_FILE = new File( "game_format.txt" );
    private static final String STANDARD_PLAYING_TEXT = "LuxBot: Tracking your terrible statistics.";
    private static final long LUXBOT_UPDATE_RATE = 300000L;
    private static final long GLOBAL_STATS_UPDATE_RATE = 86400000L;
    public static LuxBot BOT;
    public static RiotApi RIOT_API;
    public static Connection DATABASE;
    public static ArrayList< TrackSummoner > summoners = new ArrayList<>();
    public static HashMap< IGuild, IChannel > guildChannels = new HashMap<>();
    private static Thread updateThread;
    private static int updateCount;

    public LuxBot()
    {
        super( new File( "bot_token" ) );
    }

    public static Match getLastGame( Summoner s, Platform plat ) throws RiotApiException
    {
        MatchList ml = RIOT_API.getRecentMatchListByAccountId( plat, s.getAccountId() );
        if ( ml == null || ml.getMatches().size() <= 0 )
            return null;
        return RIOT_API.getMatch( plat, ml.getMatches().get( 0 ).getGameId() );
    }

    public static void main( String[] args )
    {
        // Make sure we can access the MySQL database before we even think about setting up the bot and Riot API.
        try
        {
            Class.forName( "com.mysql.jdbc.Driver" );
        } catch ( ClassNotFoundException e )
        {
            // We don't have the bot initialized, meaning we have no logger, so stdout will have to do.
            System.out.println( "Unable to initialize JDBC drivers." );
            e.printStackTrace();
            return;
        }
        if ( !OUTPUT_TEXT_FILE.exists() )
        {
            System.out.println( "Missing required file " + OUTPUT_TEXT_FILE.getPath() + "." );
            return;
        }
        // Depending on where I debug this (or anyone else), this could either be local IP address, public IP address, or loopback (localhost/127.0.0.1)
        try
        {
            DATABASE = DriverManager.getConnection( "jdbc:mysql://" + new String( Files.readAllBytes( Paths.get( "sql_ip" ) ) ) + "/luxbot?autoReconnect=true", "luxbot", new String( Files.readAllBytes( Paths.get( "sql_pw" ) ) ) );
        } catch ( SQLException | IOException e )
        {
            if ( e instanceof IOException )
                System.out.println( "A required file containing database information is missing or not accessible." );
            e.printStackTrace();
            return;
        }
        System.out.println( "MySQL connection for LuxBot established." );
        try
        {
            RIOT_API = new RiotApi( new ApiConfig().setKey( new String( Files.readAllBytes( Paths.get( "riot_token" ) ) ) ) );
        } catch ( IOException e )
        {
            System.out.println( "Unable to initialize Riot API." );
            e.printStackTrace();
            return;
        }
        BOT = new LuxBot();
    }

    public static String formatGame( TrackSummoner s, Match m ) throws RiotApiException, IOException
    {
        String fmt = new String( Files.readAllBytes( OUTPUT_TEXT_FILE.toPath() ) );
        for ( ParticipantIdentity pi : m.getParticipantIdentities() )
            if ( pi.getPlayer() == null )
                return null;
        Participant pstats = m.getParticipantBySummonerId( s.getSummoner().getId() );
        Optional< MapDetails > map = RIOT_API.getDataMaps( s.getPlatform() ).getData().values().stream().filter( md -> md.getMapId() == m.getMapId() ).findFirst();
        int ourTeamKills = 0, theirTeamKills = 0; // The fuck do I name these.
        for ( Participant p : m.getParticipants() ) // Is this the best way to actually get the two teams?
            if ( p.getTeamId() == pstats.getTeamId() )
                ourTeamKills += p.getStats().getKills();
            else
                theirTeamKills += p.getStats().getKills();
        Champion champ = RIOT_API.getDataChampion( s.getPlatform(), pstats.getChampionId(), Locale.EN_US, null );
        return String.format( fmt, s.getSummoner().getName(), ( pstats.getStats().isWin() ? "won" : "lost" ), m.getGameDuration() / 60, m.getGameCreation() % 60, ( map.isPresent() ? map.get().getMapName() : "Unknown" ), champ.getName(), pstats.getStats().getKills(), pstats.getStats().getDeaths(), pstats.getStats().getAssists(), pstats.getStats().getTotalMinionsKilled() + pstats.getStats().getNeutralMinionsKilled(), ourTeamKills, theirTeamKills );
    }

    public static String formatTime( int secondsTime )
    {
        final String fmt = "%02d:%02d:%02d";
        if ( secondsTime <= 0 )
            return "00:00:00";
        int hours = secondsTime / 3600;
        int minutes = secondsTime / 60 - ( hours * 60 );
        int seconds = secondsTime - hours * 3600 - minutes * 60;
        return String.format( fmt, hours, minutes, seconds );
    }

    // TODO: This is fucking retarded. Store the timestamp and game id individually, and calculate what games in 24 hours based on the timestamp, not fucking this dumb shit.
    public static String formatGlobalStats() throws IOException, SQLException
    {
        if ( new File( "global_stats_format.txt" ).exists() )
        {
            String fmt = new String( Files.readAllBytes( Paths.get( "global_stats_format.txt" ) ) );
            ResultSet rs = DATABASE.prepareStatement( String.format( "SELECT * FROM `global_stats` WHERE `timestamp` >= %d AND `timestamp` <= %d", getStartTime(), System.currentTimeMillis() ) ).executeQuery();
            // TODO: Probably a better way to define all these, or preform the calculations.
            int kills = 0, deaths = 0, assists = 0, win = 0, loss = 0, duration = 0, total_damage = 0, magic_damage = 0, physical_damage = 0, true_damage = 0, tower_kills = 0, double_kills = 0, triple_kills = 0, quadra_kills = 0, penta_kills = 0, unreal_kills = 0, gold_earned = 0, minions_killed = 0, wards_placed = 0, wards_killed = 0, top_players = 0, jungle_players = 0, mid_players = 0, bot_players = 0;
            while ( rs.next() )
            {
                String lane = rs.getString( "lane" );
                kills += rs.getInt( "kills" );
                deaths += rs.getInt( "deaths" );
                assists += rs.getInt( "assists" );
                win += rs.getInt( "win" );
                loss += rs.getInt( "loss" );
                duration += rs.getLong( "duration" );
                total_damage += rs.getInt( "total_damage" );
                magic_damage += rs.getInt( "magic_damage" );
                physical_damage += rs.getInt( "physical_damage" );
                true_damage += rs.getInt( "true_damage" );
                tower_kills += rs.getInt( "tower_kills" );
                top_players += ( lane.equalsIgnoreCase( "TOP" ) ? 1 : 0 );
                jungle_players += ( lane.equalsIgnoreCase( "JUNGLE" ) ? 1 : 0 );
                mid_players += ( lane.equalsIgnoreCase( "MIDDLE" ) ? 1 : 0 );
                bot_players += ( lane.equalsIgnoreCase( "BOTTOM" ) ? 1 : 0 );
                double_kills += rs.getInt( "double_kills" );
                triple_kills += rs.getInt( "triple_kills" );
                quadra_kills += rs.getInt( "quadra_kills" );
                penta_kills += rs.getInt( "penta_kills" );
                unreal_kills += rs.getInt( "unreal_kills" );
                gold_earned += rs.getInt( "gold_earned" );
                minions_killed += rs.getInt( "minions_killed" );
                wards_placed += rs.getInt( "wards_placed" );
                wards_killed += rs.getInt( "wards_killed" );
            }
            // TODO: Add win rate calculations.
            return String.format( fmt, summoners.size(), guildChannels.size(), kills, deaths, assists, win, loss, formatTime( duration ), total_damage, magic_damage, physical_damage, true_damage, tower_kills, top_players, jungle_players, mid_players, bot_players, double_kills, triple_kills, quadra_kills, penta_kills, unreal_kills, gold_earned, minions_killed, wards_placed, wards_killed, new SimpleDateFormat( "MM/dd/yyyy hh:mm aa" ).format( new Date( getStartTime() ) ), new SimpleDateFormat( "MM/dd/yyyy hh:mm aa" ).format( new Date( System.currentTimeMillis() ) ) );
        }
        return "If you see this message, blame " + BOT.getBot().getApplicationOwner().mention() + "/" + BOT.getBot().getApplicationOwner().getName() + "#" + BOT.getBot().getApplicationOwner().getDiscriminator() + "; they're retarded and didn't give me a format file to send.";
    }

    public static long getStartTime()
    {
        try
        {
            return Long.parseLong( new String( Files.readAllBytes( Paths.get( "start_time" ) ) ) );
        } catch ( IOException e )
        {
            e.printStackTrace();
        }
        return 0L;
    }

    public static long getLastUpdateTime()
    {
        try
        {
            return Long.parseLong( new String( Files.readAllBytes( Paths.get( "last_update_time" ) ) ) );
        } catch ( IOException e )
        {
            e.printStackTrace();
        }
        return 0L;
    }

    public static void updateGlobalStats( TrackSummoner sum, Match m ) throws RiotApiException
    {
        BOT.getLogger().info( "Updating global stats for match " + m.getGameId() );
        try
        {
            Participant pstats = m.getParticipantBySummonerId( sum.getSummoner().getId() );
            if ( pstats == null ) // Wasn't a drafted game, can't report Summoner stats as we don't know who they are.
                return;
            // TODO: Does this need to be so fucking long?
            DATABASE.prepareStatement( String.format( "INSERT INTO `global_stats` (`timestamp`, `game_id`, `summoner_id`, `kills`, `deaths`, `assists`, `win`, `loss`, `duration`, `total_damage`, `magic_damage`, `physical_damage`, `true_damage`, `tower_kills`, `double_kills`, `triple_kills`, `quadra_kills`, `penta_kills`, `unreal_kills`, `gold_earned`, `minions_killed`, `wards_placed`, `wards_killed`, `lane`) VALUES(%d, %d, %d, %d, %d,%d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %s)", ( m.getGameCreation() ), m.getGameId(), sum.getSummoner().getId(), pstats.getStats().getKills(), pstats.getStats().getDeaths(), pstats.getStats().getAssists(), ( pstats.getStats().isWin() ? 1 : 0 ), ( !pstats.getStats().isWin() ? 1 : 0 ), m.getGameDuration(), pstats.getStats().getTotalDamageDealt(), pstats.getStats().getMagicDamageDealt(), pstats.getStats().getPhysicalDamageDealt(), pstats.getStats().getTrueDamageDealt(), pstats.getStats().getTurretKills(), pstats.getStats().getDoubleKills(), pstats.getStats().getTripleKills(), pstats.getStats().getQuadraKills(), pstats.getStats().getPentaKills(), pstats.getStats().getUnrealKills(), pstats.getStats().getGoldEarned(), pstats.getStats().getTotalMinionsKilled() + pstats.getStats().getNeutralMinionsKilled(), pstats.getStats().getWardsPlaced(), pstats.getStats().getWardsKilled(), pstats.getTimeline().getLane() ) ).execute();
        } catch ( SQLException e )
        {
            e.printStackTrace();
        }
    }

    public static void update()
    {
        updateCount++;
        try
        {
            Files.write( Paths.get( "last_update_time" ), String.valueOf( System.currentTimeMillis() ).getBytes(), StandardOpenOption.CREATE );
        } catch ( IOException e )
        {
            e.printStackTrace();
        }
        BOT.getBot().changePlayingText( "Reporting summoner games..." );
        BOT.getLogger().info( "Beginning to update games..." );
        for ( TrackSummoner sum : summoners )
        {
            try
            {
                Match lg = getLastGame( sum.getSummoner(), sum.getPlatform() );
                if ( lg == null )
                {
                    BOT.getLogger().info( "Last game is NULL (platform " + sum.getPlatform().getName() + ", summoner " + sum.getSummoner().toString() );
                    continue;
                }
                if ( sum.getLastGame().getGameId() != lg.getGameId() )
                {
                    BOT.getLogger().info( "Summoner " + sum.getSummoner().toString() + " has a new last game (old: " + sum.getLastGame().getGameId() + " new " + lg.getGameId() + "), updating all guilds..." );
                    for ( TrackGuild tg : sum.getGuilds() )
                    {
                        String msg = formatGame( sum, lg );

                        if ( msg == null ) // This can be null when the game the user just played wasn't drafted. We still want to track successive games, so we still update everything, but can't send a message as we can't find that player in the game.
                            BOT.getLogger().info( "Not reporting last game above as it was not drafted." );
                        else
                            tg.getOutputChannel().sendMessage( msg );
                        updateGlobalStats( sum, lg );
                        sum.setLastGame( lg );
                        DATABASE.prepareStatement( String.format( "UPDATE `last_games` SET `game_id`=%d WHERE `summoner_id`=%d AND `platform`='%s'", lg.getGameId(), sum.getSummoner().getId(), sum.getPlatform().getName() ) ).executeUpdate();
                    }
                }
            } catch ( RiotApiException | SQLException | IOException e )
            {
                if ( e instanceof IOException )
                    BOT.getLogger().warning( "Unable to read game format file " + OUTPUT_TEXT_FILE.getPath() );
                e.printStackTrace();
            }
        }
        BOT.getLogger().info( "Finished updating." );
        BOT.getBot().changePlayingText( STANDARD_PLAYING_TEXT );
        if ( System.currentTimeMillis() >= getStartTime() + GLOBAL_STATS_UPDATE_RATE )
        {
            BOT.getLogger().info( "24 hours has passed since global stat tracking -- reporting to all guilds." );
            try
            {
                String msg = formatGlobalStats();
                for ( IChannel chan : guildChannels.values() )
                    chan.sendMessage( msg );
                Files.write( Paths.get( "global_stats_" + getStartTime() + ".txt" ), msg.getBytes(), StandardOpenOption.CREATE );
                Files.write( Paths.get( "start_time" ), String.valueOf( System.currentTimeMillis() ).getBytes(), StandardOpenOption.WRITE );
            } catch ( IOException | SQLException e )
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void init()
    {
        Command.registerClass( "!addsummoner", AddSummonerCommand.class );
        Command.registerClass( "!removesummoner", RemoveSummonerCommand.class );
        Command.registerClass( "!setchannel", SetOutputChannelCommand.class );
        Command.registerClass( "!forceupdate", ForceUpdateCommand.class );
        Command.registerClass( "!sendmessage", SendGlobalMessageCommand.class );
        BOT.getBot().changePlayingText( STANDARD_PLAYING_TEXT );
        File f = new File( "start_time" );
        if ( !f.exists() )
            try
            {
                Files.write( f.toPath(), String.valueOf( System.currentTimeMillis() ).getBytes(), StandardOpenOption.CREATE );
            } catch ( IOException e )
            {
                e.printStackTrace();
            }
        getLogger().info( "Getting guild and output channel info..." );
        try
        {
            PreparedStatement ps = DATABASE.prepareStatement( "SELECT * FROM `guild_output_channels`" );
            ResultSet rs = ps.executeQuery();
            while ( rs.next() )
                guildChannels.put( getBot().getGuildByID( rs.getLong( "guild_id" ) ), getBot().getChannelByID( rs.getLong( "channel_id" ) ) );
        } catch ( SQLException e )
        {
            e.printStackTrace();
        }
        getLogger().info( "Getting summoner and guild info..." );
        // TODO: Holy shit. What the actual fuck is this.
        try
        {
            ResultSet rs = DATABASE.prepareStatement( "SELECT * FROM `summoner_guilds`" ).executeQuery();
            while ( rs.next() )
            {
                try
                {
                    Platform plat = Platform.getPlatformByName( rs.getString( "platform" ) );
                    Summoner s = RIOT_API.getSummoner( plat, rs.getLong( "summoner_id" ) );
                    if ( s == null )
                    {
                        getLogger().warning( "Summoner was NULL (platform " + plat.getName() + ", id " + rs.getLong( "summoner_id" ) + ")" );
                        continue;
                    }
                    IGuild g = getBot().getGuildByID( rs.getLong( "guild_id" ) );
                    Optional< TrackSummoner > tsTest = summoners.stream().filter( ts -> ts.getSummoner() == s ).findFirst();
                    if ( tsTest.isPresent() )
                    {
                        tsTest.get().addGuild( new TrackGuild( g, guildChannels.get( g ) ) );
                    } else
                    {
                        TrackSummoner ts = new TrackSummoner( s, plat );
                        ts.addGuild( new TrackGuild( g, guildChannels.get( g ) ) );
                        ResultSet lastGameRs = DATABASE.prepareStatement( String.format( "SELECT * FROM `last_games` WHERE `summoner_id`=%d AND `platform`='%s'", s.getId(), plat.getName() ) ).executeQuery();
                        if ( !lastGameRs.next() )
                        {
                            getLogger().warning( "Last game is not in database (platform " + plat.getName() + ", summoner " + s.toString() + ")" );
                            continue;
                        }
                        Match lg = RIOT_API.getMatch( plat, lastGameRs.getLong( "game_id" ) );
                        if ( lg == null )
                        {
                            getLogger().warning( "Last game is NULL (platform " + plat.getName() + ", summoner " + s.toString() + ")" );
                            continue;
                        }
                        ts.setLastGame( lg );
                        summoners.add( ts );
                    }
                } catch ( RiotApiException e )
                {
                    e.printStackTrace();
                }
            }
        } catch ( SQLException e )
        {
            e.printStackTrace();
        }
        updateThread = new Thread( () ->
        {
            while ( true )
            {
                try
                {
                    Thread.sleep( ( updateCount >= 1 ? LUXBOT_UPDATE_RATE : getLastUpdateTime() % LUXBOT_UPDATE_RATE ) ); // This prevents update misalignment when the program is restarted. (meaning it will always be aligned to the start time)
                } catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }
                getLogger().info( "Update thread is updating..." );
                update();
            }
        } );
        updateThread.start();
        for ( TrackSummoner ts : summoners )
            getLogger().info( "Summoner " + ts.getSummoner().toString() + "'s last game is " + ts.getLastGame().getGameId() );
    }
}
