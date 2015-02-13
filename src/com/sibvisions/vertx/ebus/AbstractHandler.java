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
 * 28.12.2012 - [JR] - creation
 */
package com.sibvisions.vertx.ebus;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import com.sibvisions.rad.server.Server;
import com.sibvisions.util.type.CommonUtil;

/**
 * The <code>AbstractHandler</code> is the base class for all eventbus handlers that will be used
 * for communication between clients and JVx server.
 * 
 * @author René Jahn
 */
abstract class AbstractHandler implements Handler<Message<JsonObject>>
{
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Class members
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/** the JVx server. */
	private Server server;
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Initialization
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Creates a new instance of <code>AbstractHandler</code> for the given server.
	 * 
	 * @param pServer the server
	 */
	public AbstractHandler(Server pServer)
	{
		server = pServer;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Interface implementation
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * {@inheritDoc}
	 */
	public final void handle(Message<JsonObject> pMessage)
	{
		try
		{
			JsonObject jsobj = process(pMessage);
			
			sendResult(pMessage, jsobj);
		}
		catch (Throwable th)
		{
			sendError(pMessage, th);
		}
	}
	
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Abstract methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~	

	/**
	 * Process' a message received via eventbus.
	 *  
	 * @param pMessage the message
	 * @return the result object or <code>null</code> if no object should be send
	 * @throws Throwable if an exception occurs during processing
	 */
	protected abstract JsonObject process(Message<JsonObject> pMessage) throws Throwable; 
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// User-defined methods
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~	
	
	/**
	 * Gets the JVx server.
	 * 
	 * @return the server
	 */
	protected Server getServer()
	{
		return server;
	}
	
	/**
	 * Sends a result back to the client.
	 * The result will contain: {result: {object}}
	 * 
	 * @param pMessage the message from the eventbus
	 * @param pObject the result object for the client 
	 */
	protected void sendResult(Message<JsonObject> pMessage, JsonObject pObject)
	{
		if (pObject != null)
		{
			JsonObject jsoResult = new JsonObject();
			jsoResult.putObject("result", pObject);
			
			pMessage.reply(jsoResult);
		}
		else
		{
			//no result
			pMessage.reply();
		}
	}
	
	/**
	 * Sends an error back to the client.
	 * The result will contain: {error: {message: 'text', stacktrace: 'text'}}
	 * 
	 * @param pMessage the message from the eventbus
	 * @param pThrowable the exception that should be send
	 */
	protected void sendError(Message<JsonObject> pMessage, Throwable pThrowable)
	{
		JsonObject jsoError = new JsonObject();
		jsoError.putString("message", pThrowable.getMessage());
		jsoError.putString("stacktrace", CommonUtil.dump(pThrowable, true));
		
		JsonObject jsoResult = new JsonObject();
		jsoResult.putObject("error", jsoError);
		
		pMessage.reply(jsoResult);
	}
	
}	// AbstractHandler
