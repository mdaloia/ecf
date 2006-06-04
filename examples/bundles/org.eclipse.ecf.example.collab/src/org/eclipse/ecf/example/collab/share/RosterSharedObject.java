package org.eclipse.ecf.example.collab.share;

import java.io.IOException;

import org.eclipse.ecf.core.events.ISharedObjectMessageEvent;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.sharedobject.AbstractSharedObject;
import org.eclipse.ecf.core.sharedobject.SharedObjectMsg;
import org.eclipse.ecf.core.sharedobject.SharedObjectMsgEvent;
import org.eclipse.ecf.core.util.Event;
import org.eclipse.ecf.example.collab.ui.CollabRosterView;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class RosterSharedObject extends AbstractSharedObject {

	CollabRosterView view;

	public RosterSharedObject(CollabRosterView view) {
		this.view = view;
	}

	public void sendMessageTo(ID targetID, String message) {
		try {
			super.sendSharedObjectMsgTo(targetID, SharedObjectMsg.createMsg(
					null, "handleMessage", message));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected Event handleSharedObjectMsgEvent(ISharedObjectMessageEvent event) {
		try {
			((SharedObjectMsgEvent) event.getData()).getSharedObjectMsg()
					.invoke(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return event;
	}

	protected void handleMessage(String message) {
		// XXX this should call the view back to display the message/do other things, etc
		System.out.println("RosterSharedObject.handleMessage(" + message + ")");
	}
	
	
	/**
	 * Message sender to show a view of given ID to target user.  This method sends
	 * a message which the receiver RosterSharedObject is expected to call {@link #handleShowViewWithID(String, String, Integer)}
	 * to handle the message
	 * 
	 * @param targetID the ID of the target user to receive the sendShowViewWithID request.  Must not be null
	 * @param viewID the String identifying the view to display
	 */
	public void sendShowViewWithID(ID targetID, String viewID, String secID, Integer mode) {
		try {
			SharedObjectMsg m = SharedObjectMsg.createMsg(null,
					"handleShowViewWithID", viewID, secID, mode);
			sendSharedObjectMsgTo(targetID,m);
			sendSharedObjectMsgToSelf(m);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * Message sender to show a view of given ID to target user.  This method sends
	 * a message which the receiver RosterSharedObject is expected to call {@link #handleShowView(String)}
	 * to handle the message
	 * 
	 * @param targetID the ID of the target user to receive the sendShowView request.  Must not be null
	 * @param viewID the String identifying the view to display
	 */
	public void sendShowView(ID targetID, String viewID) {
		if (targetID == null) return;
		try {
			SharedObjectMsg m = SharedObjectMsg.createMsg(null, "handleShowView",viewID);
			sendSharedObjectMsgTo(targetID,m);
			sendSharedObjectMsgToSelf(m);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * Message handler for {@link #sendShowViewWithID(ID, String, String, Integer)} message sender.  This message
	 * handler takes the given viewID and opens it on the local Display
	 * 
	 * @param viewID the viewID to display
	 */
	protected void handleShowViewWithID(final String viewID,
			final String secID, final Integer mode) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				try {
					IWorkbenchWindow ww = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow();
					IWorkbenchPage wp = ww.getActivePage();
					if (wp == null)
						throw new NullPointerException("showViewWithID(" + viewID + ") "
								+ "workbench page is null");
					wp.showView(viewID, secID, mode.intValue());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	/**
	 * Message handler for {@link #sendShowView(ID, String)} message sender.  This message
	 * handler takes the given viewID and opens it on the local Display
	 * 
	 * @param viewID the viewID to display
	 */
	protected void handleShowView(final String viewID) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				try {
					IWorkbenchWindow ww = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow();
					IWorkbenchPage wp = ww.getActivePage();
					if (wp == null)
						throw new NullPointerException("showView(" + viewID + ") "
								+ "workbench page is null");
					wp.showView(viewID);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

}
