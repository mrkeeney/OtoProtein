package my.vaadin.ffx;

import com.vaadin.server.Page;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

public final class NotificationSender {

	private static UI mainUI = null;
	
	public static void sendNotification(final String message, final Type notificationType) {
		if (mainUI == null) {
			throw new IllegalArgumentException("UI for notification handling hasn't been set yet.");
		}
		mainUI.access(() -> {
			final Notification n = new Notification(message, notificationType);
			n.setDelayMsec(3000);
			n.show(Page.getCurrent());
			//mainUI.push();
		});
	}

	public static void setUi(final UI ui) {
		mainUI = ui;
	}
}
