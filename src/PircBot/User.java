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
public class User {

    private String _nick;
    private long lastMessage;
    private boolean isAFK;
    private boolean isOP;
    private boolean isVoice;
    private String _channel;
    private static String[] bots = {"wow_deku_onehand", "lavasbot", "facts_bot", "totally_not_facts_bot", "23forces", "twitchplaysleaderboard", "recordingbot", "twitchnotify", "io_ol7bot"};

    public static boolean isBot(String username) {
        username = username.toLowerCase();
        for (String el : bots) {
            if (el.equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

//    /**
//     * Constructs a User object with a known prefix and nick.
//     *
//     * @param prefix The status of the user, for example, "@".
//     * @param nick The nick of the user.
//     */
//    public User(String prefix, String nick) {
//        _prefix = prefix;
//        _nick = nick;
//        this.lastMessage = System.currentTimeMillis();
//        this.isAFK = false;
//    }
    public User(String name, String channel) {
        _nick = name;
        _channel = channel;
        this.lastMessage = System.currentTimeMillis();
        this.isAFK = false;
        this.isOP = false;
        this.isVoice = false;
    }

    public User(String name, String channel, long lastMessage) {
        _nick = name;
        _channel = channel;
        this.lastMessage = lastMessage;
        this.isAFK = false;
        this.isOP = false;
        this.isVoice = false;
    }

    public User(String name, String channel, long lastMessage, boolean afk) {
        _nick = name;
        _channel = channel;
        this.lastMessage = System.currentTimeMillis();
        this.isAFK = afk;
        this.isOP = false;
        this.isVoice = false;
    }

    public User(String _nick, String _channel, long lastMessage, boolean isAFK, boolean isOP, boolean isVoice) {
        this._nick = _nick;
        this.lastMessage = lastMessage;
        this.isAFK = isAFK;
        this.isOP = isOP;
        this.isVoice = isVoice;
        this._channel = _channel;
    }

    public User(User user, String channel) {
        _nick = user.getNick();
        _channel = channel;
        this.lastMessage = System.currentTimeMillis();
        this.isAFK = false;
        this.isOP = false;
        this.isVoice = false;
    }

    public void changeName(String name) {
        _nick = name;
    }

    public String getChannel() {
        return _channel;
    }

    public void setChannel(String channel) {
        _channel = channel;
    }

    public long getLastMessage() {
        return lastMessage;
    }

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

    public boolean isAFK() {
        if (isBot(this.getNick())) {
            return false;
        }
        if (isAFK == true) {
            return true;
        }
        long currentTime = System.currentTimeMillis();
        isAFK = currentTime - lastMessage > 900000;
        return isAFK;
    }

    public void setAFK(boolean afk) {
        this.isAFK = afk;
    }

    public boolean isOP() {
        return isOP;
    }

    public void setOP(boolean isOP) {
        this.isOP = isOP;
    }

    public boolean isVoice() {
        return isVoice;
    }

    public void setVoice(boolean isVoice) {
        this.isVoice = isVoice;
    }

    @Override
    public String toString() {
        String afk = isAFK ? "(AFK)" : "";
        String op = isOP ? "@" : "";
        String voice = isVoice ? "+" : "";
        return op + "" + voice + "" + _nick + " " + afk + " (" + _channel + ")";
    }

    /**
     * Returns true if the nick represented by this User object is the same as
     * the nick of the User object given as an argument. A case insensitive
     * comparison is made.
     *
     * @param obj
     * @return true if o is a User object with a matching lowercase nick.
     */
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
        if (!Objects.equals(this._channel, other._channel)) {
            return false;
        }
        return true;
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
