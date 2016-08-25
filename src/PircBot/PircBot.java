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

import java.io.*;
import java.net.*;
import java.util.*;
import static PircBot.ReplyConstants.RPL_ENDOFNAMES;
import static PircBot.ReplyConstants.RPL_LIST;
import static PircBot.ReplyConstants.RPL_NAMREPLY;
import static PircBot.ReplyConstants.RPL_TOPIC;
import static PircBot.ReplyConstants.RPL_TOPICINFO;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PircBot is a Java framework for writing IRC bots quickly and easily.
 * <p>
 * It provides an event-driven architecture to handle common IRC events, flood
 * protection, DCC support, ident support, and more. The comprehensive logfile
 * format is suitable for use with pisg to generate channel statistics.
 * <p>
 * Methods of the PircBot class can be called to send events to the IRC server
 * that it connects to. For example, calling the sendMessage method will send a
 * message to a channel or user on the IRC server. Multiple servers can be
 * supported using multiple instances of PircBot.
 * <p>
 * To perform an action when the PircBot receives a normal message from the IRC
 * server, you would override the onMessage method defined in the PircBot class.
 * All on<i>XYZ</i> methods in the PircBot class are automatically called when
 * the event <i>XYZ</i> happens, so you would override these if you wish to do
 * something when it does happen.
 * <p>
 * Some event methods, such as onPing, should only really perform a specific
 * function (i.e. respond to a PING from the server). For your convenience, such
 * methods are already correctly implemented in the PircBot and should not
 * normally need to be overridden. Please read the full documentation for each
 * method to see which ones are already implemented by the PircBot class.
 * <p>
 * Please visit the PircBot homepage at
 * <a href="http://www.jibble.org/pircbot.php">http://www.jibble.org/pircbot.php</a>
 * for full revision history, a beginners guide to creating your first PircBot
 * and a list of some existing Java IRC bots and clients that use the PircBot
 * framework.
 *
 * @author Paul James Mutton,
 * <a href="http://www.jibble.org/">http://www.jibble.org/</a>
 * @version 1.5.0 (Build time: Mon Dec 14 20:07:17 2009)
 */
public abstract class PircBot implements ReplyConstants {

    /**
     * The definitive version number of this release of PircBot. (Note: Change
     * this before automatically building releases)
     */
    public static final String VERSION = "1.5.5";

    private static final int OP_ADD = 1;
    private static final int OP_REMOVE = 2;
    private static final int VOICE_ADD = 3;
    private static final int VOICE_REMOVE = 4;

    // Connection stuff.
    private InputThread _inputThread = null;
    private OutputThread _outputThread = null;
    private String _charset = null;
    private InetAddress _inetAddress = null;

    // Details about the last server that we connected to.
    private String _server = null;
    private int _port = -1;
    private String _password = null;

    // Outgoing message stuff.
    private Queue _outQueue = new Queue();
    private long _messageDelay = 1000;

    // A ConcurrentHashMap of channels that points to a selfreferential ConcurrentHashMap of
    // User objects (used to remember which users are in which channels).
    private final ConcurrentHashMap<String, Channel> _channels = new ConcurrentHashMap<>();

    // A ConcurrentHashMap to temporarily store channel topics when we join them
    // until we find out who set that topic.
    private final ConcurrentHashMap<String, String> _topics = new ConcurrentHashMap<>();

    // DccManager to process and handle all DCC events.
    private DccManager _dccManager = new DccManager(this);
    private int[] _dccPorts = null;
    private InetAddress _dccInetAddress = null;

    // Default settings for the PircBot.
    private boolean _autoNickChange = false;
    private boolean _verbose = false;
    private String _name = "PircBot";
    private String _nick = _name;
    private String _login = "PircBot";
    private String _version = "PircBot " + VERSION + " Java IRC Bot - www.jibble.org";
    private String _finger = "You ought to be arrested for fingering a bot!";

    private String _channelPrefixes = "#&+!";

    /**
     * Constructs a PircBot with the default settings. Your own constructors in
     * classes which extend the PircBot abstract class should be responsible for
     * changing the default settings if required.
     */
    public PircBot() {
    }

    /**
     * Attempt to connect to the specified IRC server. The onConnect method is
     * called upon success.
     *
     * @param hostname The hostname of the server to connect to.
     *
     * @throws IOException if it was not possible to connect to the server.
     * @throws IrcException if the server would not let us join it.
     * @throws NickAlreadyInUseException if our nick is already in use on the
     * server.
     */
    public final synchronized void connect(String hostname) throws IOException, IrcException, NickAlreadyInUseException {
        this.connect(hostname, 6667, null);
    }

    /**
     * Attempt to connect to the specified IRC server and port number. The
     * onConnect method is called upon success.
     *
     * @param hostname The hostname of the server to connect to.
     * @param port The port number to connect to on the server.
     *
     * @throws IOException if it was not possible to connect to the server.
     * @throws IrcException if the server would not let us join it.
     * @throws NickAlreadyInUseException if our nick is already in use on the
     * server.
     */
    public final synchronized void connect(String hostname, int port) throws IOException, IrcException, NickAlreadyInUseException {
        this.connect(hostname, port, null);
    }

    /**
     * Attempt to connect to the specified IRC server using the supplied
     * password. The onConnect method is called upon success.
     *
     * @param hostname The hostname of the server to connect to.
     * @param port The port number to connect to on the server.
     * @param password The password to use to join the server.
     *
     * @throws IOException if it was not possible to connect to the server.
     * @throws IrcException if the server would not let us join it.
     * @throws NickAlreadyInUseException if our nick is already in use on the
     * server.
     */
    public final synchronized void connect(String hostname, int port, String password) throws IOException, IrcException, NickAlreadyInUseException {

        _server = hostname;
        _port = port;
        _password = password;

        if (isConnected()) {
            throw new IrcException("The PircBot is already connected to an IRC server.  Disconnect first.");
        }

        // Don't clear the outqueue - there might be something important in it!
        // Clear everything we may have know about channels.
        this.removeAllChannels();

        // Connect to the server.
        Socket socket = new Socket(hostname, port);
        this.log("*** Connected to server.");

        _inetAddress = socket.getLocalAddress();

        InputStreamReader inputStreamReader = null;
        OutputStreamWriter outputStreamWriter = null;
        if (getEncoding() != null) {
            // Assume the specified encoding is valid for this JVM.
            inputStreamReader = new InputStreamReader(socket.getInputStream(), getEncoding());
            outputStreamWriter = new OutputStreamWriter(socket.getOutputStream(), getEncoding());
        } else {
            // Otherwise, just use the JVM's default encoding.
            inputStreamReader = new InputStreamReader(socket.getInputStream());
            outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
        }

        BufferedReader breader = new BufferedReader(inputStreamReader);
        BufferedWriter bwriter = new BufferedWriter(outputStreamWriter);

        // Attempt to join the server.
        if (password != null && !password.equals("")) {
            OutputThread.sendRawLine(this, bwriter, "PASS " + password);
        }
        String nick = this.getName();
        OutputThread.sendRawLine(this, bwriter, "NICK " + nick);
        OutputThread.sendRawLine(this, bwriter, "USER " + this.getLogin() + " 8 * :" + this.getVersion());

        _inputThread = new InputThread(this, socket, breader, bwriter);

        // Read stuff back from the server to see if we connected.
        String line = null;
        int tries = 1;
        while ((line = breader.readLine()) != null) {

            this.handleLine(line);

            int firstSpace = line.indexOf(" ");
            int secondSpace = line.indexOf(" ", firstSpace + 1);
            if (secondSpace >= 0) {
                String code = line.substring(firstSpace + 1, secondSpace);

                if (code.equals("004")) {
                    // We're connected to the server.
                    break;
                } else if (code.equals("433")) {
                    if (_autoNickChange) {
                        tries++;
                        nick = getName() + tries;
                        OutputThread.sendRawLine(this, bwriter, "NICK " + nick);
                    } else {
                        socket.close();
                        _inputThread = null;
                        throw new NickAlreadyInUseException(line);
                    }
                } else if (code.equals("439")) {
                    // No action required.
                } else if (code.startsWith("5") || code.startsWith("4")) {
                    socket.close();
                    _inputThread = null;
                    throw new IrcException("Could not log into the IRC server: " + line);
                }
            }
            this.setNick(nick);

        }

        this.log("*** Logged onto server.");

        // This makes the socket timeout on read operations after 5 minutes.
        // Maybe in some future version I will let the user change this at runtime.
        socket.setSoTimeout(5 * 60 * 1000);

        // Now start the InputThread to read all other lines from the server.
        _inputThread.start();

        // Now start the outputThread that will be used to send all messages.
        if (_outputThread == null) {
            _outputThread = new OutputThread(this, _outQueue);
            _outputThread.start();
        }

        this.onConnect();

    }

    /**
     * Checks the output queue for a specified command. This method does not
     * automatically append IRC Commands, so if you want to search for a
     * specific message, make sure the input contains "PRIVMSG " and then the
     * channel name.
     *
     * @param input String to search for
     * @return True if found, false otherwise.
     */
    public synchronized final boolean checkQueue(String input) {
        if (_outputThread == null) {
            return false;
        }
        return _outputThread.checkQueue(input);
    }

    /**
     * Reconnects to the IRC server that we were previously connected to. If
     * necessary, the appropriate port number and password will be used. This
     * method will throw an IrcException if we have never connected to an IRC
     * server previously.
     *
     * @since PircBot 0.9.9
     *
     * @throws IOException if it was not possible to connect to the server.
     * @throws IrcException if the server would not let us join it.
     * @throws NickAlreadyInUseException if our nick is already in use on the
     * server.
     */
    public final synchronized void reconnect() throws IOException, IrcException, NickAlreadyInUseException {
        if (getServer() == null) {
            throw new IrcException("Cannot reconnect to an IRC server because we were never connected to one previously!");
        }
        connect(getServer(), getPort(), getPassword());
    }

    /**
     * This method disconnects from the server cleanly by calling the
     * quitServer() method. Providing the PircBot was connected to an IRC
     * server, the onDisconnect() will be called as soon as the disconnection is
     * made by the server.
     *
     * @see #quitServer() quitServer
     * @see #quitServer(String) quitServer
     */
    public final synchronized void disconnect() {
        this.quitServer();
    }

    /**
     * When you connect to a server and your nick is already in use and this is
     * set to true, a new nick will be automatically chosen. This is done by
     * adding numbers to the end of the nick until an available nick is found.
     *
     * @param autoNickChange Set to true if you want automatic nick changes
     * during connection.
     */
    public void setAutoNickChange(boolean autoNickChange) {
        _autoNickChange = autoNickChange;
    }

    /**
     * Starts an ident server (Identification Protocol Server, RFC 1413).
     * <p>
     * Most IRC servers attempt to contact the ident server on connecting hosts
     * in order to determine the user's identity. A few IRC servers will not
     * allow you to connect unless this information is provided.
     * <p>
     * So when a PircBot is run on a machine that does not run an ident server,
     * it may be necessary to call this method to start one up.
     * <p>
     * Calling this method starts up an ident server which will respond with the
     * login provided by calling getLogin() and then shut down immediately. It
     * will also be shut down if it has not been contacted within 60 seconds of
     * creation.
     * <p>
     * If you require an ident response, then the correct procedure is to start
     * the ident server and then connect to the IRC server. The IRC server may
     * then contact the ident server to get the information it needs.
     * <p>
     * The ident server will fail to start if there is already an ident server
     * running on port 113, or if you are running as an unprivileged user who is
     * unable to create a server socket on that port number.
     * <p>
     * If it is essential for you to use an ident server when connecting to an
     * IRC server, then make sure that port 113 on your machine is visible to
     * the IRC server so that it may contact the ident server.
     *
     * @since PircBot 0.9c
     */
    public final void startIdentServer() {
        new IdentServer(this, getLogin());
    }

    /**
     * Joins a channel.
     *
     * @param channel The name of the channel to join (eg "#cs").
     * @return Channel object created during this process.
     */
    public Channel joinChannel(String channel) {
        this.sendRawLine("JOIN " + channel);
        Channel chan = new Channel(channel, this.getServer());
        this._channels.put(channel, chan);
        return chan;
    }

