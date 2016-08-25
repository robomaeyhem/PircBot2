/* 
 Copyright Paul James Mutton, 2001-2009, http://www.jibble.org/

 This file is part of PircBot.

 This software is dual-licensed, allowing you to choose between the GNU
 General Public License (GPL) and the www.jibble.org Commercial License.
 Since the GPL may be too restrictive for use in a proprietary application,
 a commercial license is also provided. Full license information can be
 found at http://www.jibble.org/licenses/

 */
package PircBot;

import java.util.Objects;

/**
 * This class is used to represent a user on an IRC server. Instances of this
 * class are returned by the getUsers method in the PircBot class.
 * <p>
 * Note that this class no longer implements the Comparable interface for Java
 * 1.1 compatibility reasons.
 *
 * @since 1.0.0
 * @author Paul James Mutton,
 * <a href="http://www.jibble.org/">http://www.jibble.org/</a>
 * @version 1.5.0 (Build time: Mon Dec 14 20:07:17 2009)
 */
public class User implements Comparable {

    private String _nick;
    private long lastMessage;
    private boolean isAFK;
    private boolean isOP;
    private boolean isVoice;
    private String _channel;
    private static String[] bots = {"wow_deku_onehand", "lavasbot", "facts_bot", "totally_not_facts_bot", "23forces", "twitchplaysleaderboard", "recordingbot", "twitchnotify", "io_ol7bot", "tppstatbot", "tppstatsbot", "pikalaxbot", "wowitsbot", "wallbot303", "frunky5", "wow_statsbot_onehand", "tppbankbot", "tppmodbot", "tppinfobot", "kmsbot", "trainertimmybot", "wow_battlebot_onehand", "groudonger"};
    private String previousMessage;
    private String color;
    private boolean subscriber;
    private boolean turbo;
    private String emotes;
    private String msgId;
    private String badges;
    private String systemMsg;
    private String userLogin;
    private String emoteSets;
    private String userType;
    private String messageId;
    private long id;
    private long roomId;
    private long consecutiveMonths;
    private long bits;
    private boolean mod;

