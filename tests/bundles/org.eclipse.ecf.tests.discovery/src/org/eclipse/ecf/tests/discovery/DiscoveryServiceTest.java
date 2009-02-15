/****************************************************************************
* Copyright (c) 2004 Composent, Inc. and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Composent, Inc. - initial API and implementation
*****************************************************************************/

package org.eclipse.ecf.tests.discovery;

import org.eclipse.ecf.core.ContainerCreateException;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.discovery.IDiscoveryContainerAdapter;
import org.eclipse.ecf.discovery.service.IDiscoveryService;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public abstract class DiscoveryServiceTest extends DiscoveryTest {

	private static final String NO_PROVIDER_REGISTERED = "No discovery provider by that name seems to be registered";

	/**
	 * @param name
	 */
	public DiscoveryServiceTest(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ecf.tests.discovery.DiscoveryTest#getAdapter(java.lang.Class)
	 */
	protected IDiscoveryContainerAdapter getAdapter(Class notNeeded) {
		final ServiceTracker serviceTracker = Activator.getDefault().getDiscoveryServiceTracker();
		assertNotNull(NO_PROVIDER_REGISTERED, serviceTracker);
		final ServiceReference[] serviceReferences = serviceTracker.getServiceReferences();
		assertNotNull(NO_PROVIDER_REGISTERED, serviceReferences);
		for(int i = 0; i < serviceReferences.length; i++) {
			ServiceReference sr = serviceReferences[i];
			if(containerUnderTest.equals(sr.getProperty(IDiscoveryService.CONTAINER_NAME))) {
				return (IDiscoveryContainerAdapter) serviceTracker.getService(sr);
			}
		}
		fail(NO_PROVIDER_REGISTERED);
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ecf.tests.discovery.DiscoveryTest#getContainer(java.lang.String)
	 */
	protected IContainer getContainer(String containerUnderTest)
			throws ContainerCreateException {
		IContainer container = (IContainer) getAdapter(null);
		// we might get a pre used container but the tests expect a fresh instance
		container.disconnect();
		return container;
	}

	public void testDispose() {
		// do nothing, we cannot dispose because we intent to reuse the container in the next test
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		discoveryContainer.unregisterService(serviceInfo);
		discoveryContainer = null;
		// DST recycle the IDCA instance, thus we cannot call dispose() on the container!!!
		container.disconnect();
		container = null;
	}
}
