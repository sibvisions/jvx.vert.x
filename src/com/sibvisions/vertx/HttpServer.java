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

import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;

import javax.rad.io.IFileHandle;
import javax.rad.io.RemoteFileHandle;
import javax.rad.server.ISession;
import javax.rad.server.InjectObject;
import javax.rad.server.event.ISessionListener;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.impl.MimeMapping;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.sockjs.SockJSServer;

import com.sibvisions.rad.server.AbstractSession;
import com.sibvisions.rad.server.Server;
import com.sibvisions.util.ObjectCache;
import com.sibvisions.util.type.FileUtil;
import com.sibvisions.vertx.handler.DataHandler;
import com.sibvisions.vertx.handler.ExceptionHandler;
import com.sibvisions.vertx.handler.HttpDataHandler;
import com.sibvisions.vertx.handler.StopHandler;

/**
 * The <code>HttpServer</code> uses {@link Server} and offers a lightweight http server based
 * on {@link org.vertx.java.core.http.HttpServer}. It offers an eventbus bridge for relevant
 * server functionality.
 * 
 * @author René Jahn
 */
public class HttpServer implements ISessionListener 
{
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Class members
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/** the vert.x instance. */
	private Vertx vertx;
	
	/** the vertx http server. */
	private org.vertx.java.core.http.HttpServer srvVertx;
	
	/** the JVx server. */
	private Server srvJVx;

	/** the injection object for our vert.x instance. */
	private InjectObject ijoVertx;

	/** the service path. */
	private String sServicePath = "/services/Server";
	
    /** the download path. */
    private String sDownloadPath = "/services/Download";

    /** the upload path. */
    private String sUploadPath = "/services/Upload";
    
    /** the webcontent path. */
	private String sWebContentPath = "WebContent";

	/** the cluster hostname or ip. */
	private String sClusterHost = null;

    /** the interface for listening. */
    private String sInterface = "localhost";
	
	/** the cluster port. */
	private int iClusterPort = -1;
	
	/** the http port. */
	private int iPort = 8080;
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Initialization
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Starts a standalone http server.
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
		
		HttpServer srv = new HttpServer();
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
	 * Creates a new instance of <code>HttpServer</code> without clustering.
	 */
	public HttpServer()
	{
		this(null);
	}
	
	/**
	 * Creates a new instance of <code>HttpServer</code> with the given Vertx instance.
	 * 
	 * @param pVertx the Vertx instance
	 */
	public HttpServer(Vertx pVertx)
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
	 * Gets the current {@link org.vertx.java.core.http.HttpServer}.
	 * 
	 * @return the server instance
	 */
	public org.vertx.java.core.http.HttpServer getHttpServer()
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
		
