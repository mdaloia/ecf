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

package org.eclipse.ecf.presence.chatroom;

import org.eclipse.ecf.core.identity.ID;

/**
 * Chat room message event.
 */
public class ChatRoomMessageEvent implements IChatRoomMessageEvent {

	private static final long serialVersionUID = 241668017694136249L;

	protected ID fromID;
	
	protected IChatRoomMessage chatMessage;
	
	public ChatRoomMessageEvent(ID fromID, IChatRoomMessage message) {
		this.fromID = fromID;
		this.chatMessage = message;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ecf.presence.chatroom.IChatRoomMessageEvent#getChatRoomMessage()
	 */
	public IChatRoomMessage getChatRoomMessage() {
		return chatMessage;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ecf.presence.IIMMessageEvent#getFromID()
	 */
	public ID getFromID() {
		return fromID;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer("ChatRoomMessageEvent[");
		buf.append("fromID=").append(getFromID());
		buf.append(";chatMessage=").append(chatMessage).append("]");
		return buf.toString();
	}

}
