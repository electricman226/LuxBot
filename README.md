# LuxBot
A Discord bot that allows game statistic capturing on game completion, into a Discord channel.

# Add LuxBot to your server
Use [this link](https://discordapp.com/oauth2/authorize?client_id=268599094820208640&scope=bot&permissions=0) to invite LuxBot to your server. Once she is invited, read below on how to setup and use LuxBot.

# Using LuxBot
Use `!setchannel` to desginate an output channel. *(this requires the `Manage Server` permission)*

Use `!addsummoner <platform> <summoner name...>` to begin tracking a Summoner's stats. *(this requires the `Manage Channels` permission **OR** `Manage Channel` permission of channel it is sent from)*

# Riot API Faults
Due to certain limitations of the Riot API, only games that are **drafted** can be tracked.

Getting a Summoner's grade is also not possible, as that is kept private.

# Privacy
By adding your own Summoner *(or a Summoner under an account that isn't owned by you)*, you acknowledge that statistics achieved on that Summoner for all drafted games are being **anonymously** tracked, recorded, and saved, being kept inside of a database.