    /**
     * Joins a channel with a key.
     *
     * @param channel The name of the channel to join (eg "#cs").
     * @param key The key that will be used to join the channel.
     * @return Channel object created during this process.
     */
    public Channel joinChannel(String channel, String key) {
        return this.joinChannel(channel + " " + key);
    }

    /**
     * Parts a channel.
     *
     * @param channel The name of the channel to leave.
     */
    public void partChannel(String channel) {
        this.sendRawLine("PART " + channel);
        this._channels.remove(channel);
    }

    /**
     * Parts a channel, giving a reason.
     *
     * @param channel The name of the channel to leave.
     * @param reason The reason for parting the channel.
     */
    public void partChannel(String channel, String reason) {
        this.sendRawLine("PART " + channel + " :" + reason);
        this._channels.remove(channel);
    }

    /**
     * Quits from the IRC server. Providing we are actually connected to an IRC
     * server, the onDisconnect() method will be called as soon as the IRC
     * server disconnects us.
     */
    public final void quitServer() {
        this.quitServer("");
        this._channels.clear();
    }

    /**
     * Quits from the IRC server with a reason. Providing we are actually
     * connected to an IRC server, the onDisconnect() method will be called as
     * soon as the IRC server disconnects us.
     *
     * @param reason The reason for quitting the server.
     */
    public final void quitServer(String reason) {
        this.sendRawLine("QUIT :" + reason);
        this._channels.clear();
    }

    /**
     * Sends a raw line to the IRC server as soon as possible, bypassing the
     * outgoing message queue.
     *
     * @param line The raw line to send to the IRC server.
     */
    public synchronized void sendRawLine(String line) {
        if (isConnected()) {
            _inputThread.sendRawLine(line);
        }
    }

    /**
     * Sends a raw line through the outgoing message queue.
     *
     * @param line The raw line to send to the IRC server.
     */
    public final synchronized void sendRawLineViaQueue(String line) {
        if (line == null) {
            throw new NullPointerException("Cannot send null messages to server");
        }
        if (isConnected()) {
            _outQueue.add(line);
        }
    }

    /**
     * Sends a message to a channel or a private message to a user. These
     * messages are added to the outgoing message queue and sent at the earliest
     * possible opportunity.
     * <p>
     * Some examples: -
     * <pre>    // Send the message "Hello!" to the channel #cs.
     *    sendMessage("#cs", "Hello!");
     *
     *    // Send a private message to Paul that says "Hi".
     *    sendMessage("Paul", "Hi");</pre>
     *
     * You may optionally apply colours, boldness, underlining, etc to the
     * message by using the <code>Colors</code> class.
     *
     * @param target The name of the channel or user nick to send to.
     * @param message The message to send.
     *
     * @see Colors
     */
    public void sendMessage(Channel target, String message) {
        sendMessage(target.getChannelName(), message);
    }

    /**
     * Sends a message to a channel or a private message to a user. These
     * messages are added to the outgoing message queue and sent at the earliest
     * possible opportunity.
     * <p>
     * Some examples: -
     * <pre>    // Send the message "Hello!" to the channel #cs.
     *    sendMessage("#cs", "Hello!");
     *
     *    // Send a private message to Paul that says "Hi".
     *    sendMessage("Paul", "Hi");</pre>
     *
     * You may optionally apply colours, boldness, underlining, etc to the
     * message by using the <code>Colors</code> class.
     *
     * @param target The name of the channel or user nick to send to.
     * @param message The message to send.
     *
     * @see Colors
     */
    public void sendMessage(String target, String message) {
        _outQueue.add("PRIVMSG " + target + " :" + message);
    }

    /**
     * Sends a whisper to the server. This is used mainly for Twitch TV. In
     * order to use this, you must send <code>CAP REQ :twitch.tv/commands</code>
     * to the server, or else WHISPERs cannot be sent or received to the bot.
     *
     * @param target Target to send the Whisper to
     * @param message Message to send to the target.
     */
    public void sendWhisper(User target, String message) {
        sendWhisper(target.getNick(), message);
    }

    /**
     * Sends a whisper to the server. This is used mainly for Twitch TV. In
     * order to use this, you must send <code>CAP REQ :twitch.tv/commands</code>
     * to the server, or else WHISPERs cannot be sent or received to the bot.
     *
     * @param target Target to send the Whisper to
     * @param message Message to send to the target.
     */
    public void sendWhisper(String target, String message) {
        String channel = "#jtv";
        _outQueue.add("PRIVMSG " + channel + " :/w " + target + " " + message);
    }

    /**
     * Sends an action to the channel or to a user.
     *
     * @param target The name of the channel or user nick to send to.
     * @param action The action to send.
     *
     * @see Colors
     */
    public void sendAction(String target, String action) {
        sendCTCPCommand(target, "ACTION " + action);
    }

    /**
     * Sends a notice to the channel or to a user.
     *
     * @param target The name of the channel or user nick to send to.
     * @param notice The notice to send.
     */
    public final void sendNotice(String target, String notice) {
        _outQueue.add("NOTICE " + target + " :" + notice);
    }

    /**
     * Sends a CTCP command to a channel or user. (Client to client protocol).
     * Examples of such commands are "PING [number]", "FINGER", "VERSION", etc.
     * For example, if you wish to request the version of a user called "Dave",
     * then you would call <code>sendCTCPCommand("Dave", "VERSION");</code>. The
     * type of response to such commands is largely dependant on the target
     * client software.
     *
     * @since PircBot 0.9.5
     *
     * @param target The name of the channel or user to send the CTCP message
     * to.
     * @param command The CTCP command to send.
     */
    public final void sendCTCPCommand(String target, String command) {
        _outQueue.add("PRIVMSG " + target + " :\u0001" + command + "\u0001");
    }

    /**
     * Attempt to change the current nick (nickname) of the bot when it is
     * connected to an IRC server. After confirmation of a successful nick
     * change, the getNick method will return the new nick.
     *
     * @param newNick The new nick to use.
     */
    public final void changeNick(String newNick) {
        this.sendRawLine("NICK " + newNick);
    }

    /**
     * Identify the bot with NickServ, supplying the appropriate password. Some
     * IRC Networks (such as freenode) require users to <i>register</i> and
     * <i>identify</i> with NickServ before they are able to send private
     * messages to other users, thus reducing the amount of spam. If you are
     * using an IRC network where this kind of policy is enforced, you will need
     * to make your bot <i>identify</i> itself to NickServ before you can send
     * private messages. Assuming you have already registered your bot's nick
     * with NickServ, this method can be used to <i>identify</i> with the
     * supplied password. It usually makes sense to identify with NickServ
     * immediately after connecting to a server.
     * <p>
     * This method issues a raw NICKSERV command to the server, and is therefore
     * safer than the alternative approach of sending a private message to
     * NickServ. The latter approach is considered dangerous, as it may cause
     * you to inadvertently transmit your password to an untrusted party if you
     * connect to a network which does not run a NickServ service and where the
     * untrusted party has assumed the nick "NickServ". However, if your IRC
     * network is only compatible with the private message approach, you may
     * typically identify like so:
     * <pre>sendMessage("NickServ", "identify PASSWORD");</pre>
     *
     * @param password The password which will be used to identify with
     * NickServ.
     */
    public final void identify(String password) {
        this.sendRawLine("NICKSERV IDENTIFY " + password);
    }

    /**
     * Set the mode of a channel. This method attempts to set the mode of a
     * channel. This may require the bot to have operator status on the channel.
     * For example, if the bot has operator status, we can grant operator status
     * to "Dave" on the #cs channel by calling setMode("#cs", "+o Dave"); An
     * alternative way of doing this would be to use the op method.
     *
     * @param channel The channel on which to perform the mode change.
     * @param mode The new mode to apply to the channel. This may include zero
     * or more arguments if necessary.
     *
     * @see #op(String,String) op
     */
    public final void setMode(String channel, String mode) {
        this.sendRawLine("MODE " + channel + " " + mode);
    }

    /**
     * Sends an invitation to join a channel. Some channels can be marked as
     * "invite-only", so it may be useful to allow a bot to invite people into
     * it.
     *
     * @param nick The nick of the user to invite
     * @param channel The channel you are inviting the user to join.
     *
     */
    public final void sendInvite(String nick, String channel) {
        this.sendRawLine("INVITE " + nick + " :" + channel);
    }

    /**
     * Bans a user from a channel. An example of a valid hostmask is
     * "*!*compu@*.18hp.net". This may be used in conjunction with the kick
     * method to permanently remove a user from a channel. Successful use of
     * this method may require the bot to have operator status itself.
     *
     * @param channel The channel to ban the user from.
     * @param hostmask A hostmask representing the user we're banning.
     */
    public final void ban(String channel, String hostmask) {
        this.sendRawLine("MODE " + channel + " +b " + hostmask);
    }

    /**
     * Unbans a user from a channel. An example of a valid hostmask is
     * "*!*compu@*.18hp.net". Successful use of this method may require the bot
     * to have operator status itself.
     *
     * @param channel The channel to unban the user from.
     * @param hostmask A hostmask representing the user we're unbanning.
     */
    public final void unBan(String channel, String hostmask) {
        this.sendRawLine("MODE " + channel + " -b " + hostmask);
    }

    /**
     * Grants operator privilidges to a user on a channel. Successful use of
     * this method may require the bot to have operator status itself.
     *
     * @param channel The channel we're opping the user on.
     * @param nick The nick of the user we are opping.
     */
    public final void op(String channel, String nick) {
        this.setMode(channel, "+o " + nick);
    }

    /**
     * Removes operator privilidges from a user on a channel. Successful use of
     * this method may require the bot to have operator status itself.
     *
     * @param channel The channel we're deopping the user on.
     * @param nick The nick of the user we are deopping.
     */
    public final void deOp(String channel, String nick) {
        this.setMode(channel, "-o " + nick);
    }

    /**
     * Grants voice privilidges to a user on a channel. Successful use of this
     * method may require the bot to have operator status itself.
     *
     * @param channel The channel we're voicing the user on.
     * @param nick The nick of the user we are voicing.
     */
    public final void voice(String channel, String nick) {
        this.setMode(channel, "+v " + nick);
    }

    /**
     * Removes voice privilidges from a user on a channel. Successful use of
     * this method may require the bot to have operator status itself.
     *
     * @param channel The channel we're devoicing the user on.
     * @param nick The nick of the user we are devoicing.
     */
    public final void deVoice(String channel, String nick) {
        this.setMode(channel, "-v " + nick);
    }

    /**
     * Set the topic for a channel. This method attempts to set the topic of a
     * channel. This may require the bot to have operator status if the topic is
     * protected.
     *
     * @param channel The channel on which to perform the mode change.
     * @param topic The new topic for the channel.
     *
     */
    public final void setTopic(String channel, String topic) {
        this.sendRawLine("TOPIC " + channel + " :" + topic);
    }

    /**
     * Kicks a user from a channel. This method attempts to kick a user from a
     * channel and may require the bot to have operator status in the channel.
     *
     * @param channel The channel to kick the user from.
     * @param nick The nick of the user to kick.
     */
    public final void kick(String channel, String nick) {
        this.kick(channel, nick, "");
    }

    /**
     * Kicks a user from a channel, giving a reason. This method attempts to
     * kick a user from a channel and may require the bot to have operator
     * status in the channel.
     *
     * @param channel The channel to kick the user from.
     * @param nick The nick of the user to kick.
     * @param reason A description of the reason for kicking a user.
     */
    public final void kick(String channel, String nick, String reason) {
        this.sendRawLine("KICK " + channel + " " + nick + " :" + reason);
    }

    /**
     * Issues a request for a list of all channels on the IRC server. When the
     * PircBot receives information for each channel, it will call the
     * onChannelInfo method, which you will need to override if you want it to
     * do anything useful.
     */
    public final void listChannels() {
        this.listChannels(null);
    }

    /**
     * Issues a request for a list of all channels on the IRC server. When the
     * PircBot receives information for each channel, it will call the
     * onChannelInfo method, which you will need to override if you want it to
     * do anything useful.
     * <p>
     * Some IRC servers support certain parameters for LIST requests. One
     * example is a parameter of "greater than 10" to list only those channels
     * that have more than 10 users in them. Whether these parameters are
     * supported or not will depend on the IRC server software.
     *
     * @param parameters The parameters to supply when requesting the list.
     */
    public final void listChannels(String parameters) {
        if (parameters == null) {
            this.sendRawLine("LIST");
        } else {
            this.sendRawLine("LIST " + parameters);
        }
    }

