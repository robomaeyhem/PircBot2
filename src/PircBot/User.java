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
    private String userType;
    private long id;

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
        this.turbo = false;
        this.userType = "";
        this.id = 0;
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
        this.turbo = false;
        this.userType = "";
        this.id = 0;
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
        this.turbo = false;
        this.userType = "";
        this.id = 0;
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
        this.turbo = false;
        this.userType = "";
        this.id = 0;
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
     * @param isTurbo Is user a Turbo user?
     * @param userType User Type tag info
     */
    public User(String _nick, String _channel, long lastMessage, boolean isAFK, boolean isOP, boolean isVoice, String color, boolean isSubscriber, boolean isTurbo, String userType) {
        this._nick = _nick;
        this.lastMessage = lastMessage;
        this.isAFK = isAFK;
        this.isOP = isOP;
        this.isVoice = isVoice;
        this._channel = _channel;
        this.previousMessage = "";
        this.color = color;
        this.subscriber = isSubscriber;
        this.turbo = isTurbo;
        this.userType = userType;
        this.id = 0;
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
        this.turbo = false;
        this.userType = "";
        this.id = 0;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isSubscriber() {
        return subscriber;
    }

    public void setSubscriber(boolean subscriber) {
        this.subscriber = subscriber;
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
