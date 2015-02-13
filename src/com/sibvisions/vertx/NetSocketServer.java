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
package com.sibvisions.vertx;

import java.util.Hashtable;

import javax.rad.server.ISession;
import javax.rad.server.InjectObject;
import javax.rad.server.event.ISessionListener;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;

import com.sibvisions.rad.server.AbstractSession;
import com.sibvisions.rad.server.Server;
import com.sibvisions.vertx.handler.DataHandler;
import com.sibvisions.vertx.handler.ExceptionHandler;
import com.sibvisions.vertx.handler.StopHandler;

/**
 * The <code>NetSocketServer</code> uses {@link Server} and offers a lightweight socket server based
 * on {@link org.vertx.java.core.net.NetServer}.
 * 
 * @author René Jahn
 */
public class NetSocketServer implements ISessionListener
{
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Class members
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/** the vert.x instance. */
	private Vertx vertx;
	
	/** the vertx net server. */
	private NetServer srvVertx;
	
	/** the JVx server. */
	private Server srvJVx;

	/** the injection object for our vert.x instance. */
	private InjectObject ijoVertx;
	
	/** the cluster hostname or ip. */
	private String sClusterHost;

	/** the interface for listening. */
	private String sInterface;
	
	/** the cluster port. */
	private int iClusterPort = -1;
	
	/** the port. */
	private int iPort = 8888;
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Initialization
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Starts a standalone netsocket server.
	 * 
	 * @param pArgs startup arguments
	 */	
	public static void main(String[] pArgs)
	{
		Hashtable<String, String> htParams = Util.parseCommandLineProperties(pArgs);
		
		int iPort;
		
		try
		{
			iPort = Integer.parseInt(htParams.get("cluster-port"));
		}
		catch (Exception e)
		{
			iPort = -1;
		}
		
		NetSocketServer srv = new NetSocketServer();
		srv.setClusterHost(htParams.get("cluster-host"));

		if (iPort > 0)
		{
			srv.setClusterPort(iPort);
		}
		srv.start();
				
		synchronized(srv)
		{
			try
			{
				srv.wait();
			}
			catch (Exception e)
			{
				//nothing to be done
			}
		}
	}
	
	/**
	 * Creates a new instance of <code>NetSocketServer</code> without clustering.
	 */
	public NetSocketServer()
	{
		this(null);
	}
	
	/**
	 * Creates a new instance of <code>NetSocketServer</code> with the given Vertx instance.
	 * 
	 * @param pVertx the Vertx instance
	 */	
	public NetSocketServer(Vertx pVertx)
	{
		srvJVx = new Server();
		srvJVx.getSessionManager().addSessionListener(this);
		
		vertx = pVertx;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Interface implementation
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~	

	/**
	 * {@inheritDoc}
	 */
	public void sessionCreated(ISession pSession)
	{
		if (ijoVertx == null)
		{
			ijoVertx = new InjectObject("vertx", vertx);
		}
		
		((AbstractSession)pSession).putObject(ijoVertx);
	}

	/**
	 * {@inheritDoc}
	 */
	public void sessionDestroyed(ISession pSession)
	{
	}
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// User-defined methods
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~	
	
	/**
	 * Gets the current Vertx instance.
	 * 
	 * @return the instance
	 */
	public Vertx getVertx()
	{
		return vertx;
	}
	
	/**
	 * Gets the current {@link NetServer).
	 * 
	 * @return the server instance
	 */
	public NetServer getNetServer()
	{
		return srvVertx; 
	}
	
	/**
	 * Starts the server to listen on the configured port.
	 */
	public void start()
	{
		if (vertx == null)
		{
			if (sClusterHost != null)
			{
				//clustered
				if (iClusterPort > 0)
				{
					vertx = VertxFactory.newVertx(iClusterPort, sClusterHost);
				}
				else
				{
					vertx = VertxFactory.newVertx(sClusterHost);
				}
			}
			else
			{
				//not clustered
				vertx = VertxFactory.newVertx();
			}
		}
		
		EventBusMapper mapper = new EventBusMapper(srvJVx);
		mapper.register(vertx.eventBus());
		
		srvVertx = vertx.createNetServer();
		
		srvVertx.connectHandler(new Handler<NetSocket>()
		{
			public void handle(NetSocket pSocket)
			{
			    DataHandler dataHandler = new DataHandler(srvJVx, pSocket); 
			    
		    	pSocket.dataHandler(dataHandler);
		    	pSocket.endHandler(new StopHandler(dataHandler));
		    	pSocket.exceptionHandler(new ExceptionHandler(dataHandler));
			}
		});
		
		srvVertx.listen(iPort, sInterface);
	}
	
	/**
	 * Stops the server.
	 */
	public void stop()
	{
	    if (srvVertx != null)
	    {
	        srvVertx.close();
	        
	        //stops main thread
	        synchronized (this)
	        {
	            notifyAll();
	        }
	    }
	}
	
	/**
	 * Sets the listening interface.
	 * 
	 * @param pInterface the interface
	 */
	public void setInterface(String pInterface)
	{
	    sInterface = pInterface;
	}
	
	/**
	 * Gets the listening interface.
	 * 
	 * @return the interface
	 */
    public String getInterface()
    {
        return sInterface;
    }
	
	/**
	 * Sets the server port.
	 * 
	 * @param pPort the port number
	 */
	public void setPort(int pPort)
	{
		iPort = pPort;
	}
	
	/**
	 * Gets the server port.
	 * 
	 * @return the port number
	 */
	public int getPort()
	{
		return iPort;
	}

	/**
	 * Sets the hostname for clustering.
	 * 
	 * @param pHost the hostname or ip
	 */
	public void setClusterHost(String pHost)
	{
		sClusterHost = pHost;
	}
	
	/**
	 * Gets the cluster hostname.
	 * 
	 * @return the hostname or ip
	 */
	public String getClusterHost()
	{
		return sClusterHost;
	}
	
	/**
	 * Sets the port for clustering.
	 * 
	 * @param pPort the port number
	 */
	public void setClusterPort(int pPort)
	{
		iClusterPort = pPort;
	}
	
	/**
	 * Gets the cluster port.
	 * 
	 * @return the port number
	 */
	public int getClusterPort()
	{
		return iClusterPort;
	}
	
}	// NetSocketServer
