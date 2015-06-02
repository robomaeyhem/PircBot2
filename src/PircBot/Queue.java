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

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Queue is a definition of a data structure that may act as a queue - that is,
 * data can be added to one end of the queue and data can be requested from the
 * head end of the queue. This class is thread safe for multiple producers and a
 * single consumer. The next() method will block until there is data in the
 * queue.
 *
 * This has now been modified so that it is compatible with the earlier JDK1.1
 * in order to be suitable for running on mobile appliances. This means
 * replacing the LinkedList with a Vector, which is hardly ideal, but this Queue
 * is typically only polled every second before dispatching messages.
 *
 * @author Paul James Mutton,
 * <a href="http://www.jibble.org/">http://www.jibble.org/</a>
 * @version 1.5.0 (Build time: Mon Dec 14 20:07:17 2009)
 */
public class Queue {

    private final ConcurrentLinkedQueue _queue;
    private int size;

    /**
     * Constructs a Queue object of unlimited size.
     */
    public Queue() {
        _queue = new ConcurrentLinkedQueue();
        size = Integer.MAX_VALUE;
    }

    /**
     * Makes a new Queue with a limit on the amount of Messages (PRIVMSG) it can
     * hold.
     *
     * @param size Message Limit
     */
    public Queue(int size) {
        _queue = new ConcurrentLinkedQueue();
        this.size = size;
    }

    /**
     * Checks the Queue to see if it contains the parameter.
     *
     * @param o Object to check for
     * @return True if contained in queue, false otherwise
     */
    public boolean contains(Object o) {
        synchronized (_queue) {
            Object[] array = _queue.toArray();
            for (Object el : array) {
                if (el.equals(o)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Adds an Object to the end of the Queue.
     *
     * @param o The Object to be added to the Queue.
     */
    public void add(Object o) {
        synchronized (_queue) {
            if (o.toString().startsWith("PRIVMSG")) {
                if (messageCount() >= this.size) {
                    _queue.notify();
                    return;
                }
            }
            _queue.add(o);
            _queue.notify();

        }
    }
    
    public int messageCount() {
        int count = 0;
        synchronized (_queue) {
            for (Object el : _queue.toArray()) {
                String a = el.toString();
                if (a.startsWith("PRIVMSG ")) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Returns the Object at the front of the Queue. This Object is then removed
     * from the Queue. If the Queue is empty, then this method shall block until
     * there is an Object in the Queue to return.
     *
     * @return The next item from the front of the queue.
     */
    public Object next() {

        Object o = null;

        // Block if the Queue is empty.
        synchronized (_queue) {
            if (_queue.isEmpty()) {
                try {
                    _queue.wait();
                } catch (InterruptedException e) {
                    return null;
                }
            }

            // Return the Object.
            try {
                o = _queue.poll();
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new InternalError("Race hazard in Queue object.");
            }
        }

        return o;
    }

    /**
     * Returns true if the Queue is not empty. If another Thread empties the
     * Queue before <b>next()</b> is called, then the call to <b>next()</b>
     * shall block until the Queue has been populated again.
     *
     * @return True only if the Queue not empty.
     */
    public boolean hasNext() {
        return (this.size() != 0);
    }

    /**
     * Clears the contents of the Queue.
     */
    public void clear() {
        synchronized (_queue) {
            _queue.clear();
        }
    }

    /**
     * Returns the size of the Queue.
     *
     * @return The current size of the queue.
     */
    public int size() {
        return _queue.size();
    }

    /**
     * Sets the size of the message queue (PRIVMSG)
     *
     * @param size Size to set
     */
    public void setMessageSize(int size) {
        this.size = size;
    }

}