    /**
     * Sends a file to another user. Resuming is supported. The other user must
     * be able to connect directly to your bot to be able to receive the file.
     * <p>
     * You may throttle the speed of this file transfer by calling the
     * setPacketDelay method on the DccFileTransfer that is returned.
     * <p>
     * This method may not be overridden.
     *
     * @since 0.9c
     *
     * @param file The file to send.
     * @param nick The user to whom the file is to be sent.
     * @param timeout The number of milliseconds to wait for the recipient to
     * acccept the file (we recommend about 120000).
     *
     * @return The DccFileTransfer that can be used to monitor this transfer.
     *
     * @see DccFileTransfer
     *
     */
    public final DccFileTransfer dccSendFile(File file, String nick, int timeout) {
        DccFileTransfer transfer = new DccFileTransfer(this, _dccManager, file, nick, timeout);
        transfer.doSend(true);
        return transfer;
    }

    /**
     * Attempts to establish a DCC CHAT session with a client. This method
     * issues the connection request to the client and then waits for the client
     * to respond. If the connection is successfully made, then a DccChat object
     * is returned by this method. If the connection is not made within the time
     * limit specified by the timeout value, then null is returned.
     * <p>
     * It is <b>strongly recommended</b> that you call this method within a new
     * Thread, as it may take a long time to return.
     * <p>
     * This method may not be overridden.
     *
     * @since PircBot 0.9.8
     *
     * @param nick The nick of the user we are trying to establish a chat with.
     * @param timeout The number of milliseconds to wait for the recipient to
     * accept the chat connection (we recommend about 120000).
     *
     * @return a DccChat object that can be used to send and recieve lines of
     * text. Returns <b>null</b> if the connection could not be made.
     *
     * @see DccChat
     */
    public final DccChat dccSendChatRequest(String nick, int timeout) {
        DccChat chat = null;
        try {
            ServerSocket ss = null;

            int[] ports = getDccPorts();
            if (ports == null) {
                // Use any free port.
                ss = new ServerSocket(0);
            } else {
                for (int i = 0; i < ports.length; i++) {
                    try {
                        ss = new ServerSocket(ports[i]);
                        // Found a port number we could use.
                        break;
                    } catch (Exception e) {
                        // Do nothing; go round and try another port.
                    }
                }
                if (ss == null) {
                    // No ports could be used.
                    throw new IOException("All ports returned by getDccPorts() are in use.");
                }
            }

            ss.setSoTimeout(timeout);
            int port = ss.getLocalPort();

            InetAddress inetAddress = getDccInetAddress();
            if (inetAddress == null) {
                inetAddress = getInetAddress();
            }
            byte[] ip = inetAddress.getAddress();
            long ipNum = ipToLong(ip);

            sendCTCPCommand(nick, "DCC CHAT chat " + ipNum + " " + port);

            // The client may now connect to us to chat.
            Socket socket = ss.accept();

            // Close the server socket now that we've finished with it.
            ss.close();

            chat = new DccChat(this, nick, socket);
        } catch (Exception e) {
            // Do nothing.
        }
        return chat;
    }

    /**
     * Adds a line to the log. This log is currently output to the standard
     * output and is in the correct format for use by tools such as pisg, the
     * Perl IRC Statistics Generator. You may override this method if you wish
     * to do something else with log entries. Each line in the log begins with a
     * number which represents the logging time (as the number of milliseconds
     * since the epoch). This timestamp and the following log entry are
     * separated by a single space character, " ". Outgoing messages are
     * distinguishable by a log entry that has three greater than symbols
     * immediately following the space character after the timestamp. DCC events
     * use "+++" and warnings about unhandled Exceptions and Errors use "###".
     * <p>
     * This implementation of the method will only cause log entries to be
     * output if the PircBot has had its verbose mode turned on by calling
     * setVerbose(true);
     *
     * @param line The line to add to the log.
     */
    public void log(String line) {
        if (_verbose) {
            System.out.println(System.currentTimeMillis() + " " + line);
        }
    }

