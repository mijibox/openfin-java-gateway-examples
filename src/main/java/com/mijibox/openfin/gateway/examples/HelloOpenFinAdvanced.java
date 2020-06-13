package com.mijibox.openfin.gateway.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import javax.json.Json;
import javax.json.JsonObject;

import com.mijibox.openfin.gateway.OpenFinEventListener;
import com.mijibox.openfin.gateway.OpenFinGateway;
import com.mijibox.openfin.gateway.OpenFinGateway.OpenFinGatewayListener;
import com.mijibox.openfin.gateway.OpenFinGatewayLauncher;
import com.mijibox.openfin.gateway.OpenFinIabMessageListener;
import com.mijibox.openfin.gateway.OpenFinLauncher;
import com.mijibox.openfin.gateway.ProxyListener;
import com.mijibox.openfin.gateway.ProxyObject;

public class HelloOpenFinAdvanced {

	public static class OfObject {
		protected ProxyObject pxyObj;
		protected OpenFinGateway gateway;

		OfObject(ProxyObject obj, OpenFinGateway gateway) {
			this.pxyObj = obj;
			this.gateway = gateway;
		}

		protected static <T> T runSync(CompletionStage<T> future) {
			try {
				return future.toCompletableFuture().exceptionally(e -> {
					e.printStackTrace();
					throw new RuntimeException(e);
				}).get();
			}
			catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}

		public CompletionStage<ProxyListener> addListenerAsync(String event, OpenFinEventListener listener) {
			return this.pxyObj.addListener(true, "on", event, listener);
		}
		
		public ProxyListener addListener(String event, OpenFinEventListener listener) {
			return runSync(this.addListenerAsync(event, listener));
		}

		public CompletionStage<Void> removeListenerAsync(String event, ProxyListener listener) {
			return this.pxyObj.removeListener("removeListener", event, listener);
		}
		
		public void removeListener(String event, ProxyListener listener) {
			runSync(this.removeListenerAsync(event, listener));
		}

		public CompletionStage<Void> disposeAsync() {
			return this.pxyObj.dispose();
		}
		
		public void dispose() {
			runSync(this.disposeAsync());
		}
	}

	public static class OfApplication extends OfObject {
		private JsonObject identity;
		private OfWindow window;

		OfApplication(JsonObject identity, ProxyObject obj, OpenFinGateway gateway) {
			super(obj, gateway);
			this.identity = identity;
		}

		public static CompletionStage<OfApplication> startFromManifestAsync(String manifest, OpenFinGateway gateway) {
			return gateway.invoke(true, "fin.Application.startFromManifest", Json.createValue(manifest))
					.thenApply(r -> {
						JsonObject appId = r.getResultAsJsonObject().getJsonObject("identity");
						return new OfApplication(appId, r.getProxyObject(), gateway);
					});
		}

		public static OfApplication startFromManifest(String manifest, OpenFinGateway gateway) {
			return runSync(startFromManifestAsync(manifest, gateway));
		}
		
		public JsonObject getIdentity() {
			return this.identity;
		}

		public CompletionStage<OfWindow> getWindowAsync() {
			return this.pxyObj.invoke(true, "getWindow").thenApply(r -> {
				JsonObject winId = r.getResultAsJsonObject().getJsonObject("identity");
				return new OfWindow(winId, r.getProxyObject(), gateway);
			});
		}
		
		public OfWindow getWindow() {
			if (this.window == null) {
				this.window = runSync(this.getWindowAsync());
			}
			return this.window;
		}

		public CompletionStage<JsonObject> getInfoAsync() {
			return this.pxyObj.invoke("getInfo").thenApply(r -> {
				return r.getResultAsJsonObject();
			});
		}
		
		public JsonObject getInfo() {
			return runSync(this.getInfoAsync());
		}
		
		public CompletionStage<List<OfWindow>> getChildWindowsAsync() {
			return this.pxyObj.invoke("getChildWindows").thenApply(r->{
				return r.getResultAsJsonArray();
			}).thenApply(wins ->{
				ArrayList<OfWindow> windows = new ArrayList<>();
				wins.forEach(w ->{
					JsonObject winJson = (JsonObject)w;
					windows.add(OfWindow.wrap(winJson.getJsonObject("identity"), gateway));
				});
				return windows;
			});
		}
		
