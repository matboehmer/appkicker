package org.appkicker.app.data.entities;

/**
 * This class represents an app and contains all the information which we have
 * about an app that we can give to the user.
 * 
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public class App {

	/**
	 * Technical id of the app, provided by the server (is unique, when app data
	 * was retrieved from server).
	 */
	public int id = -1;

	/** package name of the app, can serve as a unique identifier */
	public String packagename;

	/** the title of the app for the user */
	public String name;

	/** the category of the app on english */
	public String category;

	/** textual description of the application */
	public String description;

	/** rating of the app on the market */
	public double rating;

	/** price of the app, currently in USD */
	public double price;

	/**
	 * timestamp of the moment, when the information was last queried from the
	 * server
	 */
	public long timestamp;
	
	/**
	 * server defines whether an app is recommendable by checking the following:
	 * 1. appName is not null
	 * 2. hasIcon = true
	 * 3. doRecommend = true
	 * 4. inAppstore = true
	 */
	public int recommendable;

}