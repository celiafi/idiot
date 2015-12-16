import static java.nio.file.StandardWatchEventKinds.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchEvent.Kind;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GenericEventProcessor implements EventProcessor {

	Idiot idiot;
	Map<Path, Path> directories;

	public GenericEventProcessor(Idiot idiot) {
		this.idiot = idiot;
		this.directories = new HashMap<Path, Path>();
	}

	public void initialize() {
	}

	public void processEvent(WatchKey key, WatchEvent<?> event) {

		Kind<?> kind = event.kind();

		if (kind == OVERFLOW) {
			Idiot.LOGGER.warning("Overflow error. Manual check necessary.");
			return;
		}

		Path dir = Idiot.keys.get(key);
		Path remote = directories.get(Idiot.keys.get(key));
		Idiot.LOGGER.fine("Current watched directory: " + dir);
		Idiot.LOGGER.fine("Current remote directory: " + remote);

		WatchEvent<Path> ev = Idiot.cast(event);
		Path name = ev.context();
		Path pathToFile = dir.resolve(name);

		Idiot.LOGGER.config(event.kind().name() + ": " + pathToFile);

		if (new File(pathToFile.toString()).isDirectory()) {
			// FIXME: handle directory
		}

		else {
			processFile(kind, pathToFile, remote);
		}

	}

	private void processFile(Kind<?> kind, Path pathToFile, Path remote) {

		String commandString = createCommandStringForPath(kind, pathToFile, remote);
		waitUntilPathIsAccessible(pathToFile);
		try {
			executeExternalCommand(commandString);
		} catch (IOException e) {
			Idiot.logExceptionAsSevere(e, "IOException.");
			return;
		}
	}

	private void waitUntilPathIsAccessible(Path path) {
		while (!pathIsAccessible(path)) {
			try {
				Idiot.LOGGER.finest("File not accessible, sleeping.");
				TimeUnit.MILLISECONDS.sleep(100);
			} catch (InterruptedException e) {
				Idiot.logExceptionAsSevere(e, "Interrupted while sleeping.");
			}
		}
	}

	private boolean pathIsAccessible(Path path) {
		String fileName = path.toString();
		File file = new File(fileName);
		File sameFileName = new File(fileName);
		boolean accessible = file.renameTo(sameFileName);
		if (accessible) {
			Idiot.LOGGER.fine("File " + path + " is accessible.");
		}
		return accessible;
	}

	private String createCommandStringForPath(Kind<?> kind, Path pathToFile, Path pathToRemote) {
		String commandString = "";
		if (kind == ENTRY_CREATE) {
			commandString = this.idiot.getConfig().getProperty("createCommand");
		}
		else if (kind == ENTRY_MODIFY) {
			commandString = this.idiot.getConfig().getProperty("modifyCommand");
		}
		else if (kind == ENTRY_DELETE) {
			commandString = this.idiot.getConfig().getProperty("deleteCommand");
		}

		String commands[] = commandString.split("¤");

		for (int i = 0; i < commands.length; i++) {
			String s = commands[i].trim();
			if (s.equals("FILE")) {
				commands[i] = pathToFile.toString().replace("\\", "\\\\");
			} else if (s.equals("REMOTE")) {
				commands[i] = pathToRemote.toString().replace("\\", "\\\\");
			} else {
				commands[i] = s;
			}
		}
		commandString = String.join(" ", commands);
		Idiot.LOGGER.info("Parsed command string: " + commandString);
		return commandString;
	}

	private boolean executeExternalCommand(String command) throws IOException {
		Process process;

		process = Runtime.getRuntime().exec(command);
		InputStream stream = process.getInputStream();
		Reader reader = new InputStreamReader(stream);
		BufferedReader bReader = new BufferedReader(reader);
		String nextLine = null;
		while ((nextLine = bReader.readLine()) != null) {
			Idiot.LOGGER.config("Process output: " + nextLine);
		}
		int exitValue = process.exitValue();
		Idiot.LOGGER.config("Process exited with value: " + exitValue);
		if (exitValue == 0) {
			return true;
		} else {
			Idiot.LOGGER.severe("External command not succesful.");
			return false;
		}
	}

}