		public List<OfWindow> getChildWindows() {
			return runSync(this.getChildWindowsAsync());
		}

		public CompletionStage<Void> disposeAsync() {
			if (this.window != null) {
				return this.window.disposeAsync().thenCompose(v->{
					return super.disposeAsync();
				});
			}
			else {
				return super.disposeAsync();
			}
		}
		
		public void dispose() {
			runSync(this.disposeAsync());
		}
	}

	public static class OfWindow extends OfObject {
		private JsonObject identity;

		OfWindow(JsonObject identity, ProxyObject obj, OpenFinGateway gateway) {
			super(obj, gateway);
			this.identity = identity;
		}
		
		public static CompletionStage<OfWindow> wrapAsync(JsonObject identity, OpenFinGateway gateway) {
			return gateway.invoke(true, "fin.Window.wrap", identity).thenApply(r-> {
				return new OfWindow(identity, r.getProxyObject(), gateway);
			});
		}
		
		public static OfWindow wrap(JsonObject identity, OpenFinGateway gateway) {
			return runSync(wrapAsync(identity, gateway));
		}

		public CompletionStage<Void> flashAsync() {
			return this.pxyObj.invoke("flash").thenAccept(r->{
				
			});
		}
		
		public void flash() {
			runSync(this.flashAsync());
		}
		
		@Override
		public String toString() {
			return "OfWindow: " + this.identity;
		}
	}

	void demo() {
		Thread thread = Thread.currentThread();
		OpenFinGatewayListener listener = new OpenFinGateway.OpenFinGatewayListener() {
			@Override
			public void onClose() {
				LockSupport.unpark(thread);
			}
		};

		OpenFinGatewayLauncher.newOpenFinGatewayLauncher()
				.launcherBuilder(OpenFinLauncher.newOpenFinLauncherBuilder()
						.runtimeVersion("beta")
						.addRuntimeOption("--v=1"))
				.gatewayListener(listener)
				.open()
				.thenAccept(gateway -> {
					OfApplication helloApp = OfApplication
							.startFromManifest("https://cdn.openfin.co/demos/hello/app.json", gateway);
					
					System.out.println("AppInfo: " + helloApp.getInfo());
					
					OfWindow helloWindow = helloApp.getWindow();
					AtomicBoolean flashWindow = new AtomicBoolean(true);
					// if main window is closed, then close the gateway
					helloWindow.addListener("closed", e -> {
						flashWindow.set(false);
						helloApp.dispose();
						gateway.close();
						return null;
					});

					// flash window every 10 seconds
					Thread t = new Thread() {
						@Override
						public void run() {
							while (flashWindow.get()) {
								try {
									Thread.sleep(10000);
								}
								catch (InterruptedException e) {
									e.printStackTrace();
								}
								helloWindow.flash();
							}
						}
					};
					t.setDaemon(true);
					t.start();

					OpenFinIabMessageListener iabListener = (src, msg) -> {
						System.out.println("received message from topic AAA, msg: " + msg);
					};
					// if iab window is open, subscribe to "AAA"
					ProxyListener appWindowShownListener = helloApp.addListener("window-shown", e -> {
						JsonObject evt = (JsonObject) e.get(0);
						System.out.println("window shown, evt: " + evt);

						System.out.println("helloApp.getChildWindows: " + helloApp.getChildWindows());
						
						
						if ("interAppWindow".equals(evt.getString("name"))) {
							System.out.println("interAppWindow shown, subscribe to IAB topic AAA");
							gateway.getOpenFinInterApplicationBus().subscribe(null, "AAA", iabListener);
						}
						return null;
					});

					// if iab window is hidden, remove iab listener, also remove the window-shown
					// listener
					helloApp.addListener("window-hidden", e -> {
						JsonObject evt = (JsonObject) e.get(0);
						if ("interAppWindow".equals(evt.getString("name"))) {
							System.out.println("interAppWindow hidden, unsubscribe to IAB topic AAA");
							gateway.getOpenFinInterApplicationBus().unsubscribe(null, "AAA", iabListener);
							System.out.println(
									"no longer care about window-shown events, remove appWindowShownListener");
							helloApp.removeListener("window-shown", appWindowShownListener);
						}
						return null;
					});
				});

		LockSupport.park();
	}

	public static void main(String[] args) {
		new HelloOpenFinAdvanced().demo();
	}
}
