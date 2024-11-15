/*
 * Copyright 2012 SIB Visions GmbH
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
 * 29.12.2012 - [JR] - creation
 */
package com.sibvisions.vertx;

import jvx.rad.remote.IConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.sibvisions.rad.remote.ISerializer;
import com.sibvisions.rad.remote.http.HttpConnection;

/**
 * Tests the functionality of {@link HttpServer} via {@link HttpConnection}.
 *  
 * @author René Jahn
 */
public class TestHttpConnection extends com.sibvisions.rad.remote.TestHttpConnection
{
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Class members
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /** the server. */
    private static HttpServer server;
    
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Initialization
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	/**
	 * Apply the configuration for all tests.
	 */
	@BeforeClass
	public static void beforeClass()
	{
		server = new HttpServer();
		server.setPort(8080);
		server.start();
	}	

	/**
     * Apply the configuration for all tests.
     */
    @AfterClass
    public static void afterClass()
    {
        if (server != null)
        {
            server.stop();
        }
    }
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Overwritten methods
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
    /**
     * {@inheritDoc}
     */
    @Override
	protected IConnection createConnection(ISerializer pSerializer) throws Throwable
	{
        HttpConnection con = new HttpConnection("http://localhost:8080/services/Server");
        con.setUploadURL(con.getServletURL().replace("/Server", "/Upload"));
        con.setDownloadURL(con.getServletURL().replace("/Server", "/Download"));
        con.setRetryCount(0);
        
        return con;
	}

}	// TestHttpConnection
