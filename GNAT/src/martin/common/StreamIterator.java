package martin.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class StreamIterator implements Iterator<String>, Iterable<String> {
	private BufferedReader stream;
	String line;
	private boolean ignoreHashedLines;

	public StreamIterator(File file) {
		this(file, false);
	}

	public StreamIterator(File file, boolean ignoreHashedLines) {
		this.ignoreHashedLines = ignoreHashedLines;

		try {
			this.stream = new BufferedReader(new FileReader(file));
			line = stream.readLine();

			if (ignoreHashedLines)
				while (line != null && line.startsWith("#"))
					line = stream.readLine();

		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public StreamIterator(InputStream inputStream, boolean ignoreHashedLines) {
		this.ignoreHashedLines = ignoreHashedLines;
		try {
			this.stream = new BufferedReader(new InputStreamReader(inputStream));
			line = stream.readLine();

			if (ignoreHashedLines)
				while (line != null && line.startsWith("#"))
					line = stream.readLine();
			if (line == null)
				stream.close();
			
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public boolean hasNext() {
		return line != null;
	}

	public String next() {
		if (!hasNext())
			throw new NoSuchElementException();

		String res = line;
		try {
			line = stream.readLine();
			if (ignoreHashedLines)
				while (line != null && line.startsWith("#"))
					line = stream.readLine();

			if (line == null)
				stream.close();

		} catch (IOException e) {
			System.err.println(e);
			e.printStackTrace();
			System.exit(-1);
		}
		return res;
	}

	public void remove() {
		throw new IllegalStateException();
	}

	public Iterator<String> iterator() {
		return this;
	}
}
