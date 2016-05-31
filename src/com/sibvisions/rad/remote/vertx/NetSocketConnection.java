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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.rad.io.IDownloadExecutor;
import javax.rad.io.IFileHandle;
import javax.rad.io.IUploadExecutor;
import javax.rad.io.RemoteFileHandle;
import javax.rad.io.TransferContext;
import javax.rad.remote.ConnectionInfo;

import com.sibvisions.rad.remote.AbstractSerializedConnection;
import com.sibvisions.rad.remote.ISerializer;
import com.sibvisions.rad.remote.vertx.io.BufferOutputStream;
import com.sibvisions.rad.remote.vertx.io.SyncedInputStream;
import com.sibvisions.util.io.MagicByteInputStream;
import com.sibvisions.util.io.NonClosingInputStream;
import com.sibvisions.util.log.LoggerFactory;
import com.sibvisions.util.type.FileUtil;

/**
 * The <code>NetSocketConnection</code> is an {@link javax.rad.remote.IConnection} that uses a {@link NetSocket} for
 * the communication to a {@link io.vertx.core.net.NetServer}.
 * 
 * @author René Jahn
 */
public class NetSocketConnection extends AbstractSerializedConnection
                                 implements IDownloadExecutor,
                                            IUploadExecutor

{
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Class members
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** the communication identifier. */
    public static final byte STREAM_COMMUNICATION = 0x43;
    /** the download identifier. */
    public static final byte STREAM_DOWNLOAD = 0x44;
    /** the data download identifier. */
    public static final byte DOWNLOAD_DATA = 0x44;
    /** the content length download identifier. */
    public static final byte DOWNLOAD_LENGTH = 0x4C;
    /** the download identifier. */
    public static final byte STREAM_UPLOAD = 0x55;
    
    /** the magic byte sequence. */
    public static final byte[] MAGIC_BYTES = new byte[] {(byte)0xA0, (byte)0x19, (byte)0xAA, (byte)0xFF, (byte)0xEE, (byte)0xAA};
    
    
	/** A vertx instance. */
	private Vertx vertx;
	
	/** the client. */
	private NetClient client;
	
    /** the transfer client. */
    private NetClient clientTransfer;

    /** the established socket connection. */
	private NetSocket socket;

	/** the transfer socket. */
	private NetSocket socketTransfer;
	
	/** the server hostname or ip. */
	private String sHost;

    /** the initial connection id for closing the net client. */
    private Object oInitialConId = null;
	
	/** the input stream. */
	private SyncedInputStream inputStream;
	
    /** the transfer input stream. */
    private SyncedInputStream isTransfer;

    /** the server port. */
    private int iPort = 8888;

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
			vertx = Vertx.vertx();
		}
		else
		{
			vertx = pVertx;
		}
		
		setRetryCount(0);
	}
	
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Interface implementation
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * {@inheritDoc}
     */
    public InputStream readContent(RemoteFileHandle pFileHandle) throws IOException
    {
        try
        {
            return download(pFileHandle.getObjectCacheKey());
        }
        catch (IOException ioe)
        {
            throw ioe;
        }
        catch (Exception e)
        {
            throw new IOException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getContentLength(RemoteFileHandle pFileHandle) throws IOException
    {
        try
        {
            return getLength(pFileHandle.getObjectCacheKey());
        }
        catch (IOException ioe)
        {
            throw ioe;
        }
        catch (Exception e)
        {
            throw new IOException(e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public RemoteFileHandle writeContent(IFileHandle pFileHandle) throws IOException
    {
        openTransfer();
    
        socketTransfer.write(Buffer.buffer(new byte[] {STREAM_UPLOAD}));
            
        BufferOutputStream bos = new BufferOutputStream(socketTransfer);
            
        try
        {
            // REQUEST

            GZIPOutputStream gzos = new GZIPOutputStream(bos);

            DataOutputStream dos = new DataOutputStream(gzos);
            dos.writeLong(pFileHandle.getLength());
            
            FileUtil.copy(pFileHandle.getInputStream(), true, gzos, false);

            gzos.finish();
            
            bos.write(MAGIC_BYTES);
            bos.flush();
            
            // RESPONSE

            MagicByteInputStream mbis = new MagicByteInputStream(new NonClosingInputStream(isTransfer), MAGIC_BYTES);
            
            GZIPInputStream gzis = new GZIPInputStream(mbis);
            
            try
            {
                DataInputStream dis = new DataInputStream(gzis);

                return new RemoteFileHandle(pFileHandle.getFileName(), dis.readUTF());
            }
            finally
            {
                mbis.close();
            }
        }
        catch (IOException ioe)
        {
            throw ioe;
        }
        catch (Exception e)
        {
            throw new IOException(e);
        }
        finally
        {
            closeTransfer();
        }
    }
	
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Overwritten methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object initCall(ConnectionInfo pConnectionInfo)
    {
        return new TransferContext(this, this);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void releaseCall(ConnectionInfo pConnectionInfo, Object pInit)
    {
        ((TransferContext)pInit).release();
    }    
    
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void open(ConnectionInfo pConnectionInfo) throws Throwable
	{
		closeSocket();
		
        NetClientOptions options = new NetClientOptions();
        options.setReconnectAttempts(3);
        options.setConnectTimeout(5000);
        options.setReconnectInterval(1000);
        
        client = vertx.createNetClient(options);
		client.connect(iPort, sHost, new Handler<AsyncResult<NetSocket>>()
		{
			public void handle(final AsyncResult<NetSocket> pCommunication)
			{
			    NetSocket sock = pCommunication.result();
			    
			    if (sock != null)
			    {
    			    sock.handler(new Handler<Buffer>()
    				{
    					public void handle(Buffer pBuffer)
    					{
                            if (inputStream != null)
                            {
                                inputStream.receive(pBuffer);
                            }
    					}
    				});
    
    				sock.exceptionHandler(new Handler<Throwable>()
    				{
    					public void handle(Throwable pException)
    					{
                            if (inputStream != null)
                            {
                                inputStream.finish();
                            }
    					}
    				});
    				
    				sock.endHandler(new Handler<Void>()
    				{
    					public void handle(Void pParam)
    					{
    					    if (inputStream != null)
    					    {
    					        inputStream.finish();
    					    }
    					}
    				});
			    }

			    synchronized (NetSocketConnection.this)
				{
			        if (pCommunication.succeeded())
			        {
			            socket = sock;
			        }
			        else
			        {
			            LoggerFactory.getInstance(NetSocketConnection.class).error(pCommunication.cause());

	                    socket = null;
			        }

	                NetSocketConnection.this.notify();
				}
			}
		});
		
		synchronized (this)
		{
		    if (socket == null)
		    {
		        wait(15000);
		    }
		}
        
		if (socket == null)
		{
			throw new ConnectException("Can't establish connection!"); 
		}

		socket.write(Buffer.buffer(new byte[] {STREAM_COMMUNICATION}));
		
		super.open(pConnectionInfo);
		
        if (oInitialConId == null)
        {
            oInitialConId = pConnectionInfo.getConnectionId();
        }
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close(ConnectionInfo pConnectionInfo) throws Throwable
	{
        //be sure to close the client not too early
        boolean bCloseClient = pConnectionInfo.getConnectionId() == oInitialConId;

        super.close(pConnectionInfo);
		
		if (bCloseClient)
		{
    		closeTransfer();
    		
            closeSocket();

            if (client != null)
            {
                client.close();
            }
		}
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
	
    /**
     * {@inheritDoc}
     */
    @Override
    protected Throwable prepareException(Throwable pThrowable)
    {
        return new Exception("Network communication failed!", pThrowable);
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
		    inputStream.finish();
            inputStream = null;

            socket.exceptionHandler(null);
            socket.endHandler(null);
			socket.handler(null);
			
			socket.close();
			socket = null;
		}
	}
	
	/**
	 * Closes the transfer client.
	 */
	private void closeTransfer()
	{
	    if (socketTransfer != null)
	    {
            isTransfer.finish();
            isTransfer = null;
            
            socketTransfer.endHandler(null);
            socketTransfer.exceptionHandler(null);
            socketTransfer.handler(null);
            
            socketTransfer.close();
            socketTransfer = null;
            
            clientTransfer.close();
	    }
	}
	
	/**
	 * Opens a new transfer client.
	 * 
	 * @throws IOException if opening failed
	 */
	private void openTransfer() throws IOException
	{
	    closeTransfer();
	    
        if (socketTransfer != null)
        {
            socketTransfer.close();
            socketTransfer = null;
        }
        
        NetClientOptions options = new NetClientOptions();
        options.setReconnectAttempts(3);
        options.setReconnectInterval(1000);
        
        clientTransfer = vertx.createNetClient(options);
        
        isTransfer = new SyncedInputStream();
        
        clientTransfer.connect(iPort, sHost, new Handler<AsyncResult<NetSocket>>()
        {
            public void handle(AsyncResult<NetSocket> pCommunication)
            {
                NetSocket sock = pCommunication.result();
                
                if (sock != null)
                {
                    sock.handler(new Handler<Buffer>()
                    {
                        public void handle(Buffer pBuffer)
                        {
                            if (isTransfer != null)
                            {
                                isTransfer.receive(pBuffer);
                            }
                        }
                    });
    
                    sock.exceptionHandler(new Handler<Throwable>()
                    {
                        public void handle(Throwable pException)
                        {
                            if (isTransfer != null)
                            {
                                isTransfer.finish();
                            }
                        }
                    });
                    
                    sock.endHandler(new Handler<Void>()
                    {
                        public void handle(Void pParam)
                        {
                            if (isTransfer != null)
                            {
                                isTransfer.finish();
                            }
                        }
                
                    });
                }
                
                synchronized (clientTransfer)
                {
                    socketTransfer = sock;

                    clientTransfer.notify();
                }
            }
        });
        
        try
        {
            synchronized (clientTransfer)
            {
                if (socketTransfer == null)
                {
                    clientTransfer.wait(15000);
                }
            }
        }
        catch (InterruptedException ie)
        {
            throw new IOException(ie);
        }
        
        if (socketTransfer == null)
        {
            throw new ConnectException("Can't establish transfer connection!"); 
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
	
    /**
     * Gets the input stream for a given download identifier.
     * 
     * @param pIdentifier the identifier
     * @return the stream or <code>null</code> if streaming isn't possible
     * @throws Exception if starting download failed
     */
    protected InputStream download(Object pIdentifier) throws Exception
    {
        return (InputStream)startDownload(DOWNLOAD_DATA, pIdentifier);
    }
    
    /**
     * Gets the length of the content for a given download identifier.
     * 
     * @param pIdentifier the identifier
     * @return the length or <code>0</code> if content isn't available
     * @throws Exception if starting download failed
     */
    protected long getLength(Object pIdentifier) throws Exception
    {
        Object obj = startDownload(DOWNLOAD_LENGTH, pIdentifier);
        
        if (obj == null)
        {
            return 0;
        }
        
        return ((Long)obj).longValue();
    }

    /**
     * Starts a download operation in with a separate connection.
     * 
     * @param pOperation the download operation
     * @param pIdentifier the identifier for the download
     * @return the result from the server or <code>null</code> if operation is unknown or given
     *         identifier is <code>null</code> 
     * @throws Exception if executing download operation failed
     */
    private Object startDownload(byte pOperation, Object pIdentifier) throws Exception
    {
        if (pIdentifier != null)
        {
            openTransfer();
    
            try
            {
                socketTransfer.write(Buffer.buffer(new byte[] {STREAM_DOWNLOAD, pOperation}));
                
                // REQUEST
                
                BufferOutputStream bos = new BufferOutputStream(socketTransfer);
                GZIPOutputStream gzos = new GZIPOutputStream(bos);
                
                @SuppressWarnings("resource")
                DataOutputStream dos = new DataOutputStream(gzos);
                dos.writeUTF((String)pIdentifier);
                
                gzos.finish();
                
                bos.write(NetSocketConnection.MAGIC_BYTES);
                bos.flush();
                
                // RESPONSE: get length or content (don't check magic byte sequence -> stream won't be re-used)
                
                GZIPInputStream gzis = new GZIPInputStream(isTransfer);
    
                if (pOperation == DOWNLOAD_LENGTH)
                {
                    DataInputStream dis = new DataInputStream(gzis);
    
                    try
                    {
                        return Long.valueOf(dis.readLong());
                    }
                    finally
                    {
                        dis.close();
                    }
                }
                else if (pOperation == DOWNLOAD_DATA)
                {
                    DownloadStream stream = new DownloadStream(clientTransfer, gzis);
                    
                    //reset instances because we won't close the transfer client - otherwise download would fail!
                    isTransfer = null;
                    clientTransfer = null;
                    
                    return stream; 
                }
            }
            finally
            {
                closeTransfer();
            }
        }
        
        return null;
    }
    
    //****************************************************************
    // Subclass definition
    //****************************************************************

    /**
     * The <code>DownloadStream</code> is a stream that is connected to a {@link CommunicationClient}.
     * If the stream will be closed, the client will be closed too.
     * 
     * @author René Jahn
     */
    private static final class DownloadStream extends FilterInputStream
    {
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Class members
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        
        /** the client. */
        private NetClient client;

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Initialization
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        
        /**
         * Creates a new instance of <code>DownloadStream</code>.
         * 
         * @param pClient the communication client
         * @param pStream the wrapped stream
         */
        private DownloadStream(NetClient pClient, InputStream pStream)
        {
            super(pStream);
            
            client = pClient;
        }
        
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Overwritten methods
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        
        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException
        {
            try
            {
                super.close();
            }
            finally
            {
                client.close();
            }
        }
        
    }   // DownloadStream
	
}	// NetSocketConnection