		srvVertx = vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() 
		{
		    public void handle(HttpServerRequest pRequest) 
		    {
		        String sPath = pRequest.path(); 

		        if (sPath.equals(sServicePath))
		    	{
		            doService(pRequest);
		    	}
		    	else if (sPath.equals(sUploadPath))
		    	{
		    	    doUpload(pRequest);
		    	}
		    	else if (sPath.equals(sDownloadPath))
		    	{
		    	    doDownload(pRequest);
		    	}
		    	else
		    	{
		    		pRequest.response().sendFile(sWebContentPath + pRequest.path());
		    	}
		    }
		});
		
	    JsonArray permitted = new JsonArray();
	    permitted.add(new JsonObject()); // no limits
	    
	    SockJSServer sockJSServer = vertx.createSockJSServer(srvVertx);
	    sockJSServer.bridge(new JsonObject().putString("prefix", "/eventbus"), permitted, permitted);
	    
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
	    }
	}
	
	/**
	 * Sets the service path for accessing JVx server.
	 * 
	 * @param pServicePath the service path, e.g. /services/Server
	 */
	public void setServicePath(String pServicePath)
	{
		sServicePath = pServicePath;
	}
	
	/**
	 * Gets the service path for JVx server access.
	 * 
	 * @return the service path, e.g. /services/Server
	 */
	public String getServicePath()
	{
		return sServicePath;
	}

    /**
     * Sets the path for uploading content.
     * 
     * @param pUploadPath the path, e.g. /services/Upload
     */
    public void setUploadPath(String pUploadPath)
    {
        sUploadPath = pUploadPath;
    }
    
    /**
     * Gets the path for uploading content.
     * 
     * @return the path, e.g. /services/Upload
     */
    public String getUploadPath()
    {
        return sUploadPath;
    }

    /**
     * Sets the path for downloading content.
     * 
     * @param pDownloadPath the path, e.g. /services/Download
     */
    public void setDownloadPath(String pDownloadPath)
    {
        sDownloadPath = pDownloadPath;
    }
    
    /**
     * Gets the path for downloading content.
     * 
     * @return the path, e.g. /services/Download
     */
    public String getDownloadPath()
    {
        return sDownloadPath;
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
	 * Sets the http server port.
	 * 
	 * @param pPort the port number
	 */
	public void setPort(int pPort)
	{
		iPort = pPort;
	}
	
	/**
	 * Gets the http server port.
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
	
	/**
	 * Sets the path to the webcontent directory.
	 * 
	 * @param pPath the path e.g. WebContent
	 */
	public void setWebContentPath(String pPath)
	{
		if (pPath == null)
		{
			sWebContentPath = "WebContent";
		}
		else if (pPath.endsWith("/"))
		{
			sWebContentPath = pPath.substring(0, pPath.length() - 1);
		}
		else
		{
			sWebContentPath = pPath;
		}
	}

	/**
	 * Gets the path to the webcontent directory.
	 * 
	 * @return the path e.g. Webcontent
	 */
	public String getWebContentPath()
	{
		return sWebContentPath;
	}
	
	/**
	 * Handles a service/server request.
	 * 
	 * @param pRequest the request
	 */
	protected void doService(HttpServerRequest pRequest)
	{
        DataHandler dataHandler = new HttpDataHandler(srvJVx, pRequest.response()); 

        pRequest.dataHandler(dataHandler);
        pRequest.endHandler(new StopHandler(dataHandler));
        pRequest.exceptionHandler(new ExceptionHandler(dataHandler));
	}
	
    /**
     * Handles an upload request.
     * 
     * @param pRequest the request
     */
	private void doUpload(final HttpServerRequest pRequest)
	{
        pRequest.dataHandler(new Handler<Buffer>()
        {
            private OutputStream os;
            
            public void handle(Buffer event)
            {
                try
                {
                    if (os == null)
                    {
                        String sFileName = getFileName(pRequest.headers().get("Content-Disposition"));
                        
                        if (sFileName == null)
                        {
                            pRequest.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
                            pRequest.response().end();
                            
                            return;
                        }
                        
                        RemoteFileHandle rfh = new RemoteFileHandle(sFileName, pRequest.params().get("KEY"));
                        os = rfh.getOutputStream();
                    }
                    
                    os.write(event.getBytes());
                }
                catch (IOException ioe)
                {
                    throw new RuntimeException(ioe);
                }
            }
        });
        
        pRequest.exceptionHandler(new Handler<Throwable>()
        {
            public void handle(Throwable event)
            {
                pRequest.response().end();
            }
        });
                
        pRequest.endHandler(new Handler<Void>()
        {
            public void handle(Void event)
            {
                pRequest.response().end();
            }
        });
	}

    /**
     * Handles a download request.
     * 
     * @param pRequest the request
     */
    private void doDownload(HttpServerRequest pRequest)
    {
        String sKey = pRequest.params().get("KEY");
        
        if (sKey == null)
        {
            pRequest.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
            pRequest.response().end();
            
            return;
        }
        
        IFileHandle fh = (IFileHandle)ObjectCache.get(sKey);

        HttpServerResponse response = pRequest.response();
        
        String sType = MimeMapping.getMimeTypeForExtension(FileUtil.getExtension(fh.getFileName()));
        
        if (sType != null)
        {
            response.putHeader(HttpHeaders.CONTENT_TYPE, sType);
        }
        
        response.putHeader("Content-Disposition", "attachment; filename=\"" + fh.getFileName() + "\"");

        int iLen;
        
        byte[] byContent = new byte[4096];

        try
        {
            response.putHeader(HttpHeaders.CONTENT_LENGTH, "" + fh.getLength());

            InputStream in = fh.getInputStream();
            
            Buffer buffer;
            
            while ((iLen = in.read(byContent)) >= 0)
            {
                buffer = new Buffer();
                buffer.appendBytes(byContent, 0, iLen);

                response.write(buffer);
            }
        }
        catch (IOException ioe)
        {
            throw new RuntimeException(ioe);
        }

        response.end();
    }
	
	/**
	 * Gets the filename from the given content disposition.
	 * 
	 * @param pContentDisposition the content disposition header
	 * @return the filename or <code>null</code> if content disposition is <code>null</code> or doesn't contain the filename
	 */
	private String getFileName(String pContentDisposition)
	{
	    if (pContentDisposition == null)
	    {
	        return null;
	    }
	    
	    int iPos = pContentDisposition.toLowerCase().indexOf("filename=");
	    
	    if (iPos < 0)
	    {
	        return null;
	    }
	    
	    String sName = pContentDisposition.substring(iPos + 9);
	    
        if (sName.endsWith(";"))
        {
            sName = sName.substring(0, sName.length() - 1);
        }

        if (sName.startsWith("\""))
	    {
            sName = sName.substring(1, sName.length() - 1);
	    }
	    
        return sName;
	}
	
}	// HttpServer
