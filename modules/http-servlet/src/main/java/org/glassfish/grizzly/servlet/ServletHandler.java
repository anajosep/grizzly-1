/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.grizzly.servlet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.Cookie;
import org.glassfish.grizzly.http.Note;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.http.server.AfterServiceListener;
import org.glassfish.grizzly.http.server.Constants;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.util.ClassLoaderUtil;
import org.glassfish.grizzly.http.server.util.DispatcherHelper;
import org.glassfish.grizzly.http.server.util.Globals;
import org.glassfish.grizzly.http.server.util.MappingData;
import org.glassfish.grizzly.http.util.CharChunk;
import org.glassfish.grizzly.http.util.HttpRequestURIDecoder;

import static org.glassfish.grizzly.servlet.DispatcherType.REQUEST;

/**
 * HttpHandler implementation that provides an entry point for processing
 * a Servlet request.
 * 
 * @author Jeanfrancois Arcand
 */
public class ServletHandler extends HttpHandler {

    private static final Logger LOGGER = Grizzly.logger(ServletHandler.class);

    static final Note<HttpServletRequestImpl> SERVLET_REQUEST_NOTE =
            Request.createNote(HttpServletRequestImpl.class.getName());
    static final Note<HttpServletResponseImpl> SERVLET_RESPONSE_NOTE =
            Request.createNote(HttpServletResponseImpl.class.getName());

    static final ServletAfterServiceListener servletAfterServiceListener =
            new ServletAfterServiceListener();

    protected String servletClassName;
    protected Class<? extends Servlet> servletClass;
    protected volatile Servlet servletInstance = null;
    private String contextPath = "";
    private final Object lock = new Object();


    /**
     * The {@link WebappContext}
     */
    private final WebappContext servletCtx;
    /**
     * The {@link ServletConfigImpl}
     */
    private ServletConfigImpl servletConfig;
    /**
     * Holder for our configured properties.
     */
    protected final Map<String, Object> properties = new HashMap<String, Object>();
    /**
     * Initialize the {@link ServletContext}
     */
    protected boolean initialize = true;
    protected ClassLoader classLoader;

    protected ExpectationHandler expectationHandler;

    private FilterChainFactory filterChainFactory;

    // ------------------------------------------------------------ Constructors


    protected ServletHandler(final ServletConfigImpl servletConfig) {
        this.servletConfig = servletConfig;
        servletCtx = (WebappContext) servletConfig.getServletContext();
    }

