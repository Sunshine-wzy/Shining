package io.github.sunshinewzy.shining.api.machine.event;

/**
 * A type characterizing events that may be cancelled by a plugin or the server.
 */
public interface MachineCancellable {

	/**
	 * Gets the cancellation state of this event. A cancelled event will not
	 * be executed in the server, but will still pass to other plugins
	 *
	 * @return true if this event is cancelled
	 */
	boolean isCancelled();

	/**
	 * Sets the cancellation state of this event. A cancelled event will not
	 * be executed in the server, but will still pass to other plugins.
	 *
	 * @param cancel true if you wish to cancel this event
	 */
	void setCancelled(boolean cancel);
	
}
