/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.model;

import org.jboss.as.model.socket.InterfaceElement;


/**
 * @author Emanuel Muckenhuber
 */
public class DomainInterfaceAdd extends AbstractDomainModelUpdate<Void> {

    private static final long serialVersionUID = -8081711642455069769L;
    private final String name;

    public DomainInterfaceAdd(String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(DomainModel element) throws UpdateFailedException {
        final InterfaceElement networkInterface = element.addInterface(name);
        if(networkInterface == null) {
            throw new UpdateFailedException("duplicate interface binding " + name);
        }

    }

    /** {@inheritDoc} */
    public AbstractDomainModelUpdate<?> getCompensatingUpdate(DomainModel original) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    protected AbstractServerModelUpdate<Void> getServerModelUpdate() {
        // TODO Auto-generated method stub
        return null;
    }

}