    public static boolean isBot(String username) {
        username = username.toLowerCase();
        for (String el : bots) {
            if (el.equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Constructs a new user object
     *
     * @param name Name of the user
     * @param channel Channel user is in
     */
    public User(String name, String channel) {
        this._nick = name;
        this._channel = channel;
        this.lastMessage = System.currentTimeMillis();
        this.isAFK = false;
        this.isOP = false;
        this.isVoice = false;
        this.previousMessage = "";
        this.color = "";
        this.subscriber = false;
        this.mod = false;
        this.turbo = false;
        this.emotes = "";
        this.msgId = "";
        this.badges = "";
        this.systemMsg = "";
        this.userLogin = "";
        this.emoteSets = "";
        this.userType = "";
        this.id = 0;
        this.roomId = 0;
        this.consecutiveMonths = 0;
        this.bits = 0;
        this.messageId = "";
    }

    /**
     * Constructs a new user object
     *
     * @param name Name of the user
     * @param channel Channel user is in
     */
    public User(String name, Channel channel) {
        this(name, channel.getChannelName());
    }

    /**
     * Constructs a new user object
     *
     * @param name Name of the user
     * @param channel Channel user is in
     * @param lastMessage Last Message received time
     */
    public User(String name, String channel, long lastMessage) {
        this._nick = name;
        this._channel = channel;
        this.lastMessage = lastMessage;
        this.isAFK = false;
        this.isOP = false;
        this.isVoice = false;
        this.previousMessage = "";
        this.color = "";
        this.subscriber = false;
        this.mod = false;
        this.turbo = false;
        this.emotes = "";
        this.msgId = "";
        this.badges = "";
        this.systemMsg = "";
        this.userLogin = "";
        this.emoteSets = "";
        this.userType = "";
        this.id = 0;
        this.roomId = 0;
        this.consecutiveMonths = 0;
        this.bits = 0;
        this.messageId = "";
    }

    /**
     * Constructs a new user object
     *
     * @param name Name of the user
     * @param channel Channel user is in
     * @param lastMessage Last Message received time
     * @param afk Is user AFK?
     */
    public User(String name, String channel, long lastMessage, boolean afk) {
        this._nick = name;
        this._channel = channel;
        this.lastMessage = System.currentTimeMillis();
        this.isAFK = afk;
        this.isOP = false;
        this.isVoice = false;
        this.previousMessage = "";
        this.color = "";
        this.subscriber = false;
        this.mod = false;
        this.turbo = false;
        this.emotes = "";
        this.msgId = "";
        this.badges = "";
        this.systemMsg = "";
        this.userLogin = "";
        this.emoteSets = "";
        this.userType = "";
        this.id = 0;
        this.roomId = 0;
        this.consecutiveMonths = 0;
        this.bits = 0;
        this.messageId = "";
    }

    /**
     * Constructs a new user object
     *
     * @param _nick Name of the user
     * @param _channel Channel user is in
     * @param lastMessage Last Message received time
     * @param isAFK Is user AFK?
     * @param isOP Is user OP?
     * @param isVoice Is user voiced?
     */
    public User(String _nick, String _channel, long lastMessage, boolean isAFK, boolean isOP, boolean isVoice) {
        this._nick = _nick;
        this.lastMessage = lastMessage;
        this.isAFK = isAFK;
        this.isOP = isOP;
        this.isVoice = isVoice;
        this._channel = _channel;
        this.previousMessage = "";
        this.color = "";
        this.subscriber = false;
        this.mod = false;
        this.turbo = false;
        this.emotes = "";
        this.msgId = "";
        this.badges = "";
        this.systemMsg = "";
        this.userLogin = "";
        this.emoteSets = "";
        this.userType = "";
        this.id = 0;
        this.roomId = 0;
        this.consecutiveMonths = 0;
        this.bits = 0;
        this.messageId = "";
    }

    /**
     * Constructs a new user object
     *
     * @param _nick Name of the user
     * @param _channel Channel user is in
     * @param lastMessage Last Message received time
     * @param isAFK Is user AFK?
     * @param isOP Is user OP?
     * @param isVoice Is user voiced?
     * @param color Color String of the User
     * @param isSubscriber Is user a Channel Sub?
     * @param isMod Is user a Channel Moderator?
     * @param isTurbo Is user a Turbo user?
     * @param emotes Emote String of the User
     * @param msgId Twitch System Message Id https://github.com/justintv/Twitch-API/blob/master/IRC.md#notice
     * @param userType User Type tag info
     * @param badges Get chat icons such as moderator, turbo, subscriber, bits
     * @param systemMsg Resubscription System Message
     * @param userLogin Username lowercase name for resubscription lines
     * @param emoteSets Get emote sets for USERSTATE lines
     * @param messageId Unique identifier for a message.
     * @param roomId ID of the channel.
     * @param consecutiveMonths number of consecutive months the user has subscribed for in a resub notice.
     * @param bits Amount of Bits user has used.
     */
    public User(String _nick, String _channel, long lastMessage, boolean isAFK, boolean isOP, boolean isVoice, String color, boolean isSubscriber, boolean isMod, boolean isTurbo, String userType, String emotes, String badges, String systemMsg, String userLogin, String messageId, String emoteSets, String msgId, long roomId, long bits, long consecutiveMonths) {
        this._nick = _nick;
        this.lastMessage = lastMessage;
        this.isAFK = isAFK;
        this.isOP = isOP;
        this.isVoice = isVoice;
        this._channel = _channel;
        this.previousMessage = "";
        this.color = color;
        this.subscriber = isSubscriber;
        this.mod = isMod;
        this.turbo = isTurbo;
        this.emotes = emotes;
        this.msgId = msgId;
        this.userType = userType;
        this.id = 0;
        this.roomId = 0;
        this.consecutiveMonths = 0;
        this.bits = 0;
        this.badges = badges;
        this.systemMsg = systemMsg;
        this.userLogin = userLogin;
        this.emoteSets = emoteSets;
        this.messageId = messageId;
    }

    /**
     * Constructs a new user object from an existing user object.
     *
     * @param user User object to use
     * @param channel Channel user is in
     */
    public User(User user, String channel) {
        _nick = user.getNick();
        _channel = channel;
        this.lastMessage = System.currentTimeMillis();
        this.isAFK = user.isAFK;
        this.isOP = user.isOP;
        this.isVoice = user.isVoice;
        this.previousMessage = "";
        this.color = "";
        this.subscriber = false;
        this.mod = false;
        this.turbo = false;
        this.emotes = "";
        this.msgId = "";
        this.userType = "";
        this.id = 0;
        this.roomId = 0;
        this.consecutiveMonths = 0;
        this.bits = 0;
        this.badges = "";
        this.systemMsg = "";
        this.userLogin = "";
        this.emoteSets = "";
        this.messageId = "";
    }

    /**
     * Changes the name of the user
     *
     * @param name New name of the user
     */
    public void changeName(String name) {
        _nick = name;
    }

    /**
     * Returns the current channel of the user
     *
     * @return channel of the user
     */
    public String getChannel() {
        return _channel;
    }

    /**
     * Sets the current channel of the user
     *
     * @param channel New Channel to set
     */
    public void setChannel(String channel) {
        _channel = channel;
    }

    /**
     * Returns the time of the last message by the user
     *
     * @return Last message time of the user
     */
    public long getLastMessage() {
        return lastMessage;
    }

    /**
     * Sets the time of the last message by the user
     *
     * @param lastMessage Last message
     */
    public void setLastMessage(long lastMessage) {
        this.lastMessage = lastMessage;
    }

    /**
     * Returns the nick of the user.
     *
     * @return The user's nick.
     */
    public String getNick() {
        return _nick;
    }

    /**
     * Determines if a user is AFK or not
     *
     * @return True if user is AFK, false otherwise
     */
    public boolean isAFK() {
        if (isBot(this.getNick())) {
            return false;
        }
        if (isAFK) {
            return true;
        }
        long currentTime = System.currentTimeMillis();
        isAFK = currentTime - lastMessage > 900000;
        return isAFK;
    }

    /**
     * Sets if the user is AFK
     *
     * @param afk Is user AFK?
     */
    public void setAFK(boolean afk) {
        this.isAFK = afk;
    }

    /**
     * Checks if the user is a channel operator
     *
     * @return True if opped, false otherwise
     */
    public boolean isOP() {
        return isOP;
    }

    /**
     * Sets if the user is a channel operator
     *
     * @param isOP Is user opped?
     */
    public void setOP(boolean isOP) {
        this.isOP = isOP;
    }

    /**
     * Checks if the user has room voice
     *
     * @return True if room voiced, false otherwise
     */
    public boolean isVoice() {
        return isVoice;
    }

    /**
     * Sets if the user is room voiced
     *
     * @param isVoice Is the user room voiced?
     */
    public void setVoice(boolean isVoice) {
        this.isVoice = isVoice;
    }

    /**
     * Gets the last message sent by this user
     *
     * @return Last Message sent by the user
     */
    public String getPreviousMessage() {
        return previousMessage;
    }

    /**
     * Sets the last message sent by this user
     *
     * @param previousMessage Message
     */
    public void setPreviousMessage(String previousMessage) {
        this.previousMessage = previousMessage;
    }

    /**
     * Get the twitch user-id tag for this user (if tags are enabled)
     *
     * @return user-id
     */
    public long getId() {
        return this.id;
    }

    /**
     * Set the twitch user-id for this user
     *
     * @param id User-id
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Get the twitch room-id tag for this channel (if tags are enabled)
     *
     * @return roomId
     */
    public long getRoomId() {
        return this.roomId;
    }

    /**
     * Set the twitch room-id for this channel
     *
     * @param roomId room-id
     */
    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }


    /**
     * Get the twitch msg-param-months tag an user in a subscription based channel (if tags are enabled)
     *
     * @return consecutiveMonths
     */
    public long getConsecutiveMonths() {
        return this.consecutiveMonths;
    }

    /**
     * Set the twitch msg-param-months tag an user
     *
     * @param consecutiveMonths msg-param-months
     */
    public void setConsecutiveMonths(long consecutiveMonths) {
        this.consecutiveMonths = consecutiveMonths;
    }


    /**
     * Get the twitch bits tag for an user (if tags are enabled)
     *
     * @return bits
     */
    public long getBits() {
        return this.bits;
    }

    /**
     * Set the twitch bits for an user
     *
     * @param bits bits
     */
    public void setBits(long bits) {
        this.bits = bits;
    }


    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getEmotes() {
        return emotes;
    }

    public void setEmotes(String emotes) {
        this.emotes = emotes;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getBadges() {
        return badges;
    }

    public void setBadges(String badges) {
        this.badges = badges;
    }

    public String getSystemMsg() {
        return systemMsg;
    }

    public void setSystemMsg(String systemMsg) {
        this.systemMsg = systemMsg;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public String getEmoteSets() {
        return emoteSets;
    }

    public void setEmoteSets(String emoteSets) {
        this.emoteSets = emoteSets;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public boolean isSubscriber() {
        return subscriber;
    }

    public void setSubscriber(boolean subscriber) {
        this.subscriber = subscriber;
    }

    public boolean isMod() {
        return mod;
    }

    public void setMod(boolean mod) {
        this.mod = mod;
    }

    public boolean isTurbo() {
        return turbo;
    }

    public void setTurbo(boolean turbo) {
        this.turbo = turbo;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    @Override
    public String toString() {
        String afk = isAFK ? "(AFK)" : "";
        String op = isOP ? "@" : "";
        String voice = isVoice ? "+" : "";
        return op + "" + voice + "" + _nick + " " + afk + " (" + _channel + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final User other = (User) obj;
        if (!Objects.equals(this._nick, other._nick)) {
            return false;
        }
        if (this.lastMessage != other.lastMessage) {
            return false;
        }
        if (this.isAFK != other.isAFK) {
            return false;
        }
        return Objects.equals(this._channel, other._channel);
    }

    /**
     * Returns the hash code of this User object.
     *
     * @return the hash code of the User object.
     */
    @Override
    public int hashCode() {
        return _nick.toLowerCase().hashCode();
    }

    /**
     * Returns the result of calling the compareTo method on lowercased nicks.
     * This is useful for sorting lists of User objects.
     *
     * @param o
     * @return the result of calling compareTo on lowercased nicks.
     */
    public int compareTo(Object o) {
        if (o instanceof User) {
            User other = (User) o;
            return other._nick.toLowerCase().compareTo(_nick.toLowerCase());
        }
        return -1;
    }

}
