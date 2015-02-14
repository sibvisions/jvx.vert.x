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
package com.sibvisions.vertx.io;

import java.io.IOException;

import org.vertx.java.core.http.HttpServerResponse;

import com.sibvisions.rad.remote.vertx.io.BufferOutputStream;

/**
 * The <code>HttpResponseOutputStream</code> is a BufferOutputStream that supports
 * chunked write operations.
 * 
 * @author René Jahn
 */
public class HttpResponseOutputStream extends BufferOutputStream
{
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Initialization
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Creates a new instance of <code>HttpResponseOutputStream</code>.
     * 
     * @param pResponse the http response
     */
    public HttpResponseOutputStream(HttpServerResponse pResponse)
    {
        super(pResponse);
    }
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Overwritten methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void write(int pByte) throws IOException
    {
        if (isChunked())
        {
            super.write(pByte);
        }
        else
        {
            buffer.appendByte((byte)pByte);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void flush()
    {
        if (isChunked())
        {
            super.flush();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void close()
    {
        if (isChunked())
        {
            super.close();
            
            ((HttpServerResponse)stream).end();
        }
        else
        {
            ((HttpServerResponse)stream).end(buffer);
        }
    }    
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // User-defined methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Gets whether the response stream is chunked.
     * 
     * @return <code>true</code> if chunked, <code>false</code> otherwise
     */
    private boolean isChunked()
    {
        return ((HttpServerResponse)stream).isChunked();        
    }
    
}   // HttpResponseOutputStream

