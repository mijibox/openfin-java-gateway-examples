package com.mijibox.openfin.gateway.examples;

import java.util.Date;
import java.util.concurrent.locks.LockSupport;

import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mijibox.openfin.gateway.OpenFinGateway;
import com.mijibox.openfin.gateway.OpenFinGateway.OpenFinGatewayListener;
import com.mijibox.openfin.gateway.OpenFinInterApplicationBus;
import com.mijibox.openfin.gateway.OpenFinLauncher;

public class HelloOpenFinIAB implements OpenFinGatewayListener {
	private final static Logger logger = LoggerFactory.getLogger(HelloOpenFinIAB.class);

	private Thread callingThread;
	private OpenFinInterApplicationBus iab;
	private OpenFinGateway gateway;
	private JsonObject helloOpenFinIdentity;

	HelloOpenFinIAB() {
		this.callingThread = Thread.currentThread();
		this.helloOpenFinIdentity = Json.createObjectBuilder()
				.add("uuid", "OpenFinHelloWorld")
				.add("name", "OpenFinHelloWorld").build();
		// intentionally using different version of openfin runtime.
		OpenFinLauncher.newOpenFinLauncherBuilder()
				.runtimeVersion("10.66.41.18")
				.addRuntimeOption("--v=1")
				.open(this);
	}

	void sendMessages() {
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					for (int i = 0; i < 10; i++) {
						JsonObject msg = Json.createObjectBuilder()
								.add("message", new Date().toString() + ": message #" + i).build();
						iab.send(helloOpenFinIdentity, "hello:of:notification", msg);

						try {
							Thread.sleep(9000);
						}
						catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				catch (Exception e) {
					logger.error("something wrong", e);
				}
				finally {
					gateway.close();
				}
			}
		};
		t.start();
	}

	@Override
	public void onOpen(OpenFinGateway gateway) {
		this.gateway = gateway;
		this.iab = gateway.getOpenFinInterApplicationBus();
		this.iab.subscribe(this.helloOpenFinIdentity, "hello:of:sub", (src, msg) -> {
			logger.info("received iab message from {}, msg: {}", src, msg);
			iab.send(helloOpenFinIdentity, "hello:of:notification", msg);
		});
		
		this.iab.subscribe(null, "AAA", (src, msg) -> {
			logger.info("received iab message from {}, msg: {}", src, msg);
			iab.send(helloOpenFinIdentity, "hello:of:notification", msg);
		});
		
		this.sendMessages();
	}

	@Override
	public void onError() {
	}

	@Override
	public void onClose() {
		LockSupport.unpark(this.callingThread);
	}

	public static void main(String[] args) {
		new HelloOpenFinIAB();
		LockSupport.park();
	}
}