    /**
     * This method handles events when any line of text arrives from the server,
     * then calling the appropriate method in the PircBot. This method is
     * protected and only called by the InputThread for this instance.
     * <p>
     * This method may not be overridden!
     *
     * @param line The raw line of text from the server.
     */
    protected void handleLine(String line) {
        this.log(line);

        // Check for server pings.
        if (line.startsWith("PING ")) {
            // Respond to the ping and return immediately.
            this.onServerPing(line.substring(5));
            return;
        }

        String sourceNick = "";
        String sourceLogin = "";
        String sourceHostname = "";
        boolean containsIRC3 = false;
        String ircTags = "";

        StringTokenizer tokenizer = new StringTokenizer(line);
        String senderInfo = tokenizer.nextToken();
        //twitch tags fix
        if (senderInfo.startsWith("@")) {
            containsIRC3 = true;
            senderInfo = tokenizer.nextToken();
            ircTags = line.split(" :", 2)[0].substring(1);
            line = line.split(" :", 2)[1];
        }
        String command = tokenizer.nextToken();
        String target = null;
        Channel channel = null;

        int exclamation = senderInfo.indexOf("!");
        int at = senderInfo.indexOf("@");
        if (senderInfo.startsWith(":")) {
            if (exclamation > 0 && at > 0 && exclamation < at) {
                sourceNick = senderInfo.substring(1, exclamation);
                sourceLogin = senderInfo.substring(exclamation + 1, at);
                sourceHostname = senderInfo.substring(at + 1);
            } else if (tokenizer.hasMoreTokens()) {
                String token = command;

                int code = -1;
                try {
                    code = Integer.parseInt(token);
                } catch (NumberFormatException e) {
                    // Keep the existing value.
                }

                if (code != -1) {
                    String errorStr = token;
                    String response = line.substring(line.indexOf(errorStr, senderInfo.length()) + 4, line.length());
                    this.processServerResponse(code, response);
                    // Return from the method.
                    return;
                } else {
                    // This is not a server response.
                    // It must be a nick without login and hostname.
                    // (or maybe a NOTICE or suchlike from the server)
                    sourceNick = senderInfo;
                    target = token;
                }
            } else {
                // We don't know what this line means.
                this.onUnknown(line);
                // Return from the method;
                return;
            }
        }
        command = command.toUpperCase();
        if (sourceNick.startsWith(":")) {
            sourceNick = sourceNick.substring(1);
        }
        if (target == null) {
            target = tokenizer.nextToken();
        }
        if (target.startsWith(":")) {
            target = target.substring(1);
        }
        if (target.contains("ROOMSTATE") && line.contains("#")) {
            channel = _channels.get("#" + line.split("#", 2)[1]); //topkek
        }
        if (target.contains("USERSTATE") && line.contains("#")) {
            channel = _channels.get("#" + line.split("#", 2)[1]); //kekpot
        }
        if (target.contains("USERNOTICE") && line.contains("#")) {
            channel = _channels.get("#" + line.split("#", 2)[1]); //Highest kek
        }
        if (target.contains("GLOBALUSERSTATE")) {
            // Does GLOBALUSERSTATE still work? Twitch API documentation is really bad, and I can't see it working on my end. https://github.com/justintv/Twitch-API/blob/master/IRC.md#globaluserstate
            // channel = _channels.get("#" + line.split("#", 2)[1]);
        }
        if (target.startsWith("#")) {
            channel = _channels.get(target);
        }
        User user;
        if (channel == null) {
            user = new User(sourceNick, target, System.currentTimeMillis());
        } else if (channel.containsUser(sourceNick)) {
            user = channel.getUser(sourceNick);
        } else {
            user = new User(sourceNick, channel);
        }
        user.setLastMessage(System.currentTimeMillis());
        HashMap<String, String> tags = new HashMap<>();
        if (containsIRC3) {
            switch (command) {
                case "NOTICE":
                    break;
                case "ROOMSTATE":
                    for (String tag : ircTags.split(";")) {
                        String[] kv = tag.split("=");
                        String key = kv[0];
                        String value;
                        if (kv.length == 1) {
                            value = "";
                        } else {
                            value = kv[1];
                        }
                        tags.put(key, value);
                        channel.setBroadcasterLanguage(tags.get("broadcaster-lang"));
                        boolean r9k;
                        try {
                            r9k = tags.get("r9k").equals("1");
                        } catch (Exception ex) {
                            r9k = false;
                        }
                        channel.setR9k(r9k);
                        int slow;
                        try {
                            slow = Integer.parseInt(tags.get("slow"));
                        } catch (Exception ex) {
                            slow = 0;
                        }
                        channel.setSlow(slow);
                        boolean subs;
                        try {
                            subs = tags.get("subs-only").equals("1");
                        } catch (Exception ex) {
                            subs = false;
                        }
                        channel.setSubsOnly(subs);
                        boolean emoteOnly;
                        try {
                            emoteOnly = tags.get("emote-only").equals("1");
                        } catch (Exception ex) {
                            emoteOnly = false;
                        }
                        channel.setEmoteOnly(emoteOnly);
                    }
                case "USERSTATE":
                    for (String tag : ircTags.split(";")) {
                        String[] kv = tag.split("=");
                        String key = kv[0];
                        String value;
                        if (kv.length == 1) {
                            value = "";
                        } else {
                            value = kv[1];
                        }
                        tags.put(key, value);
                        if (key.equalsIgnoreCase("display-name")) {
                            user.changeName(value);
                        } else if (key.equalsIgnoreCase("color")) {
                            user.setColor(value);
                        } else if (key.equalsIgnoreCase("subscriber")) {
                            user.setSubscriber(value.equals("1"));
                        } else if (key.equalsIgnoreCase("turbo")) {
                            user.setTurbo(value.equals("1"));
                        } else if (key.equalsIgnoreCase("mod")) {
                            user.setMod(value.equals("1"));
                        } else if (key.equalsIgnoreCase("user-type")) {
                            user.setUserType(value);
                            this.onUserState(user, channel);
                        } else if (key.equalsIgnoreCase("emote-sets")) {
                            user.setEmoteSets(value);
                        } else if (key.equalsIgnoreCase("badges")) {
                            user.setBadges(value);
                        }
                    }
                    break;
                case "GLOBALUSERSTATE":
                    for (String tag : ircTags.split(";")) {
                        String[] kv = tag.split("=");
                        String key = kv[0];
                        String value;
                        if (kv.length == 1) {
                            value = "";
                        } else {
                            value = kv[1];
                        }
                        tags.put(key, value);
                        if (key.equalsIgnoreCase("display-name")) {
                            user.changeName(value);
                        } else if (key.equalsIgnoreCase("color")) {
                            user.setColor(value);
                        } else if (key.equalsIgnoreCase("subscriber")) {
                            user.setSubscriber(value.equals("1"));
                        } else if (key.equalsIgnoreCase("turbo")) {
                            user.setTurbo(value.equals("1"));
                        } else if (key.equalsIgnoreCase("mod")) {
                            user.setMod(value.equals("1"));
                        } else if (key.equalsIgnoreCase("user-type")) {
                            user.setUserType(value);
                            this.onGlobalUserState(user);
                        } else if (key.equalsIgnoreCase("emote-sets")) {
                            user.setEmoteSets(value);
                        } else if (key.equalsIgnoreCase("badges")) {
                            user.setBadges(value);
                        }
                    }
                    break;
                case "USERNOTICE":
                    for (String tag : ircTags.split(";")) {
                        String[] kv = tag.split("=");
                        String key = kv[0];
                        String value;
                        if (kv.length == 1) {
                            value = "";
                        } else if (key.equalsIgnoreCase("system-msg")) {
                            value = kv[1].replace("\\s", " ");
                        } else {
                            value = kv[1];
                        }
                        tags.put(key, value);
                        if (key.equalsIgnoreCase("display-name")) {
                            user.changeName(value);
                        } else if (key.equalsIgnoreCase("color")) {
                            user.setColor(value);
                        } else if (key.equalsIgnoreCase("msg-id")) {
                            user.setMsgId(value);
                        } else if (key.equalsIgnoreCase("emotes")) {
                            user.setEmotes(value);
                        } else if (key.equalsIgnoreCase("msg-param-months")) {
                            user.setConsecutiveMonths(Long.parseLong(value));
                        } else if (key.equalsIgnoreCase("room-id")) {
                            user.setRoomId(Long.parseLong(value));
                        } else if (key.equalsIgnoreCase("user-id")) {
                            user.setId(Long.parseLong(value));
                        } else if (key.equalsIgnoreCase("system-msg")) {
                            user.setSystemMsg(value);
                        } else if (key.equalsIgnoreCase("login")) {
                            user.setUserLogin(value);
                        } else if (key.equalsIgnoreCase("subscriber")) {
                            user.setSubscriber(value.equals("1"));
                        } else if (key.equalsIgnoreCase("turbo")) {
                            user.setTurbo(value.equals("1"));
                        } else if (key.equalsIgnoreCase("mod")) {
                            user.setMod(value.equals("1"));
                        } else if (key.equalsIgnoreCase("badges")) {
                            user.setBadges(value);
                        } else if (key.equalsIgnoreCase("user-type")) {
                            String resubMessage = line.split(channel + " ", 2)[1];
                            user.setUserType(value);
                            this.onUserNotice(channel, user, resubMessage);
                        }
                    }
                    break;
                case "CLEARCHAT":
                    for (String tag : ircTags.split(";")) {
                        String[] kv = tag.split("=");
                        String key = kv[0];
                        String value;
                        if (kv.length == 1) {
                            value = "";
                        } else if (key.equalsIgnoreCase("ban-reason")) {
                            value = kv[1].replace("\\s", " ");
                        } else {
                            value = kv[1];
                        }
                        tags.put(key, value);
                    }
                    break;
                default:
                    for (String tag : ircTags.split(";")) {
                        String[] kv = tag.split("=");
                        String key = kv[0];
                        String value;
                        if (kv.length == 1) {
                            value = "";
                        } else {
                            value = kv[1];
                        }
                        tags.put(key, value);
                        if (key.equalsIgnoreCase("display-name")) {
                            user.changeName(value);
                        } else if (key.equalsIgnoreCase("color")) {
                            user.setColor(value);
                        } else if (key.equalsIgnoreCase("subscriber")) {
                            user.setSubscriber(value.equals("1"));
                        } else if (key.equalsIgnoreCase("turbo")) {
                            user.setTurbo(value.equals("1"));
                        } else if (key.equalsIgnoreCase("mod")) {
                            user.setMod(value.equals("1"));
                        } else if (key.equalsIgnoreCase("user-type")) {
                            user.setUserType(value);
                        } else if (key.equalsIgnoreCase("user-id")) {
                            user.setId(Long.parseLong(value));
                        } else if (key.equalsIgnoreCase("room-id")) {
                            user.setRoomId(Long.parseLong(value));
                        } else if (key.equalsIgnoreCase("emotes")) {
                            user.setEmotes(value);
                        } else if (key.equalsIgnoreCase("badges")) {
                            user.setBadges(value);
                        } else if (key.equalsIgnoreCase("id")) {
                            user.setMessageId(value);
                        } else if (key.equalsIgnoreCase("bits")) {
                            user.setBits(Long.parseLong(value));
                        }
                    }
                    long userID;
                    try {
                        userID = Long.parseLong(tags.get("user-id"));
                    } catch (Exception ex) {
                        userID = -1;
                    }
                    long roomId;
                    try {
                        roomId = Long.parseLong(tags.get("room-id"));
                    } catch (Exception ex) {
                        roomId = -1;
                    }
                    long consecutiveMonths;
                    try {
                        consecutiveMonths = Long.parseLong(tags.get("msg-param-months"));
                    } catch (Exception ex) {
                        consecutiveMonths = -1;
                    }
                    long bits;
                    try {
                        bits = Long.parseLong(tags.get("bits"));
                    } catch (Exception ex) {
                        bits = -1;
                    }
                    boolean sub;
                    try {
                        sub = tags.get("subscriber").equals("1");
                    } catch (Exception ex) {
                        sub = false;
                    }
                    boolean turbo;
                    try {
                        turbo = tags.get("turbo").equals("1");
                    } catch (Exception ex) {
                        turbo = false;
                    }
                    boolean mod;
                    try {
                        mod = tags.get("mod").equals("1");
                    } catch (Exception ex) {
                        mod = false;
                    }
                    updateUser(tags.get("display-name"), tags.get("color"), sub, turbo, mod, tags.get("user-type"), userID, roomId, consecutiveMonths, tags.get("emotes"), tags.get("badges"), tags.get("id"), bits, tags.get("emote-sets"), tags.get("msg-id"), tags.get("system-msg"), tags.get("login"));
                    break;
            }
        }
        // Check for CTCP requests.           
        if (command.equals("PRIVMSG") && line.indexOf(":\u0001") > 0 && line.endsWith("\u0001")) {
            String request = line.substring(line.indexOf(":\u0001") + 2, line.length() - 1);
            if (request.equals("VERSION")) {
                // VERSION request
                this.onVersion(user, target);
            } else if (request.startsWith("ACTION ")) {
                // ACTION request
                // so basically /me
                this.updateUserLastMessage(target, sourceNick, request.substring(7));
                this.updateUserAFK(target, sourceNick, false);
                this.onAction(user, channel, request.substring(7));
            } else if (request.startsWith("PING ")) {
                // PING request
                this.onPing(user, target, request.substring(5));
            } else if (request.equals("TIME")) {
                // TIME request
                this.onTime(user, target);
            } else if (request.equals("FINGER")) {
                // FINGER request
                this.onFinger(user, target);
            } else if ((tokenizer = new StringTokenizer(request)).countTokens() >= 5 && tokenizer.nextToken().equals("DCC")) {
                // This is a DCC request.
                boolean success = _dccManager.processRequest(sourceNick, sourceLogin, sourceHostname, request);
                if (!success) {
                    // The DccManager didn't know what to do with the line.
                    this.onUnknown(line);
                }
            } else {
                // An unknown CTCP message - ignore it.
                this.onUnknown(line);
            }
        } else if (command.equals("PRIVMSG") && _channelPrefixes.indexOf(target.charAt(0)) >= 0) {
            // This is a normal message to a channel.
            this.updateUserLastMessage(target, sourceNick, line.substring(line.indexOf(" :") + 2));
            this.updateUserAFK(target, sourceNick, false);
            this.onMessage(channel, user, line.substring(line.indexOf(" :") + 2));
        } else if (command.equals("PRIVMSG")) {
            // This is a private message to us.
            this.onPrivateMessage(user, line.substring(line.indexOf(" :") + 2));
        } else if (command.equals("WHISPER")) {
            // Whisper to us.
            this.onWhisper(user, line.split("WHISPER ", 2)[1].split(" :", 2)[0], line.split(" :", 2)[1]);

        } else if (command.equals("HOSTTARGET")) {
            //  Get Hosttarget
            String hostingChannel = line.split("HOSTTARGET ", 2)[1].split(" :", 2)[0];
            String targetChannel = line.split(" :", 2)[1].split(" ", 2)[0];
            String targetChannelViewers = line.split(targetChannel + " ", 2)[1]; // Most of the times, it returns a "-", instead of a valid integer
            this.onHostTarget(hostingChannel, targetChannel, targetChannelViewers);

        } else if (command.equals("JOIN")) {
            // Someone is joining a channel.
            //String channel = target;
            this.addUser(channel.getChannelName(), new User(sourceNick, channel));
            this.onJoin(channel, user);
        } else if (command.equals("PART")) {
            // Someone is parting from a channel.
            this.removeUser(target, sourceNick);
            if (sourceNick.equals(this.getNick())) {
                this.removeChannel(target);
            }
            this.onPart(channel, user);
        } else if (command.equals("NICK")) {
            // Somebody is changing their nick.
            String newNick = target;
            this.renameUser(sourceNick, newNick);
            if (sourceNick.equals(this.getNick())) {
                // Update our nick if it was us that changed nick.
                this.setNick(newNick);
            }
            this.onNickChange(sourceNick, sourceLogin, sourceHostname, newNick, user);
        } else if (command.equals("NOTICE")) {
            // Someone is sending a notice.
            this.onNotice(user, target, line.substring(line.indexOf(" :") + 2));
        } else if (command.equals("QUIT")) {
            // Someone has quit from the IRC server.
            if (sourceNick.equals(this.getNick())) {
                this.removeAllChannels();
            } else {
                this.removeUser(sourceNick);
            }
            this.onQuit(user, line.substring(line.indexOf(" :") + 2));
        } else if (command.equals("KICK")) {
            // Somebody has been kicked from a channel.
            String recipient = tokenizer.nextToken();
            if (recipient.equals(this.getNick())) {
                this.removeChannel(target);
            }
            this.removeUser(target, recipient);
            this.onKick(channel, user, recipient, line.substring(line.indexOf(" :") + 2));
        } else if (command.equals("MODE")) {
            // Somebody is changing the mode on a channel or user.
            String mode = line.substring(line.indexOf(target, 2) + target.length() + 1);
            if (mode.startsWith(":")) {
                mode = mode.substring(1);
            }
            this.processMode(target, sourceNick, sourceLogin, sourceHostname, mode);
        } else if (command.equals("TOPIC")) {
            // Someone is changing the topic.
            this.onTopic(channel, line.substring(line.indexOf(" :") + 2), target, System.currentTimeMillis(), true);
        } else if (command.equals("INVITE")) {
            // Somebody is inviting somebody else into a channel.
            this.onInvite(target, sourceNick, sourceLogin, sourceHostname, line.substring(line.indexOf(" :") + 2));
        } else if (command.equals("CLEARCHAT")) {
            String[] l = line.substring(line.indexOf("#")).split(" ");
            Channel ch = this._channels.get(l[0]);
            if (l.length == 1) { // Chat was cleared
                this.onChatCleared(ch);
            } else { // User was timed out
                try {
                    int duration = (tags.get("ban-duration") == null || tags.get("ban-duration").isEmpty() ? -1 : Integer.parseInt(tags.get("ban-duration")));
                    this.onUserTimedOut(ch.getUser(l[1].substring(1)), ch, duration, tags.get("ban-reason"));
                } catch (NullPointerException npe) {
                }
            }
        } else {
            // If we reach this point, then we've found something that the PircBot
            // Doesn't currently deal with.
            this.onUnknown(line);
        }

    }

    /**
     * This method is called once the PircBot has successfully connected to the
     * IRC server.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.6
     */
    protected void onConnect() {
    }

    /**
     * This method carries out the actions to be performed when the PircBot gets
     * disconnected. This may happen if the PircBot quits from the server, or if
     * the connection is unexpectedly lost.
     * <p>
     * Disconnection from the IRC server is detected immediately if either we or
     * the server close the connection normally. If the connection to the server
     * is lost, but neither we nor the server have explicitly closed the
     * connection, then it may take a few minutes to detect (this is commonly
     * referred to as a "ping timeout").
     * <p>
     * If you wish to get your IRC bot to automatically rejoin a server after
     * the connection has been lost, then this is probably the ideal method to
     * override to implement such functionality.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     */
    protected void onDisconnect() {
    }

