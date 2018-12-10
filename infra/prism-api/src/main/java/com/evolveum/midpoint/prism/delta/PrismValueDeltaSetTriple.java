/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Processor;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.Collection;
import java.util.Iterator;

/**
 * DeltaSetTriple that is limited to hold prism values. By limiting to the PrismValue descendants we gain advantage to be
 * clonnable and ability to compare real values.
 *
 * @author Radovan Semancik
 */
public interface PrismValueDeltaSetTriple<V extends PrismValue> extends DeltaSetTriple<V>, Visitable {

	/**
	 * Distributes a value in this triple similar to the placement of other value in the other triple.
	 * E.g. if the value "otherMember" is in the zero set in "otherTriple" then "myMember" will be placed
	 * in zero set in this triple.
	 */
	<O extends PrismValue> void distributeAs(V myMember, PrismValueDeltaSetTriple<O> otherTriple, O otherMember);

	Class<V> getValueClass();

	Class<?> getRealValueClass();

	boolean isRaw();

	void applyDefinition(ItemDefinition itemDefinition) throws SchemaException;

	/**
	 * Sets specified source type for all values in all sets
	 */
	void setOriginType(OriginType sourceType);

	/**
	 * Sets specified origin object for all values in all sets
	 */
	void setOriginObject(Objectable originObject);

	void removeEmptyValues(boolean allowEmptyValues);

	PrismValueDeltaSetTriple<V> clone();

	void checkConsistence();

	@Override
	void accept(Visitor visitor);

	void checkNoParent();

}
