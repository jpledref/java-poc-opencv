package com.shcompany.java.poc.opencv;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;

public class FSProvider {
	/* Singleton */
	private static FSProvider INSTANCE = new FSProvider();

	public static FSProvider getInstance() {
		return INSTANCE;
	}

	private static final int BUFFER_SIZE = 4096;

	public File getCurrentPathFile() {
		File ret = null;
		try {
			URI jarURI = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
			File fPK = new File(jarURI.getSchemeSpecificPart());
			if (jarURI.toString().endsWith(".jar")) {
				File fJar = new File(jarURI.getSchemeSpecificPart());
				fPK = new File(fJar.getAbsolutePath());
			}
			ret = fPK;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public File getCurrentPathNormalizedFile() {
		File ret = null;
		
		File f=getCurrentPathFile();
		String p=f.getParentFile().getAbsolutePath();
		if (f.getAbsolutePath().endsWith(".jar")) {
			ret=new File(p);
		}else {
			ret=f;
		}
		
		return ret;
	}
	
	public String getCurrentPathNormalizedPath() {
		return getCurrentPathNormalizedFile().getAbsolutePath();
	}

	public void copyDir(File src, File dst) {
		try {
			FileUtils.copyDirectory(src, dst);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void unzip(String zipFilePath, String destDirectory) {
		System.out.println(
				"Extracting... '" + (new File(zipFilePath).getAbsolutePath()) + "' to '" + destDirectory + "'");
		try {
			File destDir = new File(destDirectory);
			if (!destDir.exists()) {
				destDir.mkdir();
			}
			ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
			ZipEntry entry = zipIn.getNextEntry();
			while (entry != null) {
				String filePath = destDirectory + File.separator + entry.getName();
				if (!entry.isDirectory()) {
					extractFile(zipIn, filePath);
				} else {
					File dir = new File(filePath);
					dir.mkdir();
				}
				zipIn.closeEntry();
				entry = zipIn.getNextEntry();
			}
			zipIn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
		byte[] bytesIn = new byte[BUFFER_SIZE];
		int read = 0;
		while ((read = zipIn.read(bytesIn)) != -1) {
			bos.write(bytesIn, 0, read);
		}
		bos.close();
	}

	public void extractFile(String src, String dst) {
		System.out.println("Extracting... '" + (new File(src).getAbsolutePath()) + "' to '" + dst + "'");
		String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		try {
			if (jarPath.endsWith(".jar")) {
				String jarFile = getCurrentPathFile().toString();
				if (dst == null || dst.equals(""))
					dst = (new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()))
							.getParentFile().getAbsolutePath();
				extractJar(jarFile, src, dst);
			} else {
				if (dst == null || dst.equals(""))
					dst = (new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()))
							.getAbsolutePath();
				File fPK = new File(new URI("file://" + jarPath + "/" + src));
				File fDst = new File(dst);

				if (fPK.isDirectory()) {
					FileUtils.copyDirectory(fPK, fDst);
				} else {
					FileUtils.copyFile(fPK, fDst);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void extractJar(String jarFile, String src, String dst) {
		try {
			JarFile jar = new java.util.jar.JarFile(jarFile);
			Enumeration<JarEntry> enumEntries = jar.entries();
			while (enumEntries.hasMoreElements()) {
				JarEntry file = (JarEntry) enumEntries.nextElement();
				if (file.getName().startsWith(src)) {
					String fname = file.getName().replaceAll("^" + src, "");
					File f = new File(dst + File.separator + fname);
					if (file.isDirectory()) {
						f.mkdirs();
						continue;
					}
					InputStream in = jar.getInputStream(file);
					FileOutputStream out = new java.io.FileOutputStream(f);
					while (in.available() > 0) {
						out.write(in.read());
					}
					out.close();
					in.close();
				}
			}
			jar.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sedFile(String fPath, String target, String replacement) {
		if (!File.separator.equals("\\")) {
			fPath = fPath.replaceAll("\\\\", File.separator);
		}

		File file = new File(fPath);
		String contents = "";
		try {
			contents = FileUtils.readFileToString(file, StandardCharsets.UTF_8.name());
			contents = Pattern.compile(target).matcher(contents).replaceAll(replacement);
			FileUtils.write(file, contents, StandardCharsets.UTF_8.name());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void renameDir(String src, String dst) {
		if (!File.separator.equals("\\")) {
			src = src.replaceAll("\\\\", File.separator);
			dst = dst.replaceAll("\\\\", File.separator);
		}

		try {
			FileUtils.moveDirectory(new File(src), new File(dst));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void mkdir(String dst) {
		System.out.println("Creating..." + dst);
		if (!File.separator.equals("\\")) {
			dst = dst.replaceAll("\\\\", File.separator);
		}

		try {
			FileUtils.forceMkdir(new File(dst));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void rmdir(String dst) {
		System.out.println("Removing..." + dst);
		if (!File.separator.equals("\\")) {
			dst = dst.replaceAll("\\\\", File.separator);
		}

		try {
			FileUtils.deleteDirectory(new File(dst));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public String readFile(String fPath) {
		String ret = "";

		File file = new File(fPath);
		try {
			ret = FileUtils.readFileToString(file, StandardCharsets.UTF_8.name());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return ret;
	}

	public void writeFile(String fPath, String content) {
		File file = new File(fPath);
		try {
			FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8.name());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