    /**
     * This method is called by the PircBot when a numeric response is received
     * from the IRC server. We use this method to allow PircBot to process
     * various responses from the server before then passing them on to the
     * onServerResponse method.
     * <p>
     * Note that this method is private and should not appear in any of the
     * javadoc generated documenation.
     *
     * @param code The three-digit numerical code for the response.
     * @param response The full response from the IRC server.
     */
    private void processServerResponse(int code, String response) {

        if (code == RPL_LIST) {
            // This is a bit of information about a channel.
            int firstSpace = response.indexOf(' ');
            int secondSpace = response.indexOf(' ', firstSpace + 1);
            int thirdSpace = response.indexOf(' ', secondSpace + 1);
            int colon = response.indexOf(':');
            String channel = response.substring(firstSpace + 1, secondSpace);
            Channel channelObj = _channels.get(channel);
            int userCount = 0;
            try {
                userCount = Integer.parseInt(response.substring(secondSpace + 1, thirdSpace));
            } catch (NumberFormatException e) {
                // Stick with the value of zero.
            }
            String topic = response.substring(colon + 1);
            this.onChannelInfo(channelObj, userCount, topic);
        } else if (code == RPL_TOPIC) {
            // This is topic information about a channel we've just joined.
            int firstSpace = response.indexOf(' ');
            int secondSpace = response.indexOf(' ', firstSpace + 1);
            int colon = response.indexOf(':');
            String channel = response.substring(firstSpace + 1, secondSpace);
            String topic = response.substring(colon + 1);

            _topics.put(channel, topic);
        } else if (code == RPL_TOPICINFO) {
            StringTokenizer tokenizer = new StringTokenizer(response);
            tokenizer.nextToken();
            String channel = tokenizer.nextToken();
            Channel channelObj = _channels.get(channel);
            String setBy = tokenizer.nextToken();
            long date = 0;
            try {
                date = Long.parseLong(tokenizer.nextToken()) * 1000;
            } catch (NumberFormatException e) {
                // Stick with the default value of zero.
            }

            String topic = _topics.get(channel);
            _topics.remove(channel);

            this.onTopic(channelObj, topic, setBy, date, false);
        } else if (code == RPL_NAMREPLY) {
            // This is a list of nicks in a channel that we've just joined.
            int channelEndIndex = response.indexOf(" :");
            String channel = response.substring(response.lastIndexOf(' ', channelEndIndex - 1) + 1, channelEndIndex);

            StringTokenizer tokenizer = new StringTokenizer(response.substring(response.indexOf(" :") + 2));
            while (tokenizer.hasMoreTokens()) {
                String nick = tokenizer.nextToken();
                String prefix = "";
                if (nick.startsWith("@")) {
                    // User is an operator in this channel.
                    prefix = "@";
                } else if (nick.startsWith("+")) {
                    // User is voiced in this channel.
                    prefix = "+";
                } else if (nick.startsWith(".")) {
                    // Some wibbly status I've never seen before...
                    prefix = ".";
                }
                nick = nick.substring(prefix.length());
                this.addUser(channel, new User(nick, channel));
            }
        } else if (code == RPL_ENDOFNAMES) {
            // This is the end of a NAMES list, so we know that we've got
            // the full list of users in the channel that we just joined. 
            String channel = response.substring(response.indexOf(' ') + 1, response.indexOf(" :"));
            ArrayList<User> users = this.getUsers(channel);
            this.onUserList(channel, users);
        }

        this.onServerResponse(code, response);
    }

    /**
     * This method is called when we receive a numeric response from the IRC
     * server.
     * <p>
     * Numerics in the range from 001 to 099 are used for client-server
     * connections only and should never travel between servers. Replies
     * generated in response to commands are found in the range from 200 to 399.
     * Error replies are found in the range from 400 to 599.
     * <p>
     * For example, we can use this method to discover the topic of a channel
     * when we join it. If we join the channel #test which has a topic of
     * &quot;I am King of Test&quot; then the response will be
     * &quot;<code>PircBot #test :I Am King of Test</code>&quot; with a code of
     * 332 to signify that this is a topic. (This is just an example - note that
     * overriding the <code>onTopic</code> method is an easier way of finding
     * the topic for a channel). Check the IRC RFC for the full list of other
     * command response codes.
     * <p>
     * PircBot implements the interface ReplyConstants, which contains
     * contstants that you may find useful here.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param code The three-digit numerical code for the response.
     * @param response The full response from the IRC server.
     *
     * @see ReplyConstants
     */
    protected void onServerResponse(int code, String response) {
    }

    /**
     * This method is called when we receive a user list from the server after
     * joining a channel.
     * <p>
     * Shortly after joining a channel, the IRC server sends a list of all users
     * in that channel. The PircBot collects this information and calls this
     * method as soon as it has the full list.
     * <p>
     * To obtain the nick of each user in the channel, call the getNick() method
     * on each User object in the array.
     * <p>
     * At a later time, you may call the getUsers method to obtain an up to date
     * list of the users in the channel.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 1.0.0
     *
     * @param channel The name of the channel.
     * @param users An array of User objects belonging to this channel.
     *
     * @see User
     */
    protected void onUserList(String channel, ArrayList<User> users) {
    }

    /**
     * This method is called whenever a message is sent to a channel.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel to which the message was sent.
     * @param sender The nick of the person who sent the message.
     * @param message The actual message sent to the channel.
     */
    protected void onMessage(Channel channel, User sender, String message) {
    }

    /**
     * This method is called whenever a message is sent from the bot.
     *
     * @param channel Channel the message was sent to.
     * @param message Message that was sent.
     */
    protected void onSentMessage(String channel, String message) {

    }

    /**
     * This method is called whenever a private message is sent to the PircBot.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param sender The nick of the person who sent the private message.
     * @param message The actual message.
     */
    protected void onPrivateMessage(User sender, String message) {
    }

    /**
     * This method is called whenever an ACTION is sent from a user. E.g. such
     * events generated by typing "/me goes shopping" in most IRC clients.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param sender The nick of the user that sent the action.
     * @param channel The target of the action, be it a channel or our nick.
     * @param action The action carried out by the user.
     */
    protected void onAction(User sender, Channel channel, String action) {
    }

    /**
     * This method is called whenever we receive a notice.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param sender The nick of the user that sent the notice.
     * @param target The target of the notice, be it our nick or a channel name.
     * @param notice The notice message.
     */
    protected void onNotice(User sender, String target, String notice) {
    }

    /**
     * This method is called whenever someone (possibly us) joins a channel
     * which we are on.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel which somebody joined.
     * @param sender The nick of the user who joined the channel.
     */
    protected void onJoin(Channel channel, User sender) {
    }

    /**
     * This method is called whenever someone (possibly us) parts a channel
     * which we are on.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel which somebody parted from.
     * @param sender The nick of the user who parted from the channel.
     */
    protected void onPart(Channel channel, User sender) {
    }

    /**
     * This method is called whenever someone (possibly us) changes nick on any
     * of the channels that we are on.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param oldNick The old nick.
     * @param login The login of the user.
     * @param hostname The hostname of the user.
     * @param newNick The new nick.
     * @param user The user object.
     */
    protected void onNickChange(String oldNick, String login, String hostname, String newNick, User user) {
    }

    /**
     * This method is called whenever someone (possibly us) is kicked from any
     * of the channels that we are in.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel from which the recipient was kicked.
     * @param kicker The user who performed the kick.
     * @param reciepient User who is getting the boot.
     * @param reason The reason given by the user who performed the kick.
     */
    protected void onKick(Channel channel, User kicker, String reciepient, String reason) {
    }

    /**
     * This method is called whenever someone (possibly us) quits from the
     * server. We will only observe this if the user was in one of the channels
     * to which we are connected.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param user The user that quit from the server.
     * @param reason The reason given for quitting the server.
     */
    protected void onQuit(User user, String reason) {
    }

    /**
     * This method is called whenever a user sets the topic, or when PircBot
     * joins a new channel and discovers its topic.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel that the topic belongs to.
     * @param topic The topic for the channel.
     * @param setBy The nick of the user that set the topic.
     * @param date When the topic was set (milliseconds since the epoch).
     * @param changed True if the topic has just been changed, false if the
     * topic was already there.
     *
     */
    protected void onTopic(Channel channel, String topic, String setBy, long date, boolean changed) {
    }

    /**
     * After calling the listChannels() method in PircBot, the server will start
     * to send us information about each channel on the server. You may override
     * this method in order to receive the information about each channel as
     * soon as it is received.
     * <p>
     * Note that certain channels, such as those marked as hidden, may not
     * appear in channel listings.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The name of the channel.
     * @param userCount The number of users visible in this channel.
     * @param topic The topic for this channel.
     *
     * @see #listChannels() listChannels
     */
    protected void onChannelInfo(Channel channel, int userCount, String topic) {
    }

    /**
     * Called when a whisper is received to the bot. This is mainly used with
     * Twitch TV.
     *
     * @param sender The user sending the Whisper
     * @param target Who the Whisper is for
     * @param message Whisper Message
     */
    protected void onWhisper(User sender, String target, String message) {

    }

