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

import javax.rad.remote.IConnectionConstants;

import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import com.sibvisions.rad.server.Server;
import com.sibvisions.util.ChangedHashtable;

/**
 * The <code>CreateSessionHandler</code> creates a new master session.
 * 
 * @author René Jahn
 */
public class CreateSessionHandler extends AbstractHandler
{
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Initialization
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Creates a new instance of <code>CreateSessionHandler</code>.
	 * 
	 * @param pServer the server
	 */
	public CreateSessionHandler(Server pServer)
	{
		super(pServer);
	}
	
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Abstract methods implementation
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected JsonObject process(Message<JsonObject> pMessage) throws Throwable
	{
    	ChangedHashtable<String, Object> chtProp = new ChangedHashtable<String, Object>();
    	chtProp.put(IConnectionConstants.APPLICATION, pMessage.body().getString("application"));
    	chtProp.put(IConnectionConstants.USERNAME, pMessage.body().getString("username"));
    	chtProp.put(IConnectionConstants.PASSWORD, pMessage.body().getString("password"));
    	
		Object oSessId = getServer().createSession(chtProp);
		
		JsonObject jsoResult = new JsonObject();
		jsoResult.putString("sessionId", (String)oSessId);
		
		return jsoResult;
	}

}	// CreateSessionHandler
