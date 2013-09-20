/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.lens;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.common.mapping.MappingFactory;
import com.evolveum.midpoint.common.security.Authorization;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContainerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.SimpleDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
public class AssignmentEvaluator<F extends FocusType> {
	
	private static final Trace LOGGER = TraceManager.getTrace(AssignmentEvaluator.class);

	private RepositoryService repository;
	private ObjectDeltaObject<F> userOdo;
	private LensContext<F> lensContext;
	private String channel;
	private ObjectResolver objectResolver;
	private PrismContext prismContext;
	private MappingFactory valueConstructionFactory;
	
	public RepositoryService getRepository() {
		return repository;
	}

	public void setRepository(RepositoryService repository) {
		this.repository = repository;
	}
	
	public ObjectDeltaObject<F> getUserOdo() {
		return userOdo;
	}

	public void setUserOdo(ObjectDeltaObject<F> userOdo) {
		this.userOdo = userOdo;
	}

	public LensContext<F> getLensContext() {
		return lensContext;
	}

	public void setLensContext(LensContext<F> lensContext) {
		this.lensContext = lensContext;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public ObjectResolver getObjectResolver() {
		return objectResolver;
	}

	public void setObjectResolver(ObjectResolver objectResolver) {
		this.objectResolver = objectResolver;
	}

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public MappingFactory getValueConstructionFactory() {
		return valueConstructionFactory;
	}

	public void setValueConstructionFactory(MappingFactory valueConstructionFactory) {
		this.valueConstructionFactory = valueConstructionFactory;
	}

	public SimpleDelta<Assignment> evaluate(SimpleDelta<AssignmentType> assignmentTypeDelta, ObjectType source, String sourceDescription,
			OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		SimpleDelta<Assignment> delta = new SimpleDelta<Assignment>();
		delta.setType(assignmentTypeDelta.getType());
		for (AssignmentType assignmentType : assignmentTypeDelta.getChange()) {
			assertSource(source, assignmentType);
			Assignment assignment = evaluate(assignmentType, source, sourceDescription, result);
			delta.getChange().add(assignment);
		}
		return delta;
	}
	
	public Assignment evaluate(AssignmentType assignmentType, ObjectType source, String sourceDescription, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		assertSource(source, assignmentType);
		Assignment assignment = new Assignment();
		AssignmentPath assignmentPath = new AssignmentPath();
		AssignmentPathSegment assignmentPathSegment = new AssignmentPathSegment(assignmentType, null);
		assignmentPathSegment.setSource(source);
		
		evaluateAssignment(assignment, assignmentPathSegment, source, sourceDescription, assignmentPath, result);
		
		return assignment;
	}
	
	private void evaluateAssignment(Assignment assignment, AssignmentPathSegment assignmentPathSegment, ObjectType source, String sourceDescription,
			AssignmentPath assignmentPath, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		assertSource(source, assignment);
		
		assignmentPath.add(assignmentPathSegment);
		
		AssignmentType assignmentType = assignmentPathSegment.getAssignmentType();
		
		checkSchema(assignmentType, sourceDescription);
		
		if (assignmentType.getAccountConstruction() != null || assignmentType.getConstruction() != null) {
			
			evaluateConstruction(assignment, assignmentPathSegment, source, sourceDescription, assignmentPath, result);
			
		} else if (assignmentType.getTarget() != null) {
			
			evaluateTarget(assignment, assignmentPathSegment, assignmentType.getTarget(), source, null, sourceDescription, assignmentPath, result);
			
		} else if (assignmentType.getTargetRef() != null) {
			
			evaluateTargetRef(assignment, assignmentPathSegment, assignmentType.getTargetRef(), source, sourceDescription, assignmentPath, result);

		} else {
			throw new SchemaException("No target or construcion in assignment in " + source);
		}
		
		assignmentPath.remove(assignmentPathSegment);
	}

	private void evaluateConstruction(Assignment assignment, AssignmentPathSegment assignmentPathSegment, ObjectType source, String sourceDescription,
			AssignmentPath assignmentPath, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		assertSource(source, assignment);
		
		AssignmentType assignmentType = assignmentPathSegment.getAssignmentType();
		ConstructionType constructionType = assignmentType.getConstruction();
		if (constructionType == null) {
			constructionType = assignmentType.getAccountConstruction();
			constructionType.setKind(ShadowKindType.ACCOUNT);
		}
		AccountConstruction<F> accContruction = new AccountConstruction<F>(constructionType, source);
		// We have to clone here as the path is constantly changing during evaluation
		accContruction.setAssignmentPath(assignmentPath.clone());
		accContruction.setUserOdo(userOdo);
		accContruction.setLensContext(lensContext);
		accContruction.setObjectResolver(objectResolver);
		accContruction.setPrismContext(prismContext);
		accContruction.setValueConstructionFactory(valueConstructionFactory);
		accContruction.setOriginType(OriginType.ASSIGNMENTS);
		accContruction.setChannel(channel);
		
		accContruction.evaluate(result);
		
		assignment.addAccountConstruction(accContruction);
		assignmentPathSegment.setEvaluatedAssignment(assignment);
	}

	private void evaluateTargetRef(Assignment assignment, AssignmentPathSegment assignmentPathSegment, ObjectReferenceType targetRef, ObjectType source,
			String sourceDescription, AssignmentPath assignmentPath, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		assertSource(source, assignment);
		
		String oid = targetRef.getOid();
		if (oid == null) {
			throw new SchemaException("The OID is null in assignment targetRef in "+ObjectTypeUtil.toShortString(source));
		}
		// Target is referenced, need to fetch it
		Class<? extends ObjectType> clazz = null;
		if (targetRef.getType() != null) {
			clazz = (Class<? extends ObjectType>) prismContext.getSchemaRegistry().determineCompileTimeClass(targetRef.getType());
			if (clazz == null) {
				throw new SchemaException("Cannot determine type from " + targetRef.getType() + " in target reference in " + assignment + " in " + sourceDescription);
			}
		} else {
			throw new SchemaException("Missing type in target reference in " + assignment + " in " + sourceDescription);
		}
		PrismObject<? extends ObjectType> target = null;
		try {
			target = repository.getObject(clazz, oid, null, result);
			if (target == null) {
				throw new IllegalArgumentException("Got null target from repository, oid:"+oid+", class:"+clazz+" (should not happen, probably a bug) in "+sourceDescription);
			}
		} catch (ObjectNotFoundException ex) {
			// Do not throw an exception. We don't have referential integrity. Therefore if a role is deleted then throwing
			// an exception would prohibit any operations with the users that have the role, including removal of the reference.
			// The failure is recorded in the result and we will log it. It should be enough.
			LOGGER.error(ex.getMessage()+" in assignment target reference in "+sourceDescription,ex);
//			throw new ObjectNotFoundException(ex.getMessage()+" in assignment target reference in "+sourceDescription,ex);
		}
		
		if (target != null) {
			evaluateTarget(assignment, assignmentPathSegment, target.asObjectable(), source, targetRef.getRelation(), sourceDescription, assignmentPath, result);
		}
	}


	private void evaluateTarget(Assignment assignment, AssignmentPathSegment assignmentPathSegment, ObjectType target, 
			ObjectType source, QName relation, String sourceDescription,
			AssignmentPath assignmentPath, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		assertSource(source, assignment);
		assignmentPathSegment.setTarget(target);
		if (target instanceof AbstractRoleType) {
			evaluateAbstractRole(assignment, assignmentPathSegment, (AbstractRoleType)target, source, sourceDescription, assignmentPath, result);
			if (target instanceof OrgType) {
				PrismReferenceValue refVal = new PrismReferenceValue();
				refVal.setObject(target.asPrismObject());
				refVal.setRelation(relation);
				assignment.addOrgRefVal(refVal);
			} 
		} else {
			throw new SchemaException("Unknown assignment target type "+ObjectTypeUtil.toShortString(target)+" in "+sourceDescription);
		}
	}

	private void evaluateAbstractRole(Assignment assignment, AssignmentPathSegment assignmentPathSegment, AbstractRoleType role, ObjectType source, String sourceDescription,
			AssignmentPath assignmentPath, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		assertSource(source, assignment);
		for (AssignmentType roleInducement : role.getInducement()) {
			AssignmentPathSegment roleAssignmentPathSegment = new AssignmentPathSegment(roleInducement, null);
			roleAssignmentPathSegment.setSource(role);
			String subSourceDescription = role+" in "+sourceDescription;
			evaluateAssignment(assignment, roleAssignmentPathSegment, role, subSourceDescription, assignmentPath, result);
		}
		for(AuthorizationType authorizationType: role.getAuthorization()) {
			Authorization authorization = createAuthorization(authorizationType);
			assignment.addAuthorization(authorization);
		}
	}

	private Authorization createAuthorization(AuthorizationType authorizationType) {
		Authorization authorization = new Authorization(authorizationType);
		return authorization;
	}

	private void assertSource(ObjectType source, Assignment assignment) {
		if (source == null) {
			throw new IllegalArgumentException("Source cannot be null (while evaluating assignment "+assignment+")");
		}
	}
	
	private void assertSource(ObjectType source, AssignmentType assignmentType) {
		if (source == null) {
			throw new IllegalArgumentException("Source cannot be null (while evaluating assignment "+assignmentType+")");
		}
	}
	
	private void checkSchema(AssignmentType assignmentType, String sourceDescription) throws SchemaException {
		PrismContainerValue<AssignmentType> assignmentContainerValue = assignmentType.asPrismContainerValue();
		PrismContainerable<AssignmentType> assignmentContainer = assignmentContainerValue.getParent();
		if (assignmentContainer == null) {
			throw new SchemaException("The assignment "+assignmentType+" does not have a parent in "+sourceDescription);
		}
		if (assignmentContainer.getDefinition() == null) {
			throw new SchemaException("The assignment "+assignmentType+" does not have definition in "+sourceDescription);
		}
		PrismContainer<Containerable> extensionContainer = assignmentContainerValue.findContainer(AssignmentType.F_EXTENSION);
		if (extensionContainer != null) {
			if (extensionContainer.getDefinition() == null) {
				throw new SchemaException("Extension does not have a definition in assignment "+assignmentType+" in "+sourceDescription);
			}
			for (Item<?> item: extensionContainer.getValue().getItems()) {
				if (item == null) {
					throw new SchemaException("Null item in extension in assignment "+assignmentType+" in "+sourceDescription);
				}
				if (item.getDefinition() == null) {
					throw new SchemaException("Item "+item+" has no definition in extension in assignment "+assignmentType+" in "+sourceDescription);
				}
			}
		}
	}

}
