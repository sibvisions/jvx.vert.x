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
package com.sibvisions.rad.remote.vertx.io;

import java.io.IOException;
import java.io.InputStream;

import org.vertx.java.core.buffer.Buffer;

/**
 * The <code>SyncedInputStream</code> is a simple {@link InputStream} that syncs the
 * access to received content.
 * 
 * @author René Jahn
 */
public class SyncedInputStream extends InputStream
{
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Class members
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /** the current data buffer. */ 
    private Buffer buffer;

    /** the current position in the data stream. */
    private int iPos;
    
    /** whether the stream is finished. */
    boolean bFinish;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Initialization
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /**
     * Creates a new instance of <code>SyncedInputStream</code>.
     */
    public SyncedInputStream()
    {
        buffer = new Buffer();
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Overwritten methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] pByte, int pOffset, int pLength)
    {
        if (bFinish)
        {
            return -1;
        }

        if (pByte == null) 
        {
            throw new NullPointerException();
        } 
        else if (pOffset < 0 || pLength < 0 || pLength > pByte.length - pOffset) 
        {
            throw new IndexOutOfBoundsException();
        }           
        
        if (pLength == 0)
        {
            return 0;
        }

        int iBufferLength;
        
        synchronized (buffer)
        {
            iBufferLength = buffer.length();
        }
        
        if (iPos == iBufferLength)
        {
            synchronized(this)
            {
                try
                {
                    wait();
                }
                catch (Exception e)
                {
                    //ignore
                }
            }
        }

        synchronized (buffer)
        {
            iBufferLength = buffer.length();
        }
        
        if (iPos == iBufferLength)
        {
            return -1;
        }
        
        int iLength = iBufferLength - iPos;
        
        if (pLength < iLength)
        {
            iLength = pLength;
        }
        
        byte[] bytes = buffer.getBytes(iPos, iPos + iLength);
        
        System.arraycopy(bytes, 0, pByte, pOffset, iLength);

        iPos += iLength;
        
        return iLength;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException
    {
        if (bFinish)
        {
            return -1;
        }

        int iBufferLength;
            
        synchronized (buffer)
        {
            iBufferLength = buffer.length();
        }
        
        if (iPos == iBufferLength)
        {
            synchronized(this)
            {
                try
                {
                    wait();
                }
                catch (Exception e)
                {
                    //ignore
                }
            }
        }
        
        synchronized (buffer)
        {
            iBufferLength = buffer.length();
        }
        
        if (iPos == iBufferLength)
        {
            return -1;
        }
        
        return buffer.getByte(iPos++) & 0xFF;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void close()
    {
        finish();
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // User-defined methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /**
     * Notification about received data.
     * 
     * @param pBuffer the receive buffer
     */
    public void receive(Buffer pBuffer)
    {
        synchronized (buffer)
        {
            buffer.appendBuffer(pBuffer);
        }

        synchronized(this)
        {
            notify();
        }           
    }
    
    /**
     * Finish the stream. It no longer reads bytes.
     */
    public void finish()
    {
        synchronized(this)
        {
            bFinish = true;

            notify();
        }           
    }
    
}   // SyncedInputStream
