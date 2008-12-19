/*******************************************************************************
* Copyright (c) 2008 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.ecf.core.util;

/**
 * Timeout exception thrown when timeout occurs
 * 
 */
public class TimeoutException extends InterruptedException {
	private static final long serialVersionUID = 3256439218179158322L;

	public final long duration;

	public TimeoutException(long time) {
		duration = time;
	}

	public TimeoutException(long time, String message) {
		super(message);
		duration = time;
	}

	public long getDuration() {
		return duration;
	}
}