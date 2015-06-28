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
 * 14.02.2015 - [JR] - creation
 */
package com.sibvisions.vertx.handler;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.streams.WriteStream;

import java.io.OutputStream;

import com.sibvisions.rad.server.IResponse;
import com.sibvisions.rad.server.Server;
import com.sibvisions.vertx.io.HttpResponseOutputStream;

/**
 * The <code>HttpDataHandler</code> extends the {@link AbstractDataHandler} and uses a {@link HttpResponseOutputStream}
 * for handling the response.
 * 
 * @author René Jahn
 */
public class HttpDataHandler extends AbstractDataHandler
{
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Initialization
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /**
     * Creates a new instance of <code>HttpDataHandler</code>.
     * 
     * @param pServer the JVx server
     * @param pStream the write stream
     */
    public HttpDataHandler(Server pServer, WriteStream<Buffer> pStream)
    {
        super(pServer, pStream, false);
    }
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Overwritten methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected IResponse createResponse()
    {
        return new Response();
    }
    
    //****************************************************************
    // Subclass definition
    //****************************************************************

    /**
     * The <code>Response</code> is a simple {@link IResponse} for accessing the
     * socket output stream.
     * 
     * @author René Jahn
     */
    private final class Response extends AbstractResponse
    {
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Class members
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        /** the internal output stream. */
        private HttpResponseOutputStream outputStream;
        
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Abstract methods implementation
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        /**
         * {@inheritDoc}
         */
        @Override
        protected synchronized OutputStream createOutputStream()
        {
            if (outputStream != null)
            {
                outputStream.close();
            }
            
            outputStream = new HttpResponseOutputStream((HttpServerResponse)getStream()); 
            
            return outputStream; 
        }

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Overwritten methods
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        
        /**
         * {@inheritDoc}
         */
        public synchronized void close()
        {
            outputStream.close();
            
            super.close();
        }
        
    }   // Response    
    
}   // HttpDataHandler
