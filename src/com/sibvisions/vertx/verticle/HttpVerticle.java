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
 * 27.01.2015 - [JR] - creation
 */
package com.sibvisions.vertx.verticle;

import io.vertx.core.AbstractVerticle;

import com.sibvisions.vertx.HttpServer;

/**
 * The <code>HttpVerticle</code> is the {@link io.vertx.core.Verticle} for {@link HttpServer}.
 * 
 * @author René Jahn
 */
public class HttpVerticle extends AbstractVerticle
{
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Class members
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /** the server. */
    private HttpServer server;
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // User-defined methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * {@inheritDoc}
     */
    @Override
    public void start()
    {
        server = new HttpServer(vertx);
        server.start();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void stop()
    {
        if (server != null)
        {
            server.stop();
        }
    }
    
}   // HttpVerticle
