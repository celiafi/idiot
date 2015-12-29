import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.io.IOException;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.*;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Idiot {
	private WatchService watcher;
	private ArrayList<EventProcessor> eventProcessors;
	static Map<WatchKey, Path> keys;

	String passphrase;

	public final static Logger LOGGER = Logger.getLogger(Idiot.class.getName());
	private Level logLevel;
	private static FileHandler fh;
	private static SimpleFormatter formatter;
	private Properties config;

	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	public Idiot() throws IOException {

		readConfig();
		initLogger();

		this.eventProcessors = new ArrayList<EventProcessor>();

		// FIXME: finish GEP
		String mode = getConfig().getProperty("mode");

		switch (mode) {
		case "encrypt":
			eventProcessors.add(new DefaultEventProcessor(this));
			break;
		case "generic":
			eventProcessors.add(new GenericEventProcessor(this));
			break;
		default:
			break;
		}

		this.logLevel = Level.parse(getConfig().getProperty("logLevel"));
		LOGGER.setLevel(logLevel);
		LOGGER.info("Log level " + logLevel);

		this.watcher = FileSystems.getDefault().newWatchService();
		keys = new HashMap<WatchKey, Path>();

		passphrase = getConfig().getProperty("passphrase");

		register();
		initEventProcessors();
	}

	private void initLogger() {
		try {
			fh = new FileHandler(getConfig().getProperty("logLocation"));
			LOGGER.addHandler(fh);
			formatter = new SimpleFormatter();
			fh.setFormatter(formatter);
		} catch (SecurityException e) {
			LOGGER.severe("Security exception during initialization.");
			LOGGER.severe(e.getStackTrace().toString());
		} catch (IOException e) {
			LOGGER.severe("IO exception during initialization.");
			LOGGER.severe(e.getStackTrace().toString());
		}
	}

	private void readConfig() throws IOException {
		this.config = new Properties();
		InputStream in;

		in = this.getClass().getResourceAsStream("idiot.properties");

		getConfig().load(in);
		in.close();
	}

	private void initEventProcessors() {
		for (int i = 0; i < eventProcessors.size(); i++) {
			eventProcessors.get(i).initialize();
		}
	}

	private void register() throws IOException {

		int i = 1;
		String p;
		while ((p = getConfig().getProperty("watchDir" + i)) != null) {
			Path watchDir = Paths.get(p);
			if (getConfig().getProperty("remoteDir" + i) != null) {
				Path remoteDir = Paths.get(getConfig().getProperty(
						"remoteDir" + i));
				LOGGER.info("Watching " + watchDir + ", - Remote is "
						+ remoteDir);

				for (int j = 0; j < eventProcessors.size(); j++) {
					EventProcessor ep = eventProcessors.get(j);
					if (ep instanceof DefaultEventProcessor) {
						DefaultEventProcessor dep = (DefaultEventProcessor) ep;
						dep.directories.put(watchDir, remoteDir);
					}
					if (ep instanceof GenericEventProcessor) {
						GenericEventProcessor gep = (GenericEventProcessor) ep;
						gep.directories.put(watchDir, remoteDir);
					}
				}
			} else {
				LOGGER.severe("The amount of remote directories is not equivalent "
						+ "to the amount of watched directories! "
						+ "Please check your configuration file.");
				break;
			}
			if (getConfig().getProperty("passphrase" + i) != null) {
				String passphrase = getConfig().getProperty("passphrase" + i);
				LOGGER.info("Watching " + watchDir + ", - Passphrase is "
						+ passphrase);

				for (int j = 0; j < eventProcessors.size(); j++) {
					EventProcessor ep = eventProcessors.get(j);
					if (ep instanceof DefaultEventProcessor) {
						DefaultEventProcessor dep = (DefaultEventProcessor) ep;
						dep.passphrases.put(watchDir, passphrase);
					}
				}

				registerDirectory(watchDir);
				i++;
			} else {
				LOGGER.severe("The amount of passphrases is not equivalent "
						+ "to the amount of watched directories! "
						+ "Please check your configuration file.");
				break;
			}
			LOGGER.fine("Watcher created succesfully.");
		}
	}

	private void registerDirectory(Path dir) throws IOException {
		LOGGER.config("Registering directory " + dir);

		@SuppressWarnings("rawtypes")
		Kind[] events = { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW };

		WatchKey key = dir.register(watcher, events);
		keys.put(key, dir);
	}

	private void processEvents() {

		LOGGER.info("Initialization succesful.");
		for (;;) {

			WatchKey key;

			try {
				key = getWatchKey();
			} catch (InterruptedException e) {
				logExceptionAsSevere(e, "Watcher interrupted.");
				return;
			}

			processEvents(key);

			boolean valid = key.reset();
			if (!valid) {
				removeKey(key);
				if (keys.isEmpty()) {
					LOGGER.severe("All directories are inaccessible. Halting.");
					break;
				}
			}

		}
	}

	private void removeKey(WatchKey key) {
		LOGGER.severe("Directory key " + key
				+ " is no longer accessible. Removing from keys.");
		keys.remove(key);
	}

	static void logExceptionAsSevere(Exception e, String message) {
		LOGGER.severe(message);
		LOGGER.severe(e.toString());
		LOGGER.severe(e.getMessage());
		return;
	}

	private WatchKey getWatchKey() throws InterruptedException {
		WatchKey key = watcher.take();
		LOGGER.finest("Watch key: " + key);
		return key;
	}

	/**
	 * Process events for one key.
	 * 
	 * @param key
	 */
	private void processEvents(WatchKey key) {
		if (!directoryIsNull(key)) {
			for (WatchEvent<?> event : key.pollEvents()) {
				for (EventProcessor processor : eventProcessors) {
					processor.processEvent(key, event);
				}
			}
		}
	}

	private boolean directoryIsNull(WatchKey key) {
		if (keys.get(key) == null) {
			LOGGER.warning("Watched directory is non-existent or not recognized.");
			return true;
		}
		return false;
	}

	public static void main(String[] args) throws IOException {
		new Idiot().processEvents();
	}

	protected Properties getConfig() {
		return config;
	}

}
