# KenBot

A locally hosted discord bot based on reactor framework that provides dota profile and last match information

## Usage

### Pre-requisites

[Java version 18+](https://docs.oracle.com/en/java/javase/18/install/installation-jdk-microsoft-windows-platforms.html#GUID-A7E27B90-A28D-4237-9383-A58B416071CA)

### Steps

Follow the instructions at [Creating an App](https://discord.com/developers/docs/getting-started#creating-an-app)
section
of the discord developer docs. By the end, you should have an application registered and a bot configured. Copy the bot
token (only viewable the first time, will have to reset it in case you forget)

DO NOT SHARE THE BOT TOKEN WITH ANYONE as this allows unrestricted access to the bot and perform any action that the bot
is authorized to

Use the OAuth2 section to generate an invite link to your application. Make sure it at least has a scope of _bot_ and a
permission of _Send Messages_. Click on the URL, choose the guild/server you wish the application/bot to work on and
authorize
it. The bot will now join the guild as a member and will reply on behalf of the application

Clone the repository to your local file system

Open a terminal of choice and switch to the repository directory. Run the following command

```
{pathToMavenBinary} spring-boot:run -Dspring-boot.run.jvmArguments="-DBOT_TOKEN={botToken}"
```

where,  
_pathToMavenBinary_ is either ./mvnw or .\mvnw.cmd for linux based and Windows OSes respectively  
_botToken_ is the copied bot token

The bot will now start responding to _register_, _profile_ and _lastmatch_ commands