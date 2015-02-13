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
package com.sibvisions.rad.remote.vertx;

import java.io.IOException;
import java.io.OutputStream;

import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.WriteStream;

/**
 * The <code>BufferStream</code> is a simple OutputStream that stores written content in
 * a {@link Buffer}.
 * 
 * @author René Jahn
 */
public class BufferStream extends OutputStream
{
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Class members
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /** the socket. */
    private WriteStream<?> stream;
    
    /** the buffer. */
    private Buffer buffer = new Buffer();
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Initialization
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Creates a new instance of <code>BufferStream</code>.
     * 
     * @param pStream the write stream
     */
    public BufferStream(WriteStream<?> pStream)
    {
        stream = pStream;
    }
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // User-defined methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(int pByte) throws IOException
    {
        buffer.appendByte((byte)pByte);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void flush()
    {
        stream.write(buffer);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void close()
    {
        flush();
    }
    
}   // BufferStream

