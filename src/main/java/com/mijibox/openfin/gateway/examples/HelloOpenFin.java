package com.mijibox.openfin.gateway.examples;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.json.Json;
import javax.json.JsonObject;

import com.mijibox.openfin.gateway.OpenFinGateway;
import com.mijibox.openfin.gateway.OpenFinGateway.OpenFinGatewayListener;
import com.mijibox.openfin.gateway.OpenFinLauncher;
import com.mijibox.openfin.gateway.ProxyObject;

public class HelloOpenFin {
	CompletableFuture<?> hello() {
		CompletableFuture<?> gatewayClosedFuture = new CompletableFuture<>();

		OpenFinGatewayListener gatewayListener = new OpenFinGateway.OpenFinGatewayListener() {

			@Override
			public void onOpen(OpenFinGateway gateway) {
			}

			@Override
			public void onError() {
			}

			@Override
			public void onClose() {
				gatewayClosedFuture.complete(null);
			}
		};

		OpenFinLauncher.newOpenFinLauncherBuilder()
				.addRuntimeOption("--no-sandbox")
				.open(gatewayListener)
				.thenAccept(gateway -> {
					gateway.invoke("fin.System.getVersion").thenAccept(r -> {
						System.out.println("openfin version: {}" + r.getResult());
					});

					gateway.invoke(true, "fin.Application.startFromManifest",
							Json.createValue("https://cdn.openfin.co/demos/hello/app.json")).thenAccept(r -> {
								JsonObject appObj = (JsonObject) r.getResult();
								System.out.println("appUuid: {}" + appObj.getJsonObject("identity").getString("uuid"));
								ProxyObject proxyAppObj = r.getProxyObject();
								proxyAppObj.addListener("on", "closed", (e) -> {
									System.out.println("hello openfin closed, listener got event: " + e);
									gateway.close();
								});
							}).exceptionally(e -> {
								System.err.println("error starting hello openfin app");
								e.printStackTrace();
								gateway.close();
								return null;
							});
				});
		
		return gatewayClosedFuture;
	}

	public static void main(String[] args) {
		try {
			new HelloOpenFin().hello().get();
		}
		catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
}
