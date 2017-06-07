/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.grizzly;

import java.io.IOException;
import org.glassfish.grizzly.strategies.WorkerThreadPoolConfigProducer;
import java.util.concurrent.Executor;

/**
 * <tt>strategy</tt> is responsible for making decision how
 * {@link Runnable} task will be run: in current thread, worker thread.
 *
 * <tt>strategy</tt> can make any other processing decisions.
 * 
 *
 */
public interface IOStrategy extends WorkerThreadPoolConfigProducer {
    /**
     * The {@link org.glassfish.grizzly.nio.SelectorRunner} will invoke this
     * method to allow the strategy implementation to decide how the
     * {@link IOEvent} will be handled.
     *
     * @param connection the {@link Connection} upon which the provided
     *  {@link IOEvent} occurred.
     * @param ioEvent the {@link IOEvent} that triggered execution of this
     *  <code>strategy</code>
     *
     * @return <tt>true</tt>, if this thread should keep processing IOEvents on
     * the current and other Connections, or <tt>false</tt> if this thread
     * should hand-off the farther IOEvent processing on any Connections,
     * which means IOStrategy is becoming responsible for continuing IOEvent
     * processing (possibly starting new thread, which will handle IOEvents).
     */
    boolean executeIOEvent(Connection connection, IOEvent ioEvent)
            throws IOException;
    
    /**
     * The {@link org.glassfish.grizzly.nio.SelectorRunner} will invoke this
     * method to allow the strategy implementation to decide how the
     * {@link IOEvent} will be handled.
     *
     * @param connection the {@link Connection} upon which the provided
     *  {@link IOEvent} occurred.
     * @param ioEvent the {@link IOEvent} that triggered execution of this
     *  <code>strategy</code>
     * @param lifeCycleListener I/O event life-cycle listener.
     *
     * @return <tt>true</tt>, if this thread should keep processing IOEvents on
     * the current and other Connections, or <tt>false</tt> if this thread
     * should hand-off the farther IOEvent processing on any Connections,
     * which means IOStrategy is becoming responsible for continuing IOEvent
     * processing (possibly starting new thread, which will handle IOEvents).
     */
    boolean executeIOEvent(Connection connection, IOEvent ioEvent,
            EventLifeCycleListener lifeCycleListener) throws IOException;

    /**
     * The {@link org.glassfish.grizzly.nio.SelectorRunner} will invoke this
     * method to allow the strategy implementation to decide how the
     * {@link IOEvent} will be handled.
     *
     * @param connection the {@link Connection} upon which the provided
     *  {@link IOEvent} occurred.
     * @param ioEvent the {@link IOEvent} that triggered execution of this
     *  <code>strategy</code>
     * @param listener to be notified about <tt>IOStrategy</tt> decision.
     *
     * @return <tt>true</tt>, if this thread should keep processing IOEvents on
     * the current and other Connections, or <tt>false</tt> if this thread
     * should hand-off the farther IOEvent processing on any Connections,
     * which means IOStrategy is becoming responsible for continuing IOEvent
     * processing (possibly starting new thread, which will handle IOEvents).
     */
    boolean executeIOEvent(Connection connection, IOEvent ioEvent,
            DecisionListener listener) throws IOException;
    
    /**
     * Listener, which would be called by {@link IOStrategy} implementation
     * notifying whether submitted {@link IOEvent} will be executed synchronously
     * in the current thread or asynchronously in the worker thread.
     */
    public interface DecisionListener {
        public EventLifeCycleListener goSync(Connection connection, IOEvent ioEvent) throws IOException;
        public EventLifeCycleListener goAsync(Connection connection, IOEvent ioEvent) throws IOException;
    }

    /**
     * Returns an {@link Executor} to be used to run given <tt>ioEvent</tt>
     * processing for the given <tt>connection</tt>. A <tt>null</tt> value will
     * be returned if the <tt>ioEvent</tt> should be executed in the kernel thread.
     * 
     * @param connection
     * @param ioEvent
     * @return an {@link Executor} to be used to run given <tt>ioEvent</tt>
     * processing for the given <tt>connection</tt>
     */
    Executor getThreadPoolFor(Connection connection, IOEvent ioEvent);
}
