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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.WriteStream;

import com.sibvisions.rad.remote.vertx.BufferStream;
import com.sibvisions.rad.remote.vertx.SyncedInputStream;
import com.sibvisions.rad.server.IRequest;
import com.sibvisions.rad.server.IResponse;
import com.sibvisions.rad.server.Server;
import com.sibvisions.util.ThreadHandler;

/**
 * The <code>DataHandler</code> receives data from the client and delegates command
 * execution to the JVx server.
 * 
 * @author René Jahn
 */
public class DataHandler implements Handler<Buffer>
{
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Class members
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /** the server processing thread. */
    private Thread thServer;
    
    /** the JVx server. */
    private Server server;
    
    /** the socket. */
    private WriteStream<?> stream;
    
    /** the input stream. */
    private SyncedInputStream inputStream;

    /** the sync object for procesing. */
    private Object sync = new Object();

    /** the sync object for stream access. */
    private Object syncStream = new Object();
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Initialization
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /**
     * Creates a new instance of <code>DataHandler</code>.
     * 
     * @param pServer the JVx server
     * @param pStream the write stream
     */
    public DataHandler(Server pServer, WriteStream<?> pStream)
    {
        server = pServer;
        stream = pStream;
        
        inputStream = new SyncedInputStream();
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
            synchronized(sync)
            {
                sync.notify();
            }
        }
        else
        {
            thServer = new Thread(new Runnable()
            {
                public void run()
                {
                    while (!ThreadHandler.isStopped(thServer))
                    {
                        try
                        {
                            server.process(new Request(), new Response());
                        }
                        catch (Exception e)
                        {
                            thServer = ThreadHandler.stop(thServer);
                            
                            throw new RuntimeException(e);
                        }
                        
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
            });
            
            thServer.setName("DataHandler@" + System.identityHashCode(this));
            thServer.start();
        }
    }
    
    /**
     * Closes the handler and stops processing of commands.
     */
    public void close()
    {
        thServer = ThreadHandler.stop(thServer);

        synchronized (sync)
        {
            sync.notify();
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
    private final class Response implements IResponse
    {
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Class members
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        
        /** whether the response is closed. */
        private boolean bClosed;
        
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Interface implementation
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        /**
         * {@inheritDoc}
         */
        public void setProperty(String pKey, Object pValue)
        {
        }
        
        /**
         * {@inheritDoc}
         */
        public OutputStream getOutputStream() throws IOException
        {
            bClosed = false;

            return new BufferStream(stream);
        }

        /**
         * {@inheritDoc}
         */
        public void close()
        {
            bClosed = true;
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean isClosed()
        {
            return bClosed;
        }
        
    }   // Response
    
}   // DataHandler
