/*
 * Copyright 2015 SIB Visions GmbH
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
 * 14.02.2015 - [JR] - creation
 */
package com.sibvisions.vertx.handler;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.streams.WriteStream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.rad.io.IFileHandle;
import javax.rad.io.RemoteFileHandle;

import com.sibvisions.rad.remote.vertx.NetSocketConnection;
import com.sibvisions.rad.remote.vertx.io.BufferOutputStream;
import com.sibvisions.rad.server.Server;
import com.sibvisions.util.ObjectCache;
import com.sibvisions.util.io.MagicByteInputStream;
import com.sibvisions.util.io.NonClosingInputStream;
import com.sibvisions.util.io.ShadowCopyOutputStream;
import com.sibvisions.util.type.CommonUtil;
import com.sibvisions.util.type.FileUtil;

/**
 * The <code>NetDataHandler</code> extends the {@link AbstractDataHandler} and supports up/downloading of remote
 * file handles.
 * 
 * @author René Jahn
 */
public class NetDataHandler extends AbstractDataHandler 
{
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Class members
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** the operation mode. */
    private int iMode = -1;
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Initialization
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /**
     * Creates a new instance of <code>NetDataHandler</code>.
     * 
     * @param pServer the JVx server
     * @param pStream the write stream
     */
    public NetDataHandler(Server pServer, WriteStream<Buffer> pStream)
    {
        super(pServer, pStream);
    }
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Overwritten methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /**
     * Forwards processing to the server.
     */
    protected void process()
    {
        if (iMode == -1)
        {
            try
            {
                iMode = getInputStream().read();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        
        if (iMode == NetSocketConnection.STREAM_COMMUNICATION)
        {
            super.process();
        }
        else if (iMode == NetSocketConnection.STREAM_UPLOAD)
        {
            handleUpload();
        }
        else if (iMode == NetSocketConnection.STREAM_DOWNLOAD)
        {
            handleDownload();
        }
    }
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // User-defined methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Handles content upload.
     */
    private void handleUpload()
    {
        try
        {
            InputStream in = getInputStream();

            MagicByteInputStream mbis = new MagicByteInputStream(new NonClosingInputStream(in), NetSocketConnection.MAGIC_BYTES);
            
            long lContentLength; 
            
            RemoteFileHandle rfh = new RemoteFileHandle(null, RemoteFileHandle.createObjectCacheKey());

            try
            {
                GZIPInputStream gzis = new GZIPInputStream(mbis);
                
                DataInputStream dis = new DataInputStream(gzis);
                
                lContentLength = dis.readLong();

                OutputStream osFile = rfh.getOutputStream();
                
                try
                {
                    byte[] byContent = new byte[8192];
                    
                    int iExpectedBytes = byContent.length;
                    int iLen = 0;

                    long lRead = 0;
                    
                    while (iLen != -1 && lRead < lContentLength)
                    {
                        if (lContentLength - lRead < iExpectedBytes)
                        {
                            iExpectedBytes = (int)(lContentLength - lRead);
                        }
                        else
                        {
                            iExpectedBytes = byContent.length;
                        }

                        iLen = gzis.read(byContent, 0, iExpectedBytes);
                        
                        if (iLen > 0)
                        {
                            osFile.write(byContent, 0, iLen);
                            
                            lRead += iLen;
                        }
                    }
                }
                finally
                {
                    mbis.close();
                }
            }
            finally
            {
                mbis.readMagicByte();
                
                in.close();
            }
            
            NetSocket socket = (NetSocket)getStream();
            
            OutputStream os = new BufferOutputStream(socket);
            
            GZIPOutputStream gzos = new GZIPOutputStream(os);
            
            DataOutputStream dos = new DataOutputStream(gzos);
            dos.writeUTF((String)rfh.getObjectCacheKey());
            
            gzos.finish();
            
            os.write(NetSocketConnection.MAGIC_BYTES);
            os.flush();
            
            dos.close();
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }        
    }

    /**
     * Handles content download.
     */
    private void handleDownload()
    {
        NetSocket socket = (NetSocket)getStream();
        
        try
        {
            InputStream in = getInputStream();
            
            char chDownloadMode;
            
            String sKey;

            try
            {
                chDownloadMode = (char)in.read();

                MagicByteInputStream mbis = new MagicByteInputStream(new NonClosingInputStream(in), 
                                                                     NetSocketConnection.MAGIC_BYTES);
                
                DataInputStream dis = null;
                
                try
                {
                    GZIPInputStream gzin = new GZIPInputStream(mbis);

                    dis = new DataInputStream(gzin);
                    sKey = dis.readUTF();
                }
                finally
                {
                    CommonUtil.close(dis);
                }
            }
            finally
            {
                in.close();
            }

            IFileHandle tempFile = (IFileHandle)ObjectCache.get(sKey);
            
            OutputStream os = new ShadowCopyOutputStream(new BufferOutputStream(socket));

            GZIPOutputStream gzos = new GZIPOutputStream(os);
            
            DataOutputStream dos = null;
            
            if (chDownloadMode == NetSocketConnection.DOWNLOAD_DATA)
            {
                if (tempFile != null)
                {
                    //send back the content
                    FileUtil.copy(tempFile.getInputStream(), true, gzos, false);
                    
                    gzos.finish();
                }
            }
            else if (chDownloadMode == NetSocketConnection.DOWNLOAD_LENGTH)
            {
                dos = new DataOutputStream(gzos);

                dos.writeLong(tempFile.getLength());
            }
            
            gzos.finish();
            
            os.write(NetSocketConnection.MAGIC_BYTES);
            os.flush();
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }        
    }    
    
}   // NetDataHandler
