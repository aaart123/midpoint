/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.users.dto;

import java.io.Serializable;

/**
 * @author mserbak
 */
public class SubmitPersonalProvider implements Serializable {
	private String attribute;
	private String oldValue;
	private String newValue;

	public SubmitPersonalProvider(String attribute, String oldValue, String newValue) {
		this.attribute = attribute;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	public String getAttribute() {
		return attribute;
	}

	public String getOldValue() {
		return oldValue;
	}

	public String getNewValue() {
		return newValue;
	}

	
}
