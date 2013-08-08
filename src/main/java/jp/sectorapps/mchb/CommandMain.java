/*
 * MinecraftHeatBeat
 * 
 * http://www.sectorapps.jp/mcheatbeat/index/html
 * Copyright (c) 2013 sectorapps.jp All Rights reserved.
 * 
 * Args4j included: MIT License
 * (c) 2003-2013 Kohsuke Kawaguchi
 */
package jp.sectorapps.mchb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class CommandMain {
	
	private static final String SUCCESS = "0";
	private static final String SERVER_NOT_RESPONDING = "1";
	private static final String SERVER_REFUSED = "2";
	private static final String PROGRAM_TIMEOUT = "3";

	public static void main(String[] args) {
		
		CommandArguments comArgs = new CommandArguments();
		CmdLineParser parser = new CmdLineParser(comArgs);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.out.println("mchb: " + e.getMessage());
			System.out.println("Try `mchb -h' for more infomation.");
			System.exit(1);
		}
		
		if (comArgs.versionFlag) {
			System.out.println("MCHeartBeat version \"1.0.5\"");
			return;
		}
		
		if (comArgs.helpFlag) {
			System.out.println("Usage: mchb [option]... host");
			System.out.println("           (to execute heartbeat)");
			System.out.println("where options include:");
			parser.printUsage(System.out);
			System.out.println("See http://www.sectorapps.jp/mcheartbeat/index.html for more details.");
			return;
		}
		
		if (null == comArgs.host) {
			System.out.println("mchb: Argument \"host\" is required");
			System.out.println("Try `mchb -h' for more infomation.");
			System.exit(1);
		}
		
		Socket socket = null;
		String result = null;
		try {
			socket = new Socket(comArgs.host, comArgs.port);
			socket.setSoTimeout(comArgs.timeout);
			OutputStream out = socket.getOutputStream();
			InputStream in = socket.getInputStream();
			out.write(0xfe);
			out.write(0x01);
			out.flush();
			
			in.skip(2L);
			int length = in.read();
			char[] buffer = new char[length];
			
			for (int i = 0; i < length; i++ ) {
				int buf1 = in.read();
				int buf2 = in.read();
				buffer[i] = (char)((buf1 << 16) + buf2);
			}
				
			StringBuilder[] resultArgs = new StringBuilder[] { new StringBuilder(), new StringBuilder(), new StringBuilder(), new StringBuilder(), new StringBuilder()};
			int argI = 0;
			boolean escapeFlag = false;
			for (int i = 3; i < length ; i++)
				if (0 != buffer[i]) 
					if (0xA7 == buffer[i])
						escapeFlag = true;
					else if (true == escapeFlag)
						escapeFlag = false;
					else
						resultArgs[argI].append(buffer[i]);
				else
					argI++;
			
			result = SUCCESS + (comArgs.detailFlag ? "," + resultArgs[1] + ',' + resultArgs[2] + ',' + resultArgs[3] + ',' + resultArgs[4] : "");
			
			out.close();
			in.close();
			
		} catch (UnknownHostException e) {
			System.out.println("Error: unknown host name \"" + e.getMessage() + "\"");
			System.exit(1);
		} catch (IOException e) {
			if ("Connection timed out: connect".equals(e.getMessage()) || "Connection timed out".equals(e.getMessage()) || "No route to host".equals(e.getMessage())) {
				result = SERVER_NOT_RESPONDING;
			} else if ("Connection refused: connect".equals(e.getMessage())) {
				result = SERVER_REFUSED;
			} else if ("Read timed out".equals(e.getMessage())) {
				result = PROGRAM_TIMEOUT;
			}else {
				System.out.println("Error: " + e.getMessage());
				System.exit(1);
			}
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				System.out.println("Error: " + e.getMessage());
				System.exit(1);
			}
		}
		
		System.out.println(result);
	}
	
	static class CommandArguments {	
		@Option(name="-v", aliases="--version", usage="print version and exit")
		private boolean versionFlag = false;
		
		@Option(name="-h", aliases="--help", usage="print this help message")
		private boolean helpFlag = false;
		
		@Option(name="-p", aliases="--port", usage="execute the specified port")
		private int port = 25565;
		
		@Option(name="-d", aliases="--detail", usage="enable verbose output")
		private boolean detailFlag = false;
		
		@Option(name="-t", aliases="--timeout", usage="waits for response by specified timeout\n(milliseconds)")
		private int timeout = 8000;
		
		@Argument(index=0, metaVar="host")
		private String host;
	}
}
