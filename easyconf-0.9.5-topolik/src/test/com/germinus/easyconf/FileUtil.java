/*
 * Copyright 2004-2005 Germinus XXI, Liferay LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * This is a derived work from source code taken from Liferay.
 */
package com.germinus.easyconf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

import java.nio.channels.FileChannel;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.commons.lang.StringUtils;

/**
 * File utility methods
 * @author  Brian Wing Shun Chan, Jorge Ferrer
 *
 */
public class FileUtil {

	public static void copyDirectory(
		String sourceDirName, String destinationDirName) {

		copyDirectory(new File(sourceDirName), new File(destinationDirName));
	}

	public static void copyDirectory(File source, File destination) {
		if (source.exists() && source.isDirectory()) {
			if (!destination.exists()) {
				destination.mkdirs();
			}

			File[] fileArray = source.listFiles();

			for (int i = 0; i < fileArray.length; i++) {
				if (fileArray[i].isDirectory()) {
					copyDirectory(
						fileArray[i],
						new File(destination.getPath() + File.separator
							+ fileArray[i].getName()));
				}
				else {
					copyFile(
						fileArray[i],
						new File(destination.getPath() + File.separator
							+ fileArray[i].getName()));
				}
			}
		}
	}

	public static void copyFile(
		String sourceFileName, String destinationFileName) {

		copyFile(new File(sourceFileName), new File(destinationFileName));
	}

