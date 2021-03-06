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
package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ADMIN_OBJECTS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BACKGROUNDVALIDATIONMINUTES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BEANVALIDATIONGROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BOOTSTRAPCONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CLASS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONFIG_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONNECTIONDEFINITIONS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.INTERLIVING;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.JNDI_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NOTXSEPARATEPOOL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.PAD_XID;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.POOL_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.POOL_PREFILL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTERS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SAME_RM_OVERRIDE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN_AND_APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.TRANSACTIONSUPPORT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WRAP_XA_DATASOURCE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.XA_RESOURCE_TIMEOUT;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.SUBSYSTEM;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.SUBSYSTEM_ADD_DESC;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.common.CommonAdminObject;
import org.jboss.jca.common.api.metadata.common.CommonConnDef;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.CommonSecurity;
import org.jboss.jca.common.api.metadata.common.CommonTimeOut;
import org.jboss.jca.common.api.metadata.common.CommonValidation;
import org.jboss.jca.common.api.metadata.common.CommonXaPool;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapter;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapters;
import org.jboss.jca.common.metadata.resourceadapter.ResourceAdapterParser;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
public class ResourceAdaptersExtension implements Extension {

    private static final Logger log = Logger.getLogger("org.jboss.as.datasources");

    @Override
    public void initialize(final ExtensionContext context) {
        log.debugf("Initializing ResourceAdapters Extension");
        // Register the remoting subsystem
        final SubsystemRegistration registration = context.registerSubsystem(RESOURCEADAPTER);

        registration.registerXMLElementWriter(ResourceAdapterSubsystemParser.INSTANCE);

        // Remoting subsystem description and operation handlers
        final ModelNodeRegistration subsystem = registration.registerSubsystemModel(SUBSYSTEM);
        subsystem.registerOperationHandler(ADD, ResourceAdaptersSubsystemAdd.INSTANCE, SUBSYSTEM_ADD_DESC, false);
        subsystem.registerOperationHandler(DESCRIBE, ResourceAdaptersSubsystemDescribeHandler.INSTANCE, ResourceAdaptersSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), ResourceAdapterSubsystemParser.INSTANCE);
    }

    private static ModelNode createAddSubsystemOperation() {
        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, RESOURCEADAPTER);
        address.protect();

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address);
        return subsystem;
    }

    static final class ResourceAdapterSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
            XMLElementWriter<SubsystemMarshallingContext> {

        static final ResourceAdapterSubsystemParser INSTANCE = new ResourceAdapterSubsystemParser();

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            ModelNode node = context.getModelNode();
            boolean hasChildren = node.hasDefined(RESOURCEADAPTER) && node.get(RESOURCEADAPTER).asInt() > 0;

            context.startSubsystemElement(Namespace.CURRENT.getUriString(), !hasChildren);

            if (hasChildren) {
                writer.writeStartElement(Element.RESOURCE_ADAPTERS.getLocalName());
                for (ModelNode ra : node.get(RESOURCEADAPTER).asList()) {
                    writeRaElement(writer, ra);
                }
                writer.writeEndElement();
                // Close the subsystem element
                writer.writeEndElement();
            }
        }

        private void writeRaElement(XMLExtendedStreamWriter streamWriter, ModelNode ra) throws XMLStreamException {
            streamWriter.writeStartElement(ResourceAdapters.Tag.RESOURCE_ADPTER.getLocalName());

            writeElementIfHas(streamWriter, ra, ResourceAdapter.Tag.ARCHIVE, ARCHIVE);

            if (ra.has(BEANVALIDATIONGROUPS)) {
                for (ModelNode bvg : ra.get(BEANVALIDATIONGROUPS).asList()) {
                    writeElementIfHas(streamWriter, bvg, ResourceAdapter.Tag.BEAN_VALIDATION_GROUP, BEANVALIDATIONGROUPS);
                }
            }

            writeElementIfHas(streamWriter, ra, ResourceAdapter.Tag.BOOTSTRAP_CONTEXT, BOOTSTRAPCONTEXT);
            writeElementIfHas(streamWriter, ra, ResourceAdapter.Tag.TRANSACTION_SUPPORT, TRANSACTIONSUPPORT);
            writeConfigProperties(streamWriter, ra);

            if (ra.has(CONNECTIONDEFINITIONS)) {
                streamWriter.writeStartElement(ResourceAdapter.Tag.CONNECTION_DEFINITIONS.getLocalName());
                for (ModelNode conDef : ra.get(CONNECTIONDEFINITIONS).asList()) {
                    writeConDef(streamWriter, conDef);
                }
                streamWriter.writeEndElement();
            }

            if (ra.has(ADMIN_OBJECTS)) {
                streamWriter.writeStartElement(ResourceAdapter.Tag.ADMIN_OBJECTS.getLocalName());
                for (ModelNode adminObject : ra.get(ADMIN_OBJECTS).asList()) {
                    writeAdminObject(streamWriter, adminObject);
                }
                streamWriter.writeEndElement();
            }
            streamWriter.writeEndElement();

        }

        private void writeConfigProperties(XMLExtendedStreamWriter streamWriter, ModelNode ra) throws XMLStreamException {
            if (ra.has(CONFIG_PROPERTIES)) {
                for (ModelNode property : ra.get(CONFIG_PROPERTIES).asList()) {
                    streamWriter.writeStartElement(ResourceAdapter.Tag.CONFIG_PROPERTY.getLocalName());
                    streamWriter.writeCharacters(property.asString());
                    streamWriter.writeEndElement();
                }
            }
        }

        private void writeAdminObject(XMLExtendedStreamWriter streamWriter, ModelNode adminObject) throws XMLStreamException {
            streamWriter.writeStartElement(ResourceAdapter.Tag.ADMIN_OBJECT.getLocalName());
            writeAttributeIfHas(streamWriter, adminObject, CommonAdminObject.Attribute.CLASS_NAME, CLASS_NAME);
            writeAttributeIfHas(streamWriter, adminObject, CommonAdminObject.Attribute.JNDINAME, JNDI_NAME);
            writeAttributeIfHas(streamWriter, adminObject, CommonAdminObject.Attribute.ENABLED, ENABLED);
            writeAttributeIfHas(streamWriter, adminObject, CommonAdminObject.Attribute.USEJAVACONTEXT, USE_JAVA_CONTEXT);
            writeAttributeIfHas(streamWriter, adminObject, CommonAdminObject.Attribute.POOL_NAME, POOL_NAME);

            writeConfigProperties(streamWriter, adminObject);
            streamWriter.writeEndElement();

        }

        private void writeConDef(XMLExtendedStreamWriter streamWriter, ModelNode conDef) throws XMLStreamException {
            streamWriter.writeStartElement(ResourceAdapter.Tag.CONNECTION_DEFINITION.getLocalName());
            writeAttributeIfHas(streamWriter, conDef, CommonConnDef.Attribute.CLASS_NAME, CLASS_NAME);
            writeAttributeIfHas(streamWriter, conDef, CommonConnDef.Attribute.JNDINAME, JNDI_NAME);
            writeAttributeIfHas(streamWriter, conDef, CommonConnDef.Attribute.ENABLED, ENABLED);
            writeAttributeIfHas(streamWriter, conDef, CommonConnDef.Attribute.USEJAVACONTEXT, USE_JAVA_CONTEXT);
            writeAttributeIfHas(streamWriter, conDef, CommonConnDef.Attribute.POOL_NAME, POOL_NAME);

            writeConfigProperties(streamWriter, conDef);

            if (conDef.has(MAX_POOL_SIZE) || conDef.has(MIN_POOL_SIZE) || conDef.has(POOL_USE_STRICT_MIN)
                    || conDef.has(POOL_PREFILL)) {
                if (conDef.has(INTERLIVING) || conDef.has(WRAP_XA_DATASOURCE) || conDef.has(NOTXSEPARATEPOOL)
                        || conDef.has(PAD_XID) || conDef.has(SAME_RM_OVERRIDE)) {
                    streamWriter.writeStartElement(CommonConnDef.Tag.XA_POOL.getLocalName());
                    writeElementIfHas(streamWriter, conDef, CommonPool.Tag.MIN_POOL_SIZE, MIN_POOL_SIZE);
                    writeElementIfHas(streamWriter, conDef, CommonPool.Tag.MAXPOOLSIZE, MAX_POOL_SIZE);
                    writeElementIfHas(streamWriter, conDef, CommonPool.Tag.PREFILL, POOL_PREFILL);
                    writeElementIfHas(streamWriter, conDef, CommonPool.Tag.USE_STRICT_MIN, POOL_USE_STRICT_MIN);

                    writeElementIfHas(streamWriter, conDef, CommonXaPool.Tag.ISSAMERMOVERRIDEVALUE, SAME_RM_OVERRIDE);
                    writeElementIfHas(streamWriter, conDef, CommonXaPool.Tag.INTERLEAVING, INTERLIVING);
                    writeElementIfHas(streamWriter, conDef, CommonXaPool.Tag.NO_TX_SEPARATE_POOLS, NOTXSEPARATEPOOL);
                    writeElementIfHas(streamWriter, conDef, CommonXaPool.Tag.PAD_XID, PAD_XID);
                    writeElementIfHas(streamWriter, conDef, CommonXaPool.Tag.WRAP_XA_RESOURCE, WRAP_XA_DATASOURCE);

                    streamWriter.writeEndElement();
                } else {
                    streamWriter.writeStartElement(CommonConnDef.Tag.POOL.getLocalName());
                    writeElementIfHas(streamWriter, conDef, CommonPool.Tag.MIN_POOL_SIZE, MIN_POOL_SIZE);
                    writeElementIfHas(streamWriter, conDef, CommonPool.Tag.MAXPOOLSIZE, MAX_POOL_SIZE);
                    writeElementIfHas(streamWriter, conDef, CommonPool.Tag.PREFILL, POOL_PREFILL);
                    writeElementIfHas(streamWriter, conDef, CommonPool.Tag.USE_STRICT_MIN, POOL_USE_STRICT_MIN);
                    streamWriter.writeEndElement();
                }

            }

            if (conDef.hasDefined(APPLICATION) || conDef.hasDefined(SECURITY_DOMAIN)
                    || conDef.hasDefined(SECURITY_DOMAIN_AND_APPLICATION)) {
                streamWriter.writeStartElement(CommonConnDef.Tag.SECURITY.getLocalName());
                writeElementIfHas(streamWriter, conDef, CommonSecurity.Tag.APPLICATION, APPLICATION);
                writeElementIfHas(streamWriter, conDef, CommonSecurity.Tag.SECURITY_DOMAIN, SECURITY_DOMAIN);
                writeElementIfHas(streamWriter, conDef, CommonSecurity.Tag.SECURITY_DOMAIN_AND_APPLICATION,
                        SECURITY_DOMAIN_AND_APPLICATION);

                streamWriter.writeEndElement();
            }

            if (conDef.has(BLOCKING_TIMEOUT_WAIT_MILLIS) || conDef.has(IDLETIMEOUTMINUTES) || conDef.has(ALLOCATION_RETRY)
                    || conDef.has(ALLOCATION_RETRY_WAIT_MILLIS) || conDef.has(XA_RESOURCE_TIMEOUT)) {
                streamWriter.writeStartElement(CommonConnDef.Tag.TIMEOUT.getLocalName());
                writeElementIfHas(streamWriter, conDef, CommonTimeOut.Tag.BLOCKINGTIMEOUTMILLIS, BLOCKING_TIMEOUT_WAIT_MILLIS);
                writeElementIfHas(streamWriter, conDef, CommonTimeOut.Tag.IDLETIMEOUTMINUTES, IDLETIMEOUTMINUTES);
                writeElementIfHas(streamWriter, conDef, CommonTimeOut.Tag.ALLOCATIONRETRY, ALLOCATION_RETRY);
                writeElementIfHas(streamWriter, conDef, CommonTimeOut.Tag.ALLOCATIONRETRYWAITMILLIS,
                        ALLOCATION_RETRY_WAIT_MILLIS);
                writeElementIfHas(streamWriter, conDef, CommonTimeOut.Tag.XARESOURCETIMEOUT, XA_RESOURCE_TIMEOUT);
                streamWriter.writeEndElement();
            }

            if (conDef.has(BACKGROUNDVALIDATION) || conDef.has(BACKGROUNDVALIDATIONMINUTES) || conDef.has(USE_FAST_FAIL)) {
                streamWriter.writeStartElement(CommonConnDef.Tag.VALIDATION.getLocalName());
                writeElementIfHas(streamWriter, conDef, CommonValidation.Tag.BACKGROUNDVALIDATION, BACKGROUNDVALIDATION);
                writeElementIfHas(streamWriter, conDef, CommonValidation.Tag.BACKGROUNDVALIDATIONMINUTES,
                        BACKGROUNDVALIDATIONMINUTES);
                writeElementIfHas(streamWriter, conDef, CommonValidation.Tag.USEFASTFAIL, USE_FAST_FAIL);
                streamWriter.writeEndElement();
            }

            streamWriter.writeEndElement();

        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, String localName, String identifier)
                throws XMLStreamException {
            if (has(node, identifier)) {
                writer.writeStartElement(localName);
                writer.writeCharacters(node.get(identifier).asString());
                writer.writeEndElement();
            }
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, ResourceAdapter.Tag element,
                String identifier) throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, CommonPool.Tag element, String identifier)
                throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, CommonXaPool.Tag element,
                String identifier) throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, CommonSecurity.Tag element,
                String identifier) throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, CommonTimeOut.Tag element,
                String identifier) throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, CommonValidation.Tag element,
                String identifier) throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private boolean has(ModelNode node, String name) {
            return node.has(name) && node.get(name).isDefined();
        }

        private void writeAttributeIfHas(final XMLExtendedStreamWriter writer, final ModelNode node,
                final CommonAdminObject.Attribute attr, final String identifier) throws XMLStreamException {
            if (has(node, identifier)) {
                writer.writeAttribute(attr.getLocalName(), node.get(identifier).asString());
            }
        }

        private void writeAttributeIfHas(final XMLExtendedStreamWriter writer, final ModelNode node,
                final CommonConnDef.Attribute attr, final String identifier) throws XMLStreamException {
            if (has(node, identifier)) {
                writer.writeAttribute(attr.getLocalName(), node.get(identifier).asString());
            }
        }

        @Override
        public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {

            ModelNode subsystem = createAddSubsystemOperation();
            list.add(subsystem);

            ResourceAdapters ras = null;
            try {
                String localName = null;
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case RESOURCEADAPTERS_1_0: {
                        localName = reader.getLocalName();
                        final Element element = Element.forName(reader.getLocalName());
                        log.tracef("%s -> %s", localName, element);
                        switch (element) {
                            case SUBSYSTEM: {
                                ResourceAdapterParser parser = new ResourceAdapterParser();
                                ras = parser.parse(reader);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new XMLStreamException(e);
            }

            if (ras != null) {
                ModelNode rasNode = subsystem.get(RESOURCEADAPTERS);
                for (ResourceAdapter ra : ras.getResourceAdapters()) {
                    ModelNode raModel = new ModelNode();
                    for (Entry<String, String> entry : ra.getConfigProperties().entrySet()) {
                        raModel.get(CONFIG_PROPERTIES, entry.getKey()).set(entry.getValue());
                    }
                    raModel.get(ARCHIVE).set(ra.getArchive());
                    raModel.get(TRANSACTIONSUPPORT).set(ra.getTransactionSupport().name());
                    raModel.get(BOOTSTRAPCONTEXT).set(ra.getBootstrapContext());
                    for (String beanValidationGroup : ra.getBeanValidationGroups()) {
                        raModel.get(BEANVALIDATIONGROUPS).add(beanValidationGroup);
                    }

                    for (CommonConnDef conDef : ra.getConnectionDefinitions()) {
                        raModel.get(CONNECTIONDEFINITIONS).add(createConnectionDefinitionModel(conDef));

                    }

                    for (CommonAdminObject adminObject : ra.getAdminObjects()) {
                        raModel.get(ADMIN_OBJECTS).add(createAdminObjectModel(adminObject));

                    }

                    rasNode.add(raModel);
                }
            }

        }

        private ModelNode createAdminObjectModel(CommonAdminObject adminObject) {
            ModelNode adminObjectModel = new ModelNode();
            for (Entry<String, String> entry : adminObject.getConfigProperties().entrySet()) {
                adminObjectModel.get(CONFIG_PROPERTIES, entry.getKey()).set(entry.getValue());
            }
            adminObjectModel.get(CLASS_NAME).set(adminObject.getClassName());
            adminObjectModel.get(JNDI_NAME).set(adminObject.getJndiName());
            adminObjectModel.get(POOL_NAME).set(adminObject.getPoolName());
            adminObjectModel.get(ENABLED).set(adminObject.isEnabled());
            adminObjectModel.get(USE_JAVA_CONTEXT).set(adminObject.isUseJavaContext());

            return adminObjectModel;
        }

        private ModelNode createConnectionDefinitionModel(CommonConnDef conDef) {
            ModelNode condefModel = new ModelNode();
            for (Entry<String, String> entry : conDef.getConfigProperties().entrySet()) {
                condefModel.get(CONFIG_PROPERTIES, entry.getKey()).set(entry.getValue());
            }
            condefModel.get(CLASS_NAME).set(conDef.getClassName());
            condefModel.get(JNDI_NAME).set(conDef.getJndiName());
            condefModel.get(POOL_NAME).set(conDef.getPoolName());
            condefModel.get(ENABLED).set(conDef.isEnabled());
            condefModel.get(USE_JAVA_CONTEXT).set(conDef.isUseJavaContext());

            if (conDef.getPool() != null) {
                condefModel.get(MAX_POOL_SIZE).set(conDef.getPool().getMaxPoolSize());
                condefModel.get(MIN_POOL_SIZE).set(conDef.getPool().getMinPoolSize());
                condefModel.get(POOL_PREFILL).set(conDef.getPool().isPrefill());
                condefModel.get(POOL_USE_STRICT_MIN).set(conDef.getPool().isUseStrictMin());
                if (conDef.isXa()) {
                    CommonXaPool xaPool = (CommonXaPool) conDef.getPool();
                    condefModel.get(INTERLIVING).set(xaPool.isInterleaving());
                    condefModel.get(PAD_XID).set(xaPool.isPadXid());
                    condefModel.get(SAME_RM_OVERRIDE).set(xaPool.isSameRmOverride());
                    condefModel.get(NOTXSEPARATEPOOL).set(xaPool.isNoTxSeparatePool());
                    condefModel.get(WRAP_XA_DATASOURCE).set(xaPool.isWrapXaDataSource());

                }
            }

            if (conDef.getTimeOut() != null) {
                condefModel.get(ALLOCATION_RETRY).set(conDef.getTimeOut().getAllocationRetry());
                condefModel.get(ALLOCATION_RETRY_WAIT_MILLIS).set(conDef.getTimeOut().getAllocationRetryWaitMillis());
                condefModel.get(BLOCKING_TIMEOUT_WAIT_MILLIS).set(conDef.getTimeOut().getBlockingTimeoutMillis());
                condefModel.get(IDLETIMEOUTMINUTES).set(conDef.getTimeOut().getIdleTimeoutMinutes());
                condefModel.get(XA_RESOURCE_TIMEOUT).set(conDef.getTimeOut().getXaResourceTimeout());
            }

            if (conDef.getSecurity() != null) {
                condefModel.get(APPLICATION).set(conDef.getSecurity().isApplication());
                condefModel.get(SECURITY_DOMAIN).set(conDef.getSecurity().getSecurityDomain());
                condefModel.get(SECURITY_DOMAIN_AND_APPLICATION).set(conDef.getSecurity().getSecurityDomainAndApplication());

            }

            if (conDef.getValidation() != null) {
                condefModel.get(BACKGROUNDVALIDATIONMINUTES).set(conDef.getValidation().getBackgroundValidationMinutes());
                condefModel.get(BACKGROUNDVALIDATION).set(conDef.getValidation().isBackgroundValidation());
                condefModel.get(USE_FAST_FAIL).set(conDef.getValidation().isUseFastFail());
            }

            return condefModel;
        }
    }

    private static class ResourceAdaptersSubsystemDescribeHandler implements ModelQueryOperationHandler, DescriptionProvider {
        static final ResourceAdaptersSubsystemDescribeHandler INSTANCE = new ResourceAdaptersSubsystemDescribeHandler();

        @Override
        public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {

            ModelNode add = createAddSubsystemOperation();

            ModelNode model = context.getSubModel();

            // FIXME remove when equivalent workaround in ResourceAdaptersSubsystemAdd is gone
            boolean workaround = true;

            if (workaround) {
                if (model.hasDefined(RESOURCEADAPTERS)) {
                    ModelNode datasources = model.get(RESOURCEADAPTERS);
                    add.get(RESOURCEADAPTERS).set(datasources);
                }
            } else {
                // TODO Fill in the details
            }

            ModelNode result = new ModelNode();
            result.add(add);

            resultHandler.handleResultFragment(Util.NO_LOCATION, result);
            resultHandler.handleResultComplete();
            return new BasicOperationResult();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    }
}
