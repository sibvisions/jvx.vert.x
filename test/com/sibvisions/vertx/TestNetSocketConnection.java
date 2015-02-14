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

import javax.rad.remote.IConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.sibvisions.rad.remote.BaseConnectionTest;
import com.sibvisions.rad.remote.ISerializer;
import com.sibvisions.rad.remote.vertx.NetSocketConnection;

/**
 * Tests the functionality of {@link NetSocketServer} via {@link NetSocketConnection}.
 *  
 * @author René Jahn
 */
public class TestNetSocketConnection extends BaseConnectionTest
{
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Class members
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /** socket server. */
    private static NetSocketServer server;
    
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Initialization
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	/**
	 * Starts server.
	 */
	@BeforeClass
	public static void beforeClass()
	{
	    server = new NetSocketServer();
	    server.setPort(8888);
	    server.setInterface("127.0.0.1");
	    server.start();
	}	

    /**
     * Stops server.
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
	
	@Override
	protected IConnection createConnection(ISerializer pSerializer) throws Throwable
	{
		return new NetSocketConnection("127.0.0.1", 8888);
	}

}	// TestNetSocketConnection