    /**
     * Called when the mode of a channel is set. We process this in order to
     * call the appropriate onOp, onDeop, etc method before finally calling the
     * override-able onMode method.
     * <p>
     * Note that this method is private and is not intended to appear in the
     * javadoc generated documentation.
     *
     * @param target The channel or nick that the mode operation applies to.
     * @param sourceNick The nick of the user that set the mode.
     * @param sourceLogin The login of the user that set the mode.
     * @param sourceHostname The hostname of the user that set the mode.
     * @param mode The mode that has been set.
     */
    private void processMode(String target, String sourceNick, String sourceLogin, String sourceHostname, String mode) {

        if (_channelPrefixes.indexOf(target.charAt(0)) >= 0) {
            // The mode of a channel is being changed.
            Channel channel = _channels.get(target);
            StringTokenizer tok = new StringTokenizer(mode);
            String[] params = new String[tok.countTokens()];

            int t = 0;
            while (tok.hasMoreTokens()) {
                params[t] = tok.nextToken();
                t++;
            }

            char pn = ' ';
            int p = 1;

            // All of this is very large and ugly, but it's the only way of providing
            // what the users want :-/
            for (int i = 0; i < params[0].length(); i++) {
                char atPos = params[0].charAt(i);

                if (atPos == '+' || atPos == '-') {
                    pn = atPos;
                } else if (atPos == 'o') {
                    if (pn == '+') {
                        this.updateUser(channel.getChannelName(), OP_ADD, params[p]);
                        onOp(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                    } else {
                        this.updateUser(channel.getChannelName(), OP_REMOVE, params[p]);
                        onDeop(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                    }
                    p++;
                } else if (atPos == 'v') {
                    if (pn == '+') {
                        this.updateUser(channel.getChannelName(), VOICE_ADD, params[p]);
                        onVoice(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                    } else {
                        this.updateUser(channel.getChannelName(), VOICE_REMOVE, params[p]);
                        onDeVoice(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                    }
                    p++;
                } else if (atPos == 'k') {
                    if (pn == '+') {
                        onSetChannelKey(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                    } else {
                        onRemoveChannelKey(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                    }
                    p++;
                } else if (atPos == 'l') {
                    if (pn == '+') {
                        onSetChannelLimit(channel, sourceNick, sourceLogin, sourceHostname, Integer.parseInt(params[p]));
                        p++;
                    } else {
                        onRemoveChannelLimit(channel, sourceNick, sourceLogin, sourceHostname);
                    }
                } else if (atPos == 'b') {
                    if (pn == '+') {
                        onSetChannelBan(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                    } else {
                        onRemoveChannelBan(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                    }
                    p++;
                } else if (atPos == 't') {
                    if (pn == '+') {
                        onSetTopicProtection(channel, sourceNick, sourceLogin, sourceHostname);
                    } else {
                        onRemoveTopicProtection(channel, sourceNick, sourceLogin, sourceHostname);
                    }
                } else if (atPos == 'n') {
                    if (pn == '+') {
                        onSetNoExternalMessages(channel, sourceNick, sourceLogin, sourceHostname);
                    } else {
                        onRemoveNoExternalMessages(channel, sourceNick, sourceLogin, sourceHostname);
                    }
                } else if (atPos == 'i') {
                    if (pn == '+') {
                        onSetInviteOnly(channel, sourceNick, sourceLogin, sourceHostname);
                    } else {
                        onRemoveInviteOnly(channel, sourceNick, sourceLogin, sourceHostname);
                    }
                } else if (atPos == 'm') {
                    if (pn == '+') {
                        onSetModerated(channel, sourceNick, sourceLogin, sourceHostname);
                    } else {
                        onRemoveModerated(channel, sourceNick, sourceLogin, sourceHostname);
                    }
                } else if (atPos == 'p') {
                    if (pn == '+') {
                        onSetPrivate(channel, sourceNick, sourceLogin, sourceHostname);
                    } else {
                        onRemovePrivate(channel, sourceNick, sourceLogin, sourceHostname);
                    }
                } else if (atPos == 's') {
                    if (pn == '+') {
                        onSetSecret(channel, sourceNick, sourceLogin, sourceHostname);
                    } else {
                        onRemoveSecret(channel, sourceNick, sourceLogin, sourceHostname);
                    }
                }
            }

            this.onMode(channel, sourceNick, sourceLogin, sourceHostname, mode);
        } else {
            // The mode of a user is being changed.
            String nick = target;
            this.onUserMode(nick, sourceNick, sourceLogin, sourceHostname, mode);
        }
    }

    /**
     * Called when the mode of a channel is set.
     * <p>
     * You may find it more convenient to decode the meaning of the mode string
     * by overriding the onOp, onDeOp, onVoice, onDeVoice, onChannelKey,
     * onDeChannelKey, onChannelLimit, onDeChannelLimit, onChannelBan or
     * onDeChannelBan methods as appropriate.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel that the mode operation applies to.
     * @param sourceNick The nick of the user that set the mode.
     * @param sourceLogin The login of the user that set the mode.
     * @param sourceHostname The hostname of the user that set the mode.
     * @param mode The mode that has been set.
     *
     */
    protected void onMode(Channel channel, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
    }

    /**
     * Called when the mode of a user is set.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 1.2.0
     *
     * @param targetNick The nick that the mode operation applies to.
     * @param sourceNick The nick of the user that set the mode.
     * @param sourceLogin The login of the user that set the mode.
     * @param sourceHostname The hostname of the user that set the mode.
     * @param mode The mode that has been set.
     *
     */
    protected void onUserMode(String targetNick, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
    }

    /**
     * Called when a user (possibly us) gets granted operator status for a
     * channel.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     * @param recipient The nick of the user that got 'opped'.
     */
    protected void onOp(Channel channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
    }

    /**
     * Called when a user (possibly us) gets operator status taken away.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     * @param recipient The nick of the user that got 'deopped'.
     */
    protected void onDeop(Channel channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
    }

    /**
     * Called when a user (possibly us) gets voice status granted in a channel.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     * @param recipient The nick of the user that got 'voiced'.
     */
    protected void onVoice(Channel channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
    }

    /**
     * Called when a user (possibly us) gets voice status removed.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     * @param recipient The nick of the user that got 'devoiced'.
     */
    protected void onDeVoice(Channel channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
    }

    /**
     * Called when a channel key is set. When the channel key has been set,
     * other users may only join that channel if they know the key. Channel keys
     * are sometimes referred to as passwords.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     * @param key The new key for the channel.
     */
    protected void onSetChannelKey(Channel channel, String sourceNick, String sourceLogin, String sourceHostname, String key) {
    }

    /**
     * Called when a channel key is removed.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     * @param key The key that was in use before the channel key was removed.
     */
    protected void onRemoveChannelKey(Channel channel, String sourceNick, String sourceLogin, String sourceHostname, String key) {
    }

    /**
     * Called when a user limit is set for a channel. The number of users in the
     * channel cannot exceed this limit.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     * @param limit The maximum number of users that may be in this channel at
     * the same time.
     */
    protected void onSetChannelLimit(Channel channel, String sourceNick, String sourceLogin, String sourceHostname, int limit) {
    }

    /**
     * Called when the user limit is removed for a channel.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     */
    protected void onRemoveChannelLimit(Channel channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a user (possibly us) gets banned from a channel. Being banned
     * from a channel prevents any user with a matching hostmask from joining
     * the channel. For this reason, most bans are usually directly followed by
     * the user being kicked :-)
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     * @param hostmask The hostmask of the user that has been banned.
     */
    protected void onSetChannelBan(Channel channel, String sourceNick, String sourceLogin, String sourceHostname, String hostmask) {
    }

    /**
     * Called when a hostmask ban is removed from a channel.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     * @param hostmask
     */
    protected void onRemoveChannelBan(Channel channel, String sourceNick, String sourceLogin, String sourceHostname, String hostmask) {
    }

    /**
     * Called when topic protection is enabled for a channel. Topic protection
     * means that only operators in a channel may change the topic.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     */
    protected void onSetTopicProtection(Channel channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when topic protection is removed for a channel.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     */
    protected void onRemoveTopicProtection(Channel channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel is set to only allow messages from users that are
     * in the channel.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     */
    protected void onSetNoExternalMessages(Channel channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel is set to allow messages from any user, even if
     * they are not actually in the channel.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     */
    protected void onRemoveNoExternalMessages(Channel channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel is set to 'invite only' mode. A user may only join
     * the channel if they are invited by someone who is already in the channel.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     */
    protected void onSetInviteOnly(Channel channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel has 'invite only' removed.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     */
    protected void onRemoveInviteOnly(Channel channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel is set to 'moderated' mode. If a channel is
     * moderated, then only users who have been 'voiced' or 'opped' may speak or
     * change their nicks.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     */
    protected void onSetModerated(Channel channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel has moderated mode removed.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     */
    protected void onRemoveModerated(Channel channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel is marked as being in private mode.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     */
    protected void onSetPrivate(Channel channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel is marked as not being in private mode.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     */
    protected void onRemovePrivate(Channel channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel is set to be in 'secret' mode. Such channels
     * typically do not appear on a server's channel listing.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     */
    protected void onSetSecret(Channel channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel has 'secret' mode removed.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     */
    protected void onRemoveSecret(Channel channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when we are invited to a channel by a user.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 0.9.5
     *
     * @param targetNick The nick of the user being invited - should be us!
     * @param sourceNick The nick of the user that sent the invitation.
     * @param sourceLogin The login of the user that sent the invitation.
     * @param sourceHostname The hostname of the user that sent the invitation.
     * @param channel The channel that we're being invited to.
     */
    protected void onInvite(String targetNick, String sourceNick, String sourceLogin, String sourceHostname, String channel) {
    }

    /**
     * This method is called whenever a DCC SEND request is sent to the PircBot.
     * This means that a client has requested to send a file to us. This
     * abstract implementation performs no action, which means that all DCC SEND
     * requests will be ignored by default. If you wish to receive the file,
     * then you may override this method and call the receive method on the
     * DccFileTransfer object, which connects to the sender and downloads the
     * file.
     * <p>
     * Example:
     * <pre> public void onIncomingFileTransfer(DccFileTransfer transfer) {
     *     // Use the suggested file name.
     *     File file = transfer.getFile();
     *     // Receive the transfer and save it to the file, allowing resuming.
     *     transfer.receive(file, true);
     * }</pre>
     * <p>
     * <b>Warning:</b> Receiving an incoming file transfer will cause a file to
     * be written to disk. Please ensure that you make adequate security checks
     * so that this file does not overwrite anything important!
     * <p>
     * Each time a file is received, it happens within a new Thread in order to
     * allow multiple files to be downloaded by the PircBot at the same time.
     * <p>
     * If you allow resuming and the file already partly exists, it will be
     * appended to instead of overwritten. If resuming is not enabled, the file
     * will be overwritten if it already exists.
     * <p>
     * You can throttle the speed of the transfer by calling the setPacketDelay
     * method on the DccFileTransfer object, either before you receive the file
     * or at any moment during the transfer.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 1.2.0
     *
     * @param transfer The DcccFileTransfer that you may accept.
     *
     * @see DccFileTransfer
     *
     */
    protected void onIncomingFileTransfer(DccFileTransfer transfer) {
    }

    /**
     * This method gets called when a DccFileTransfer has finished. If there was
     * a problem, the Exception will say what went wrong. If the file was sent
     * successfully, the Exception will be null.
     * <p>
     * Both incoming and outgoing file transfers are passed to this method. You
     * can determine the type by calling the isIncoming or isOutgoing methods on
     * the DccFileTransfer object.
     *
     * @since PircBot 1.2.0
     *
     * @param transfer The DccFileTransfer that has finished.
     * @param e null if the file was transfered successfully, otherwise this
     * will report what went wrong.
     *
     * @see DccFileTransfer
     *
     */
    protected void onFileTransferFinished(DccFileTransfer transfer, Exception e) {
    }

    /**
     * This method will be called whenever a DCC Chat request is received. This
     * means that a client has requested to chat to us directly rather than via
     * the IRC server. This is useful for sending many lines of text to and from
     * the bot without having to worry about flooding the server or any
     * operators of the server being able to "spy" on what is being said. This
     * abstract implementation performs no action, which means that all DCC CHAT
     * requests will be ignored by default.
     * <p>
     * If you wish to accept the connection, then you may override this method
     * and call the accept() method on the DccChat object, which connects to the
     * sender of the chat request and allows lines to be sent to and from the
     * bot.
     * <p>
     * Your bot must be able to connect directly to the user that sent the
     * request.
     * <p>
     * Example:
     * <pre> public void onIncomingChatRequest(DccChat chat) {
     *     try {
     *         // Accept all chat, whoever it's from.
     *         chat.accept();
     *         chat.sendLine("Hello");
     *         String response = chat.readLine();
     *         chat.close();
     *     }
     *     catch (IOException e) {}
     * }</pre>
     *
     * Each time this method is called, it is called from within a new Thread so
     * that multiple DCC CHAT sessions can run concurrently.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @since PircBot 1.2.0
     *
     * @param chat A DccChat object that represents the incoming chat request.
     *
     * @see DccChat
     *
     */
    protected void onIncomingChatRequest(DccChat chat) {
    }

    /**
     * This method is called whenever we receive a VERSION request. This
     * abstract implementation responds with the PircBot's _version string, so
     * if you override this method, be sure to either mimic its functionality or
     * to call super.onVersion(...);
     *
     * @param sender The nick of the user that sent the VERSION request..
     * @param target The target of the VERSION request, be it our nick or a
     * channel name.
     */
    protected void onVersion(User sender, String target) {
        this.sendRawLine("NOTICE " + sender.getNick() + " :\u0001VERSION " + _version + "\u0001");
    }

    /**
     * This method is called whenever we receive a PING request from another
     * user.
     * <p>
     * This abstract implementation responds correctly, so if you override this
     * method, be sure to either mimic its functionality or to call
     * super.onPing(...);
     *
     * @param sender The nick of the user that sent the PING request.
     * @param target The target of the PING request, be it our nick or a channel
     * name.
     * @param pingValue The value that was supplied as an argument to the PING
     * command.
     */
    protected void onPing(User sender, String target, String pingValue) {
        this.sendRawLine("NOTICE " + sender.getNick() + " :\u0001PING " + pingValue + "\u0001");
    }

    /**
     * The actions to perform when a PING request comes from the server.
     * <p>
     * This sends back a correct response, so if you override this method, be
     * sure to either mimic its functionality or to call
     * super.onServerPing(response);
     *
     * @param response The response that should be given back in your PONG.
     */
    protected void onServerPing(String response) {
        this.sendRawLine("PONG " + response);
    }

    /**
     * This method is called whenever we receive a TIME request.
     * <p>
     * This abstract implementation responds correctly, so if you override this
     * method, be sure to either mimic its functionality or to call
     * super.onTime(...);
     *
     * @param sender User sending the TIME request.
     * @param target The target of the TIME request, be it our nick or a channel
     * name.
     */
    protected void onTime(User sender, String target) {
        this.sendRawLine("NOTICE " + sender.getNick() + " :\u0001TIME " + new Date().toString() + "\u0001");
    }

    /**
     * This method is called whenever we receive a FINGER request.
     * <p>
     * This abstract implementation responds correctly, so if you override this
     * method, be sure to either mimic its functionality or to call
     * super.onFinger(...);
     *
     * @param sender The nick of the user that sent the FINGER request.
     * @param target The target of the FINGER request, be it our nick or a
     * channel name.
     */
    protected void onFinger(User sender, String target) {
        this.sendRawLine("NOTICE " + sender.getNick() + " :\u0001FINGER " + _finger + "\u0001");
    }

    /**
     * This method is called whenever a user is timed out in a channel.
     * <p>
     * This is for use with twitch
     *
     * @param user the user who was timed out
     * @param channel the channel where user was timed out
     * @param duration Duration user was timed out for. If duration is -1, the
     * ban is permanent.
     * @param reason Reason user was timed out.
     */
    protected void onUserTimedOut(User user, Channel channel, int duration, String reason) {

    }

    /**
     * This method is called whenever a USERSTATE line is received from Twitch
     * TV IRC Servers.
     * <p>
     * This is for use with twitch
     *
     * @param user username
     * @param channel channel name
     */
    protected void onUserState(User user, Channel channel) {

    }

    /**
     * This method is called whenever a USERNOTICE resubscription line is
     * received from Twitch TV IRC Servers.
     * <p>
     * This is for use with twitch
     * 
     * This is untested since there's no way to send a test line to a channel
     * You'd have to join a channel that has partnership and wait for an user to
     * resubscribe
     *
     * @param channel Channel name
     * @param sender Username
     * @param message Resubscription message
     */
    protected void onUserNotice(Channel channel, User sender, String message) {

    }

    /**
     * This method is called whenever a GLOBALUSERSTATE line is received from
     * Twitch TV IRC Servers.
     * <p>
     * This is for use with twitch
     *
     * @param user username
     */
    protected void onGlobalUserState(User user) {

    }

    /**
     * This method is called whenever chat is cleared in a channel.
     * <p>
     * This is for use with twitch
     *
     * @param channel the channel where chat was cleared
     */
    protected void onChatCleared(Channel channel) {

    }

    /**
     * This method is called whenever a channel hosts another channel
     * <p>
     * This is for use with Twitch TV
     *
     * @param hostingChannel that is hosting
     * @param targetChannel the chat that is being hosted
     * @param viewers viewer count for the channel that is being hosted
     */
    protected void onHostTarget(String hostingChannel, String targetChannel, String viewers) {

    }

    /**
     * This method is called whenever we receive a line from the server that the
     * PircBot has not been programmed to recognise.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param line The raw line that was received from the server.
     */
    protected void onUnknown(String line) {
        // And then there were none :)
    }

    /**
     * Sets the verbose mode. If verbose mode is set to true, then log entries
     * will be printed to the standard output. The default value is false and
     * will result in no output. For general development, we strongly recommend
     * setting the verbose mode to true.
     *
     * @param verbose true if verbose mode is to be used. Default is false.
     */
    public final void setVerbose(boolean verbose) {
        _verbose = verbose;
    }

    /**
     * Sets the name of the bot, which will be used as its nick when it tries to
     * join an IRC server. This should be set before joining any servers,
     * otherwise the default nick will be used. You would typically call this
     * method from the constructor of the class that extends PircBot.
     * <p>
     * The changeNick method should be used if you wish to change your nick when
     * you are connected to a server.
     *
     * @param name The new name of the Bot.
     */
    protected final void setName(String name) {
        _name = name;
    }

    /**
     * Sets the internal nick of the bot. This is only to be called by the
     * PircBot class in response to notification of nick changes that apply to
     * us.
     *
     * @param nick The new nick.
     */
    private void setNick(String nick) {
        _nick = nick;
    }

    /**
     * Sets the internal login of the Bot. This should be set before joining any
     * servers.
     *
     * @param login The new login of the Bot.
     */
    protected final void setLogin(String login) {
        _login = login;
    }

    /**
     * Sets the internal version of the Bot. This should be set before joining
     * any servers.
     *
     * @param version The new version of the Bot.
     */
    protected final void setVersion(String version) {
        _version = version;
    }

    /**
     * Sets the interal finger message. This should be set before joining any
     * servers.
     *
     * @param finger The new finger message for the Bot.
     */
    protected final void setFinger(String finger) {
        _finger = finger;
    }

    /**
     * Gets the name of the PircBot. This is the name that will be used as as a
     * nick when we try to join servers.
     *
     * @return The name of the PircBot.
     */
    public final String getName() {
        return _name;
    }

    /**
     * Returns the current nick of the bot. Note that if you have just changed
     * your nick, this method will still return the old nick until confirmation
     * of the nick change is received from the server.
     * <p>
     * The nick returned by this method is maintained only by the PircBot class
     * and is guaranteed to be correct in the context of the IRC server.
     *
     * @since PircBot 1.0.0
     *
     * @return The current nick of the bot.
     */
    public String getNick() {
        return _nick;
    }

    /**
     * Gets the internal login of the PircBot.
     *
     * @return The login of the PircBot.
     */
    public final String getLogin() {
        return _login;
    }

    /**
     * Gets the internal version of the PircBot.
     *
     * @return The version of the PircBot.
     */
    public final String getVersion() {
        return _version;
    }

    /**
     * Gets the internal finger message of the PircBot.
     *
     * @return The finger message of the PircBot.
     */
    public final String getFinger() {
        return _finger;
    }

    /**
     * Returns whether or not the PircBot is currently connected to a server.
     * The result of this method should only act as a rough guide, as the result
     * may not be valid by the time you act upon it.
     *
     * @return True if and only if the PircBot is currently connected to a
     * server.
     */
    public final synchronized boolean isConnected() {
        return _inputThread != null && _inputThread.isConnected();
    }

    /**
     * Sets the number of milliseconds to delay between consecutive messages
     * when there are multiple messages waiting in the outgoing message queue.
     * This has a default value of 1000ms. It is a good idea to stick to this
     * default value, as it will prevent your bot from spamming servers and
     * facing the subsequent wrath! However, if you do need to change this delay
     * value (<b>not recommended</b>), then this is the method to use.
     *
     * @param delay The number of milliseconds between each outgoing message.
     *
     */
    public final void setMessageDelay(long delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("Cannot have a negative time.");
        }
        _messageDelay = delay;
    }

    /**
     * Sets the OutQueue PRIVMSG max length. Default value is unlimited. If set
     * to an amount (e.g. 10), and more than 10 messages are added to the Queue
     * before they are sent, they will not be added. This feature is to prevent
     * spam.
     *
     * @param length Length of the message queue
     */
    public final void setMaxMessageQueueLength(int length) {
        _outQueue.setMessageSize(length);
    }

    /**
     * Returns the number of milliseconds that will be used to separate
     * consecutive messages to the server from the outgoing message queue.
     *
     * @return Number of milliseconds.
     */
    public final long getMessageDelay() {
        return _messageDelay;
    }

    /**
     * Gets the maximum length of any line that is sent via the IRC protocol.
     *
     * @return The maximum line length (512 default)
     */
    public final int getMaxLineLength() {
        return InputThread.MAX_LINE_LENGTH;
    }

    /**
     * Set the maximum length of any line that is sent via the IRC protocol. The
     * IRC RFC specifies that line lengths, including the trailing \r\n must not
     * exceed 512 bytes. All lines greater than this length will be truncated
     * before being sent to the IRC server.
     *
     * @param length New max line length
     */
    public final void setMaxLineLength(int length) {
        InputThread.MAX_LINE_LENGTH = length;
    }

    /**
     * Gets the number of lines currently waiting in the outgoing message Queue.
     * If this returns 0, then the Queue is empty and any new message is likely
     * to be sent to the IRC server immediately.
     *
     * @since PircBot 0.9.9
     *
     * @return The number of lines in the outgoing message Queue.
     */
    public final int getOutgoingQueueSize() {
        return _outQueue.size();
    }

    /**
     * Returns the name of the last IRC server the PircBot tried to connect to.
     * This does not imply that the connection attempt to the server was
     * successful (we suggest you look at the onConnect method). A value of null
     * is returned if the PircBot has never tried to connect to a server.
     *
     * @return The name of the last machine we tried to connect to. Returns null
     * if no connection attempts have ever been made.
     */
    public final String getServer() {
        return _server;
    }

    /**
     * Returns the port number of the last IRC server that the PircBot tried to
     * connect to. This does not imply that the connection attempt to the server
     * was successful (we suggest you look at the onConnect method). A value of
     * -1 is returned if the PircBot has never tried to connect to a server.
     *
     * @since PircBot 0.9.9
     *
     * @return The port number of the last IRC server we connected to. Returns
     * -1 if no connection attempts have ever been made.
     */
    public final int getPort() {
        return _port;
    }

    /**
     * Returns the last password that we used when connecting to an IRC server.
     * This does not imply that the connection attempt to the server was
     * successful (we suggest you look at the onConnect method). A value of null
     * is returned if the PircBot has never tried to connect to a server using a
     * password.
     *
     * @since PircBot 0.9.9
     *
     * @return The last password that we used when connecting to an IRC server.
     * Returns null if we have not previously connected using a password.
     */
    public final String getPassword() {
        return _password;
    }

    /**
     * A convenient method that accepts an IP address represented as a long and
     * returns an integer array of size 4 representing the same IP address.
     *
     * @since PircBot 0.9.4
     *
     * @param address the long value representing the IP address.
     *
     * @return An int[] of size 4.
     */
    public int[] longToIp(long address) {
        int[] ip = new int[4];
        for (int i = 3; i >= 0; i--) {
            ip[i] = (int) (address % 256);
            address = address / 256;
        }
        return ip;
    }

    /**
     * A convenient method that accepts an IP address represented by a byte[] of
     * size 4 and returns this as a long representation of the same IP address.
     *
     * @since PircBot 0.9.4
     *
     * @param address the byte[] of size 4 representing the IP address.
     *
     * @return a long representation of the IP address.
     */
    public long ipToLong(byte[] address) {
        if (address.length != 4) {
            throw new IllegalArgumentException("byte array must be of length 4");
        }
        long ipNum = 0;
        long multiplier = 1;
        for (int i = 3; i >= 0; i--) {
            int byteVal = (address[i] + 256) % 256;
            ipNum += byteVal * multiplier;
            multiplier *= 256;
        }
        return ipNum;
    }

    /**
     * Sets the encoding charset to be used when sending or receiving lines from
     * the IRC server. If set to null, then the platform's default charset is
     * used. You should only use this method if you are trying to send text to
     * an IRC server in a different charset, e.g. "GB2312" for Chinese encoding.
     * If a PircBot is currently connected to a server, then it must reconnect
     * before this change takes effect.
     *
     * @since PircBot 1.0.4
     *
     * @param charset The new encoding charset to be used by PircBot.
     *
     * @throws UnsupportedEncodingException If the named charset is not
     * supported.
     */
    public void setEncoding(String charset) throws UnsupportedEncodingException {
        // Just try to see if the charset is supported first...
        "".getBytes(charset);

        _charset = charset;
    }

    /**
     * Returns the encoding used to send and receive lines from the IRC server,
     * or null if not set. Use the setEncoding method to change the encoding
     * charset.
     *
     * @since PircBot 1.0.4
     *
     * @return The encoding used to send outgoing messages, or null if not set.
     */
    public String getEncoding() {
        return _charset;
    }

    /**
     * Returns the InetAddress used by the PircBot. This can be used to find the
     * I.P. address from which the PircBot is connected to a server.
     *
     * @since PircBot 1.4.4
     *
     * @return The current local InetAddress, or null if never connected.
     */
    public InetAddress getInetAddress() {
        return _inetAddress;
    }

    /**
     * Sets the InetAddress to be used when sending DCC chat or file transfers.
     * This can be very useful when you are running a bot on a machine which is
     * behind a firewall and you need to tell receiving clients to connect to a
     * NAT/router, which then forwards the connection.
     *
     * @since PircBot 1.4.4
     *
     * @param dccInetAddress The new InetAddress, or null to use the default.
     */
    public void setDccInetAddress(InetAddress dccInetAddress) {
        _dccInetAddress = dccInetAddress;
    }

    /**
     * Returns the InetAddress used when sending DCC chat or file transfers. If
     * this is null, the default InetAddress will be used.
     *
     * @since PircBot 1.4.4
     *
     * @return The current DCC InetAddress, or null if left as default.
     */
    public InetAddress getDccInetAddress() {
        return _dccInetAddress;
    }

    /**
     * Returns the set of port numbers to be used when sending a DCC chat or
     * file transfer. This is useful when you are behind a firewall and need to
     * set up port forwarding. The array of port numbers is traversed in
     * sequence until a free port is found to listen on. A DCC tranfer will fail
     * if all ports are already in use. If set to null, <i>any</i> free port
     * number will be used.
     *
     * @since PircBot 1.4.4
     *
     * @return An array of port numbers that PircBot can use to send DCC
     * transfers, or null if any port is allowed.
     */
    public int[] getDccPorts() {
        if (_dccPorts == null || _dccPorts.length == 0) {
            return null;
        }
        // Clone the array to prevent external modification.
        return (int[]) _dccPorts.clone();
    }

    /**
     * Sets the choice of port numbers that can be used when sending a DCC chat
     * or file transfer. This is useful when you are behind a firewall and need
     * to set up port forwarding. The array of port numbers is traversed in
     * sequence until a free port is found to listen on. A DCC tranfer will fail
     * if all ports are already in use. If set to null, <i>any</i> free port
     * number will be used.
     *
     * @since PircBot 1.4.4
     *
     * @param ports The set of port numbers that PircBot may use for DCC
     * transfers, or null to let it use any free port (default).
     *
     */
    public void setDccPorts(int[] ports) {
        if (ports == null || ports.length == 0) {
            _dccPorts = null;
        } else {
            // Clone the array to prevent external modification.
            _dccPorts = (int[]) ports.clone();
        }
    }

    /**
     * Returns true if and only if the object being compared is the exact same
     * instance as this PircBot. This may be useful if you are writing a
     * multiple server IRC bot that uses more than one instance of PircBot.
     *
     * @param obj
     * @since PircBot 0.9.9
     *
     * @return true if and only if Object o is a PircBot and equal to this.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PircBot other = (PircBot) obj;
        if (!Objects.equals(this._inputThread, other._inputThread)) {
            return false;
        }
        if (!Objects.equals(this._outputThread, other._outputThread)) {
            return false;
        }
        if (!Objects.equals(this._charset, other._charset)) {
            return false;
        }
        if (!Objects.equals(this._inetAddress, other._inetAddress)) {
            return false;
        }
        if (!Objects.equals(this._server, other._server)) {
            return false;
        }
        if (this._port != other._port) {
            return false;
        }
        if (!Objects.equals(this._password, other._password)) {
            return false;
        }
        if (!Objects.equals(this._outQueue, other._outQueue)) {
            return false;
        }
        if (this._messageDelay != other._messageDelay) {
            return false;
        }
        if (!Objects.equals(this._channels, other._channels)) {
            return false;
        }
        if (!Objects.equals(this._topics, other._topics)) {
            return false;
        }
        if (!Objects.equals(this._dccManager, other._dccManager)) {
            return false;
        }
        if (!Arrays.equals(this._dccPorts, other._dccPorts)) {
            return false;
        }
        if (!Objects.equals(this._dccInetAddress, other._dccInetAddress)) {
            return false;
        }
        if (this._autoNickChange != other._autoNickChange) {
            return false;
        }
        if (this._verbose != other._verbose) {
            return false;
        }
        if (!Objects.equals(this._name, other._name)) {
            return false;
        }
        if (!Objects.equals(this._nick, other._nick)) {
            return false;
        }
        if (!Objects.equals(this._login, other._login)) {
            return false;
        }
        if (!Objects.equals(this._version, other._version)) {
            return false;
        }
        if (!Objects.equals(this._finger, other._finger)) {
            return false;
        }
        return Objects.equals(this._channelPrefixes, other._channelPrefixes);
    }

    /**
     * Returns the hashCode of this PircBot. This method can be called by hashed
     * collection classes and is useful for managing multiple instances of
     * PircBots in such collections.
     *
     * @since PircBot 0.9.9
     *
     * @return the hash code for this instance of PircBot.
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(this._inputThread);
        hash = 67 * hash + Objects.hashCode(this._outputThread);
        hash = 67 * hash + Objects.hashCode(this._charset);
        hash = 67 * hash + Objects.hashCode(this._inetAddress);
        hash = 67 * hash + Objects.hashCode(this._server);
        hash = 67 * hash + this._port;
        hash = 67 * hash + Objects.hashCode(this._password);
        hash = 67 * hash + Objects.hashCode(this._outQueue);
        hash = 67 * hash + (int) (this._messageDelay ^ (this._messageDelay >>> 32));
        hash = 67 * hash + Objects.hashCode(this._channels);
        hash = 67 * hash + Objects.hashCode(this._topics);
        hash = 67 * hash + Objects.hashCode(this._dccManager);
        hash = 67 * hash + Arrays.hashCode(this._dccPorts);
        hash = 67 * hash + Objects.hashCode(this._dccInetAddress);
        hash = 67 * hash + (this._autoNickChange ? 1 : 0);
        hash = 67 * hash + (this._verbose ? 1 : 0);
        hash = 67 * hash + Objects.hashCode(this._name);
        hash = 67 * hash + Objects.hashCode(this._nick);
        hash = 67 * hash + Objects.hashCode(this._login);
        hash = 67 * hash + Objects.hashCode(this._version);
        hash = 67 * hash + Objects.hashCode(this._finger);
        hash = 67 * hash + Objects.hashCode(this._channelPrefixes);
        return hash;
    }

    /**
     * Returns a String representation of this object. You may find this useful
     * for debugging purposes, particularly if you are using more than one
     * PircBot instance to achieve multiple server connectivity. The format of
     * this String may change between different versions of PircBot but is
     * currently something of the form      <code>
     *   Version{PircBot x.y.z Java IRC Bot - www.jibble.org}
     *   Connected{true}
     *   Server{irc.dal.net}
     *   Port{6667}
     *   Password{}
     * </code>
     *
     * @since PircBot 0.9.10
     *
     * @return a String representation of this object.
     */
    @Override
    public String toString() {
        return "Version{" + _version + "}"
                + " Connected{" + isConnected() + "}"
                + " Server{" + _server + "}"
                + " Port{" + _port + "}"
                + " Password{" + _password + "}";
    }

    /**
     * Returns an array of all users in the specified channel.
     * <p>
     * There are some important things to note about this method:-
     * <ul>
     * <li>This method may not return a full list of users if you call it before
     * the complete nick list has arrived from the IRC server.
     * </li>
     * <li>If you wish to find out which users are in a channel as soon as you
     * join it, then you should override the onUserList method instead of
     * calling this method, as the onUserList method is only called as soon as
     * the full user list has been received.
     * </li>
     * <li>This method will return immediately, as it does not require any
     * interaction with the IRC server.
     * </li>
     * <li>The bot must be in a channel to be able to know which users are in
     * it.
     * </li>
     * </ul>
     *
     * @since PircBot 1.0.0
     *
     * @param channel The name of the channel to list.
     *
     * @return An array of User objects. This array is empty if we are not in
     * the channel.
     *
     */
    public final ArrayList<User> getUsers(String channel) {
        channel = channel.toLowerCase();
        synchronized (_channels) {
            if (_channels.get(channel) == null) {
                return null;
            }
            return _channels.get(channel).getUserlist();
        }
    }

    /**
     * Returns an array of all channels that we are in. Note that if you call
     * this method immediately after joining a new channel, the new channel may
     * not appear in this array as it is not possible to tell if the join was
     * successful until a response is received from the IRC server.
     *
     * @since PircBot 1.0.0
     *
     * @return A String array containing the names of all channels that we are
     * in.
     */
    public final String[] getChannels() {
        synchronized (_channels) {
            ArrayList<String> channels = new ArrayList<>();
            _channels.keySet().stream().forEach((el) -> {
                channels.add(el);
            });
            return channels.toArray(new String[channels.size()]);
        }
    }

    /**
     * Returns a channel object that this bot is connected to from name. If the
     * bot is not connected to the channel, returns null.
     *
     * @param channel channel to get
     * @return Channel object
     */
    public Channel getChannel(String channel) {
        synchronized (_channels) {
            return this._channels.get(channel);
        }
    }

    /**
     * Disposes of all thread resources used by this PircBot. This may be useful
     * when writing bots or clients that use multiple servers (and therefore
     * multiple PircBot instances) or when integrating a PircBot with an
     * existing program.
     * <p>
     * Each PircBot runs its own threads for dispatching messages from its
     * outgoing message queue and receiving messages from the server. Calling
     * dispose() ensures that these threads are stopped, thus freeing up system
     * resources and allowing the PircBot object to be garbage collected if
     * there are no other references to it.
     * <p>
     * Once a PircBot object has been disposed, it should not be used again.
     * Attempting to use a PircBot that has been disposed may result in
     * unpredictable behaviour.
     *
     * @since 1.2.2
     */
    public synchronized void dispose() {
        //System.out.println("disposing...");
        _outputThread.interrupt();
        _inputThread.dispose();
    }

    /**
     * Add a user to the specified channel in our memory. Overwrite the existing
     * entry if it exists.
     */
    private void addUser(String channel, User user) {
        channel = channel.toLowerCase();
        synchronized (_channels) {
            Channel chan = _channels.get(channel);
            if (chan == null) {
                return;
            }
            chan.addUser(user);
        }
    }

    /**
     * Remove a user from the specified channel in our memory.
     */
    private boolean removeUser(String channel, String nick) {
        channel = channel.toLowerCase();
        synchronized (_channels) {
            Channel chan = _channels.get(channel);
            if (chan == null) {
                return false;
            }
            try {
                chan.removeUser(nick);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    /**
     * Remove a user from all channels in our memory.
     */
    private void removeUser(String nick) {
        synchronized (_channels) {
            _channels.values().stream().forEach((el) -> {
                el.removeUser(nick);
            });
        }
    }

    /**
     * Rename a user if they appear in any of the channels we know about.
     */
    private void renameUser(String oldNick, String newNick) {
        oldNick = oldNick.toLowerCase();
        synchronized (_channels) {
            for (Channel el : _channels.values()) {
                User user = el.getUser(oldNick);
                if (user == null) {
                    continue;
                }
                user.changeName(newNick);
            }
        }
    }

    /**
     * Removes an entire channel from our memory of users.
     */
    private void removeChannel(String channel) {
        channel = channel.toLowerCase();
        synchronized (_channels) {
            _channels.remove(channel);
        }
    }

    /**
     * Removes all channels from our memory of users.
     */
    private void removeAllChannels() {
        synchronized (_channels) {
            _channels.clear();
        }
    }

    protected void updateUserAFK(String channel, String username, boolean afk) {
        synchronized (_channels) {
            User user = _channels.get(channel).getUser(username);
            if (user == null) {
                return;
            }
            user.setAFK(afk);
        }
    }

    protected void updateUserLastMessage(String channel, String username, String lastMessage) {
        synchronized (_channels) {
            User user = _channels.get(channel).getUser(username);
            if (user == null) {
                return;
            }
            user.setPreviousMessage(lastMessage);
        }
    }

    /**
     * Updates a user with Twitch IRC3 tags in all known channels we're
     * connected to and returns the User Object.
     *
     * @param username Username to look for
     * @param color User color info
     * @param subscriber User subscriber
     * @param turbo User turbo
     * @param mod User moderator
     * @param userType Usertype
     * @param emotes emote String
     * @param badges Badges string
     * @param systemMsg Resubscription System  string
     * @param userLogin Lowercase username name
     * @param emoteSets Emoticon Sets string
     * @param messageId Unique identifier for a message.
     * @param roomId ID of the channel.
     * @param consecutiveMonths number of consecutive months the user has subscribed for in a resub notice.
     * @param bits Amount of Bits user has used.
     * @param msgId Twitch's type of notice
     */
    private void updateUser(String username, String color, boolean subscriber, boolean turbo, boolean mod, String userType, long id, long roomId, long consecutiveMonths, String emotes, String badges, String messageId, long bits, String emoteSets, String msgId, String systemMsg, String userLogin) {
        synchronized (_channels) {
            for (Channel el : _channels.values()) {
                User user = el.getUser(username);
                if (user == null) {
                    return;
                }
                user.setColor(color);
                user.setSubscriber(subscriber);
                user.setTurbo(turbo);
                user.setMod(mod);
                user.setUserType(userType);
                user.setId(id);
                user.setEmotes(emotes);
                user.setMsgId(msgId);
                user.setBadges(badges);
                user.setSystemMsg(systemMsg);
                user.setUserLogin(userLogin);
                user.setEmoteSets(emoteSets);
                user.setMessageId(messageId);
                user.setRoomId(roomId);
                user.setConsecutiveMonths(consecutiveMonths);
                user.setBits(bits);
            }
        }
    }

    private void updateUser(String channel, int userMode, String nick) {
        channel = channel.toLowerCase();
        synchronized (_channels) {
            User user = _channels.get(channel).getUser(nick);
            if (user == null) {
                return;
            }
            if (userMode == OP_ADD) {
                user.setOP(true);
            } else if (userMode == OP_REMOVE) {
                user.setOP(false);
            } else if (userMode == VOICE_ADD) {
                user.setVoice(true);
            } else if (userMode == VOICE_REMOVE) {
                user.setVoice(false);
            }
        }
    }
}
