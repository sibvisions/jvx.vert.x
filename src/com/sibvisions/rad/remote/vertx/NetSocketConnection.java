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
 * 24.01.2012 - [JR] - close "old" socket otherwise events from old sockets will call notify()
 */
package com.sibvisions.rad.remote.vertx;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;

import javax.rad.remote.ConnectionInfo;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetSocket;

import com.sibvisions.rad.remote.AbstractSerializedConnection;
import com.sibvisions.rad.remote.ISerializer;
import com.sibvisions.rad.remote.vertx.io.BufferOutputStream;
import com.sibvisions.rad.remote.vertx.io.SyncedInputStream;

/**
 * The <code>NetSocketConnection</code> is an {@link javax.rad.remote.IConnection} that uses a {@link NetSocket} for
 * the communication to a {@link org.vertx.java.core.net.NetServer}.
 * 
 * @author René Jahn
 */
public class NetSocketConnection extends AbstractSerializedConnection
{
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Class members
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/** A vertx instance. */
	private Vertx vertx;
	
	/** the client. */
	private NetClient client;
	
	/** the established socket connection. */
	private NetSocket socket;

	/** the server hostname or ip. */
	private String sHost;
	
	/** the server port. */
	private int iPort = 8888;

	/** the input stream. */
	private SyncedInputStream inputStream;
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Initialization
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Creates a new instance of <code>NetSocketConnection</code> that connects
	 * to the given server.
	 * 
	 * @param pHost the server hostname or ip
	 */
	public NetSocketConnection(String pHost)
	{
		//default
		this(null, pHost, -1);
	}
	
	/**
	 * Creates a new instance of <code>NetSocketConnection</code> that connects
	 * to the given server.
	 * 
	 * @param pHost the server hostname or ip
	 * @param pPort the server port
	 */
	public NetSocketConnection(String pHost, int pPort)
	{
		this(null, pHost, pPort);
	}

	/**
	 * Creates a new instance of <code>NetSocketConnection</code> that connects
	 * to the given server.
	 * 
	 * @param pVertx the vert.x instance or <code>null</code> to create a new (standalone) instance
	 * @param pHost the server hostname or ip
	 * @param pPort the server port
	 */
	public NetSocketConnection(Vertx pVertx, String pHost, int pPort)
	{
		super((ISerializer)null);
		
		sHost = pHost;
		
		if (pPort > 0)
		{
			iPort = pPort;
		}
		
		if (pVertx == null)
		{
			vertx = VertxFactory.newVertx();
		}
		else
		{
			vertx = pVertx;
		}
		
		client = vertx.createNetClient();
		client.setReconnectAttempts(3);
		client.setReconnectInterval(1000);
	}
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Overwritten methods
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void open(ConnectionInfo pConnectionInfo) throws Throwable
	{
		closeSocket();
		
		client.connect(iPort, sHost, new Handler<AsyncResult<NetSocket>>()
		{
			public void handle(AsyncResult<NetSocket> pCommunication)
			{
			    pCommunication.result().dataHandler(new Handler<Buffer>()
				{
					public void handle(Buffer pBuffer)
					{
                        if (inputStream != null)
                        {
                            inputStream.receive(pBuffer);
                        }
					}
				});

				pCommunication.result().exceptionHandler(new Handler<Throwable>()
				{
					public void handle(Throwable pException)
					{
                        if (inputStream != null)
                        {
                            inputStream.finish();
                        }
					}
				});
				
				pCommunication.result().endHandler(new Handler<Void>()
				{
					public void handle(Void pParam)
					{
					    if (inputStream != null)
					    {
					        inputStream.finish();
					    }
					}
			
				});
				
				socket = pCommunication.result();
				
				synchronized (NetSocketConnection.this)
				{
				    NetSocketConnection.this.notify();
				}
			}
		});
		
		synchronized (this)
		{
		    wait(15000);
		}
		
		if (socket == null)
		{
			throw new ConnectException("Can't establish connection!"); 
		}
		
		super.open(pConnectionInfo);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close(ConnectionInfo pConnectionInfo) throws Throwable
	{
		super.close(pConnectionInfo);
		
        closeSocket();

        client.close();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public OutputStream getOutputStream(ConnectionInfo pConnectionInfo) throws Throwable
	{
	    if (inputStream != null)
	    {
	        inputStream.close();
	    }
	    
        inputStream = new SyncedInputStream();

        return new BufferOutputStream(socket);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InputStream getInputStream(ConnectionInfo pConnectionInfo) throws Throwable
	{
	    return inputStream;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// User-defined methods
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~	

	/**
	 * Close the "old" socket.
	 */
	private void closeSocket()
	{
		if (socket != null)
		{
            inputStream = null;

            socket.exceptionHandler(null);
			socket.endHandler(null);
			socket.dataHandler(null);
			
			socket.close();
			socket = null;
		}
	}
	
	/**
	 * Gets the current Vertx instance.
	 * 
	 * @return the instance
	 */
	public Vertx getVertx()
	{
		return vertx;
	}
	
}	// NetSocketConnection
