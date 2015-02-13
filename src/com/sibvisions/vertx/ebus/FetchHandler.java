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

import java.util.List;

import javax.rad.type.bean.IBean;

import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.sibvisions.rad.persist.AbstractStorage;
import com.sibvisions.rad.server.AbstractSession;
import com.sibvisions.rad.server.Server;

/**
 * The <code>FetchHandler</code> fetches data from a storage.
 * 
 * @author René Jahn
 */
public class FetchHandler extends AbstractHandler
{
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Initialization
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Creates a new instance of <code>FetchHandler</code>.
	 * 
	 * @param pServer the server
	 */
	public FetchHandler(Server pServer)
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
    	AbstractSession session = getServer().getSessionManager().get(pMessage.body().getString("sessionId"));
    	Object obj = session.get(pMessage.body().getString("object"));
    	
    	if (obj instanceof AbstractStorage)
    	{
			List<IBean> liBeans = ((AbstractStorage)obj).fetchBean(null, null, 0, -1);
			
	        JsonObject jsobj = new JsonObject();
	        jsobj.putArray("records", new JsonArray(liBeans.toArray(new Object[liBeans.size()])));
	        
	        return jsobj;
    	}
    	
    	throw new UnsupportedOperationException("Object is not an instance of " + AbstractStorage.class.getSimpleName());
	}

}	// FetchHandler
