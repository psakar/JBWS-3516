/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

import javax.jws.WebService;
import javax.xml.ws.soap.Addressing;

import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.interceptor.Fault;

@WebService(
   portName = "AnnotatedSecurityServicePort",
   serviceName = "AnnotatedSecurityService",
   name = "AnnotatedServiceIface",
   endpointInterface = "org.jboss.as.jbossws.jbws3516.AnnotatedServiceIface",
   targetNamespace = Constants.NAMESPACE
)
@Addressing(required=true, enabled=true)
@EndpointProperties(value={})
public class AnnotatedServiceThrowFaultImpl implements AnnotatedServiceIface
{
   static final String HELLO_WORLD = "Hello World!";

   @Override
   public String sayHello()
   {
      throw new Fault(new Exception("Fault"));
   }

}
