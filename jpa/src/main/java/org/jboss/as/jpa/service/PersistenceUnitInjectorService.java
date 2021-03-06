/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.service;

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;

import javax.persistence.EntityManagerFactory;
import javax.validation.ValidatorFactory;


/**
 * Represents the PersistenceUnit injected into a component.
 * TODO:  support injecting into a HibernateSessionFactory.  Initially, hack it by checknig injectionTypeName parameter
 * for HibernateSessionFactory.  If/when JPA supports unwrap on the EMF, switch to that.
 *
 * @author Scott Marlow
 */
public class PersistenceUnitInjectorService implements Service<ManagedReferenceFactory> {

    private final PersistenceUnitJndiInjectable injectable;

    public PersistenceUnitInjectorService(
        final AnnotationInstance annotation,
        final ServiceName puServiceName,
        final DeploymentUnit deploymentUnit,
        final String scopedPUName,
        final String injectionTypeName) {

        injectable = new PersistenceUnitJndiInjectable(puServiceName, deploymentUnit, scopedPUName, injectionTypeName);
    }

    @Override
    public void start(StartContext context) throws StartException {

    }

    @Override
    public void stop(StopContext context) {

    }

    @Override
    public ManagedReferenceFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return injectable;
    }

    private static final class PersistenceUnitJndiInjectable implements ManagedReferenceFactory {

        final ServiceName puServiceName;
        final DeploymentUnit deploymentUnit;

        public PersistenceUnitJndiInjectable(
            final ServiceName puServiceName,
            final DeploymentUnit deploymentUnit,
            final String scopedPUName,
            final String injectionTypeName) {

            this.puServiceName = puServiceName;
            this.deploymentUnit = deploymentUnit;
        }

        @Override
        public ManagedReference getReference() {
            PersistenceUnitService service = (PersistenceUnitService)deploymentUnit.getServiceRegistry().getRequiredService(puServiceName).getValue();
            EntityManagerFactory emf = service.getEntityManagerFactory();
            return new ValueManagedReference(new ImmediateValue<Object>(emf));
        }
    }
}
