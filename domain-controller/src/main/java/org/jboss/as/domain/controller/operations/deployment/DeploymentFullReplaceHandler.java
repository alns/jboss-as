/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.domain.controller.operations.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;

import java.io.IOException;
import java.util.Locale;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.server.controller.descriptions.DeploymentDescription;
import org.jboss.as.server.deployment.api.DeploymentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Handles replacement in the runtime of one deployment by another.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentFullReplaceHandler implements ModelUpdateOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "full-replace-deployment";

    static final ModelNode getOperation(ModelNode address) {
        return Util.getEmptyOperation(OPERATION_NAME, address);
    }

    private final DeploymentRepository deploymentRepository;
    private final boolean isMaster;

    private final ParametersValidator validator = new ParametersValidator();

    public DeploymentFullReplaceHandler(final DeploymentRepository deploymentRepository, final boolean isMaster) {
        this.deploymentRepository = deploymentRepository;
        this.validator.registerValidator(NAME, new StringLengthValidator(1, Integer.MAX_VALUE, false, false));
        this.validator.registerValidator(RUNTIME_NAME, new StringLengthValidator(1, Integer.MAX_VALUE, true, false));
        this.validator.registerValidator(HASH, new ModelTypeValidator(ModelType.BYTES, true));
        this.validator.registerValidator(INPUT_STREAM_INDEX, new ModelTypeValidator(ModelType.INT, true));
        this.isMaster = isMaster;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return DeploymentDescription.getFullReplaceDeploymentOperation(locale);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) throws OperationFailedException {

        validator.validate(operation);

        String name = operation.require(NAME).asString();
        String runtimeName = operation.hasDefined(RUNTIME_NAME) ? operation.get(RUNTIME_NAME).asString() : name;
        byte[] hash;
        if (operation.hasDefined(INPUT_STREAM_INDEX) && operation.hasDefined(HASH)) {
            throw new OperationFailedException(new ModelNode().set("Can't pass in both an input-stream-index and a hash"));
        } else if (operation.hasDefined(HASH)) {

            hash = operation.get(HASH).asBytes();
            if (!deploymentRepository.hasDeploymentContent(hash)) {
                throw new OperationFailedException(new ModelNode().set(String.format(
                        "No deployment content with hash %s is available in the deployment content repository.",
                        HashUtil.bytesToHexString(hash))));
            }
        } else if (operation.hasDefined(INPUT_STREAM_INDEX)) {
            if (!isMaster) {
                // This is a slave DC. We can't handle this operation; it should have been fixed up on the master DC
                throw new OperationFailedException(new ModelNode().set("A slave domain controller cannot accept deployment content uploads"));
            }

            try {
                hash = DeploymentUploadUtil.storeDeploymentContent(context, operation, deploymentRepository);
            } catch (IOException e) {
                throw new OperationFailedException(new ModelNode().set(e.toString()));
            }

        } else {
            throw new OperationFailedException(new ModelNode().set("Neither an attachment or a hash were passed in"));
        }

        ModelNode rootModel = context.getSubModel();
        ModelNode deployments = rootModel.get(DEPLOYMENT);

        ModelNode replaceNode = deployments.hasDefined(name) ? deployments.get(name) : null;
        if (replaceNode == null) {
            throw new OperationFailedException(new ModelNode().set(String.format("No deployment with name %s found", name)));
        }

        ModelNode deployNode = new ModelNode();
        deployNode.get(NAME).set(name);
        deployNode.get(RUNTIME_NAME).set(runtimeName);
        deployNode.get(HASH).set(hash);

        deployments.get(name).set(deployNode);

        if (rootModel.hasDefined(SERVER_GROUP)) {
            for (Property server : rootModel.get(SERVER_GROUP).asPropertyList()) {
                ModelNode serverConfig = server.getValue();
                if (serverConfig.hasDefined(DEPLOYMENT) && serverConfig.get(DEPLOYMENT).hasDefined(name)) {
                    ModelNode groupDeployNode = serverConfig.get(DEPLOYMENT, name);
                    groupDeployNode.get(RUNTIME_NAME).set(runtimeName);
                    groupDeployNode.get(HASH).set(hash);
                }
            }
        }

        ModelNode compensatingOp = operation.clone();
        compensatingOp.get(RUNTIME_NAME).set(replaceNode.get(RUNTIME_NAME).asString());
        compensatingOp.get(HASH).set(replaceNode.get(HASH).asBytes());
        if (operation.hasDefined(INPUT_STREAM_INDEX)) {
            operation.remove(INPUT_STREAM_INDEX);
        }

        resultHandler.handleResultComplete();

        return new BasicOperationResult(compensatingOp);
    }
}