    // ---------------------------------------------------------- Public Methods

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        try {
            configureServletEnv();
            
            if (initialize) {
                configureClassLoader(new File(servletCtx.getBasePath()).getCanonicalPath());
            }

        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "start", t);
        }
    }

    /**
     * Create a {@link java.net.URLClassLoader} which has the capability of
     * loading classes jar under an exploded war application.
     *
     * @param applicationPath Application class path.
     * @throws java.io.IOException I/O error.
     */
    protected void configureClassLoader(String applicationPath) throws IOException {
        if (classLoader == null) {
            classLoader = ClassLoaderUtil.createURLClassLoader(applicationPath);
        }
    }
    
    /**
     * Override parent's {@link HttpHandler#sendAcknowledgment(org.glassfish.grizzly.http.server.Request, org.glassfish.grizzly.http.server.Response)}
     * to let {@link ExpectationHandler} (if one is registered) process the expectation.
     */
    @Override
    protected boolean sendAcknowledgment(Request request, Response response)
            throws IOException {
        if (expectationHandler == null) {
            return super.sendAcknowledgment(request, response);
        }
        
        return true;
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void service(Request request, Response response) throws Exception {
        if (classLoader != null) {
            final ClassLoader prevClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            try {
                doServletService(request, response);
            } finally {
                Thread.currentThread().setContextClassLoader(prevClassLoader);
            }
        } else {
            doServletService(request, response);
        }
    }

    protected void doServletService(final Request request, final Response response) {
        try {
            final String uri = request.getRequestURI();

            // The request is not for us.
            if (contextPath.length() > 0 && !uri.startsWith(contextPath)) {
                customizeErrorPage(response, "Resource Not Found", 404);
                return;
            }

            final HttpServletRequestImpl servletRequest = HttpServletRequestImpl.create();
            final HttpServletResponseImpl servletResponse = HttpServletResponseImpl.create();

            setPathData(request, servletRequest);
            servletRequest.initialize(request);
            servletResponse.initialize(response);

            request.setNote(SERVLET_REQUEST_NOTE, servletRequest);
            request.setNote(SERVLET_RESPONSE_NOTE, servletResponse);

            request.addAfterServiceListener(servletAfterServiceListener);
            
            final Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if (Constants.SESSION_COOKIE_NAME.equals(c.getName())) {
                        request.setRequestedSessionId(c.getValue());
                        request.setRequestedSessionCookie(true);
                        break;
                    }
                }
            }

            loadServlet();

            servletRequest.setContextImpl(servletCtx);
            servletRequest.initSession();
            setDispatcherPath(request, getCombinedPath(servletRequest));

            //TODO: Make this configurable.
            servletResponse.addHeader("server", "grizzly/" + Grizzly.getDotedVersion());
            
            if (expectationHandler != null) {
                final AckActionImpl ackAction = new AckActionImpl(response);
                expectationHandler.onExpectAcknowledgement(servletRequest,
                        servletResponse, ackAction);
                if (!ackAction.isAcknowledged()) {
                    ackAction.acknowledge();
                } else if (ackAction.isFailAcknowledgement()) {
                    return;
                }
            }
            
            FilterChainInvoker filterChain = getFilterChain(request);
            if (filterChain != null) {
                filterChain.invokeFilterChain(servletRequest, servletResponse);
            } else {
                servletInstance.service(servletRequest, servletResponse);
            }
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, "service exception:", ex);
            customizeErrorPage(response, "Internal Error", 500);
        }
    }

    protected FilterChainInvoker getFilterChain(Request request) {
        if (filterChainFactory != null) {
            return filterChainFactory.createFilterChain(request, servletInstance, REQUEST);
        }
        return null;
    }

    private void setDispatcherPath(final Request request, final String path) {
        request.setAttribute(Globals.DISPATCHER_REQUEST_PATH_ATTR, path);
    }

    /**
     * Combines the servletPath and the pathInfo.
     *
     * If pathInfo is <code>null</code>, it is ignored. If servletPath
     * is <code>null</code>, then <code>null</code> is returned.
     *
     * @return The combined path with pathInfo appended to servletInfo
     */
    private String getCombinedPath(final HttpServletRequest request) {
        if (request.getServletPath() == null) {
            return null;
        }
        if (request.getPathInfo() == null) {
            return request.getServletPath();
        }
        return request.getServletPath() + request.getPathInfo();
    }

    protected void setPathData(final Request from,
                               final HttpServletRequestImpl to) {

        final MappingData data = from.obtainMappingData();
        to.setServletPath(data.wrapperPath.toString());
        to.setPathInfo(data.pathInfo.toString());

    }

    void doServletService(final ServletRequest servletRequest,
                          final ServletResponse servletResponse,
                          final DispatcherType dispatcherType)
            throws IOException, ServletException {
        try {
            loadServlet();
            FilterChainImpl filterChain =
                    filterChainFactory.createFilterChain(servletRequest,
                                                         servletInstance,
                                                         dispatcherType);
            if (filterChain != null) {
                filterChain.invokeFilterChain(servletRequest, servletResponse);
            } else {
                servletInstance.service(servletRequest, servletResponse);
            }
        } catch (ServletException se) {
            LOGGER.log(Level.SEVERE, "service exception:", se);
            throw se;
        } catch (IOException ie) {
            LOGGER.log(Level.SEVERE, "service exception:", ie);
            throw ie;
        }
    }

    /**
     * Customize the error page returned to the client.
     * @param response  the {@link Response}
     * @param message   the HTTP error message
     * @param errorCode the error code.
     */
    public void customizeErrorPage(Response response, String message, int errorCode) {
        response.setStatus(errorCode, message);
        response.setContentType("text/html");
        try {
            response.getWriter().write("<html><body><h1>" + message
                    + "</h1></body></html>");
            response.getWriter().flush();
        } catch (IOException ex) {
            // We are in a very bad shape. Ignore.
        }
    }

    /**
     * Load a {@link Servlet} instance.
     *
     * @throws javax.servlet.ServletException If failed to
     * {@link Servlet#init(javax.servlet.ServletConfig)}.
     */
    protected void loadServlet() throws ServletException {

        if (servletInstance == null) {
            synchronized (lock) {
                if (servletInstance == null) {
                    if (servletClassName != null) {
                        servletInstance = (Servlet) ClassLoaderUtil.load(servletClassName);
                    } else {
                        try {
                            servletInstance = servletClass.newInstance();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    LOGGER.log(Level.INFO, "Loading Servlet: {0}", servletInstance.getClass().getName());
                    servletInstance.init(servletConfig);
                }
            }
        }

    }

    /**
     * Configure the {@link WebappContext}
     * and {@link ServletConfigImpl}
     * 
     * @throws javax.servlet.ServletException Error while configuring
     * {@link Servlet}.
     */
    protected void configureServletEnv() throws ServletException {
        if (contextPath.length() > 0) {
            final CharChunk cc = new CharChunk();
            char[] ch = contextPath.toCharArray();
            cc.setChars(ch, 0, ch.length);
            HttpRequestURIDecoder.normalizeChars(cc, false);
            contextPath = cc.toString();
        }

        if ("".equals(contextPath)) {
            contextPath = "";
        }

    }

    /**
     * Return the {@link Servlet} instance used by this {@link ServletHandler}
     * @return {@link Servlet} instance.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public Servlet getServletInstance() {
        return servletInstance;
    }

    /**
     * Set the {@link Servlet} instance used by this {@link ServletHandler}
     * @param servletInstance an instance of Servlet.
     */
    protected void setServletInstance(Servlet servletInstance) {
        this.servletInstance = servletInstance;
    }

    protected void setServletClassName(final String servletClassName) {
        this.servletClassName = servletClassName;
    }

    protected void setServletClass(final Class<? extends Servlet> servletClass) {
        this.servletClass = servletClass;
    }

    /**
     *
     * Returns the portion of the request URI that indicates the context
     * of the request. The context path always comes first in a request
     * URI. The path starts with a "/" character but does not end with a "/"
     * character. For servlets in the default (root) context, this method
     * returns "". The container does not decode this string.
     *
     * <p>It is possible that a servlet container may match a context by
     * more than one context path. In such cases this method will return the
     * actual context path used by the request and it may differ from the
     * path returned by the
     * {@link javax.servlet.ServletContext#getContextPath()} method.
     * The context path returned by
     * {@link javax.servlet.ServletContext#getContextPath()}
     * should be considered as the prime or preferred context path of the
     * application.
     *
     * @return		a <code>String</code> specifying the
     *			portion of the request URI that indicates the context
     *			of the request
     *
     * @see javax.servlet.ServletContext#getContextPath()
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * Programmatically set the context path of the Servlet.
     *
     * @param contextPath Context path.
     */
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    /**
     * Destroy this Servlet and its associated
     * {@link javax.servlet.ServletContextListener}
     */
    @Override
    public void destroy() {
        if (classLoader != null) {
            ClassLoader prevClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            try {
                super.destroy();

                if (servletInstance != null) {
                    servletInstance.destroy();
                    servletInstance = null;
                }
            } finally {
                Thread.currentThread().setContextClassLoader(prevClassLoader);
            }
        } else {
            super.destroy();
        }
    }


    protected WebappContext getServletCtx() {
        return servletCtx;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public ServletConfigImpl getServletConfig() {
        return servletConfig;
    }

    @Override
    public String getName() {
        return servletConfig.getServletName();
    }

    /**
     * Get the {@link ExpectationHandler} responsible for processing
     * <tt>Expect:</tt> header (for example "Expect: 100-Continue").
     * 
     * @return the {@link ExpectationHandler} responsible for processing
     * <tt>Expect:</tt> header (for example "Expect: 100-Continue").
     */
    public ExpectationHandler getExpectationHandler() {
        return expectationHandler;
    }

    /**
     * Set the {@link ExpectationHandler} responsible for processing
     * <tt>Expect:</tt> header (for example "Expect: 100-Continue").
     * 
     * @param expectationHandler  the {@link ExpectationHandler} responsible
     * for processing <tt>Expect:</tt> header (for example "Expect: 100-Continue").
     */
    public void setExpectationHandler(ExpectationHandler expectationHandler) {
        this.expectationHandler = expectationHandler;
    }
    
    @Override
    protected void setDispatcherHelper( final DispatcherHelper dispatcherHelper ) {
        servletCtx.setDispatcherHelper( dispatcherHelper );
    }

    protected void setFilterChainFactory(final FilterChainFactory filterChainFactory) {
        this.filterChainFactory = filterChainFactory;
    }

    // ---------------------------------------------------------- Nested Classes

    /**
     * AfterServiceListener, which is responsible for recycle servlet request and response
     * objects.
     */
    static final class ServletAfterServiceListener implements AfterServiceListener {

        @Override
        public void onAfterService(final Request request) {
            final HttpServletRequestImpl servletRequest = request.getNote(SERVLET_REQUEST_NOTE);
            final HttpServletResponseImpl servletResponse = request.getNote(SERVLET_RESPONSE_NOTE);

            if (servletRequest != null) {
                servletRequest.recycle();
                servletResponse.recycle();
            }
        }
    }
    
    static final class AckActionImpl implements ExpectationHandler.AckAction {
        private boolean isAcknowledged;
        private boolean isFailAcknowledgement;
        
        private final Response response;

        private AckActionImpl(final Response response) {
            this.response = response;
        }
        
        @Override
        public void acknowledge() throws IOException {
            if (isAcknowledged) {
                throw new IllegalStateException("Already acknowledged");
            }
            
            isAcknowledged = true;
            response.setStatus(HttpStatus.CONINTUE_100);
            response.sendAcknowledgement();
        }

        @Override
        public void fail() throws IOException {
            if (isAcknowledged) {
                throw new IllegalStateException("Already acknowledged");
            }
            isAcknowledged = true;
            isFailAcknowledgement = true;
            
            response.setStatus(HttpStatus.EXPECTATION_FAILED_417);
            response.finish();
        }

        public boolean isAcknowledged() {
            return isAcknowledged;
        }

        public boolean isFailAcknowledgement() {
            return isFailAcknowledgement;
        }
    }
}
