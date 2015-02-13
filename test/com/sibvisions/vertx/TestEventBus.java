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

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import com.sibvisions.util.type.StringUtil;

/**
 * Tests the functionality of eventbus handlers. This test class starts a clustered Vertx instance.
 * @author René Jahn
 */
public class TestEventBus
{
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Class members
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/** the Vertx instance. */
	private static Vertx vertx;

	/** the eventbus. */
	private static EventBus ebus;
	
	/** the current session id. */
	private Object oSessionId;
	
	/** whether a test was successful. */
	private boolean bSuccess;
	
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Initialization
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
    /**
     * Initializes the unit test.
     */
    @BeforeClass
    public static void beforeClass()
    {
   		vertx = VertxFactory.newVertx(5701, "10.0.5.1");
   		
		ebus = vertx.eventBus();
	}
	
    /**
     * Resets success mark. 
     */
    @Before
    public void beforeTest()
    {
    	bSuccess = false;
    }	
    
    /**
     * Checks successful test execution.
     */
    @After
    public void afterTest()
    {
    	if (!bSuccess)
    	{
    		Assert.fail("Test was not successful");
    	}
    }
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Overwritten methods
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	/**
	 * {@inheritDoc}
	 */
	@Test
	public void testFetch() throws Exception
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("application", "demo");
		map.put("username", "rene");
		map.put("password", "rene");
		
		ebus.send("jvx.createSession", new JsonObject(map), new Handler<Message<JsonObject>>() 
		{
			public void handle(Message<JsonObject> pMessage)
			{
				JsonObject jso = pMessage.body();
				
				oSessionId = jso.getObject("result").getString("sessionId");
				
				if (oSessionId != null)
				{
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("sessionId", oSessionId);
					map.put("object", "adrData");
					
					ebus.send("jvx.fetch", new JsonObject(map), new Handler<Message<JsonObject>>() 
					{
						public void handle(Message<JsonObject> pMessage)
						{
							JsonObject jso = pMessage.body();
							
							System.out.println(StringUtil.toString(jso.getObject("result").getArray("records").toArray()));
							
							bSuccess = true;
							
							synchronized(TestEventBus.this)
							{
								TestEventBus.this.notify();
							}
						}
					});
				}
				else
				{
					Assert.fail("No sessionId");
				}
			}
		});
		
		synchronized(this)
		{
			try
			{
				wait(10000);
			}
			catch (Exception e)
			{
				//nothing to be done
			}
		}
	}

}	// TestHttpConnection
