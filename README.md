# PircBot2
A newer version of PIRC Bot with more modern capabilities.

Credit for the Original PIRC Bot goes to Jibble at http://www.jibble.org/pircbot.php

This takes PIRC bot and makes it a little more modern. It fully gets rid of Vectors, which PIRC Bot relied on and replaces them with more reliable collections from the Collection Framework. I've also gotten rid of many "private final" methods, as well as made some selected methods public non-final.

# Getting Started
Simply place the PircBot2.jar into your project lib, and add 
`import PircBot.*;`
to your imports.

It's then recommended you override the PircBot class with another class, and then initialize this class inside of your main class.

```Java
public class IrcBot extends PircBot{
  private String name;
  public IrcBot(String name){
    this.name = name;
  }
}
public class IrcBotMain{
  private String oAuth = "...";
  public static void main(String[] args){
    IrcBot irc = new IrcBot("TestBot");
    irc.setVerbose(true);
    try{
      irc.connect("irc.chat.twitch.tv", 6667, oAuth);
    }catch(IrcException|IOExcepion|NickAlreadyInUseException ex){
      //logic to log exception
    }
  }
}
```
The above code will implement a very basic bot named "TestBot" that will sit in an IRC Server and do nothing.
## Joining channels
To join a channel, simply call
```Java
  joinChannel("#test");
```
This will make the bot join the #test channel in the server.
## Responding to messages
If you are implementing a Chat Bot and want it to, for example, reply to a command like `!ping`, you'd implemnt it like so:

IrcBot class:
```Java
@Override
public void onMessage(Channel channel, User sender, String message){
  if(message.startsWith("!ping")){
    this.sendMessage(channel,"Pong!");
  }
}
```
The bot will now respond "Pong!" to any "!ping" command it receives in any channel it is connected to.
## Receiving Whispers (on Twitch.TV)
Twitch.tv is a little weird when it comes to their IRC implementation. We've tried to make it as simple and compatable as possible with their IRC servers, including the abilitiy to send and recive whispers. To do this, you MUST call `sendRawLine("CAP REQ :twitch.tv/commands");` in your class. Then, just simply override the `onWhisper()` method to your needs. You can also send whispers with the `sendWhisper()` method.