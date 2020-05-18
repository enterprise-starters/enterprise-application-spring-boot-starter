/**
 * 
 */
package de.enterprise.starters.jpa;

/**
 * @author Malte Geßner
 *
 */
public final class TransactionTimeouts {

	private TransactionTimeouts() {

	}

	/**
	 * define 10 seconds for timeout.
	 */
	public final static int SHORT = 10;
	/**
	 * define 20 seconds for timeout.
	 */
	public final static int MIDDLE = 20;
	/**
	 * define 30 seconds for timeout.
	 */
	public final static int LONG = 30;
	/**
	 * define 180 seconds for timeout.
	 */
	public final static int VERY_LONG = 180;
}
