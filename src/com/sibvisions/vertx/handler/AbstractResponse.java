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

import java.io.IOException;
import java.io.OutputStream;

import com.sibvisions.rad.server.IResponse;

/**
 * The <code>AbstractResponse</code> is a simple {@link IResponse} for accessing an
 * output stream.
 * 
 * @author René Jahn
 */
abstract class AbstractResponse implements IResponse
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
        
        return createOutputStream();
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

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Abstract methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Creates a new {@link OutputStream}.
     * 
     * @return the stream
     */
    protected abstract OutputStream createOutputStream();
    
}   // AbstractResponse