	public static void copyFile(File source, File destination) {
		if (!source.exists()) {
			return;
		}

		if ((destination.getParentFile() != null) &&
			(!destination.getParentFile().exists())) {

			destination.getParentFile().mkdirs();
		}

		try {
			FileChannel srcChannel = new FileInputStream(source).getChannel();
			FileChannel dstChannel = new FileOutputStream(
				destination).getChannel();

			dstChannel.transferFrom(srcChannel, 0, srcChannel.size());

			srcChannel.close();
			dstChannel.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public static void copyFileLazy(String source, String destination)
		throws IOException {

		String oldContent = null;
		try {
			oldContent = FileUtil.read(source);
		}
		catch (FileNotFoundException fnfe) {
			return;
		}

		String newContent = null;
		try {
			newContent = FileUtil.read(destination);
		}
		catch (FileNotFoundException fnfe) {
		}

		if (oldContent == null || !oldContent.equals(newContent)) {
			FileUtil.copyFile(source, destination);
		}
	}

	public static void deltree(String directory) {
		deltree(new File(directory));
	}

	public static void deltree(File directory) {
		if (directory.exists() && directory.isDirectory()) {
			File[] fileArray = directory.listFiles();

			for (int i = 0; i < fileArray.length; i++) {
				if (fileArray[i].isDirectory()) {
					deltree(fileArray[i]);
				}
				else {
					fileArray[i].delete();
				}
			}

			directory.delete();
		}
	}

	public static byte[] getBytes(File file) throws IOException {
		if (file == null || !file.exists()) {
			return null;
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		FileInputStream in = new FileInputStream(file);

		int c = in.read();

		while (c != -1) {
			out.write(c);
			c = in.read();
		}

		in.close();
		out.close();

		return out.toByteArray();
	}

	public static String getPath(String fullFileName) {
		int pos = fullFileName.lastIndexOf("/");

		if (pos == -1) {
			pos = fullFileName.lastIndexOf("\\");
		}

		String shortFileName = fullFileName.substring(0, pos);

		if (StringUtils.isEmpty(shortFileName)) {
			return "/";
		}

		return shortFileName;
	}

	public static String getShortFileName(String fullFileName) {
		int pos = fullFileName.lastIndexOf("/");

		if (pos == -1) {
			pos = fullFileName.lastIndexOf("\\");
		}

		String shortFileName =
			fullFileName.substring(pos + 1, fullFileName.length());

		return shortFileName;
	}

	public static boolean exists(String fileName) {
		File file = new File(fileName);

		return file.exists();
	}

	public static String[] listDirs(String fileName) throws IOException {
		return listDirs(new File(fileName));
	}

	public static String[] listDirs(File file) throws IOException {
		List dirs = new ArrayList();

		File[] fileArray = file.listFiles();

		for (int i = 0; i < fileArray.length; i++) {
			if (fileArray[i].isDirectory()) {
				dirs.add(fileArray[i].getName());
			}
		}

		return (String[])dirs.toArray(new String[0]);
	}

	public static String[] listFiles(String fileName) throws IOException {
		return listFiles(new File(fileName));
	}

	public static String[] listFiles(File file) throws IOException {
		List files = new ArrayList();

		File[] fileArray = file.listFiles();

		for (int i = 0; i < fileArray.length; i++) {
			if (fileArray[i].isFile()) {
				files.add(fileArray[i].getName());
			}
		}

		return (String[])files.toArray(new String[0]);
	}

	public static void mkdirs(String pathName) {
		File file = new File(pathName);
		file.mkdirs();
	}

	public static boolean move(
		String sourceFileName, String destinationFileName) {

		return move(new File(sourceFileName), new File(destinationFileName));
	}

	public static boolean move(File source, File destination) {
		if (!source.exists()) {
			return false;
		}

		destination.delete();

		return source.renameTo(destination);
	}

	public static String read(String fileName) throws IOException {
		return read(new File(fileName));
	}

	public static String read(File file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));

		StringBuffer sb = new StringBuffer();
		String line = null;

		while ((line = br.readLine()) != null) {
			sb.append(line).append('\n');
		}

		br.close();

		return sb.toString().trim();
	}

	public static String replaceSeparator(String fileName) {
		return StringUtils.replace(fileName, "\\", "/");
	}

	public static List toList(Reader reader) {
		List list = new ArrayList();

		try {
			BufferedReader br = new BufferedReader(reader);

			StringBuffer sb = new StringBuffer();
			String line = null;

			while ((line = br.readLine()) != null) {
				list.add(line);
			}

			br.close();
		}
		catch (IOException ioe) {
		}

		return list;
	}

	public static List toList(String fileName) {
		try {
			return toList(new FileReader(fileName));
		}
		catch (IOException ioe) {
			return new ArrayList();
		}
	}

	public static Properties toProperties(FileInputStream fis) {
		Properties props = new Properties();

		try {
			props.load(fis);
		}
		catch (IOException ioe) {
		}

		return props;
	}

	public static Properties toProperties(String fileName) {
		try {
			return toProperties(new FileInputStream(fileName));
		}
		catch (IOException ioe) {
			return new Properties();
		}
	}

	public static void write(File file, String s) throws IOException {
		if (file.getParent() != null) {
			mkdirs(file.getParent());
		}

		BufferedWriter bw = new BufferedWriter(new FileWriter(file));

		bw.flush();
		bw.write(s);
		bw.flush();

		bw.close();
	}

	public static void write(String fileName, String s) throws IOException {
		write(new File(fileName), s);
	}

	public static void write(String pathName, String fileName, String s)
		throws IOException {

		write(new File(pathName, fileName), s);
	}

    public static void write(File dest, Properties props) throws IOException {
        write(dest, propertiesToString(props));             
    }
    
    public static String propertiesToString(Properties p) {
        StringBuffer sb = new StringBuffer();

        Enumeration enu = p.propertyNames();

        while (enu.hasMoreElements()) {
                String key = (String)enu.nextElement();

                sb.append(key);
                sb.append("=");
                sb.append(p.getProperty(key));
                sb.append("\n");
        }

        return sb.toString();
}

    public static void writeAsJAR(File dest, String propsFileName, Properties props) 
    	throws FileNotFoundException, IOException {
        JarOutputStream out = new JarOutputStream(new FileOutputStream(dest));
        JarEntry propertiesFile = new JarEntry(propsFileName);
        propertiesFile.setExtra(propertiesToString(props).getBytes());
        out.putNextEntry(propertiesFile);
        out.close();        
    }


}