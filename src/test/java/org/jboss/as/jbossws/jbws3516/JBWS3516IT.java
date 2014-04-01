/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.jbossws.jbws3516;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class JBWS3516IT
{

   private static final String name = "JBWS3516IT";
   private static final int port = 8080;
   protected static final String SOAP_ADDRESS_LOCATION_PREFIX = "<soap:address location=\"";
   private static final int replayToPort = 7777;
   private static final int faultToPort = 7778;

   //@Rule
   //public Timeout timeout = new Timeout(30*1000);


   @Deployment
   static WebArchive creatDeployment() {
      WebArchive archive = ShrinkWrap.create(WebArchive.class, name + ".war")
            .setManifest(new StringAsset("Manifest-Version: 1.0\n"
                  + "Dependencies: org.jboss.ws.cxf.jbossws-cxf-client\n"))
            .addClass(Constants.class)
            .addClass(AnnotatedServiceIface.class)
            .addClass(AnnotatedServiceThrowFaultImpl.class);
      archive.as(ZipExporter.class).exportTo(new File("target", archive.getName()), true);
      return archive;
   }


   @After
   public void after() throws Exception {
   }

   @Test
   public void testFaultMessageIsSentToSpecifiedAddressOneway() throws Exception
   {
      String message = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
            "<soap:Header>" +
            "    <Action xmlns=\"http://www.w3.org/2005/08/addressing\">sayHelloWorldFrom</Action>" +
            "    <MessageID xmlns=\"http://www.w3.org/2005/08/addressing\">urn:uuid:f1b33789-4a21-447a-95fb-5ea15b0e4545</MessageID>" +
            "    <To xmlns=\"http://www.w3.org/2005/08/addressing\">http://localhost:" + port + "/JBWS3516IT/AnnotatedSecurityService</To>" +
            "    <ReplyTo xmlns=\"http://www.w3.org/2005/08/addressing\"><Address>http://localhost:" + replayToPort + "</Address></ReplyTo>" +
            "    <FaultTo xmlns=\"http://www.w3.org/2005/08/addressing\"><Address>http://localhost:" + faultToPort + "</Address></FaultTo>" +
            "</soap:Header>" +
            "<soap:Body><ns2:oneWay xmlns:ns2=\"http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy\"/></soap:Body></soap:Envelope>";

      assertFaultSentCorrectPort(message);
   }

   @Test
   public void testFaultMessageIsSentToSpecifiedAddress() throws Exception
   {
      String message = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
            "<soap:Header>" +
            "    <Action xmlns=\"http://www.w3.org/2005/08/addressing\">sayHelloWorldFrom</Action>" +
            "    <MessageID xmlns=\"http://www.w3.org/2005/08/addressing\">urn:uuid:f1b33789-4a21-447a-95fb-5ea15b0e4545</MessageID>" +
            "    <To xmlns=\"http://www.w3.org/2005/08/addressing\">http://localhost:" + port + "/JBWS3516IT/AnnotatedSecurityService</To>" +
            "    <ReplyTo xmlns=\"http://www.w3.org/2005/08/addressing\"><Address>http://localhost:" + replayToPort + "</Address></ReplyTo>" +
            "    <FaultTo xmlns=\"http://www.w3.org/2005/08/addressing\"><Address>http://localhost:" + faultToPort + "</Address></FaultTo>" +
            "</soap:Header>" +
            "<soap:Body><ns2:sayHello xmlns:ns2=\"http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy\"/></soap:Body></soap:Envelope>";

      assertFaultSentCorrectPort(message);
   }


   void assertFaultSentCorrectPort(String message) throws IOException, ClientProtocolException, InterruptedException
   {
      StringBuffer replayToSocketBuffer = new StringBuffer();
      RunnableImplementation replyToSocketTask = createServerSocketThread(replayToPort, replayToSocketBuffer);
      Thread replyToSocketThread = new Thread(replyToSocketTask);
      replyToSocketThread.start();

      StringBuffer faultToSocketBuffer = new StringBuffer();
      RunnableImplementation faultToSocketTask = createServerSocketThread(faultToPort, faultToSocketBuffer);
      Thread faultToSocketThread = new Thread(faultToSocketTask);
      faultToSocketThread.start();


      HttpClient client = new DefaultHttpClient();
      HttpPost post = new HttpPost("http://localhost:8080/" + name + "/AnnotatedSecurityService");
      HttpEntity entity = new ByteArrayEntity(message.getBytes(), ContentType.create(ContentType.TEXT_XML.getMimeType(), "utf-8"));
      post.setEntity(entity);
      HttpResponse response = client.execute(post);
      BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
      String line = "";
      while ((line = rd.readLine()) != null) {
        System.out.println(line);
      }

      Thread.sleep(1000);
      replyToSocketThread.interrupt();
      IOUtils.closeQuietly(replyToSocketTask.serverSocket);
      faultToSocketThread.interrupt();
      IOUtils.closeQuietly(faultToSocketTask.serverSocket);
      String replayToSocketData = replayToSocketBuffer.toString();
      String faultToSocketData = faultToSocketBuffer.toString();
      assertEquals("", replayToSocketData);
      assertFalse("".equals(faultToSocketData));

   }

   RunnableImplementation createServerSocketThread(final int port, final StringBuffer socketBuffer)
   {
      return new RunnableImplementation(socketBuffer, port);
   }


   private static final class RunnableImplementation implements Runnable
   {
      private final StringBuffer socketBuffer;

      private final int port;

      public ServerSocket serverSocket = null;

      private RunnableImplementation(StringBuffer socketBuffer, int port)
      {
         this.socketBuffer = socketBuffer;
         this.port = port;
      }

      @Override
      public void run()
      {
         try
         {
            serverSocket = new ServerSocket(port);
            System.out.println("Waiting for clients to connect to port " + port + "...");

            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected to port " + port);
            InputStreamReader inputStreamReader = new InputStreamReader(clientSocket.getInputStream(), "utf-8");
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line;
            do
            {
                line = reader.readLine();
                System.out.println("Client request to port " + port + " line " + line);
                if (line == null) // || line.length() == 0)
                   break;
                socketBuffer.append(line);
            }
            while (true);
            System.out.println("Client request to port " + port + " " + socketBuffer.toString());
            reader.close();
            clientSocket.close();
         }
         catch (Exception e)
         {
            System.err.println("Unable to process client request " + e.getMessage());
            e.printStackTrace();
         }
         finally
         {
            IOUtils.closeQuietly(serverSocket);
         }
      }
   }

}
