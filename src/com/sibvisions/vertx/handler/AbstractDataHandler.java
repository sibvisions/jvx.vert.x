/*
 * Copyright 2015 SIB Visions GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 * History
 *
 * 07.02.2015 - [JR] - creation
 */
package com.sibvisions.vertx.handler;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;

import com.sibvisions.rad.remote.vertx.io.BufferOutputStream;
import com.sibvisions.rad.remote.vertx.io.SyncedInputStream;
import com.sibvisions.rad.server.IRequest;
import com.sibvisions.rad.server.IResponse;
import com.sibvisions.rad.server.Server;
import com.sibvisions.util.ThreadHandler;

/**
 * The <code>AbstractDataHandler</code> receives data from the client and delegates command
 * execution to the JVx server.
 * 
 * @author René Jahn
 */
public abstract class AbstractDataHandler implements Handler<Buffer>
{
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Class members
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /** the server processing thread. */
    private Thread thServer;
    
    /** the JVx server. */
    private Server server;
    
    /** the socket. */
    private WriteStream<Buffer> stream;
    
    /** the input stream. */
    private SyncedInputStream inputStream;

    /** the sync object for procesing. */
    private Object sync = new Object();

    /** the sync object for stream access. */
    private Object syncStream = new Object();
    
    /** whether to wait for end (endless processing). */
    private boolean bWaitForEnd;
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Initialization
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /**
     * Creates a new instance of <code>AbstractDataHandler</code>.
     * 
     * @param pServer the JVx server
     * @param pStream the write stream
     */
    public AbstractDataHandler(Server pServer, WriteStream<Buffer> pStream)
    {
        this(pServer, pStream, true);
    }

    /**
     * Creates a new instance of <code>AbstractDataHandler</code>.
     * 
     * @param pServer the JVx server
     * @param pStream the write stream
     * @param pWaitForEnd <code>true</code> to wait for end of processing, <code>false</code> to continue processing
     */
    protected AbstractDataHandler(Server pServer, WriteStream<Buffer> pStream, boolean pWaitForEnd)
    {
        server = pServer;
        stream = pStream;
        
        inputStream = new SyncedInputStream();
        
        bWaitForEnd = pWaitForEnd;
    }
    
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Interface implementation
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * {@inheritDoc}
     */
    public void handle(Buffer pBuffer)
    {
        synchronized (syncStream)
        {
            inputStream.receive(pBuffer);
        }
        
        if (thServer != null)
        {
            if (bWaitForEnd)
            {
                synchronized(sync)
                {
                    sync.notify();
                }
            }
        }
        else
        {
            thServer = new Thread(new Runnable()
            {
                public void run()
                {
                    if (bWaitForEnd)
                    {
                        while (!ThreadHandler.isStopped(thServer))
                        {
                            process();
                            
                            synchronized (sync)
                            {
                                try
                                {
                                    sync.wait();
                                }
                                catch (Exception e)
                                {
                                    //ignore
                                }
                            }
                        }
                    }
                    else
                    {
                        try
                        {
                            process();
                        }
                        finally
                        {
                            thServer = null;
                        }
                    }
                }
            });
            
            thServer.setName("AbstractDataHandler@" + System.identityHashCode(this));
            thServer.start();
        }
    }
    
    /**
     * Forwards processing to the server.
     */
    protected void process()
    {
        try
        {
            server.process(createRequest(), createResponse());
        }
        catch (Exception e)
        {
            thServer = ThreadHandler.stop(thServer);
            
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Closes the handler and stops processing of commands.
     */
    public void close()
    {
        if (bWaitForEnd)
        {
            thServer = ThreadHandler.stop(thServer);
            
            synchronized (sync)
            {
                sync.notify();
            }
        }
    }
    
    /**
     * Creates a new request.
     * 
     * @return the request
     */
    protected IRequest createRequest()
    {
        return new Request();
    }
    
    /**
     * Creates a new response.
     * 
     * @return the response
     */
    protected IResponse createResponse()
    {
        return new Response();
    }
    
    /**
     * Gets access to the server.
     * 
     * @return the server
     */
    protected Server getServer()
    {
        return server;
    }
    
    /**
     * Gets access to the write stream.
     * 
     * @return the stream
     */
    protected WriteStream<?> getStream()
    {
        return stream;
    }
    
    /**
     * Reads a single byte from the input stream.
     * 
     * @return the read byte
     * @throws IOException if reading failed
     */
    protected InputStream getInputStream() throws IOException
    {
        synchronized (syncStream)
        {
            return inputStream;
        }        
    }
    
    //****************************************************************
    // Subclass definition
    //****************************************************************

    /**
     * The <code>Request</code> is a simple {@link IRequest} implementation
     * for accessing the socket input stream.
     * 
     * @author René Jahn
     */
    private final class Request implements IRequest
    {
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Class members
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        /** whether the request is close. */
        private boolean bClosed;

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Interface implementation
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        
        /**
         * {@inheritDoc}
         */
        public Object getProperty(String pKey)
        {
            return null;
        }
        
        /**
         * {@inheritDoc}
         */
        public Hashtable<String, Object> getProperties()
        {
            return null;
        }
        
        /**
         * {@inheritDoc}
         */
        public InputStream getInputStream() throws IOException
        {
            bClosed = false;
            
            synchronized (syncStream)
            {
                return inputStream;
            }
        }
        
        /**
         * {@inheritDoc}
         */
        public void close()
        {
            synchronized (syncStream)
            {
                inputStream.close();
                    
                inputStream = new SyncedInputStream();
            }
            
            bClosed = true;
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean isClosed()
        {
            return bClosed;
        }
        
    }   // Request
    
    /**
     * The <code>Response</code> is a simple {@link IResponse} for accessing the
     * socket output stream.
     * 
     * @author René Jahn
     */
    private final class Response extends AbstractResponse
    {
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Abstract methods implementation
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        /**
         * {@inheritDoc}
         */
        protected OutputStream createOutputStream()
        {
            return new BufferOutputStream(stream);
        }
        
    }   // Response
    
}   // AbstractDataHandler
