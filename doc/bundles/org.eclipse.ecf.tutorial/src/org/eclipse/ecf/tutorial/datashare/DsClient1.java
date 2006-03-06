package org.eclipse.ecf.tutorial.datashare;

import java.util.HashMap;
import org.eclipse.ecf.core.ContainerFactory;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.identity.IDFactory;
import org.eclipse.ecf.core.util.ECFException;
import org.eclipse.ecf.datashare.IChannel;
import org.eclipse.ecf.datashare.IChannelContainer;
import org.eclipse.ecf.datashare.IChannelListener;
import org.eclipse.ecf.datashare.events.IChannelEvent;

public class DsClient1 {

	protected static final String CONTAINER_TYPE = "ecf.generic.channel";
	protected static final String TARGET_SERVER = "ecftcp://localhost:3282/server";
	
	IContainer container = null;
	IChannel channel = null;
	
	protected IChannel createChannel(IContainer container) throws ECFException {
		IChannelContainer channelContainer = (IChannelContainer) container.getAdapter(IChannelContainer.class);
		if (channelContainer == null) throw new NullPointerException("cannot get channel container adapter");
		
		ID channelID = IDFactory.getDefault().createID(channelContainer.getChannelNamespace(), "channel1");
		IChannelListener channelListener = new IChannelListener() {
			public void handleChannelEvent(IChannelEvent event) {
				System.out.println("handleChannelEvent("+event+")");
			}};
		return channelContainer.createChannel(channelID, channelListener, new HashMap());
	}
	
	public void createAndConnect() throws ECFException {
		// create container instance from ECF ContainerFactory
		container = ContainerFactory.getDefault().createContainer(CONTAINER_TYPE);
		// create channel
		channel = createChannel(container);
		// create target ID
		ID targetID = IDFactory.getDefault().createID(container.getConnectNamespace(),TARGET_SERVER);
		// connect container to target
		container.connect(targetID, null);
	}
	
	public void sendData() throws ECFException {
		for(int i=0; i < 5; i++ ) {
			channel.sendMessage("hello".getBytes());
		}
	}

}
