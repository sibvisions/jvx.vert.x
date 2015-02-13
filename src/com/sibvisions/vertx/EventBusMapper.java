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
 * 30.12.2012 - [JR] - creation
 */
package com.sibvisions.vertx;

import java.util.Hashtable;
import java.util.Map.Entry;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import com.sibvisions.rad.server.Server;
import com.sibvisions.vertx.ebus.CreateSessionHandler;
import com.sibvisions.vertx.ebus.FetchHandler;

/**
 * The <code>EventBusMapper</code> registers all supported JVx handlers in an eventbus.
 * 
 * @author René Jahn
 */
class EventBusMapper
{
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Class members
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/** the JVx server. */
	private Server srvJVx;
	
	/** the registered eventbus handlers. */
	private Hashtable<String, Handler<Message<JsonObject>>> htHandlers = new Hashtable<String, Handler<Message<JsonObject>>>();
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Initialization
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Creates a new instance of <code>EventBusMapper</code>.
	 * 
	 * @param pServer the JVx server
	 */
	public EventBusMapper(Server pServer)
	{
		srvJVx = pServer;
	}
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// User-defined methods
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~	
	
	/**
	 * Register all supported handlers in the given eventbus.
	 *  
	 * @param pBus the eventbus
	 */
	public void register(EventBus pBus)
	{
		htHandlers.put("jvx.createSession", new CreateSessionHandler(srvJVx));
		htHandlers.put("jvx.fetch", new FetchHandler(srvJVx));
		
		for (Entry<String, Handler<Message<JsonObject>>> entry : htHandlers.entrySet())
		{
			pBus.registerHandler(entry.getKey(), entry.getValue());
		}
	}
	
	/**
	 * Unregisters all handlers from the given eventbus.
	 *  
	 * @param pBus the eventbus
	 */
	public void unregister(EventBus pBus)
	{
		for (Entry<String, Handler<Message<JsonObject>>> entry : htHandlers.entrySet())
		{
			pBus.unregisterHandler(entry.getKey(), entry.getValue());
		}
	}
	
}	// EventBusMapper
